package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.createdPipeline;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.job;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.pipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadata;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class PipelineTest {

    private static final String ID = "my-group/my-project";
    private static final long PIPELINE_ID = 5L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabCiPipelineWithMetadata response = pipeline(PIPELINE_ID, "running");
    private final Pipeline sut = new Pipeline(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenPipelineIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(PIPELINE_ID);
            softly.assertThat(sut.status()).isEqualTo(PipelineStatus.RUNNING);
            softly.assertThat(sut.ref()).contains("main");
            softly.assertThat(sut.sha()).isEmpty();
            softly.assertThat(sut.webUrl()).isEmpty();
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void status_shouldMapAllCoreValues_whenGitLabReturnsThem() {
        final List<PipelineStatus> statuses = Stream.of(
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
                .map(value -> new Pipeline(project, pipeline(PIPELINE_ID, value)).status())
                .toList();

        assertThat(statuses).containsExactly(PipelineStatus.values());
    }

    @Test
    void statusName_shouldReturnRawValue_whenStatusIsUnknownToTheSdk() {
        final Pipeline unknown = new Pipeline(project, pipeline(PIPELINE_ID, "UNRECOGNIZED"));

        assertThat(unknown.statusName()).isEqualTo("UNRECOGNIZED");
    }

    @Test
    void statusName_shouldReturnRawValue_whenStatusIsKnown() {
        assertThat(sut.statusName()).isEqualTo("running");
    }

    @Test
    void status_shouldThrowIllegalArgumentException_whenUnrecognized() {
        final Pipeline unknown = new Pipeline(project, pipeline(PIPELINE_ID, "UNRECOGNIZED"));

        final Throwable thrown = catchThrowable(unknown::status);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UNRECOGNIZED");
    }

    @Test
    void retry_shouldReturnNewPipeline_whenGitLabRetries() throws IOException {
        when(gitLab.pipelines().retry(ID, PIPELINE_ID)).thenReturn(createdPipeline(PIPELINE_ID));
        final GitLabCiPipelineWithMetadata reloaded = pipeline(PIPELINE_ID, "pending");
        when(gitLab.pipelines().get(ID, PIPELINE_ID)).thenReturn(Optional.of(reloaded));

        final Pipeline retried = sut.retry();

        assertSoftly(softly -> {
            softly.assertThat(retried).isNotSameAs(sut);
            softly.assertThat(retried.response()).isSameAs(reloaded);
        });
    }

    @Test
    void cancel_shouldReturnNewPipeline_whenGitLabCancels() throws IOException {
        when(gitLab.pipelines().cancel(ID, PIPELINE_ID)).thenReturn(createdPipeline(PIPELINE_ID));
        final GitLabCiPipelineWithMetadata reloaded = pipeline(PIPELINE_ID, "canceling");
        when(gitLab.pipelines().get(ID, PIPELINE_ID)).thenReturn(Optional.of(reloaded));

        final Pipeline canceled = sut.cancel();

        assertSoftly(softly -> {
            softly.assertThat(canceled).isNotSameAs(sut);
            softly.assertThat(canceled.status()).isEqualTo(PipelineStatus.CANCELING);
        });
    }

    @Test
    void cancel_shouldThrowIllegalStateException_whenGoneAfterCancel() throws IOException {
        when(gitLab.pipelines().cancel(ID, PIPELINE_ID)).thenReturn(createdPipeline(PIPELINE_ID));
        when(gitLab.pipelines().get(ID, PIPELINE_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::cancel);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Pipeline 5 is gone");
    }

    @Test
    void jobs_shouldListJobsOfPipeline_whenGitLabReturnsThem() throws IOException {
        when(gitLab.jobs().project().listPipelinesJobs(ID, PIPELINE_ID).stream())
                .thenReturn(Stream.of(job(1L, "build", "success"), job(2L, "test", "running")));

        final List<Job> result = sut.jobs().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Job::name).containsExactly("build", "test");
            softly.assertThat(result)
                    .allSatisfy(j -> softly.assertThat(j.project()).isSameAs(project));
        });
    }

    @Test
    void jobs_shouldReturnEmptyStream_whenPipelineHasNoJobs() throws IOException {
        when(gitLab.jobs().project().listPipelinesJobs(ID, PIPELINE_ID).stream())
                .thenReturn(Stream.empty());

        final List<Job> result = sut.jobs().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void delete_shouldSendDelete_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.pipelines()).delete(ID, PIPELINE_ID);
    }

    @Test
    void refresh_shouldReturnNewPipeline_whenGitLabHasIt() throws IOException {
        final GitLabCiPipelineWithMetadata reloaded = pipeline(PIPELINE_ID, "success");
        when(gitLab.pipelines().get(ID, PIPELINE_ID)).thenReturn(Optional.of(reloaded));

        final Pipeline refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.pipelines().get(ID, PIPELINE_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Pipeline 5 is gone");
    }
}
