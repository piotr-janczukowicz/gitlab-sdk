package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.issue;
import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.issueBasic;
import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.mergeRequestBasic;
import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.milestone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestoneBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MilestoneTest {

    private static final String ID = "my-group/my-project";
    private static final long MILESTONE_ID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabMilestone response = milestone(MILESTONE_ID, "v1");
    private final Milestone sut = new Milestone(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenMilestoneIsLoaded() {
        final GitLabMilestone full = GitLabMilestoneBuilder.builder()
                .id(MILESTONE_ID)
                .iid(3L)
                .title("v1")
                .description("Goals")
                .state("closed")
                .dueDate("2026-12-31")
                .startDate("2026-01-01")
                .expired(true)
                .build();

        final Milestone result = new Milestone(project, full);

        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(MILESTONE_ID);
            softly.assertThat(result.iid()).isEqualTo(3L);
            softly.assertThat(result.title()).isEqualTo("v1");
            softly.assertThat(result.description()).contains("Goals");
            softly.assertThat(result.state()).isEqualTo(MilestoneState.CLOSED);
            softly.assertThat(result.stateName()).isEqualTo("closed");
            softly.assertThat(result.dueDate()).contains(LocalDate.of(2026, 12, 31));
            softly.assertThat(result.startDate()).contains(LocalDate.of(2026, 1, 1));
            softly.assertThat(result.isExpired()).isTrue();
            softly.assertThat(result.project()).isSameAs(project);
            softly.assertThat(result.response()).isSameAs(full);
        });
    }

    @Test
    void description_shouldBeEmpty_whenAbsent() {
        assertThat(sut.description()).isEmpty();
    }

    @Test
    void isExpired_shouldBeFalse_whenFlagIsAbsent() {
        assertThat(sut.isExpired()).isFalse();
    }

    @Test
    void dueDate_shouldBeEmpty_whenBlankOrAbsent() {
        final Milestone blank = new Milestone(
                project,
                GitLabMilestoneBuilder.builder(response)
                        .dueDate("  ")
                        .startDate("")
                        .build());

        assertSoftly(softly -> {
            softly.assertThat(sut.dueDate()).isEmpty();
            softly.assertThat(sut.startDate()).isEmpty();
            softly.assertThat(blank.dueDate()).isEmpty();
            softly.assertThat(blank.startDate()).isEmpty();
        });
    }

    @Test
    void dueDate_shouldThrowIllegalStateException_whenMalformed() {
        final Milestone broken = new Milestone(
                project,
                GitLabMilestoneBuilder.builder(response)
                        .dueDate("31.12.2026")
                        .startDate("soon")
                        .build());

        assertSoftly(softly -> {
            softly.assertThatThrownBy(broken::dueDate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("31.12.2026");
            softly.assertThatThrownBy(broken::startDate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("soon");
        });
    }

    @Test
    void state_shouldThrowIllegalArgumentException_whenUnknown() {
        final Milestone odd = new Milestone(
                project,
                GitLabMilestoneBuilder.builder(response).state("paused").build());

        assertSoftly(softly -> {
            softly.assertThatThrownBy(odd::state)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paused");
            softly.assertThat(odd.stateName()).isEqualTo("paused");
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new Milestone(null, response)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> new Milestone(project, null)).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void refresh_shouldReturnReloadedMilestone_whenGitLabHasIt() throws IOException {
        final GitLabMilestone reloaded = milestone(MILESTONE_ID, "v1 renamed");
        when(gitLab.projectmilestones().get(ID, MILESTONE_ID)).thenReturn(Optional.of(reloaded));

        final Milestone result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projectmilestones().get(ID, MILESTONE_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Milestone 7 is gone");
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.projectmilestones()).delete(ID, MILESTONE_ID);
    }

    @Test
    void promote_shouldCallCore_whenCalled() throws IOException {
        sut.promote();

        verify(gitLab.projectmilestones()).promote(ID, MILESTONE_ID);
    }

    private GitLabProjectProjectMilestonesUpdatePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectMilestonesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectMilestonesUpdatePayload.class);
        verify(gitLab.projectmilestones()).update(eq(ID), eq(MILESTONE_ID), payload.capture());
        return payload.getValue();
    }

    @Test
    void close_shouldSendCloseStateEvent_whenCalled() throws IOException {
        final GitLabMilestone closed =
                GitLabMilestoneBuilder.builder(response).state("closed").build();
        when(gitLab.projectmilestones()
                        .update(eq(ID), eq(MILESTONE_ID), any(GitLabProjectProjectMilestonesUpdatePayload.class)))
                .thenReturn(closed);

        final Milestone result = sut.close();

        final GitLabProjectProjectMilestonesUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.stateEvent()).isEqualTo(GitLabProjectProjectMilestonesUpdateStateEvent.CLOSE);
            softly.assertThat(payload.title()).isNull();
            softly.assertThat(result.response()).isSameAs(closed);
            softly.assertThat(sut.state()).isEqualTo(MilestoneState.ACTIVE);
        });
        verify(gitLab.projectmilestones(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void reopen_shouldSendActivateStateEvent_whenCalled() throws IOException {
        final GitLabMilestone reopened = milestone(MILESTONE_ID, "v1");
        when(gitLab.projectmilestones()
                        .update(eq(ID), eq(MILESTONE_ID), any(GitLabProjectProjectMilestonesUpdatePayload.class)))
                .thenReturn(reopened);

        final Milestone result = sut.reopen();

        final var payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.stateEvent()).isEqualTo(GitLabProjectProjectMilestonesUpdateStateEvent.ACTIVATE);
            softly.assertThat(result.response()).isSameAs(reopened);
        });
    }

    @Test
    void issues_shouldLoadEachIssue_whenGitLabListsBasicRecords() throws IOException {
        when(gitLab.projectmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, null), issueBasic(2, null)));
        when(gitLab.issues().project().get(ID, 1L)).thenReturn(Optional.of(issue(1)));
        when(gitLab.issues().project().get(ID, 2L)).thenReturn(Optional.of(issue(2)));

        final List<Issue> result = sut.issues().toList();

        assertThat(result).extracting(Issue::iid).containsExactly(1L, 2L);
    }

    @Test
    void issues_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.projectmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, null), issueBasic(2, null), issueBasic(3, null)));
        when(gitLab.issues().project().get(ID, 1L)).thenReturn(Optional.of(issue(1)));
        when(gitLab.issues().project().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.issues().project().get(ID, 3L)).thenReturn(Optional.of(issue(3)));

        final List<Issue> result = sut.issues().toList();

        assertThat(result).extracting(Issue::iid).containsExactly(1L, 3L);
    }

    @Test
    void issues_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return issueBasic(iid, null);
                }));
        when(gitLab.issues().project().get(eq(ID), any(Long.class))).thenReturn(Optional.of(issue(1)));

        final Stream<Issue> stream = sut.issues();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void issues_shouldLoadThroughOwningProject_whenSummaryNamesIt() throws IOException {
        final Project numeric = GitLabSdk.of(gitLab).project("42");
        final Milestone inNumeric = new Milestone(numeric, response);
        when(gitLab.projectmilestones().listIssues("42", MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, 42L)));
        when(gitLab.issues().project().get("42", 1L)).thenReturn(Optional.of(issue(1)));

        final List<Issue> result = inNumeric.issues().toList();

        assertThat(result)
                .singleElement()
                .satisfies(found -> assertThat(found.project()).isSameAs(numeric));
    }

    @Test
    void mergeRequests_shouldLoadEachMergeRequestAndSkipGoneOnes_whenGitLabListsBasicRecords() throws IOException {
        when(gitLab.projectmilestones().listMergeRequests(ID, MILESTONE_ID).stream())
                .thenReturn(
                        Stream.of(mergeRequestBasic(1, null), mergeRequestBasic(2, null), mergeRequestBasic(3, null)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(1L), any())).thenReturn(Optional.of(mergeRequest(1)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(2L), any())).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get(eq(ID), eq(3L), any())).thenReturn(Optional.of(mergeRequest(3)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 3L);
    }

    @Test
    void mergeRequests_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectmilestones().listMergeRequests(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return mergeRequestBasic(iid, null);
                }));
        when(gitLab.mergerequests().project().get(eq(ID), any(Long.class), any()))
                .thenReturn(Optional.of(mergeRequest(1)));

        final Stream<MergeRequest> stream = sut.mergeRequests();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void mergeRequests_shouldLoadThroughOtherProject_whenSummaryNamesAnotherProject() throws IOException {
        when(gitLab.projectmilestones().listMergeRequests(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(mergeRequestBasic(5, 99L)));
        when(gitLab.mergerequests().project().get(eq("99"), eq(5L), any())).thenReturn(Optional.of(mergeRequest(5)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result)
                .singleElement()
                .satisfies(found -> assertThat(found.project().id()).isEqualTo("99"));
    }

    @Test
    void issues_shouldLoadThroughOwningProject_whenSummaryNamesTheProjectOfTheMilestone() throws IOException {
        final Milestone inProject42 = new Milestone(
                project, GitLabMilestoneBuilder.builder(response).projectId(42L).build());
        when(gitLab.projectmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, 42L)));
        when(gitLab.issues().project().get(ID, 1L)).thenReturn(Optional.of(issue(1)));

        final List<Issue> result = inProject42.issues().toList();

        assertThat(result)
                .singleElement()
                .satisfies(found -> assertThat(found.project()).isSameAs(project));
        verify(gitLab.issues().project(), never()).get("42", 1L);
    }
}
