package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.basicJob;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.job;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.jobOfPipeline;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.pipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJob;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJobBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsCancelPayload;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsPlayPayload;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsRetryPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class JobTest {

    private static final String ID = "my-group/my-project";
    private static final long JOB_ID = 4L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabCiJob response = job(JOB_ID, "build", "running");
    private final Job sut = new Job(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenJobIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(JOB_ID);
            softly.assertThat(sut.name()).isEqualTo("build");
            softly.assertThat(sut.status()).isEqualTo(JobStatus.RUNNING);
            softly.assertThat(sut.stage()).isEmpty();
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void status_shouldMapAllCoreValues_whenGitLabReturnsThem() {
        final List<JobStatus> statuses = Stream.of(
                        "created",
                        "waiting_for_resource",
                        "preparing",
                        "waiting_for_callback",
                        "pending",
                        "running",
                        "success",
                        "failed",
                        "canceling",
                        "canceled",
                        "skipped",
                        "manual",
                        "scheduled")
                .map(value -> new Job(project, job(JOB_ID, "build", value)).status())
                .toList();

        assertThat(statuses).containsExactly(JobStatus.values());
    }

    @Test
    void status_shouldThrowIllegalArgumentException_whenUnrecognized() {
        final Job unknown = new Job(project, job(JOB_ID, "build", "UNRECOGNIZED"));

        final Throwable thrown = catchThrowable(unknown::status);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UNRECOGNIZED");
    }

    @Test
    void retry_shouldReturnNewJob_whenGitLabRetries() throws IOException {
        final GitLabCiJob retried = job(JOB_ID + 1, "build", "pending");
        when(gitLab.jobs().project().retry(eqId(), eqJob(), any(GitLabProjectJobsRetryPayload.class)))
                .thenReturn(retried);

        final Job result = sut.retry();

        assertSoftly(softly -> {
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(retried);
        });
    }

    @Test
    void cancel_shouldReturnNewJob_whenGitLabCancels() throws IOException {
        final GitLabCiJob canceled = job(JOB_ID, "build", "canceled");
        when(gitLab.jobs().project().cancel(eqId(), eqJob(), any(GitLabProjectJobsCancelPayload.class)))
                .thenReturn(canceled);

        final Job result = sut.cancel();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(canceled);
            softly.assertThat(result.status()).isEqualTo(JobStatus.CANCELED);
        });
    }

    @Test
    void erase_shouldReturnNewJob_whenGitLabErases() throws IOException {
        final GitLabCiJob erased = job(JOB_ID, "build", "success");
        when(gitLab.jobs().project().erase(ID, JOB_ID)).thenReturn(erased);

        final Job result = sut.erase();

        assertThat(result.response()).isSameAs(erased);
    }

    @Test
    void play_shouldLoadJobAfterPlay_whenGitLabReturnsBasicRecord() throws IOException {
        when(gitLab.jobs().project().play(eqId(), eqJob(), any(GitLabProjectJobsPlayPayload.class)))
                .thenReturn(basicJob(JOB_ID, "pending"));
        final GitLabCiJob loaded = job(JOB_ID, "build", "pending");
        when(gitLab.jobs().project().get(ID, JOB_ID)).thenReturn(Optional.of(loaded));

        final Job result = sut.play();

        assertSoftly(softly -> {
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(loaded);
        });
    }

    @Test
    void play_shouldThrowIllegalStateException_whenGoneAfterPlay() throws IOException {
        when(gitLab.jobs().project().play(eqId(), eqJob(), any(GitLabProjectJobsPlayPayload.class)))
                .thenReturn(basicJob(JOB_ID, "pending"));
        when(gitLab.jobs().project().get(ID, JOB_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::play);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Job 4 is gone");
    }

    @Test
    void pipeline_shouldLoadPipelineOfJob_whenJobBelongsToPipeline() throws IOException {
        final Job inPipeline = new Job(project, jobOfPipeline(JOB_ID, 5L));
        when(gitLab.pipelines().get(ID, 5L)).thenReturn(Optional.of(pipeline(5L, "success")));

        final Optional<Pipeline> found = inPipeline.pipeline();

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().id()).isEqualTo(5L);
    }

    @Test
    void pipeline_shouldReturnEmpty_whenJobHasNoPipeline() throws IOException {
        final Optional<Pipeline> found = sut.pipeline();

        assertThat(found).isEmpty();
    }

    @Test
    void pipeline_shouldReturnEmpty_whenJobPipelineHasNoId() throws IOException {
        final Job noId = new Job(
                project,
                GitLabCiJobBuilder.builder()
                        .id(JOB_ID)
                        .pipeline(GitLabCiPipelineBasicBuilder.builder().build())
                        .build());

        final Optional<Pipeline> found = noId.pipeline();

        assertThat(found).isEmpty();
    }

    @Test
    void refresh_shouldReturnNewJob_whenGitLabHasIt() throws IOException {
        final GitLabCiJob reloaded = job(JOB_ID, "build", "success");
        when(gitLab.jobs().project().get(ID, JOB_ID)).thenReturn(Optional.of(reloaded));

        final Job refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.jobs().project().get(ID, JOB_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Job 4 is gone");
    }

    private static String eqId() {
        return org.mockito.ArgumentMatchers.eq(ID);
    }

    private static long eqJob() {
        return org.mockito.ArgumentMatchers.eq(JOB_ID);
    }
}
