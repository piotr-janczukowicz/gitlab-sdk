package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.TimeStats;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.DiffVersion;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.DraftNote;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestNote;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestState;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Merging and approving are not run live: the sandbox settings may forbid them, and a merge cannot be undone. A draft
 * note is created, edited, listed and deleted but never published, because publishing would leave a real note; the
 * throwaway merge request is deleted at the end, which removes a draft note left by a failure.
 */
@Tag("smoke")
class MergeRequestsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void mergeRequest_shouldRoundTrip_whenCreatedCommentedUpdatedClosedAndReopened() throws IOException {
        final Project project = ENV.sandbox();
        final String target = project.fetch().orElseThrow().response().defaultBranch();
        final String source = ENV.name("mr");
        project.createBranch(source, target);
        SmokeCleanup.run(
                () -> {
                    project.createFile(ENV.name("mr") + ".txt", source, "smoke", "smoke: change for a merge request");
                    final MergeRequest merge =
                            project.createMergeRequest(GitLabProjectMergeRequestsCreatePayloadBuilder.builder()
                                    .title(ENV.name("merge-request"))
                                    .sourceBranch(source)
                                    .targetBranch(target)
                                    .build());
                    SmokeCleanup.run(
                            () -> {
                                final MergeRequestNote note = merge.comment("smoke comment");
                                final MergeRequestNote edited = note.update("smoke comment, edited");
                                final List<MergeRequestNote> notes =
                                        merge.notes().limit(20).toList();
                                final MergeRequest updated = merge.update()
                                        .title(ENV.name("merge-request-updated"))
                                        .description("changed by a smoke test")
                                        .send();
                                final MergeRequest closed = updated.close();
                                final MergeRequest reopened = closed.reopen();
                                final Optional<MergeRequest> reloaded = project.mergeRequest(merge.iid());
                                final long commits = reopened.commits().limit(5).count();
                                final DraftNote draft = reopened.createDraftNote("smoke draft");
                                final DraftNote draftEdited = draft.update()
                                        .note("smoke draft, edited")
                                        .send();
                                final List<DraftNote> drafts =
                                        reopened.draftNotes().limit(20).toList();
                                draftEdited.delete();

                                assertSoftly(softly -> {
                                    softly.assertThat(merge.sourceBranch()).contains(source);
                                    softly.assertThat(merge.targetBranch()).contains(target);
                                    softly.assertThat(notes)
                                            .extracting(MergeRequestNote::id)
                                            .contains(note.id());
                                    softly.assertThat(edited.body()).contains("smoke comment, edited");
                                    softly.assertThat(updated.title()).isEqualTo(ENV.name("merge-request-updated"));
                                    softly.assertThat(closed.state()).isEqualTo(MergeRequestState.CLOSED);
                                    softly.assertThat(reopened.state()).isEqualTo(MergeRequestState.OPENED);
                                    softly.assertThat(reloaded)
                                            .map(MergeRequest::iid)
                                            .contains(merge.iid());
                                    softly.assertThat(commits).isGreaterThanOrEqualTo(1);
                                    softly.assertThat(draftEdited.note()).contains("smoke draft, edited");
                                    softly.assertThat(drafts)
                                            .extracting(DraftNote::id)
                                            .contains(draft.id());
                                });
                            },
                            merge::delete);
                },
                () -> {
                    final Optional<Branch> branch = project.branch(source);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void mergeRequest_shouldReadDiffsTrackTimeAndRebase_whenItHasAChange() throws IOException {
        final Project project = ENV.sandbox();
        final String target = project.fetch().orElseThrow().response().defaultBranch();
        final String source = ENV.name("mr-parity");
        project.createBranch(source, target);
        SmokeCleanup.run(
                () -> {
                    project.createFile(
                            ENV.name("mr-parity") + ".txt", source, "smoke", "smoke: change for a merge request");
                    final MergeRequest merge =
                            project.createMergeRequest(GitLabProjectMergeRequestsCreatePayloadBuilder.builder()
                                    .title(ENV.name("merge-request-parity"))
                                    .sourceBranch(source)
                                    .targetBranch(target)
                                    .build());
                    SmokeCleanup.run(
                            () -> {
                                final List<GitLabDiff> diffs = SmokeSkips.probe(
                                        "merge request diffs",
                                        () -> merge.diffs().limit(20).toList());
                                final List<DiffVersion> versions = SmokeSkips.probe(
                                        "merge request versions",
                                        () -> merge.versions().limit(20).toList());
                                final List<GitLabUserBasic> participants =
                                        merge.participants().limit(20).toList();
                                final TimeStats estimated = merge.estimateTime(Duration.ofHours(1));
                                final TimeStats spent = merge.addSpentTime(Duration.ofMinutes(30));
                                final Optional<TimeStats> loaded = merge.timeStats();
                                final TimeStats spentReset = merge.resetSpentTime();
                                final TimeStats estimateReset = merge.resetEstimate();
                                final Optional<MergeRequest> rebased = SmokeSkips.probe("rebase", () -> {
                                    merge.rebase();
                                    return Optional.of(merge.refresh());
                                });
                                final List<MergeRequest> listed =
                                        ENV.sdk().mergeRequests().limit(5).toList();

                                assertSoftly(softly -> {
                                    softly.assertThat(diffs).isNotEmpty();
                                    softly.assertThat(versions).isNotEmpty();
                                    softly.assertThat(participants).isNotNull();
                                    softly.assertThat(estimated.estimate()).isEqualTo(Duration.ofHours(1));
                                    softly.assertThat(spent.totalSpent()).isEqualTo(Duration.ofMinutes(30));
                                    softly.assertThat(loaded).isPresent();
                                    softly.assertThat(spentReset.totalSpent()).isEqualTo(Duration.ZERO);
                                    softly.assertThat(estimateReset.estimate()).isEqualTo(Duration.ZERO);
                                    softly.assertThat(rebased).isPresent();
                                    softly.assertThat(listed).isNotNull();
                                });
                            },
                            merge::delete);
                },
                () -> {
                    final Optional<Branch> branch = project.branch(source);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void mergeRequest_shouldReturnEmpty_whenIidDoesNotExist() throws IOException {
        final Optional<MergeRequest> missing = ENV.sandbox().mergeRequest(Integer.MAX_VALUE);

        assertThat(missing).isEmpty();
    }
}
