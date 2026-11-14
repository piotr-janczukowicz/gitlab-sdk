package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJob;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsCancelPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsPlayPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsRetryPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabCiJobArtifactEntry;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabProjectJobsArtifactsListTreeFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

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
     * Downloads the artifacts archive of the job into a file ({@code GET /projects/{id}/jobs/{job_id}/artifacts}). The
     * client's own authentication is used; the SDK offers no job-token parameter. The body is first written to a
     * temporary file in the directory of {@code target} and moved onto {@code target} (replacing an existing file) only
     * when it was received completely, so {@code target} never holds a partial download; the temporary file is deleted
     * on failure and nothing is created for an unsuccessful response. The directory of {@code target} must exist. Where
     * the file system has POSIX permissions the file is created owner-only. Artifacts may contain secrets; the SDK
     * writes only where the caller says.
     *
     * @param target the file to write
     * @return {@code target}
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code target} has no parent directory
     * @throws IOException if the transport fails or the file cannot be written
     */
    public Path downloadArtifacts(final Path target) throws IOException {
        return gitLab().jobsartifacts().get(project.id(), id(), requireNonNull(target, "target"));
    }

    /**
     * Downloads the artifacts archive of the job into a stream ({@code GET /projects/{id}/jobs/{job_id}/artifacts}).
     * The client's own authentication is used; the SDK offers no job-token parameter. The body is copied without
     * buffering it. The stream is the caller's: the SDK never closes it. When the transfer fails the stream may hold a
     * partial body. Artifacts may contain secrets; the SDK writes only where the caller says.
     *
     * @param out the stream to write to
     * @return the number of bytes written
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or the stream cannot be written
     */
    public long downloadArtifacts(final OutputStream out) throws IOException {
        return gitLab().jobsartifacts().get(project.id(), id(), requireNonNull(out, "out"));
    }

    /**
     * Lists the root of the artifacts of the job ({@code GET /projects/{id}/jobs/{job_id}/artifacts/tree}). The core
     * fetches the first page eagerly when this method is called, so a failure of that request is thrown here; later
     * pages are fetched as the stream is consumed. The entries are the core's records, and no filter is sent.
     *
     * @return the entries
     * @throws IOException if the transport fails
     */
    public Stream<GitLabCiJobArtifactEntry> artifactTree() throws IOException {
        return gitLab().jobsartifacts().listTree(project.id(), id()).stream();
    }

    /**
     * Lists a directory of the artifacts of the job ({@code GET
     * /projects/{id}/jobs/{job_id}/artifacts/tree?path=...&recursive=...}). The core fetches the first page eagerly
     * when this method is called, so a failure of that request is thrown here; later pages are fetched as the stream is
     * consumed. Use {@link #artifactTree()} for the root.
     *
     * @param path the directory within the artifacts; not blank
     * @param recursive whether to list nested directories too
     * @return the entries
     * @throws IllegalArgumentException if {@code path} is blank
     * @throws IOException if the transport fails
     */
    public Stream<GitLabCiJobArtifactEntry> artifactTree(final String path, final boolean recursive)
            throws IOException {
        requireNotBlank(path, "path");
        return gitLab()
                .jobsartifacts()
                .listTree(
                        project.id(),
                        id(),
                        GitLabProjectJobsArtifactsListTreeFilterBuilder.builder()
                                .path(path)
                                .recursive(recursive)
                                .build())
                .stream();
    }

    /**
     * Keeps the artifacts of the job, which otherwise expire ({@code POST
     * /projects/{id}/jobs/{job_id}/artifacts/keep}). This object is not changed.
     *
     * @return the job as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Job keepArtifacts() throws IOException {
        return new Job(project, gitLab().jobsartifacts().keep(project.id(), id()));
    }

    /**
     * Deletes the artifacts of the job ({@code DELETE /projects/{id}/jobs/{job_id}/artifacts}). The job and its trace
     * stay; see {@link #erase()} to remove those too.
     *
     * @throws IOException if the transport fails
     */
    public void deleteArtifacts() throws IOException {
        gitLab().jobsartifacts().delete(project.id(), id());
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
