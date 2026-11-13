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
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
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
class GroupMilestoneTest {

    private static final String ID = "my-group";
    private static final long MILESTONE_ID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);
    private final GitLabMilestone response = milestone(MILESTONE_ID, "v1");
    private final GroupMilestone sut = new GroupMilestone(group, response);

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

        final GroupMilestone result = new GroupMilestone(group, full);

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
            softly.assertThat(result.group()).isSameAs(group);
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
        final GroupMilestone blank = new GroupMilestone(
                group,
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
        final GroupMilestone broken = new GroupMilestone(
                group,
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
        final GroupMilestone odd = new GroupMilestone(
                group, GitLabMilestoneBuilder.builder(response).state("paused").build());

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
            softly.assertThatThrownBy(() -> new GroupMilestone(null, response))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> new GroupMilestone(group, null)).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void refresh_shouldReturnReloadedMilestone_whenGitLabHasIt() throws IOException {
        final GitLabMilestone reloaded = milestone(MILESTONE_ID, "v1 renamed");
        when(gitLab.groupmilestones().get(ID, MILESTONE_ID)).thenReturn(Optional.of(reloaded));

        final GroupMilestone result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.groupmilestones().get(ID, MILESTONE_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Group milestone 7 is gone");
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.groupmilestones()).delete(ID, MILESTONE_ID);
    }

    private GitLabGroupGroupMilestonesUpdatePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupMilestonesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupMilestonesUpdatePayload.class);
        verify(gitLab.groupmilestones()).update(eq(ID), eq(MILESTONE_ID), payload.capture());
        return payload.getValue();
    }

    @Test
    void close_shouldSendCloseStateEvent_whenCalled() throws IOException {
        final GitLabMilestone closed =
                GitLabMilestoneBuilder.builder(response).state("closed").build();
        when(gitLab.groupmilestones()
                        .update(eq(ID), eq(MILESTONE_ID), any(GitLabGroupGroupMilestonesUpdatePayload.class)))
                .thenReturn(closed);

        final GroupMilestone result = sut.close();

        final GitLabGroupGroupMilestonesUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.stateEvent()).isEqualTo(GitLabGroupGroupMilestonesUpdateStateEvent.CLOSE);
            softly.assertThat(payload.title()).isNull();
            softly.assertThat(result.response()).isSameAs(closed);
            softly.assertThat(sut.state()).isEqualTo(MilestoneState.ACTIVE);
        });
        verify(gitLab.groupmilestones(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void reopen_shouldSendActivateStateEvent_whenCalled() throws IOException {
        final GitLabMilestone reopened = milestone(MILESTONE_ID, "v1");
        when(gitLab.groupmilestones()
                        .update(eq(ID), eq(MILESTONE_ID), any(GitLabGroupGroupMilestonesUpdatePayload.class)))
                .thenReturn(reopened);

        final GroupMilestone result = sut.reopen();

        final var payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.stateEvent()).isEqualTo(GitLabGroupGroupMilestonesUpdateStateEvent.ACTIVATE);
            softly.assertThat(result.response()).isSameAs(reopened);
        });
    }

    @Test
    void issues_shouldLoadEachItemThroughItsProject_whenGroupMilestone() throws IOException {
        when(gitLab.groupmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, 11L), issueBasic(1, 22L)));
        when(gitLab.issues().project().get("11", 1L)).thenReturn(Optional.of(issue(1)));
        when(gitLab.issues().project().get("22", 1L)).thenReturn(Optional.of(issue(1)));

        final List<Issue> result = sut.issues().toList();

        assertThat(result).extracting(found -> found.project().id()).containsExactly("11", "22");
        verify(gitLab.issues().project(), never()).get(ID, 1L);
    }

    @Test
    void issues_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.groupmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, 11L), issueBasic(2, 11L), issueBasic(3, 22L)));
        when(gitLab.issues().project().get("11", 1L)).thenReturn(Optional.of(issue(1)));
        when(gitLab.issues().project().get("11", 2L)).thenReturn(Optional.empty());
        when(gitLab.issues().project().get("22", 3L)).thenReturn(Optional.of(issue(3)));

        final List<Issue> result = sut.issues().toList();

        assertThat(result).extracting(Issue::iid).containsExactly(1L, 3L);
    }

    @Test
    void issues_shouldSkipItem_whenSummaryNamesNoProject() throws IOException {
        when(gitLab.groupmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(issueBasic(1, null), issueBasic(2, 11L)));
        when(gitLab.issues().project().get("11", 2L)).thenReturn(Optional.of(issue(2)));

        final List<Issue> result = sut.issues().toList();

        assertThat(result).extracting(Issue::iid).containsExactly(2L);
    }

    @Test
    void issues_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groupmilestones().listIssues(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return issueBasic(iid, 11L);
                }));
        when(gitLab.issues().project().get(eq("11"), any(Long.class))).thenReturn(Optional.of(issue(1)));

        final Stream<Issue> stream = sut.issues();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void mergeRequests_shouldLoadEachItemThroughItsProject_whenGroupMilestone() throws IOException {
        when(gitLab.groupmilestones().listMergeRequests(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(mergeRequestBasic(5, 11L), mergeRequestBasic(5, 22L)));
        when(gitLab.mergerequests().project().get("11", 5L)).thenReturn(Optional.of(mergeRequest(5)));
        when(gitLab.mergerequests().project().get("22", 5L)).thenReturn(Optional.of(mergeRequest(5)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result).extracting(found -> found.project().id()).containsExactly("11", "22");
    }

    @Test
    void mergeRequests_shouldSkipItems_whenGoneOrWithoutProject() throws IOException {
        when(gitLab.groupmilestones().listMergeRequests(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(
                        mergeRequestBasic(1, 11L),
                        mergeRequestBasic(2, 11L),
                        mergeRequestBasic(3, null),
                        mergeRequestBasic(4, 22L)));
        when(gitLab.mergerequests().project().get("11", 1L)).thenReturn(Optional.of(mergeRequest(1)));
        when(gitLab.mergerequests().project().get("11", 2L)).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get("22", 4L)).thenReturn(Optional.of(mergeRequest(4)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 4L);
    }

    @Test
    void mergeRequests_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groupmilestones().listMergeRequests(ID, MILESTONE_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return mergeRequestBasic(iid, 11L);
                }));
        when(gitLab.mergerequests().project().get(eq("11"), any(Long.class))).thenReturn(Optional.of(mergeRequest(1)));

        final Stream<MergeRequest> stream = sut.mergeRequests();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }
}
