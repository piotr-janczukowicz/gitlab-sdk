package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.basicPipeline;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.createdPipeline;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.job;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.pipeline;
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
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsFilter;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsScope;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadata;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesListProjectFilter;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesListProjectFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectCiTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void pipeline_shouldReturnLoadedPipeline_whenGitLabHasIt() throws IOException {
        final GitLabCiPipelineWithMetadata response = pipeline(5L, "running");
        when(gitLab.pipelines().get(ID, 5L)).thenReturn(Optional.of(response));

        final Optional<Pipeline> found = sut.pipeline(5L);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void pipeline_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.pipelines().get(ID, 5L)).thenReturn(Optional.empty());

        final Optional<Pipeline> found = sut.pipeline(5L);

        assertThat(found).isEmpty();
    }

    @Test
    void pipeline_shouldPassNumericProjectIdUnchanged_whenProjectIsNumeric() throws IOException {
        final GitLab other = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(other.pipelines().get("42", 3L)).thenReturn(Optional.of(pipeline(3L, "success")));

        final Optional<Pipeline> found = GitLabSdk.of(other).project("42").pipeline(3L);

        assertThat(found).isPresent();
    }

    @Test
    void pipelines_shouldLoadEachItem_whenListReturnsBasicRecords() throws IOException {
        when(gitLab.pipelines().listProject(ID).stream()).thenReturn(Stream.of(basicPipeline(1L), basicPipeline(2L)));
        when(gitLab.pipelines().get(ID, 1L)).thenReturn(Optional.of(pipeline(1L, "success")));
        when(gitLab.pipelines().get(ID, 2L)).thenReturn(Optional.of(pipeline(2L, "failed")));

        final List<Pipeline> result = sut.pipelines().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Pipeline::id).containsExactly(1L, 2L);
            softly.assertThat(result)
                    .extracting(Pipeline::status)
                    .containsExactly(PipelineStatus.SUCCESS, PipelineStatus.FAILED);
        });
    }

    @Test
    void pipelines_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.pipelines().listProject(ID).stream())
                .thenReturn(Stream.of(basicPipeline(1L), basicPipeline(2L), basicPipeline(3L)));
        when(gitLab.pipelines().get(ID, 1L)).thenReturn(Optional.of(pipeline(1L, "success")));
        when(gitLab.pipelines().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.pipelines().get(ID, 3L)).thenReturn(Optional.of(pipeline(3L, "success")));

        final List<Pipeline> result = sut.pipelines().toList();

        assertThat(result).extracting(Pipeline::id).containsExactly(1L, 3L);
    }

    @Test
    void pipelines_shouldReturnEmptyStream_whenProjectHasNoPipelines() throws IOException {
        when(gitLab.pipelines().listProject(ID).stream()).thenReturn(Stream.empty());

        final List<Pipeline> result = sut.pipelines().toList();

        assertThat(result).isEmpty();
        verify(gitLab.pipelines(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void pipelines_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectPipelinesListProjectFilter filter = GitLabProjectPipelinesListProjectFilterBuilder.builder()
                .ref("main")
                .build();
        when(gitLab.pipelines().listProject(ID, filter).stream()).thenReturn(Stream.of(basicPipeline(1L)));
        when(gitLab.pipelines().get(ID, 1L)).thenReturn(Optional.of(pipeline(1L, "success")));

        final List<Pipeline> result = sut.pipelines(filter).toList();

        assertThat(result).extracting(Pipeline::id).containsExactly(1L);
    }

    @Test
    void pipelines_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.pipelines((GitLabProjectPipelinesListProjectFilter) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createPipeline_shouldSendRefAndLoadPipeline_whenRefIsGiven() throws IOException {
        when(gitLab.pipelines().create(any(String.class), any(GitLabProjectPipelinesCreatePayload.class)))
                .thenReturn(createdPipeline(9L));
        final GitLabCiPipelineWithMetadata loaded = pipeline(9L, "created");
        when(gitLab.pipelines().get(ID, 9L)).thenReturn(Optional.of(loaded));

        final Pipeline created = sut.createPipeline("main");

        final ArgumentCaptor<GitLabProjectPipelinesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelinesCreatePayload.class);
        verify(gitLab.pipelines()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().ref()).isEqualTo("main");
            softly.assertThat(created.response()).isSameAs(loaded);
        });
    }

    @Test
    void createPipeline_shouldThrowIllegalArgumentException_whenRefIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createPipeline(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPipeline_shouldThrowIllegalStateException_whenGoneAfterCreate() throws IOException {
        final GitLabProjectPipelinesCreatePayload payload =
                GitLabProjectPipelinesCreatePayloadBuilder.builder().ref("main").build();
        when(gitLab.pipelines().create(ID, payload)).thenReturn(createdPipeline(9L));
        when(gitLab.pipelines().get(ID, 9L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.createPipeline(payload));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Pipeline 9 is gone");
    }

    @Test
    void job_shouldReturnLoadedJob_whenGitLabHasIt() throws IOException {
        when(gitLab.jobs().project().get(ID, 4L)).thenReturn(Optional.of(job(4L, "build", "running")));

        final Optional<Job> found = sut.job(4L);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().name()).isEqualTo("build");
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void job_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.jobs().project().get(ID, 4L)).thenReturn(Optional.empty());

        final Optional<Job> found = sut.job(4L);

        assertThat(found).isEmpty();
    }

    @Test
    void jobs_shouldListJobsOfProject_whenGitLabReturnsThem() throws IOException {
        when(gitLab.jobs().project().listJobs(ID).stream())
                .thenReturn(Stream.of(job(1L, "build", "success"), job(2L, "test", "failed")));

        final List<Job> result = sut.jobs().toList();

        assertThat(result).extracting(Job::name).containsExactly("build", "test");
    }

    @Test
    void jobs_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectJobsListJobsFilter filter = GitLabProjectJobsListJobsFilterBuilder.builder()
                .scope(List.of(GitLabProjectJobsListJobsScope.FAILED))
                .ref("main")
                .build();
        when(gitLab.jobs().project().listJobs(ID, filter).stream()).thenReturn(Stream.of(job(2L, "test", "failed")));

        final List<Job> result = sut.jobs(filter).toList();

        assertThat(result).extracting(Job::name).containsExactly("test");
    }

    @Test
    void jobs_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.jobs((GitLabProjectJobsListJobsFilter) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void jobs_shouldReturnEmptyStream_whenProjectHasNoJobs() throws IOException {
        when(gitLab.jobs().project().listJobs(ID).stream()).thenReturn(Stream.empty());

        final List<Job> result = sut.jobs().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void downloadArtifacts_shouldSendRefAndJobName_whenTargetGiven() throws IOException {
        final Path target = Path.of("artifacts.zip");
        when(gitLab.jobs().project().getArtifactsDownload(ID, "main", "build", target))
                .thenReturn(target);

        final Path result = sut.downloadArtifacts("main", "build", target);

        assertThat(result).isSameAs(target);
    }

    @Test
    void downloadArtifacts_shouldPassRefWithSlashUnchanged_whenRefIsABranchPath() throws IOException {
        final OutputStream out = new ByteArrayOutputStream();
        when(gitLab.jobs().project().getArtifactsDownload(ID, "feature/x", "build job", out))
                .thenReturn(7L);

        final long written = sut.downloadArtifacts("feature/x", "build job", out);

        assertThat(written).isEqualTo(7L);
    }

    @Test
    void downloadArtifacts_shouldThrowIllegalArgumentException_whenRefOrJobNameIsBlank() throws IOException {
        final Path target = Path.of("artifacts.zip");
        final OutputStream out = new ByteArrayOutputStream();

        final List<Throwable> thrown = List.of(
                catchThrowable(() -> sut.downloadArtifacts(" ", "build", target)),
                catchThrowable(() -> sut.downloadArtifacts("main", "", target)),
                catchThrowable(() -> sut.downloadArtifacts(" ", "build", out)),
                catchThrowable(() -> sut.downloadArtifacts("main", " ", out)));

        assertSoftly(softly -> {
            softly.assertThat(thrown)
                    .allSatisfy(t -> softly.assertThat(t).isInstanceOf(IllegalArgumentException.class));
            softly.assertThat(thrown).hasSize(4);
        });
        verify(gitLab.jobs().project(), never()).getArtifactsDownload(any(), any(), any(), any(Path.class));
        verify(gitLab.jobs().project(), never()).getArtifactsDownload(any(), any(), any(), any(OutputStream.class));
    }

    @Test
    void downloadArtifacts_shouldThrowNullPointerException_whenTargetOrStreamIsNull() throws IOException {
        final Throwable pathNull = catchThrowable(() -> sut.downloadArtifacts("main", "build", (Path) null));
        final Throwable streamNull = catchThrowable(() -> sut.downloadArtifacts("main", "build", (OutputStream) null));

        assertSoftly(softly -> {
            softly.assertThat(pathNull).isInstanceOf(NullPointerException.class);
            softly.assertThat(streamNull).isInstanceOf(NullPointerException.class);
        });
        verify(gitLab.jobs().project(), never()).getArtifactsDownload(any(), any(), any(), any(Path.class));
        verify(gitLab.jobs().project(), never()).getArtifactsDownload(any(), any(), any(), any(OutputStream.class));
    }
}
