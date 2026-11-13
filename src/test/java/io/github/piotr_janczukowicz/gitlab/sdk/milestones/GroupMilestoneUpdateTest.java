package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupMilestoneUpdateTest {

    private static final String ID = "42";
    private static final long MILESTONE_ID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GroupMilestone milestone =
            new GroupMilestone(GitLabSdk.of(gitLab).group(ID), milestone(MILESTONE_ID, "v1"));

    private GitLabGroupGroupMilestonesUpdatePayload sent() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupMilestonesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupMilestonesUpdatePayload.class);
        verify(gitLab.groupmilestones()).update(eq(ID), eq(MILESTONE_ID), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate(final GitLabMilestone answer) throws IOException {
        when(gitLab.groupmilestones()
                        .update(eq(ID), eq(MILESTONE_ID), any(GitLabGroupGroupMilestonesUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GroupMilestoneUpdate update = milestone.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.groupmilestones());
    }

    @Test
    void send_shouldSendOnlySetFields_whenTitleAndDescriptionSet() throws IOException {
        stubUpdate(milestone(MILESTONE_ID, "v2"));

        milestone.update().title("v2").description("Goals").send();

        final GitLabGroupGroupMilestonesUpdatePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.title()).isEqualTo("v2");
            softly.assertThat(payload.description()).isEqualTo("Goals");
            softly.assertThat(payload.dueDate()).isNull();
            softly.assertThat(payload.startDate()).isNull();
            softly.assertThat(payload.stateEvent()).isNull();
        });
    }

    @Test
    void update_shouldSendDatesAsIsoStrings_whenDatesSet() throws IOException {
        stubUpdate(milestone(MILESTONE_ID, "v1"));

        milestone
                .update()
                .dueDate(LocalDate.of(2026, 12, 31))
                .startDate(LocalDate.of(2026, 1, 5))
                .send();

        final GitLabGroupGroupMilestonesUpdatePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.dueDate()).isEqualTo("2026-12-31");
            softly.assertThat(payload.startDate()).isEqualTo("2026-01-05");
        });
    }

    @Test
    void send_shouldReturnMilestoneFromAnswerWithoutExtraGet_whenCalled() throws IOException {
        final GitLabMilestone answer = milestone(MILESTONE_ID, "v2");
        stubUpdate(answer);

        final GroupMilestone result = milestone.update().title("v2").send();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(answer);
            softly.assertThat(result.group()).isSameAs(milestone.group());
            softly.assertThat(milestone.title()).isEqualTo("v1");
        });
        verify(gitLab.groupmilestones(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void title_shouldThrowIllegalArgumentException_whenBlank() {
        final GroupMilestoneUpdate update = milestone.update();

        assertSoftly(softly ->
                softly.assertThatThrownBy(() -> update.title(" ")).isInstanceOf(IllegalArgumentException.class));
        verifyNoInteractions(gitLab.groupmilestones());
    }

    @Test
    void setters_shouldThrowNullPointerException_whenArgumentIsNull() {
        final GroupMilestoneUpdate update = milestone.update();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> update.title(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.description(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.dueDate(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.startDate(null)).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.groupmilestones());
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenMilestoneIsNull() {
        assertThat(catchThrowable(() -> new GroupMilestoneUpdate(null))).isInstanceOf(NullPointerException.class);
    }
}
