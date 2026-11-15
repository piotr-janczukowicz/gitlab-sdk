package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.timeStats;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStats;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsAddSpentTimePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsTimeEstimatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.TimeStats;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestTimeTrackingTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest sut =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID, "Fix", "opened"));

    @Test
    void timeStats_shouldWrapResponse_whenGitLabHasIt() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(3600, 1800);
        when(gitLab.mergerequests().project().getTimeStats(ID, IID)).thenReturn(Optional.of(stats));

        final Optional<TimeStats> found = sut.timeStats();

        assertThat(found)
                .hasValueSatisfying(value -> assertThat(value.response()).isSameAs(stats));
    }

    @Test
    void timeStats_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequests().project().getTimeStats(ID, IID)).thenReturn(Optional.empty());

        final Optional<TimeStats> found = sut.timeStats();

        assertThat(found).isEmpty();
    }

    @Test
    void estimateTime_shouldSendWholeSeconds_whenDurationIsGiven() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(5400, 0);
        when(gitLab.mergerequests()
                        .project()
                        .timeEstimate(eq(ID), eq(IID), any(GitLabProjectMergeRequestsTimeEstimatePayload.class)))
                .thenReturn(stats);

        final TimeStats result = sut.estimateTime(Duration.ofMinutes(90).plusMillis(900));

        final ArgumentCaptor<GitLabProjectMergeRequestsTimeEstimatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsTimeEstimatePayload.class);
        verify(gitLab.mergerequests().project()).timeEstimate(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().duration()).isEqualTo("5400s");
            softly.assertThat(result.response()).isSameAs(stats);
        });
    }

    @Test
    void estimateTime_shouldThrowIllegalArgumentException_whenDurationIsNegative() {
        final GitLab untouched = mock(GitLab.class);
        final MergeRequest other = new MergeRequest(GitLabSdk.of(untouched).project(ID), mergeRequest(IID, "t", "x"));

        final Throwable thrown = catchThrowable(() -> other.estimateTime(Duration.ofSeconds(-1)));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("estimate");
        verifyNoInteractions(untouched);
    }

    @Test
    void estimateTime_shouldThrowNullPointerException_whenDurationIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.estimateTime(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("estimate");
    }

    @Test
    void estimateTime_shouldSendZeroSeconds_whenDurationIsZero() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .timeEstimate(eq(ID), eq(IID), any(GitLabProjectMergeRequestsTimeEstimatePayload.class)))
                .thenReturn(timeStats(0, 0));

        sut.estimateTime(Duration.ZERO);

        final ArgumentCaptor<GitLabProjectMergeRequestsTimeEstimatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsTimeEstimatePayload.class);
        verify(gitLab.mergerequests().project()).timeEstimate(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().duration()).isEqualTo("0s");
    }

    @Test
    void resetEstimate_shouldWrapResponse_whenCalled() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(0, 0);
        when(gitLab.mergerequests().project().resetTimeEstimate(ID, IID)).thenReturn(stats);

        final TimeStats result = sut.resetEstimate();

        assertThat(result.response()).isSameAs(stats);
    }

    @Test
    void addSpentTime_shouldSendWholeSeconds_whenDurationIsGiven() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(0, 1800);
        when(gitLab.mergerequests()
                        .project()
                        .addSpentTime(eq(ID), eq(IID), any(GitLabProjectMergeRequestsAddSpentTimePayload.class)))
                .thenReturn(stats);

        final TimeStats result = sut.addSpentTime(Duration.ofMinutes(30));

        final ArgumentCaptor<GitLabProjectMergeRequestsAddSpentTimePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsAddSpentTimePayload.class);
        verify(gitLab.mergerequests().project()).addSpentTime(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().duration()).isEqualTo("1800s");
            softly.assertThat(result.response()).isSameAs(stats);
        });
    }

    @Test
    void addSpentTime_shouldThrowIllegalArgumentException_whenDurationIsNegative() {
        final GitLab untouched = mock(GitLab.class);
        final MergeRequest other = new MergeRequest(GitLabSdk.of(untouched).project(ID), mergeRequest(IID, "t", "x"));

        final Throwable thrown = catchThrowable(() -> other.addSpentTime(Duration.ofSeconds(-1)));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("spent");
        verifyNoInteractions(untouched);
    }

    @Test
    void addSpentTime_shouldThrowNullPointerException_whenDurationIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addSpentTime(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("spent");
    }

    @Test
    void resetSpentTime_shouldWrapResponse_whenCalled() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(3600, 0);
        when(gitLab.mergerequests().project().resetSpentTime(ID, IID)).thenReturn(stats);

        final TimeStats result = sut.resetSpentTime();

        assertThat(result.response()).isSameAs(stats);
    }
}
