package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Job;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Reads only: creating a pipeline needs a CI configuration and runners that a sandbox may not have. */
@Tag("smoke")
class PipelinesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void pipelines_shouldListLoadAndRefresh_whenSandboxHasPipelines() throws IOException {
        final Project project = ENV.sandbox();
        final List<Pipeline> pipelines = project.pipelines().limit(3).toList();
        assumeFalse(pipelines.isEmpty(), "the sandbox has no pipelines");
        final Pipeline first = pipelines.get(0);

        final Optional<Pipeline> loaded = project.pipeline(first.id());
        final Pipeline refreshed = first.refresh();
        final List<Job> jobs = first.jobs().limit(5).toList();

        assertSoftly(softly -> {
            softly.assertThat(loaded).map(Pipeline::id).contains(first.id());
            softly.assertThat(refreshed.id()).isEqualTo(first.id());
            softly.assertThat(jobs).extracting(Job::name).doesNotContainNull();
        });
    }

    @Test
    void jobs_shouldListAndLoad_whenSandboxHasJobs() throws IOException {
        final Project project = ENV.sandbox();
        final List<Job> jobs = project.jobs().limit(3).toList();
        assumeFalse(jobs.isEmpty(), "the sandbox has no jobs");

        final Optional<Job> loaded = project.job(jobs.get(0).id());

        assertThat(loaded).map(Job::id).contains(jobs.get(0).id());
    }

    @Test
    void pipeline_shouldReturnEmpty_whenIdDoesNotExist() throws IOException {
        final Optional<Pipeline> missing = ENV.sandbox().pipeline(Long.MAX_VALUE);

        assertThat(missing).isEmpty();
    }
}
