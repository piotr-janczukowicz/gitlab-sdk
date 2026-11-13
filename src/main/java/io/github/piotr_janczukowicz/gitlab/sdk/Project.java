package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsFilter;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesListProjectFilter;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabPublicGroupDetails;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Job;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.code.RepositoryFile;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.FileForms;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A project, identified by its numeric id or plain full path, such as {@code my-group/my-project} (the core encodes
 * it). A handle from {@link GitLabSdk#project(String)} holds no data from GitLab; {@link #fetch()} returns a loaded
 * project. It is the starting point for the operations that belong to a project. The object is immutable: every
 * operation returns a new project built from GitLab's answer.
 */
public final class Project {

    private static final String IS_GONE = " is gone";

    static {
        Internals.registerProjectAccess(project -> project.sdk.gitLab());
        Internals.registerSiblingAccess((project, otherId) -> project.sdk.project(otherId));
    }

    private final GitLabSdk sdk;
    private final String id;
    private final @Nullable GitLabProjectWithAccessAndCatalog response;

    Project(final GitLabSdk sdk, final String id, final @Nullable GitLabProjectWithAccessAndCatalog response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.id = requireNotBlank(id, "id");
        this.response = response;
    }

    /**
     * Returns the id this object was addressed with.
     *
     * @return the numeric id or the plain full path
     */
    public String id() {
        return id;
    }

    /**
     * Loads the project ({@code GET /projects/{id}}). This object is not changed.
     *
     * @return the loaded project, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Project> fetch() throws IOException {
        return sdk.gitLab().projects().project().get(id).map(loaded -> new Project(sdk, id, loaded));
    }

    /**
     * Reloads the project ({@code GET /projects/{id}}). This object is not changed.
     *
     * @return the reloaded project
     * @throws IllegalStateException if GitLab no longer has the project
     * @throws IOException if the transport fails
     */
    public Project refresh() throws IOException {
        return fetch().orElseThrow(() -> new IllegalStateException("Project " + id + IS_GONE));
    }

    /**
     * Starts a change of the name, path, description, visibility or default branch. Nothing is sent until
     * {@link ProjectUpdate#send()}.
     *
     * @return the update builder
     */
    public ProjectUpdate update() {
        return new ProjectUpdate(sdk, id);
    }

    /**
     * Archives the project ({@code POST /projects/{id}/archive}) and loads it again.
     *
     * @return the archived project as GitLab returns it afterwards
     * @throws IllegalStateException if the project is gone when it is loaded again after the change; a project that is
     *     already gone fails the {@code POST} first, with the core's exception
     * @throws IOException if the transport fails
     */
    public Project archive() throws IOException {
        sdk.gitLab().projects().project().archive(id);
        return refresh();
    }

    /**
     * Unarchives the project ({@code POST /projects/{id}/unarchive}) and loads it again.
     *
     * @return the unarchived project as GitLab returns it afterwards
     * @throws IllegalStateException if the project is gone when it is loaded again after the change; a project that is
     *     already gone fails the {@code POST} first, with the core's exception
     * @throws IOException if the transport fails
     */
    public Project unarchive() throws IOException {
        sdk.gitLab().projects().project().unarchive(id);
        return refresh();
    }

    /**
     * Deletes the project ({@code DELETE /projects/{id}}). GitLab may only mark it for deletion, depending on the
     * instance settings.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        sdk.gitLab().projects().project().delete(id);
    }

    /**
     * Transfers the project to another namespace ({@code PUT /projects/{id}/transfer}) and loads it ({@code GET
     * /projects/{id}}) by the id in GitLab's answer, because the answer is a smaller record. The id of the returned
     * project is numeric.
     *
     * @param namespace the id or path of the target namespace
     * @return the moved project as GitLab returns it afterwards
     * @throws NullPointerException if {@code namespace} is {@code null}
     * @throws IllegalArgumentException if {@code namespace} is blank
     * @throws IllegalStateException if the project is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Project transfer(final String namespace) throws IOException {
        final GitLabProjectProjectsTransferPayload payload =
                new GitLabProjectProjectsTransferPayload(requireNotBlank(namespace, "namespace"));
        return reload(sdk.gitLab().projects().project().transfer(id, payload).id());
    }

    /**
     * Forks the project into a namespace ({@code POST /projects/{id}/fork}) and loads the fork ({@code GET
     * /projects/{id}}) by the id in GitLab's answer, because the answer is a smaller record. GitLab creates the fork
     * asynchronously, so it may still be importing.
     *
     * @param namespacePath the path of the target namespace
     * @return the fork
     * @throws NullPointerException if {@code namespacePath} is {@code null}
     * @throws IllegalArgumentException if {@code namespacePath} is blank
     * @throws IllegalStateException if the fork is gone when it is loaded
     * @throws IOException if the transport fails
     */
    public Project fork(final String namespacePath) throws IOException {
        final GitLabProjectProjectForksCreatePayload payload = GitLabProjectProjectForksCreatePayloadBuilder.builder()
                .namespacePath(requireNotBlank(namespacePath, "namespacePath"))
                .build();
        return reload(sdk.gitLab().projectforks().create(id, payload).id());
    }

    /**
     * Lists the forks of the project ({@code GET /projects/{id}/forks}). GitLab lists a summary of each fork, so each
     * one is loaded ({@code GET /projects/{id}}) as the stream reaches it, and one that is gone by then is skipped.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the forks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Project> forks() throws IOException {
        return sdk.gitLab().projectforks().list(id).stream()
                .map(listed -> String.valueOf(requireNonNull(listed.id(), "id")))
                .flatMap(forkId -> Lookups.find(() -> sdk.project(forkId).fetch()).stream());
    }

    /**
     * Removes the fork relationship of the project ({@code DELETE /projects/{id}/fork}); the project stays.
     *
     * @throws IOException if the transport fails
     */
    public void removeForkRelationship() throws IOException {
        sdk.gitLab().projectforks().delete(id);
    }

    /**
     * Stops sharing the project with a group ({@code DELETE /projects/{id}/share/{group_id}}).
     *
     * @param groupId the id of the group the sharing is removed for
     * @throws IOException if the transport fails
     */
    public void unshare(final long groupId) throws IOException {
        sdk.gitLab().projects().project().deleteShare(id, groupId);
    }

    /**
     * Lists the groups the project is shared with ({@code GET /projects/{id}/groups}); these are the records of the
     * core, which carry the group's id, name and path. Pages are fetched only as the stream is consumed; transport
     * failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the groups, as the core returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabPublicGroupDetails> sharedGroups() throws IOException {
        return sdk.gitLab().projects().project().listGroups(id).stream();
    }

    /**
     * Lists the issues of the project ({@code GET /projects/{id}/issues}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return issues(GitLabProjectIssuesListFilter.empty());
    }

    /**
     * Lists the issues of the project that match a filter ({@code GET /projects/{id}/issues}). Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the query parameters of the core
     * @return the issues
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues(final GitLabProjectIssuesListFilter filter) throws IOException {
        return sdk.gitLab().issues().project().list(id, filter).stream().map(listed -> new Issue(this, listed));
    }

    /**
     * Loads an issue ({@code GET /projects/{id}/issues/{issue_iid}}).
     *
     * @param iid the issue number within the project
     * @return the issue, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Issue> issue(final long iid) throws IOException {
        return sdk.gitLab().issues().project().get(id, iid).map(loaded -> new Issue(this, loaded));
    }

    /**
     * Creates an issue with a title ({@code POST /projects/{id}/issues}).
     *
     * @param title the issue title
     * @return the created issue
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final String title) throws IOException {
        return createIssue(GitLabProjectIssuesCreatePayloadBuilder.builder()
                .title(requireNotBlank(title, "title"))
                .build());
    }

    /**
     * Creates an issue from the payload of the core ({@code POST /projects/{id}/issues}).
     *
     * @param payload the payload
     * @return the created issue
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final GitLabProjectIssuesCreatePayload payload) throws IOException {
        return new Issue(this, sdk.gitLab().issues().project().create(id, payload));
    }

    /**
     * Lists the merge requests of the project ({@code GET /projects/{id}/merge_requests}). GitLab lists a summary of
     * each merge request, so each one is loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) as the
     * stream reaches it, and one that is gone by then is skipped. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return mergeRequests(GitLabProjectMergeRequestsListFilter.empty());
    }

    /**
     * Lists the merge requests of the project that match a filter ({@code GET /projects/{id}/merge_requests}). Each one
     * is loaded as the stream reaches it, and one that is gone by then is skipped, as for {@link #mergeRequests()}.
     *
     * @param filter the query parameters of the core
     * @return the merge requests
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests(final GitLabProjectMergeRequestsListFilter filter) throws IOException {
        return sdk.gitLab().mergerequests().project().list(id, filter).stream()
                .map(listed -> Lookups.find(() -> mergeRequest(requireNonNull(listed.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Loads a merge request ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}).
     *
     * @param iid the merge request number within the project
     * @return the merge request, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<MergeRequest> mergeRequest(final long iid) throws IOException {
        return sdk.gitLab().mergerequests().project().get(id, iid).map(loaded -> new MergeRequest(this, loaded));
    }

    /**
     * Creates a merge request from the payload of the core ({@code POST /projects/{id}/merge_requests}).
     *
     * @param payload the payload
     * @return the created merge request
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public MergeRequest createMergeRequest(final GitLabProjectMergeRequestsCreatePayload payload) throws IOException {
        return new MergeRequest(this, sdk.gitLab().mergerequests().project().create(id, payload));
    }

    /**
     * Lists the pipelines of the project ({@code GET /projects/{id}/pipelines}). GitLab lists a summary of each
     * pipeline, so each one is loaded ({@code GET /projects/{id}/pipelines/{pipeline_id}}) as the stream reaches it,
     * and one that is gone by then is skipped. Pages are fetched only as the stream is consumed; transport failures
     * during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the pipelines
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Pipeline> pipelines() throws IOException {
        return loaded(sdk.gitLab().pipelines().listProject(id).stream());
    }

    /**
     * Lists the pipelines of the project that match a filter ({@code GET /projects/{id}/pipelines}). GitLab lists a
     * summary of each pipeline, so each one is loaded ({@code GET /projects/{id}/pipelines/{pipeline_id}}) as the
     * stream reaches it, and one that is gone by then is skipped. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter of the core client, for example by ref, status or user
     * @return the pipelines
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Pipeline> pipelines(final GitLabProjectPipelinesListProjectFilter filter) throws IOException {
        return loaded(sdk.gitLab().pipelines().listProject(id, requireNonNull(filter, "filter")).stream());
    }

    /**
     * Loads a pipeline ({@code GET /projects/{id}/pipelines/{pipeline_id}}).
     *
     * @param pipelineId the pipeline id
     * @return the pipeline, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Pipeline> pipeline(final long pipelineId) throws IOException {
        return sdk.gitLab().pipelines().get(id, pipelineId).map(loaded -> new Pipeline(this, loaded));
    }

    /**
     * Creates a pipeline for a branch or tag ({@code POST /projects/{id}/pipeline}) and loads it.
     *
     * @param ref the branch or tag to run the pipeline for
     * @return the created pipeline
     * @throws NullPointerException if {@code ref} is {@code null}
     * @throws IllegalArgumentException if {@code ref} is blank
     * @throws IllegalStateException if GitLab no longer has the pipeline after creating it
     * @throws IOException if the transport fails
     */
    public Pipeline createPipeline(final String ref) throws IOException {
        return createPipeline(GitLabProjectPipelinesCreatePayloadBuilder.builder()
                .ref(requireNotBlank(ref, "ref"))
                .build());
    }

    /**
     * Creates a pipeline from the payload of the core ({@code POST /projects/{id}/pipeline}) and loads it ({@code GET
     * /projects/{id}/pipelines/{pipeline_id}}), because GitLab answers the creation with a smaller record.
     *
     * @param payload the payload
     * @return the created pipeline
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IllegalStateException if GitLab no longer has the pipeline after creating it
     * @throws IOException if the transport fails
     */
    public Pipeline createPipeline(final GitLabProjectPipelinesCreatePayload payload) throws IOException {
        final long created =
                requireNonNull(sdk.gitLab().pipelines().create(id, payload).id(), "id");
        return pipeline(created).orElseThrow(() -> new IllegalStateException("Pipeline " + created + " is gone"));
    }

    /**
     * Lists the jobs of the project ({@code GET /projects/{id}/jobs}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the jobs
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Job> jobs() throws IOException {
        return sdk.gitLab().jobs().project().listJobs(id).stream().map(listed -> new Job(this, listed));
    }

    /**
     * Lists the jobs of the project that match a filter ({@code GET /projects/{id}/jobs}). Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter of the core client, by scope and ref
     * @return the jobs
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Job> jobs(final GitLabProjectJobsListJobsFilter filter) throws IOException {
        return sdk.gitLab().jobs().project().listJobs(id, requireNonNull(filter, "filter")).stream()
                .map(listed -> new Job(this, listed));
    }

    /**
     * Loads a job ({@code GET /projects/{id}/jobs/{job_id}}).
     *
     * @param jobId the job id
     * @return the job, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Job> job(final long jobId) throws IOException {
        return sdk.gitLab().jobs().project().get(id, jobId).map(loaded -> new Job(this, loaded));
    }

    /**
     * Lists the labels of the project ({@code GET /projects/{id}/labels}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the labels
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Label> labels() throws IOException {
        return sdk.gitLab().labels().list(id).stream().map(listed -> new Label(this, listed));
    }

    /**
     * Creates a label ({@code POST /projects/{id}/labels}).
     *
     * @param name the label name
     * @param color the color, as a hexadecimal value with a leading {@code #} or a CSS color name
     * @return the created label
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Label createLabel(final String name, final String color) throws IOException {
        final GitLabProjectLabelsCreatePayload payload = GitLabProjectLabelsCreatePayloadBuilder.builder()
                .name(requireNotBlank(name, "name"))
                .color(requireNotBlank(color, "color"))
                .build();
        return new Label(this, sdk.gitLab().labels().create(id, payload));
    }

    /**
     * Loads a file of the repository at a ref ({@code GET /projects/{id}/repository/files/{file_path}?ref=...}). The
     * path is passed to the core as it is; the core encodes it.
     *
     * @param path the path of the file within the repository, for example {@code src/main/App.java}
     * @param ref the branch, tag or commit to read the file at
     * @return the file, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Optional<RepositoryFile> file(final String path, final String ref) throws IOException {
        return sdk.gitLab().repositoryfiles().get(id, path, ref).map(loaded -> new RepositoryFile(this, ref, loaded));
    }

    /**
     * Creates a file by a commit to a branch ({@code POST /projects/{id}/repository/files/{file_path}}) and loads it
     * ({@code GET /projects/{id}/repository/files/{file_path}?ref=...}), because GitLab answers the creation with a
     * smaller record.
     *
     * @param path the path of the new file within the repository
     * @param branch the branch to commit to
     * @param content the content, sent as UTF-8 text; may be empty
     * @param message the commit message
     * @return the created file, read at {@code branch}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument other than {@code content} is blank
     * @throws IllegalStateException if GitLab no longer has the file after creating it
     * @throws IOException if the transport fails
     */
    public RepositoryFile createFile(final String path, final String branch, final String content, final String message)
            throws IOException {
        sdk.gitLab().repositoryfiles().create(id, path, FileForms.of(branch, content, message));
        return file(path, branch).orElseThrow(() -> new IllegalStateException("File " + path + IS_GONE));
    }

    /**
     * Creates a file with binary content by a commit to a branch ({@code POST
     * /projects/{id}/repository/files/{file_path}}, the content Base64-encoded with {@code encoding=base64}) and loads
     * it ({@code GET /projects/{id}/repository/files/{file_path}?ref=...}), because GitLab answers the creation with a
     * smaller record.
     *
     * @param path the path of the new file within the repository
     * @param branch the branch to commit to
     * @param content the content; may be empty
     * @param message the commit message
     * @return the created file, read at {@code branch}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument other than {@code content} is blank
     * @throws IllegalStateException if GitLab no longer has the file after creating it
     * @throws IOException if the transport fails
     */
    public RepositoryFile createFile(final String path, final String branch, final byte[] content, final String message)
            throws IOException {
        sdk.gitLab().repositoryfiles().create(id, path, FileForms.of(branch, content, message));
        return file(path, branch).orElseThrow(() -> new IllegalStateException("File " + path + IS_GONE));
    }

    /**
     * Lists the branches of the project ({@code GET /projects/{id}/repository/branches}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the branches
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Branch> branches() throws IOException {
        return sdk.gitLab().branches().list(id).stream().map(listed -> new Branch(this, listed));
    }

    /**
     * Loads a branch ({@code GET /projects/{id}/repository/branches/{branch}}).
     *
     * @param name the branch name
     * @return the branch, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Branch> branch(final String name) throws IOException {
        return sdk.gitLab().branches().get(id, name).map(loaded -> new Branch(this, loaded));
    }

    /**
     * Creates a branch from a ref ({@code POST /projects/{id}/repository/branches}).
     *
     * @param name the name of the new branch
     * @param ref the branch, tag or commit to create it from
     * @return the created branch
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Branch createBranch(final String name, final String ref) throws IOException {
        final GitLabProjectBranchesCreatePayload payload = GitLabProjectBranchesCreatePayloadBuilder.builder()
                .branch(requireNotBlank(name, "name"))
                .ref(requireNotBlank(ref, "ref"))
                .build();
        return new Branch(this, sdk.gitLab().branches().create(id, payload));
    }

    /**
     * Lists the commits of the project ({@code GET /projects/{id}/repository/commits}). GitLab lists a summary of each
     * commit, so each one is loaded ({@code GET /projects/{id}/repository/commits/{sha}}) as the stream reaches it, and
     * one that is gone by then is skipped. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the commits
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits() throws IOException {
        return sdk.gitLab().commits().list(id).stream()
                .map(listed -> Lookups.find(() -> commit(requireNonNull(listed.id(), "id"))))
                .flatMap(Optional::stream);
    }

    /**
     * Loads a commit ({@code GET /projects/{id}/repository/commits/{sha}}).
     *
     * @param sha the commit SHA, or a branch or tag name
     * @return the commit, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Commit> commit(final String sha) throws IOException {
        return sdk.gitLab().commits().get(id, sha).map(loaded -> new Commit(this, loaded));
    }

    /**
     * Returns the complete response of a loaded project.
     *
     * @return the response record
     * @throws IllegalStateException if this is a handle that was not loaded with {@link #fetch()}
     */
    public GitLabProjectWithAccessAndCatalog response() {
        if (response == null) {
            throw new IllegalStateException("Project " + id + " is not loaded; call fetch()");
        }
        return response;
    }

    private Project reload(final @Nullable Long newId) throws IOException {
        final String loadId = String.valueOf(requireNonNull(newId, "id"));
        return sdk.project(loadId).fetch().orElseThrow(() -> new IllegalStateException("Project " + loadId + IS_GONE));
    }

    private Stream<Pipeline> loaded(final Stream<GitLabCiPipelineBasic> listed) {
        return listed.map(item -> Lookups.find(() -> pipeline(requireNonNull(item.id(), "id"))))
                .flatMap(Optional::stream);
    }
}
