package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsFilter;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesListProjectFilter;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsShareGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsSharePayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabPublicGroupDetails;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesCreateMergeAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesCreatePushAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProjectProtectedTagsCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProjectProtectedTagsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Job;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.code.RepositoryFile;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.CreatedDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployKey;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.FileForms;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.members.Member;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedTag;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.Webhook;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhookEvent;
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
    private static final String NAME = "name";

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
        return loadById(sdk.gitLab().projects().project().transfer(id, payload).id());
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
        return loadById(sdk.gitLab().projectforks().create(id, payload).id());
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
     * Shares the project with a group ({@code POST /projects/{id}/share}).
     *
     * @param groupId the id of the group to share with
     * @param level the access level the group's members get in the project
     * @return the sharing as GitLab answers it
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IOException if the transport fails
     */
    public ProjectGroupLink share(final long groupId, final AccessLevel level) throws IOException {
        final GitLabProjectProjectsSharePayload payload = new GitLabProjectProjectsSharePayload(
                groupId,
                GitLabProjectProjectsShareGroupAccess.fromWire(
                        requireNonNull(level, "level").value()),
                null,
                null);
        return new ProjectGroupLink(sdk.gitLab().projects().project().share(id, payload));
    }

    /**
     * Lists the direct members of the project ({@code GET /projects/{id}/members}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Member> members() throws IOException {
        return sdk.gitLab().members().project().list(id).stream().map(listed -> new Member(this, listed));
    }

    /**
     * Lists the members of the project including those inherited from its groups ({@code GET
     * /projects/{id}/members/all}). Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Member> allMembers() throws IOException {
        return sdk.gitLab().members().project().listAll(id).stream().map(listed -> new Member(this, listed));
    }

    /**
     * Loads a direct member ({@code GET /projects/{id}/members/{user_id}}).
     *
     * @param userId the numeric user id
     * @return the member, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Member> member(final long userId) throws IOException {
        return sdk.gitLab().members().project().get(id, userId).map(loaded -> new Member(this, loaded));
    }

    /**
     * Adds a user as a member ({@code POST /projects/{id}/members}).
     *
     * @param userId the numeric user id
     * @param level the access level to grant
     * @return the new member
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IOException if the transport fails
     */
    public Member addMember(final long userId, final AccessLevel level) throws IOException {
        return createMember(String.valueOf(userId), null, level);
    }

    /**
     * Adds a user as a member by username ({@code POST /projects/{id}/members}).
     *
     * @param username the username
     * @param level the access level to grant
     * @return the new member
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code username} is blank
     * @throws IOException if the transport fails
     */
    public Member addMember(final String username, final AccessLevel level) throws IOException {
        return createMember(null, requireNotBlank(username, "username"), level);
    }

    /**
     * Requests access to the project for the user of the client ({@code POST /projects/{id}/access_requests}).
     *
     * @return the request
     * @throws IOException if the transport fails
     */
    public AccessRequest requestAccess() throws IOException {
        return new AccessRequest(this, sdk.gitLab().accessrequests().project().create(id));
    }

    /**
     * Lists the pending access requests of the project ({@code GET /projects/{id}/access_requests}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<AccessRequest> accessRequests() throws IOException {
        return sdk.gitLab().accessrequests().project().list(id).stream().map(listed -> new AccessRequest(this, listed));
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
                .name(requireNotBlank(name, NAME))
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
                .branch(requireNotBlank(name, NAME))
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

    /**
     * Lists the webhooks of the project ({@code GET /projects/{id}/hooks}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the webhooks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Webhook> webhooks() throws IOException {
        return sdk.gitLab().projectwebhooks().list(id).stream().map(listed -> new Webhook(this, listed));
    }

    /**
     * Loads a webhook ({@code GET /projects/{id}/hooks/{hook_id}}).
     *
     * @param hookId the id of the webhook
     * @return the webhook, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Webhook> webhook(final long hookId) throws IOException {
        return sdk.gitLab().projectwebhooks().get(id, hookId).map(loaded -> new Webhook(this, loaded));
    }

    /**
     * Creates a webhook that sends to a URL ({@code POST /projects/{id}/hooks}). GitLab's defaults decide which events
     * it is switched on for; change them with {@link Webhook#update()}.
     *
     * @param url the URL to send the events to
     * @return the new webhook
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     * @throws IOException if the transport fails
     */
    public Webhook createWebhook(final String url) throws IOException {
        return createWebhook(GitLabProjectProjectWebhooksCreatePayloadBuilder.builder()
                .url(requireNotBlank(url, "url"))
                .build());
    }

    /**
     * Creates a webhook from the core's payload ({@code POST /projects/{id}/hooks}), for the fields the SDK does not
     * wrap, such as the events outside {@link WebhookEvent}, URL variables and custom headers.
     *
     * @param payload the request body
     * @return the new webhook
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Webhook createWebhook(final GitLabProjectProjectWebhooksCreatePayload payload) throws IOException {
        return new Webhook(this, sdk.gitLab().projectwebhooks().create(id, requireNonNull(payload, "payload")));
    }

    /**
     * Lists the deploy keys of the project ({@code GET /projects/{id}/deploy_keys}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the deploy keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<DeployKey> deployKeys() throws IOException {
        return sdk.gitLab().deploykeys().project().list(id).stream().map(listed -> new DeployKey(this, listed));
    }

    /**
     * Loads a deploy key ({@code GET /projects/{id}/deploy_keys/{key_id}}).
     *
     * @param keyId the id of the key
     * @return the key, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<DeployKey> deployKey(final long keyId) throws IOException {
        return sdk.gitLab().deploykeys().project().get(id, keyId).map(loaded -> new DeployKey(this, loaded));
    }

    /**
     * Adds a deploy key that cannot push ({@code POST /projects/{id}/deploy_keys}).
     *
     * @param title the title of the key
     * @param key the public key
     * @return the new deploy key
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public DeployKey addDeployKey(final String title, final String key) throws IOException {
        return createDeployKey(GitLabProjectDeployKeysCreatePayloadBuilder.builder(), title, key);
    }

    /**
     * Adds a deploy key ({@code POST /projects/{id}/deploy_keys}).
     *
     * @param title the title of the key
     * @param key the public key
     * @param canPush {@code true} to let the key push to the repository
     * @return the new deploy key
     * @throws NullPointerException if {@code title} or {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code key} is blank
     * @throws IOException if the transport fails
     */
    public DeployKey addDeployKey(final String title, final String key, final boolean canPush) throws IOException {
        return createDeployKey(
                GitLabProjectDeployKeysCreatePayloadBuilder.builder().canPush(canPush), title, key);
    }

    /**
     * Enables a deploy key of another project for this project ({@code POST
     * /projects/{id}/deploy_keys/{key_id}/enable}) and loads it again, because the core answers with a different
     * record.
     *
     * @param keyId the id of the key
     * @return the key as GitLab holds it for this project
     * @throws IllegalStateException if the key is gone after it was enabled
     * @throws IOException if the transport fails
     */
    public DeployKey enableDeployKey(final long keyId) throws IOException {
        sdk.gitLab().deploykeys().project().enable(id, keyId);
        return deployKey(keyId).orElseThrow(() -> new IllegalStateException("Deploy key " + keyId + IS_GONE));
    }

    /**
     * Lists the deploy tokens of the project ({@code GET /projects/{id}/deploy_tokens}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the deploy tokens
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<DeployToken> deployTokens() throws IOException {
        return sdk.gitLab().deploytokens().project().list(id).stream().map(listed -> new DeployToken(this, listed));
    }

    /**
     * Loads a deploy token ({@code GET /projects/{id}/deploy_tokens/{token_id}}).
     *
     * @param tokenId the id of the token
     * @return the token, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<DeployToken> deployToken(final long tokenId) throws IOException {
        return sdk.gitLab().deploytokens().project().get(id, tokenId).map(loaded -> new DeployToken(this, loaded));
    }

    /**
     * Creates a deploy token ({@code POST /projects/{id}/deploy_tokens}). The secret of the token is in the answer
     * only; GitLab never returns it again.
     *
     * @param name the name of the token
     * @param scopes what the token may be used for
     * @return the new token with its secret
     * @throws NullPointerException if {@code name} or a scope is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank, or no scope is given
     * @throws IOException if the transport fails
     */
    public CreatedDeployToken createDeployToken(final String name, final DeployTokenScope @Nullable ... scopes)
            throws IOException {
        requireNotBlank(name, NAME);
        if (scopes == null || scopes.length == 0) {
            throw new IllegalArgumentException("At least one scope is required");
        }
        final GitLabProjectDeployTokensCreatePayload payload = GitLabProjectDeployTokensCreatePayloadBuilder.builder()
                .name(name)
                .scopes(Stream.of(scopes)
                        .map(scope -> GitLabProjectDeployTokensCreateScopes.fromWire(
                                requireNonNull(scope, "scope").wireValue()))
                        .toList())
                .build();
        return new CreatedDeployToken(
                this, sdk.gitLab().deploytokens().project().create(id, payload));
    }

    /**
     * Lists the protected branches of the project ({@code GET /projects/{id}/protected_branches}). Pages are fetched
     * only as the stream is consumed; transport failures during consumption surface as
     * {@link java.io.UncheckedIOException}.
     *
     * @return the protected branches
     * @throws IOException if the transport fails
     */
    public Stream<ProtectedBranch> protectedBranches() throws IOException {
        return sdk.gitLab().protectedbranches().list(id).stream().map(listed -> new ProtectedBranch(this, listed));
    }

    /**
     * Loads a protected branch ({@code GET /projects/{id}/protected_branches/{name}}).
     *
     * @param name the branch name or wildcard, which may contain slashes; the core encodes it
     * @return the protected branch, or empty when GitLab answers HTTP 404
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if the name is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<ProtectedBranch> protectedBranch(final String name) throws IOException {
        return sdk.gitLab()
                .protectedbranches()
                .get(id, requireNotBlank(name, NAME))
                .map(loaded -> new ProtectedBranch(this, loaded));
    }

    /**
     * Protects a branch or wildcard with the default rules of GitLab ({@code POST /projects/{id}/protected_branches});
     * only the name is sent. The v1 {@code Branch.protect()} stays the quick way for an existing branch.
     *
     * @param name the branch name or wildcard, which may contain slashes; the core encodes it
     * @return the protected branch
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if the name is {@code null}
     * @throws IOException if the transport fails
     */
    public ProtectedBranch protectBranch(final String name) throws IOException {
        final GitLabProjectProtectedBranchesCreatePayloadBuilder builder =
                GitLabProjectProtectedBranchesCreatePayloadBuilder.builder().name(requireNotBlank(name, NAME));
        return new ProtectedBranch(this, sdk.gitLab().protectedbranches().create(id, builder.build()));
    }

    /**
     * Protects a branch or wildcard and says who may push and merge ({@code POST /projects/{id}/protected_branches}).
     *
     * @param name the branch name or wildcard, which may contain slashes; the core encodes it
     * @param push who may push
     * @param merge who may merge
     * @return the protected branch
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if an argument is {@code null}
     * @throws IOException if the transport fails
     */
    public ProtectedBranch protectBranch(final String name, final ProtectedAccess push, final ProtectedAccess merge)
            throws IOException {
        requireNotBlank(name, NAME);
        final GitLabProjectProtectedBranchesCreatePayloadBuilder builder =
                GitLabProjectProtectedBranchesCreatePayloadBuilder.builder()
                        .name(name)
                        .pushAccessLevel(GitLabProjectProtectedBranchesCreatePushAccessLevel.fromWire(
                                requireNonNull(push, "push").value()))
                        .mergeAccessLevel(GitLabProjectProtectedBranchesCreateMergeAccessLevel.fromWire(
                                requireNonNull(merge, "merge").value()));
        return new ProtectedBranch(this, sdk.gitLab().protectedbranches().create(id, builder.build()));
    }

    /**
     * Lists the protected tags of the project ({@code GET /projects/{id}/protected_tags}). Pages are fetched only as
     * the stream is consumed; transport failures during consumption surface as {@link java.io.UncheckedIOException}.
     *
     * @return the protected tags
     * @throws IOException if the transport fails
     */
    public Stream<ProtectedTag> protectedTags() throws IOException {
        return sdk.gitLab().protectedtags().list(id).stream().map(listed -> new ProtectedTag(this, listed));
    }

    /**
     * Loads a protected tag ({@code GET /projects/{id}/protected_tags/{name}}).
     *
     * @param name the tag name or wildcard; the core encodes it
     * @return the protected tag, or empty when GitLab answers HTTP 404
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if the name is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<ProtectedTag> protectedTag(final String name) throws IOException {
        return sdk.gitLab()
                .protectedtags()
                .get(id, requireNotBlank(name, NAME))
                .map(loaded -> new ProtectedTag(this, loaded));
    }

    /**
     * Protects a tag or wildcard ({@code POST /projects/{id}/protected_tags}).
     *
     * @param name the tag name or wildcard; the core encodes it
     * @param create who may create matching tags
     * @return the protected tag
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if an argument is {@code null}
     * @throws IOException if the transport fails
     */
    public ProtectedTag protectTag(final String name, final ProtectedAccess create) throws IOException {
        requireNotBlank(name, NAME);
        final GitLabProjectProtectedTagsCreatePayloadBuilder builder =
                GitLabProjectProtectedTagsCreatePayloadBuilder.builder()
                        .name(name)
                        .createAccessLevel(GitLabProjectProtectedTagsCreateAccessLevel.fromWire(
                                requireNonNull(create, "create").value()));
        return new ProtectedTag(this, sdk.gitLab().protectedtags().create(id, builder.build()));
    }

    private DeployKey createDeployKey(
            final GitLabProjectDeployKeysCreatePayloadBuilder builder, final String title, final String key)
            throws IOException {
        final GitLabProjectDeployKeysCreatePayload payload = builder.title(requireNotBlank(title, "title"))
                .key(requireNotBlank(key, "key"))
                .build();
        return new DeployKey(this, sdk.gitLab().deploykeys().project().create(id, payload));
    }

    private Project loadById(final @Nullable Long newId) throws IOException {
        final String loadId = String.valueOf(requireNonNull(newId, "id"));
        return sdk.project(loadId).fetch().orElseThrow(() -> new IllegalStateException("Project " + loadId + IS_GONE));
    }

    private Stream<Pipeline> loaded(final Stream<GitLabCiPipelineBasic> listed) {
        return listed.map(item -> Lookups.find(() -> pipeline(requireNonNull(item.id(), "id"))))
                .flatMap(Optional::stream);
    }

    private Member createMember(final @Nullable String userId, final @Nullable String username, final AccessLevel level)
            throws IOException {
        final GitLabProjectMembersCreatePayload payload = GitLabProjectMembersCreatePayloadBuilder.builder()
                .userId(userId)
                .username(username)
                .accessLevel(requireNonNull(level, "level").value())
                .build();
        return new Member(this, sdk.gitLab().members().project().create(id, payload));
    }
}
