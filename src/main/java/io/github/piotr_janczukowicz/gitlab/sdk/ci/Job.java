package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJob;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsCancelPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsPlayPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsRetryPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A job of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Job {

    private final Project project;
    private final GitLabCiJob response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Job(final Project project, final GitLabCiJob response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the job belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the job id, unique within the GitLab instance.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the job name.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the job status.
     *
     * @return the status
     * @throws IllegalArgumentException if GitLab returns a status this SDK does not know; {@link #statusName()} still
     *     returns it
     */
    public JobStatus status() {
        return JobStatus.from(statusName());
    }

    /**
     * Returns the job status as GitLab sent it, for example {@code running}. Use it when {@link #status()} throws
     * {@link IllegalArgumentException} because GitLab returned a status this SDK does not know yet.
     *
     * @return the raw status value
     */
    public String statusName() {
        return requireNonNull(response.status(), "status");
    }

    /**
     * Returns the stage the job belongs to.
     *
     * @return the stage, or empty when GitLab does not return one
     */
    public Optional<String> stage() {
        return Optional.ofNullable(response.stage());
    }

    /**
     * Loads the pipeline the job belongs to ({@code GET /projects/{id}/pipelines/{pipeline_id}}), by the pipeline id in
     * the job's response.
     *
     * @return the pipeline, or empty when the job response names none or GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Pipeline> pipeline() throws IOException {
        final GitLabCiPipelineBasic listed = response.pipeline();
        if (listed == null || listed.id() == null) {
            return Optional.empty();
        }
        return project.pipeline(listed.id());
    }

    /**
     * Retries the job ({@code POST /projects/{id}/jobs/{job_id}/retry}). This object is not changed.
     *
     * @return the new job GitLab started
     * @throws IOException if the transport fails
     */
    public Job retry() throws IOException {
        return new Job(
                project,
                gitLab().jobs()
                        .project()
                        .retry(
                                project.id(),
                                id(),
                                GitLabProjectJobsRetryPayloadBuilder.builder().build()));
    }

    /**
     * Cancels the job ({@code POST /projects/{id}/jobs/{job_id}/cancel}). This object is not changed.
     *
     * @return the job as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Job cancel() throws IOException {
        return new Job(
                project,
                gitLab().jobs()
                        .project()
                        .cancel(
                                project.id(),
                                id(),
                                GitLabProjectJobsCancelPayloadBuilder.builder().build()));
    }

    /**
     * Starts a manual job ({@code POST /projects/{id}/jobs/{job_id}/play}) and loads it again ({@code GET
     * /projects/{id}/jobs/{job_id}}), because GitLab answers the play with a smaller record. This object is not
     * changed.
     *
     * @return the job as GitLab holds it afterwards
     * @throws IllegalStateException if GitLab no longer has the job
     * @throws IOException if the transport fails
     */
    public Job play() throws IOException {
        gitLab().jobs()
                .project()
                .play(
                        project.id(),
                        id(),
                        GitLabProjectJobsPlayPayloadBuilder.builder().build());
        return refresh();
    }

    /**
     * Erases the job's artifacts and trace ({@code POST /projects/{id}/jobs/{job_id}/erase}). This object is not
     * changed.
     *
     * @return the job as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Job erase() throws IOException {
        return new Job(project, gitLab().jobs().project().erase(project.id(), id()));
    }

    /**
     * Loads the job again ({@code GET /projects/{id}/jobs/{job_id}}). This object is not changed.
     *
     * @return the job as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the job
     * @throws IOException if the transport fails
     */
    public Job refresh() throws IOException {
        return project.job(id()).orElseThrow(() -> new IllegalStateException("Job " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCiJob response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
