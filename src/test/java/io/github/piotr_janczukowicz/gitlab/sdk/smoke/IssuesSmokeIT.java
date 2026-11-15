package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.IssueLink;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.IssueLinkType;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.IssueState;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Note;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.TimeStats;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class IssuesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void issue_shouldRoundTrip_whenCreatedCommentedUpdatedClosedAndReopened() throws IOException {
        final Project project = ENV.sandbox();
        final Issue created = project.createIssue(ENV.name("issue"));
        SmokeCleanup.run(
                () -> {
                    final Note note = created.comment("smoke comment");
                    final Note edited = note.update("smoke comment, edited");
                    final List<Note> notes = created.notes().limit(20).toList();
                    final Issue updated = created.update()
                            .title(ENV.name("issue-updated"))
                            .description("changed by a smoke test")
                            .send();
                    final Issue closed = updated.close();
                    final Issue reopened = closed.reopen();
                    final Optional<Issue> reloaded = project.issue(created.iid());
                    edited.delete();

                    assertSoftly(softly -> {
                        softly.assertThat(notes).extracting(Note::id).contains(note.id());
                        softly.assertThat(edited.body()).contains("smoke comment, edited");
                        softly.assertThat(updated.title()).isEqualTo(ENV.name("issue-updated"));
                        softly.assertThat(updated.description()).contains("changed by a smoke test");
                        softly.assertThat(closed.state()).isEqualTo(IssueState.CLOSED);
                        softly.assertThat(reopened.state()).isEqualTo(IssueState.OPENED);
                        softly.assertThat(reloaded).map(Issue::iid).contains(created.iid());
                        softly.assertThat(created.title()).isEqualTo(ENV.name("issue"));
                    });
                },
                created::delete);
    }

    @Test
    void label_shouldRoundTrip_whenCreatedUpdatedAndAppliedToAnIssue() throws IOException {
        final Project project = ENV.sandbox();
        final Label label = project.createLabel(ENV.name("label"), "#428bca");
        SmokeCleanup.run(
                () -> {
                    final Issue created = project.createIssue(ENV.name("labelled"));
                    SmokeCleanup.run(
                            () -> {
                                final Label described =
                                        label.update().description("smoke").send();
                                final Issue issue = created.update()
                                        .labels(List.of(label.name()))
                                        .send();
                                final boolean listed = project.labels()
                                        .limit(100)
                                        .anyMatch(each -> each.name().equals(label.name()));

                                assertSoftly(softly -> {
                                    softly.assertThat(described.description()).contains("smoke");
                                    softly.assertThat(listed).isTrue();
                                });
                                assertThat(issue.labels()).contains(label.name());
                            },
                            created::delete);
                },
                label::delete);
    }

    @Test
    void issue_shouldTrackTimeAndListParticipants_whenEstimatedAndBooked() throws IOException {
        final Issue created = ENV.sandbox().createIssue(ENV.name("time"));
        SmokeCleanup.run(
                () -> {
                    final List<GitLabUserBasic> participants = SmokeSkips.probe(
                            "issue participants",
                            () -> created.participants().limit(20).toList());
                    final TimeStats estimated = created.estimateTime(Duration.ofHours(2));
                    final TimeStats booked = created.addSpentTime(Duration.ofMinutes(30));
                    final Optional<TimeStats> loaded = created.timeStats();
                    final TimeStats spentReset = created.resetSpentTime();
                    final TimeStats estimateReset = created.resetEstimate();

                    assertSoftly(softly -> {
                        softly.assertThat(participants).isNotNull();
                        softly.assertThat(estimated.estimate()).isEqualTo(Duration.ofHours(2));
                        softly.assertThat(booked.totalSpent()).isEqualTo(Duration.ofMinutes(30));
                        softly.assertThat(loaded).map(TimeStats::totalSpent).contains(Duration.ofMinutes(30));
                        softly.assertThat(spentReset.totalSpent()).isEqualTo(Duration.ZERO);
                        softly.assertThat(estimateReset.estimate()).isEqualTo(Duration.ZERO);
                    });
                },
                created::delete);
    }

    @Test
    void issue_shouldLinkTwoIssues_whenLinkedAndUnlinked() throws IOException {
        final Project project = ENV.sandbox();
        final Issue source = project.createIssue(ENV.name("link-source"));
        SmokeCleanup.run(
                () -> {
                    final Issue target = project.createIssue(ENV.name("link-target"));
                    SmokeCleanup.run(
                            () -> {
                                final List<IssueLink> before = SmokeSkips.probe(
                                        "issue links",
                                        () -> source.links().limit(20).toList());
                                final IssueLink link = source.linkTo(target, IssueLinkType.RELATES_TO);
                                final Optional<IssueLink> loaded = source.link(link.id());
                                final List<IssueLink> listed =
                                        source.links().limit(20).toList();
                                link.delete();
                                final Optional<IssueLink> gone = source.link(link.id());

                                assertSoftly(softly -> {
                                    softly.assertThat(before).isEmpty();
                                    softly.assertThat(link.type()).isEqualTo(IssueLinkType.RELATES_TO);
                                    softly.assertThat(loaded).map(IssueLink::id).contains(link.id());
                                    softly.assertThat(listed)
                                            .extracting(IssueLink::id)
                                            .contains(link.id());
                                    softly.assertThat(gone).isEmpty();
                                });
                            },
                            target::delete);
                },
                source::delete);
    }

    @Test
    void issue_shouldReturnEmpty_whenIidDoesNotExist() throws IOException {
        final Optional<Issue> missing = ENV.sandbox().issue(Integer.MAX_VALUE);

        assertThat(missing).isEmpty();
    }
}
