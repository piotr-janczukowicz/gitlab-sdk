package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadata;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A pipeline of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * GitLab lists, creates, retries and cancels a pipeline with smaller records, so every pipeline is loaded with
 * {@code GET /projects/{id}/pipelines/{pipeline_id}} and all of them have the same shape.
 */
public final class Pipeline {

    private final Project project;
    private final GitLabCiPipelineWithMetadata response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Pipeline(final Project project, final GitLabCiPipelineWithMetadata response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the pipeline belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the pipeline id, unique within the GitLab instance.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the pipeline status.
     *
     * @return the status
     * @throws IllegalArgumentException if GitLab returns a status this SDK does not know; {@link #statusName()} still
     *     returns it
     */
    public PipelineStatus status() {
        return PipelineStatus.from(statusName());
    }

    /**
     * Returns the pipeline status as GitLab sent it, for example {@code running}. Use it when {@link #status()} throws
     * {@link IllegalArgumentException} because GitLab returned a status this SDK does not know yet.
     *
     * @return the raw status value
     */
    public String statusName() {
        return requireNonNull(response.status(), "status");
    }

    /**
     * Returns the branch or tag the pipeline ran for.
     *
     * @return the ref, or empty when GitLab does not return one
     */
    public Optional<String> ref() {
        return Optional.ofNullable(response.ref());
    }

    /**
     * Returns the commit the pipeline ran for.
     *
     * @return the commit SHA, or empty when GitLab does not return one
     */
    public Optional<String> sha() {
        return Optional.ofNullable(response.sha());
    }

    /**
     * Returns the address of the pipeline in the GitLab web interface.
     *
     * @return the URL, or empty when GitLab does not return one
     */
    public Optional<String> webUrl() {
        return Optional.ofNullable(response.webUrl());
    }

    /**
     * Retries the failed jobs of the pipeline ({@code POST /projects/{id}/pipelines/{pipeline_id}/retry}) and loads the
     * pipeline again ({@code GET /projects/{id}/pipelines/{pipeline_id}}). This object is not changed.
     *
     * @return the pipeline as GitLab holds it afterwards
     * @throws IllegalStateException if GitLab no longer has the pipeline
     * @throws IOException if the transport fails
     */
    public Pipeline retry() throws IOException {
        gitLab().pipelines().retry(project.id(), id());
        return refresh();
    }

    /**
     * Cancels the running jobs of the pipeline ({@code POST /projects/{id}/pipelines/{pipeline_id}/cancel}) and loads
     * the pipeline again ({@code GET /projects/{id}/pipelines/{pipeline_id}}). This object is not changed.
     *
     * @return the pipeline as GitLab holds it afterwards
     * @throws IllegalStateException if GitLab no longer has the pipeline
     * @throws IOException if the transport fails
     */
    public Pipeline cancel() throws IOException {
        gitLab().pipelines().cancel(project.id(), id());
        return refresh();
    }

    /**
     * Lists the jobs of the pipeline ({@code GET /projects/{id}/pipelines/{pipeline_id}/jobs}). Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the jobs
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Job> jobs() throws IOException {
        return gitLab().jobs().project().listPipelinesJobs(project.id(), id()).stream()
                .map(listed -> new Job(project, listed));
    }

    /**
     * Deletes the pipeline ({@code DELETE /projects/{id}/pipelines/{pipeline_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().pipelines().delete(project.id(), id());
    }

    /**
     * Loads the pipeline again ({@code GET /projects/{id}/pipelines/{pipeline_id}}). This object is not changed.
     *
     * @return the pipeline as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the pipeline
     * @throws IOException if the transport fails
     */
    public Pipeline refresh() throws IOException {
        return project.pipeline(id()).orElseThrow(() -> new IllegalStateException("Pipeline " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCiPipelineWithMetadata response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
