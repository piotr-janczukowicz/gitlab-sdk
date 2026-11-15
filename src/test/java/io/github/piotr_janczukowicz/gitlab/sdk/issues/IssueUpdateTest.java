package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issue;
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
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueUpdateTest {

    private static final String ID = "42";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Issue issue = new Issue(GitLabSdk.of(gitLab).project(ID), issue(IID, "Old", "opened"));

    @Test
    void send_shouldSendOnlySetFields_whenTitleAndLabelsAreSet() throws IOException {
        final GitLabIssue updated = issue(IID, "New", "opened");
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(updated);

        final Issue result = issue.update().title("New").labels(List.of("bug")).send();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("New");
            softly.assertThat(payload.getValue().labels()).containsExactly("bug");
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(payload.getValue().assigneeIds()).isNull();
            softly.assertThat(payload.getValue().stateEvent()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
        });
    }

    @Test
    void send_shouldSendEmptyDescription_whenClearDescriptionIsSet() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Old", "opened"));

        issue.update().clearDescription().send();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().description()).isEmpty();
    }

    @Test
    void send_shouldSendDescriptionAndAssignees_whenSet() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Old", "opened"));

        issue.update().description("Text").assignees(List.of(3L, 4L)).send();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("Text");
            softly.assertThat(payload.getValue().assigneeIds()).containsExactly(3L, 4L);
        });
    }

    @Test
    void send_shouldSendConfidentialMilestoneAndDueDate_whenSet() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Old", "opened"));

        issue.update()
                .confidential(true)
                .milestone(5L)
                .dueDate(LocalDate.of(2026, 12, 1))
                .send();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().confidential()).isTrue();
            softly.assertThat(payload.getValue().milestoneId()).isEqualTo(5L);
            softly.assertThat(payload.getValue().dueDate()).isEqualTo("2026-12-01");
            softly.assertThat(payload.getValue().title()).isNull();
        });
    }

    @Test
    void send_shouldSendMilestoneZero_whenMilestoneIsCleared() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Old", "opened"));

        issue.update().clearMilestone().send();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().milestoneId()).isEqualTo(0L);
    }

    @Test
    void send_shouldSendConfidentialFalse_whenSetToFalse() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Old", "opened"));

        issue.update().confidential(false).send();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().confidential()).isFalse();
    }

    @Test
    void dueDate_shouldThrowNullPointerException_whenDateIsNull() {
        final IssueUpdate update = issue.update();

        final Throwable thrown = catchThrowable(() -> update.dueDate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("dueDate");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GitLab untouched = mock(GitLab.class);
        final Issue other = new Issue(GitLabSdk.of(untouched).project(ID), issue(IID, "Old", "opened"));

        final Throwable thrown = catchThrowable(() -> other.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(untouched);
    }

    @Test
    void title_shouldThrowIllegalArgumentException_whenBlank() {
        final IssueUpdate update = issue.update();

        final Throwable thrown = catchThrowable(() -> update.title(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void labels_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final IssueUpdate update = issue.update();

        final Throwable thrown = catchThrowable(() -> update.labels(List.of(" ")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
