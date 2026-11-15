package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestUpdateTest {

    private static final String ID = "42";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest mergeRequest =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID, "Old", "opened"));

    @Test
    void send_shouldSendOnlySetFields_whenTitleAndLabelsAreSet() throws IOException {
        final GitLabMergeRequest updated = mergeRequest(IID, "New", "opened");
        when(gitLab.mergerequests()
                        .project()
                        .update(eq(ID), eq(IID), any(GitLabProjectMergeRequestsUpdatePayload.class)))
                .thenReturn(updated);

        final MergeRequest result =
                mergeRequest.update().title("New").labels(List.of("bug")).send();

        final ArgumentCaptor<GitLabProjectMergeRequestsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsUpdatePayload.class);
        verify(gitLab.mergerequests().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("New");
            softly.assertThat(payload.getValue().labels()).containsExactly("bug");
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(payload.getValue().targetBranch()).isNull();
            softly.assertThat(payload.getValue().stateEvent()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
        });
    }

    @Test
    void send_shouldSendDescriptionAndTargetBranch_whenSet() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .update(eq(ID), eq(IID), any(GitLabProjectMergeRequestsUpdatePayload.class)))
                .thenReturn(mergeRequest(IID, "Old", "opened"));

        mergeRequest.update().description("Text").targetBranch("release").send();

        final ArgumentCaptor<GitLabProjectMergeRequestsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsUpdatePayload.class);
        verify(gitLab.mergerequests().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("Text");
            softly.assertThat(payload.getValue().targetBranch()).isEqualTo("release");
        });
    }

    @Test
    void send_shouldSendPeopleAndMilestone_whenSet() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .update(eq(ID), eq(IID), any(GitLabProjectMergeRequestsUpdatePayload.class)))
                .thenReturn(mergeRequest(IID, "Old", "opened"));

        mergeRequest
                .update()
                .assignees(List.of(1L, 2L))
                .reviewers(List.of(3L))
                .milestone(9L)
                .send();

        final ArgumentCaptor<GitLabProjectMergeRequestsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsUpdatePayload.class);
        verify(gitLab.mergerequests().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().assigneeIds()).containsExactly(1L, 2L);
            softly.assertThat(payload.getValue().reviewerIds()).containsExactly(3L);
            softly.assertThat(payload.getValue().milestoneId()).isEqualTo(9L);
            softly.assertThat(payload.getValue().title()).isNull();
        });
    }

    @Test
    void send_shouldSendTheSwitches_whenSet() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .update(eq(ID), eq(IID), any(GitLabProjectMergeRequestsUpdatePayload.class)))
                .thenReturn(mergeRequest(IID, "Old", "opened"));

        mergeRequest
                .update()
                .squash(true)
                .removeSourceBranch(true)
                .discussionLocked(true)
                .allowCollaboration(false)
                .send();

        final ArgumentCaptor<GitLabProjectMergeRequestsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsUpdatePayload.class);
        verify(gitLab.mergerequests().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().squash()).isTrue();
            softly.assertThat(payload.getValue().removeSourceBranch()).isTrue();
            softly.assertThat(payload.getValue().discussionLocked()).isTrue();
            softly.assertThat(payload.getValue().allowCollaboration()).isFalse();
        });
    }

    @Test
    void assigneesAndReviewers_shouldThrowNullPointerException_whenListIsNull() {
        final MergeRequestUpdate update = mergeRequest.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> update.assignees(null)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userIds");
            softly.assertThat(catchThrowable(() -> update.reviewers(null)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userIds");
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GitLab untouched = mock(GitLab.class);
        final MergeRequest other =
                new MergeRequest(GitLabSdk.of(untouched).project(ID), mergeRequest(IID, "Old", "opened"));

        final Throwable thrown = catchThrowable(() -> other.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(untouched);
    }

    @Test
    void title_shouldThrowIllegalArgumentException_whenBlank() {
        final MergeRequestUpdate update = mergeRequest.update();

        final Throwable thrown = catchThrowable(() -> update.title(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void targetBranch_shouldThrowIllegalArgumentException_whenBlank() {
        final MergeRequestUpdate update = mergeRequest.update();

        final Throwable thrown = catchThrowable(() -> update.targetBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void labels_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final MergeRequestUpdate update = mergeRequest.update();

        final Throwable thrown = catchThrowable(() -> update.labels(List.of(" ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
