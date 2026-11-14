package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.FILTER;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.IS_GONE;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.NAME;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.loadedRunners;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDeployment;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabProjectContainerRegistryListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsCreateStatus;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsFilter;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabProjectPackagesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesListProjectFilter;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineSchedule;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabProjectPipelineTriggersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabProjectPipelineTriggersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesGetFilter;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesGetFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListFilter;
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
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersListFilter;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsListFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Job;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.code.RepositoryFile;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Tag;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.CreatedDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployKey;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.Deployment;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.DeploymentStatus;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.Environment;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.FileForms;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.members.Member;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.milestones.Milestone;
import io.github.piotr_janczukowicz.gitlab.sdk.packages.ProjectPackage;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedTag;
import io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryRepository;
import io.github.piotr_janczukowicz.gitlab.sdk.releases.Release;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnerToken;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.PipelineSchedule;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.PipelineTrigger;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.Variable;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.Webhook;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhookEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
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

    private static final String PAYLOAD = "payload";
    private static final String KEY = "key";
    private static final String TAG_NAME = "tagName";
    private static final String REF = "ref";

    static {
        Internals.registerProjectAccess(project -> project.sdk);
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
     * Shares the project with a group ({@code POST /projects/{id}/share}).
     *
     * @param groupId the id of the group to share with
     * @param level the access level the group's members get in the project; {@link AccessLevel#MINIMAL_ACCESS} is not
     *     accepted, because it exists for groups only and the core's project-share request has no such level
     * @return the sharing as GitLab answers it
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IllegalArgumentException if {@code level} is {@link AccessLevel#MINIMAL_ACCESS}; no request is made
     * @throws IOException if the transport fails
     */
    public ProjectGroupLink share(final long groupId, final AccessLevel level) throws IOException {
        if (requireNonNull(level, "level") == AccessLevel.MINIMAL_ACCESS) {
            throw new IllegalArgumentException("A project cannot be shared with access level " + level);
        }
        final GitLabProjectProjectsSharePayload payload = new GitLabProjectProjectsSharePayload(
                groupId, GitLabProjectProjectsShareGroupAccess.fromWire(level.value()), null, null);
        return new ProjectGroupLink(this, sdk.gitLab().projects().project().share(id, payload));
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
     * <p>A listed member may be a member only through a parent group. Its {@code refresh()} loads it from this list
     * ({@code GET /projects/{id}/members/all/{user_id}}); its {@code update()} and {@code remove()} act on direct
     * memberships only and fail with the core's exception for GitLab's HTTP 404 when the membership is inherited.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Member> allMembers() throws IOException {
        return sdk.gitLab().members().project().listAll(id).stream()
                .map(listed -> Internals.fromAllMembers(new Member(this, listed)));
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
     * Lists the labels of the project that match a filter ({@code GET /projects/{id}/labels}). Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (counts, ancestor groups, search, archived)
     * @return the labels
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Label> labels(final GitLabProjectLabelsListFilter filter) throws IOException {
        return sdk.gitLab().labels().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new Label(this, listed));
    }

    /**
     * Loads a label by its name ({@code GET /projects/{id}/labels/{name}}). A name with a slash or a space reaches the
     * core unchanged.
     *
     * @param name the label name
     * @return the label, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Label> label(final String name) throws IOException {
        return sdk.gitLab().labels().get(id, requireNotBlank(name, NAME)).map(loaded -> new Label(this, loaded));
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
        return loaded(sdk.gitLab().pipelines().listProject(id, requireNonNull(filter, FILTER)).stream());
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
                .ref(requireNotBlank(ref, REF))
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
        return sdk.gitLab().jobs().project().listJobs(id, requireNonNull(filter, FILTER)).stream()
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
     * Downloads the artifacts archive of the latest successful job of that name on a ref into a file ({@code GET
     * /projects/{id}/jobs/artifacts/{ref_name}/download?job=...}). The ref (a branch or tag name, possibly with a slash
     * such as {@code feature/x}) and the job name reach the core unchanged; the core encodes them. The client's own
     * authentication is used; the SDK offers no job-token parameter. The body is first written to a temporary file in
     * the directory of {@code target} and moved onto it (replacing an existing file) only when received completely, so
     * {@code target} never holds a partial download; the temporary file is deleted on failure and nothing is created
     * for an unsuccessful response. The directory must exist. Where the file system has POSIX permissions the file is
     * created owner-only. Artifacts may contain secrets; the SDK writes only where the caller says.
     *
     * @param ref the branch or tag name
     * @param jobName the name of the job
     * @param target the file to write
     * @return {@code target}
     * @throws IllegalArgumentException if {@code ref} or {@code jobName} is blank, or {@code target} has no parent
     *     directory
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IOException if the transport fails or the file cannot be written
     */
    public Path downloadArtifacts(final String ref, final String jobName, final Path target) throws IOException {
        requireNotBlank(ref, "ref");
        requireNotBlank(jobName, "jobName");
        return sdk.gitLab().jobs().project().getArtifactsDownload(id, ref, jobName, requireNonNull(target, "target"));
    }

    /**
     * Downloads the artifacts archive of the latest successful job of that name on a ref into a stream ({@code GET
     * /projects/{id}/jobs/artifacts/{ref_name}/download?job=...}). The ref and the job name reach the core unchanged.
     * The client's own authentication is used; the SDK offers no job-token parameter. The stream is the caller's: the
     * SDK never closes it, and it may hold a partial body when the transfer fails. Artifacts may contain secrets.
     *
     * @param ref the branch or tag name
     * @param jobName the name of the job
     * @param out the stream to write to
     * @return the number of bytes written
     * @throws IllegalArgumentException if {@code ref} or {@code jobName} is blank
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or the stream cannot be written
     */
    public long downloadArtifacts(final String ref, final String jobName, final OutputStream out) throws IOException {
        requireNotBlank(ref, "ref");
        requireNotBlank(jobName, "jobName");
        return sdk.gitLab().jobs().project().getArtifactsDownload(id, ref, jobName, requireNonNull(out, "out"));
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
                .ref(requireNotBlank(ref, REF))
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
        return new Webhook(this, sdk.gitLab().projectwebhooks().create(id, requireNonNull(payload, PAYLOAD)));
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
                        .map(scope -> GitLabProjectDeployTokensCreateScopes.valueOf(
                                requireNonNull(scope, "scope").name()))
                        .toList())
                .build();
        return new CreatedDeployToken(
                this, sdk.gitLab().deploytokens().project().create(id, payload));
    }

    /**
     * Lists the protected branches of the project ({@code GET /projects/{id}/protected_branches}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the protected branches
     * @throws IOException if the first page cannot be loaded
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
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the protected tags
     * @throws IOException if the first page cannot be loaded
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

    /**
     * Lists the tags of the project ({@code GET /projects/{id}/repository/tags}). Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the tags
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Tag> tags() throws IOException {
        return sdk.gitLab().tags().list(id).stream().map(listed -> new Tag(this, listed));
    }

    /**
     * Lists the tags of the project that match a filter ({@code GET /projects/{id}/repository/tags}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (sort, order, search)
     * @return the tags
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Tag> tags(final GitLabProjectTagsListFilter filter) throws IOException {
        return sdk.gitLab().tags().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new Tag(this, listed));
    }

    /**
     * Loads a tag ({@code GET /projects/{id}/repository/tags/{tag_name}}).
     *
     * @param name the tag name, which may contain slashes; the core encodes it
     * @return the tag, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Tag> tag(final String name) throws IOException {
        return sdk.gitLab().tags().get(id, requireNotBlank(name, NAME)).map(loaded -> new Tag(this, loaded));
    }

    /**
     * Creates a tag at a ref ({@code POST /projects/{id}/repository/tags}); the tag is lightweight.
     *
     * @param name the name of the new tag, which may contain slashes
     * @param ref the branch, tag or commit to tag
     * @return the created tag
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Tag createTag(final String name, final String ref) throws IOException {
        return createTag(name, ref, null);
    }

    /**
     * Creates an annotated tag at a ref ({@code POST /projects/{id}/repository/tags}).
     *
     * @param name the name of the new tag, which may contain slashes
     * @param ref the branch, tag or commit to tag
     * @param message the message of the annotated tag; {@code null} creates a lightweight tag
     * @return the created tag
     * @throws NullPointerException if {@code name} or {@code ref} is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code ref} is blank
     * @throws IOException if the transport fails
     */
    public Tag createTag(final String name, final String ref, final @Nullable String message) throws IOException {
        final GitLabProjectTagsCreatePayload payload = GitLabProjectTagsCreatePayloadBuilder.builder()
                .tagName(requireNotBlank(name, NAME))
                .ref(requireNotBlank(ref, REF))
                .message(message)
                .build();
        return new Tag(this, sdk.gitLab().tags().create(id, payload));
    }

    /**
     * Lists the releases of the project ({@code GET /projects/{id}/releases}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the releases
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Release> releases() throws IOException {
        return sdk.gitLab().releases().list(id).stream().map(listed -> new Release(this, listed));
    }

    /**
     * Lists the releases of the project that match a filter ({@code GET /projects/{id}/releases}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (sort, order)
     * @return the releases
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Release> releases(final GitLabProjectReleasesListFilter filter) throws IOException {
        return sdk.gitLab().releases().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new Release(this, listed));
    }

    /**
     * Loads the release of a tag ({@code GET /projects/{id}/releases/{tag_name}}).
     *
     * @param tagName the tag name, which may contain slashes; the core encodes it
     * @return the release, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code tagName} is {@code null}
     * @throws IllegalArgumentException if {@code tagName} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Release> release(final String tagName) throws IOException {
        return sdk.gitLab()
                .releases()
                .get(id, requireNotBlank(tagName, TAG_NAME))
                .map(loaded -> new Release(this, loaded));
    }

    /**
     * Creates a release for an existing tag ({@code POST /projects/{id}/releases}).
     *
     * @param tagName the name of the existing tag, which may contain slashes
     * @return the created release
     * @throws NullPointerException if {@code tagName} is {@code null}
     * @throws IllegalArgumentException if {@code tagName} is blank
     * @throws IOException if the transport fails
     */
    public Release createRelease(final String tagName) throws IOException {
        return createRelease(GitLabProjectReleasesCreatePayloadBuilder.builder()
                .tagName(requireNotBlank(tagName, TAG_NAME))
                .build());
    }

    /**
     * Creates a release and the tag it is made for ({@code POST /projects/{id}/releases}); GitLab creates the tag from
     * the ref when it does not exist yet.
     *
     * @param tagName the name of the tag, which may contain slashes
     * @param ref the branch, tag or commit the new tag is created from
     * @return the created release
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Release createRelease(final String tagName, final String ref) throws IOException {
        return createRelease(GitLabProjectReleasesCreatePayloadBuilder.builder()
                .tagName(requireNotBlank(tagName, TAG_NAME))
                .ref(requireNotBlank(ref, REF))
                .build());
    }

    /**
     * Creates a release from the core's payload ({@code POST /projects/{id}/releases}), for the fields the SDK does not
     * wrap, such as the name, the description, the milestones, the assets and the release time.
     *
     * @param payload the request body
     * @return the created release
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Release createRelease(final GitLabProjectReleasesCreatePayload payload) throws IOException {
        return new Release(this, sdk.gitLab().releases().create(id, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the milestones of the project ({@code GET /projects/{id}/milestones}). Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the milestones
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Milestone> milestones() throws IOException {
        return sdk.gitLab().projectmilestones().list(id).stream().map(listed -> new Milestone(this, listed));
    }

    /**
     * Lists the milestones of the project that match a filter ({@code GET /projects/{id}/milestones}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (state, iids, title, search, dates, sort, parent milestones)
     * @return the milestones
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Milestone> milestones(final GitLabProjectProjectMilestonesListFilter filter) throws IOException {
        return sdk.gitLab().projectmilestones().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new Milestone(this, listed));
    }

    /**
     * Loads a milestone ({@code GET /projects/{id}/milestones/{milestone_id}}).
     *
     * @param milestoneId the id of the milestone (not its iid)
     * @return the milestone, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Milestone> milestone(final long milestoneId) throws IOException {
        return sdk.gitLab().projectmilestones().get(id, milestoneId).map(loaded -> new Milestone(this, loaded));
    }

    /**
     * Creates a milestone with a title ({@code POST /projects/{id}/milestones}).
     *
     * @param title the title
     * @return the created milestone
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     */
    public Milestone createMilestone(final String title) throws IOException {
        return createMilestone(GitLabProjectProjectMilestonesCreatePayloadBuilder.builder()
                .title(requireNotBlank(title, "title"))
                .build());
    }

    /**
     * Creates a milestone from the core's payload ({@code POST /projects/{id}/milestones}), for the fields the SDK does
     * not wrap, such as the description and the dates.
     *
     * @param payload the request body
     * @return the created milestone
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Milestone createMilestone(final GitLabProjectProjectMilestonesCreatePayload payload) throws IOException {
        return new Milestone(this, sdk.gitLab().projectmilestones().create(id, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the CI/CD variables of the project ({@code GET /projects/{id}/variables}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}. The values are
     * secrets, do not log them.
     *
     * @return the variables
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Variable> variables() throws IOException {
        return sdk.gitLab().projectlevelvariables().list(id).stream().map(listed -> new Variable(this, listed));
    }

    /**
     * Loads a variable by its key ({@code GET /projects/{id}/variables/{key}}). A key with special characters reaches
     * the core unchanged. Use {@link #variable(String, String)} when several variables share the key; GitLab's answer
     * to a lookup without a scope is not verified.
     *
     * @param key the key of the variable
     * @return the variable, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Variable> variable(final String key) throws IOException {
        return sdk.gitLab()
                .projectlevelvariables()
                .get(id, requireNotBlank(key, KEY))
                .map(loaded -> new Variable(this, loaded));
    }

    /**
     * Loads the variable of a key in one environment scope ({@code GET /projects/{id}/variables/{key}} with
     * {@code filter[environment_scope]}). Use it when several variables of the project share the key.
     *
     * @param key the key of the variable; it reaches the core unchanged
     * @param environmentScope the environment scope of the variable
     * @return the variable, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Optional<Variable> variable(final String key, final String environmentScope) throws IOException {
        final GitLabProjectProjectLevelVariablesGetFilter filter =
                GitLabProjectProjectLevelVariablesGetFilterBuilder.builder()
                        .filterEnvironmentScope(requireNotBlank(environmentScope, "environmentScope"))
                        .build();
        return sdk.gitLab()
                .projectlevelvariables()
                .get(id, requireNotBlank(key, KEY), filter)
                .map(loaded -> new Variable(this, loaded));
    }

    /**
     * Creates a variable with a key and a value ({@code POST /projects/{id}/variables}). GitLab's defaults decide the
     * type, the scope and the flags; an empty value is sent as given. Use
     * {@link #createVariable(GitLabProjectProjectLevelVariablesCreatePayload)} for the other fields, such as the
     * environment scope, masking and the hidden flag.
     *
     * @param key the key of the variable
     * @param value the value; it is secret, do not log it
     * @return the new variable
     * @throws NullPointerException if {@code key} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public Variable createVariable(final String key, final String value) throws IOException {
        return createVariable(GitLabProjectProjectLevelVariablesCreatePayloadBuilder.builder()
                .key(requireNotBlank(key, KEY))
                .value(requireNonNull(value, "value"))
                .build());
    }

    /**
     * Creates a variable from the core's payload ({@code POST /projects/{id}/variables}), for the fields the SDK does
     * not wrap, such as the environment scope, the type, masking and {@code maskedAndHidden}.
     *
     * @param payload the request body
     * @return the new variable
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Variable createVariable(final GitLabProjectProjectLevelVariablesCreatePayload payload) throws IOException {
        return new Variable(this, sdk.gitLab().projectlevelvariables().create(id, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the environments of the project ({@code GET /projects/{id}/environments}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the environments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Environment> environments() throws IOException {
        return sdk.gitLab().environments().list(id).stream().map(listed -> new Environment(this, listed));
    }

    /**
     * Lists the environments of the project that match a filter ({@code GET /projects/{id}/environments}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (name, search, states)
     * @return the environments
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Environment> environments(final GitLabProjectEnvironmentsListFilter filter) throws IOException {
        return sdk.gitLab().environments().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new Environment(this, listed));
    }

    /**
     * Loads an environment ({@code GET /projects/{id}/environments/{environment_id}}).
     *
     * @param environmentId the id of the environment
     * @return the environment, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Environment> environment(final long environmentId) throws IOException {
        return sdk.gitLab().environments().get(id, environmentId).map(loaded -> new Environment(this, loaded));
    }

    /**
     * Creates an environment with a name ({@code POST /projects/{id}/environments}). Use
     * {@link #createEnvironment(GitLabProjectEnvironmentsCreatePayload)} for the other fields, such as the external
     * URL, the tier and the auto-stop setting.
     *
     * @param name the name of the environment
     * @return the new environment
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Environment createEnvironment(final String name) throws IOException {
        return createEnvironment(GitLabProjectEnvironmentsCreatePayloadBuilder.builder()
                .name(requireNotBlank(name, NAME))
                .build());
    }

    /**
     * Creates an environment from the core's payload ({@code POST /projects/{id}/environments}), for the fields the SDK
     * does not wrap, such as the external URL, the tier, the cluster agent and the auto-stop setting.
     *
     * @param payload the request body
     * @return the new environment
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Environment createEnvironment(final GitLabProjectEnvironmentsCreatePayload payload) throws IOException {
        return new Environment(this, sdk.gitLab().environments().create(id, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the deployments of the project ({@code GET /projects/{id}/deployments}). GitLab lists a summary of each
     * deployment, so each one is loaded ({@code GET /projects/{id}/deployments/{deployment_id}}) as the stream reaches
     * it, and one that is gone by then is skipped. Pages are fetched only as the stream is consumed; transport failures
     * during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the deployments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Deployment> deployments() throws IOException {
        return loadedDeployments(sdk.gitLab().deployments().list(id).stream());
    }

    /**
     * Lists the deployments of the project that match a filter ({@code GET /projects/{id}/deployments}); each one is
     * loaded as the stream reaches it, as for {@link #deployments()}.
     *
     * @param filter the core's filter (order, sort, dates, environment name, status)
     * @return the deployments
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Deployment> deployments(final GitLabProjectDeploymentsListFilter filter) throws IOException {
        return loadedDeployments(sdk.gitLab().deployments().list(id, requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Loads a deployment ({@code GET /projects/{id}/deployments/{deployment_id}}).
     *
     * @param deploymentId the id of the deployment (not its iid)
     * @return the deployment, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Deployment> deployment(final long deploymentId) throws IOException {
        return sdk.gitLab().deployments().get(id, deploymentId).map(loaded -> new Deployment(this, loaded));
    }

    /**
     * Creates a deployment ({@code POST /projects/{id}/deployments}) for a commit of a branch. A deployment of a tag
     * needs the core's payload through the client.
     *
     * @param environment the name of the environment
     * @param sha the commit SHA
     * @param ref the branch name
     * @param status the status; GitLab accepts only {@link DeploymentStatus#RUNNING}, {@link DeploymentStatus#SUCCESS},
     *     {@link DeploymentStatus#FAILED} and {@link DeploymentStatus#CANCELED}
     * @return the new deployment
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a text is blank or GitLab does not accept the status; no request is made
     * @throws IOException if the transport fails
     */
    public Deployment createDeployment(
            final String environment, final String sha, final String ref, final DeploymentStatus status)
            throws IOException {
        requireNotBlank(environment, "environment");
        requireNotBlank(sha, "sha");
        requireNotBlank(ref, REF);
        if (!requireNonNull(status, "status").isSettable()) {
            throw new IllegalArgumentException(
                    "A deployment can be created as running, success, failed or canceled, not " + status.value());
        }
        return new Deployment(
                this,
                sdk.gitLab()
                        .deployments()
                        .create(
                                id,
                                GitLabProjectDeploymentsCreatePayloadBuilder.builder()
                                        .environment(environment)
                                        .sha(sha)
                                        .ref(ref)
                                        .status(GitLabProjectDeploymentsCreateStatus.valueOf(status.name()))
                                        .build()));
    }

    /**
     * Lists the pipeline schedules of the project ({@code GET /projects/{id}/pipeline_schedules}). GitLab lists a
     * summary of each schedule, so each one is loaded ({@code GET /projects/{id}/pipeline_schedules/{id}}) as the
     * stream reaches it, and one that is gone by then is skipped. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the schedules
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PipelineSchedule> pipelineSchedules() throws IOException {
        return sdk.gitLab().pipelineschedules().list(id).stream()
                .map(this::loadedSchedule)
                .flatMap(Optional::stream);
    }

    /**
     * Loads a pipeline schedule ({@code GET /projects/{id}/pipeline_schedules/{pipeline_schedule_id}}).
     *
     * @param scheduleId the id of the schedule
     * @return the schedule, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PipelineSchedule> pipelineSchedule(final long scheduleId) throws IOException {
        return sdk.gitLab().pipelineschedules().get(id, scheduleId).map(loaded -> new PipelineSchedule(this, loaded));
    }

    /**
     * Creates a pipeline schedule ({@code POST /projects/{id}/pipeline_schedules}). The cron expression is sent as
     * given; set the time zone afterwards with {@link PipelineSchedule#update()}, or use
     * {@link #createPipelineSchedule(GitLabProjectPipelineSchedulesCreatePayload)}.
     *
     * @param description the description
     * @param ref the branch or tag to run the schedule for
     * @param cron the cron expression
     * @return the new schedule
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public PipelineSchedule createPipelineSchedule(final String description, final String ref, final String cron)
            throws IOException {
        return createPipelineSchedule(GitLabProjectPipelineSchedulesCreatePayloadBuilder.builder()
                .description(requireNotBlank(description, "description"))
                .ref(requireNotBlank(ref, REF))
                .cron(requireNotBlank(cron, "cron"))
                .build());
    }

    /**
     * Creates a pipeline schedule from the core's payload ({@code POST /projects/{id}/pipeline_schedules}), for the
     * fields the SDK does not wrap, such as the time zone, the active flag and the inputs.
     *
     * @param payload the request body
     * @return the new schedule
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PipelineSchedule createPipelineSchedule(final GitLabProjectPipelineSchedulesCreatePayload payload)
            throws IOException {
        final GitLabProjectPipelineSchedulesCreatePayload checked = requireNonNull(payload, PAYLOAD);
        return new PipelineSchedule(this, sdk.gitLab().pipelineschedules().create(id, checked));
    }

    /**
     * Lists the pipeline triggers of the project ({@code GET /projects/{id}/triggers}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}. The tokens are
     * secrets, do not log them.
     *
     * @return the triggers
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PipelineTrigger> pipelineTriggers() throws IOException {
        return sdk.gitLab().pipelinetriggers().list(id).stream().map(listed -> new PipelineTrigger(this, listed));
    }

    /**
     * Loads a pipeline trigger ({@code GET /projects/{id}/triggers/{trigger_id}}).
     *
     * @param triggerId the id of the trigger
     * @return the trigger, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PipelineTrigger> pipelineTrigger(final long triggerId) throws IOException {
        return sdk.gitLab().pipelinetriggers().get(id, triggerId).map(loaded -> new PipelineTrigger(this, loaded));
    }

    /**
     * Creates a pipeline trigger that does not expire ({@code POST /projects/{id}/triggers}). The token in the answer
     * is secret, do not log it.
     *
     * @param description the description
     * @return the new trigger
     * @throws NullPointerException if {@code description} is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     * @throws IOException if the transport fails
     */
    public PipelineTrigger createPipelineTrigger(final String description) throws IOException {
        return createTrigger(GitLabProjectPipelineTriggersCreatePayloadBuilder.builder()
                .description(requireNotBlank(description, "description"))
                .build());
    }

    /**
     * Creates a pipeline trigger that expires ({@code POST /projects/{id}/triggers}). The token in the answer is
     * secret, do not log it.
     *
     * @param description the description
     * @param expiresAt the time the trigger expires
     * @return the new trigger
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     * @throws IOException if the transport fails
     */
    public PipelineTrigger createPipelineTrigger(final String description, final Instant expiresAt) throws IOException {
        return createTrigger(GitLabProjectPipelineTriggersCreatePayloadBuilder.builder()
                .description(requireNotBlank(description, "description"))
                .expiresAt(requireNonNull(expiresAt, "expiresAt"))
                .build());
    }

    /**
     * Lists the runners of the project ({@code GET /projects/{id}/runners}). GitLab lists a summary of each runner, so
     * each one is loaded ({@code GET /runners/{id}}) as the stream reaches it, and one that is gone by then is skipped.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the runners
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Runner> runners() throws IOException {
        return loadedRunners(sdk, sdk.gitLab().runners().project().list(id).stream());
    }

    /**
     * Lists the runners of the project that match a filter ({@code GET /projects/{id}/runners}), loaded one by one as
     * described at {@link #runners()}.
     *
     * @param filter the query parameters
     * @return the runners
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Runner> runners(final GitLabProjectRunnersListFilter filter) throws IOException {
        final GitLabProjectRunnersListFilter checked = requireNonNull(filter, FILTER);
        return loadedRunners(sdk, sdk.gitLab().runners().project().list(id, checked).stream());
    }

    /**
     * Enables an existing runner in the project ({@code POST /projects/{id}/runners}) and loads it ({@code GET
     * /runners/{id}}), because the answer of the enabling is a summary. The runner must already exist; to stop using it
     * in the project again, use {@link Runner#disable(Project)}.
     *
     * @param runnerId the id of the runner
     * @return the runner
     * @throws IllegalStateException if the runner is gone after it was enabled
     * @throws IOException if the transport fails
     */
    public Runner enableRunner(final long runnerId) throws IOException {
        sdk.gitLab()
                .runners()
                .project()
                .create(
                        id,
                        GitLabProjectRunnersCreatePayloadBuilder.builder()
                                .runnerId(runnerId)
                                .build());
        return sdk.runner(runnerId)
                .orElseThrow(
                        () -> new IllegalStateException("Runner " + runnerId + " was enabled but could not be loaded"));
    }

    /**
     * Resets the runner registration token of the project ({@code POST
     * /projects/{id}/runners/reset_registration_token}). The old token stops working, and the new one is secret, do not
     * log it.
     *
     * @return the new token
     * @throws IOException if the transport fails
     */
    public RunnerToken resetRunnerRegistrationToken() throws IOException {
        return new RunnerToken(sdk.gitLab().runners().project().resetRegistrationToken(id));
    }

    /**
     * Lists the packages of the project ({@code GET /projects/{id}/packages}). GitLab lists and loads a package with
     * the same record, so none is loaded again. Each package answers {@link ProjectPackage#project()} with this project
     * without a request. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}. The ecosystem endpoints (Maven, npm, NuGet and the others) are not wrapped.
     *
     * @return the packages
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ProjectPackage> packages() throws IOException {
        return sdk.gitLab().packages().list(id).stream().map(item -> new ProjectPackage(this, item));
    }

    /**
     * Lists the packages of the project that match a filter ({@code GET /projects/{id}/packages}), as described at
     * {@link #packages()}.
     *
     * @param filter the query parameters
     * @return the packages
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ProjectPackage> packages(final GitLabProjectPackagesListFilter filter) throws IOException {
        final GitLabProjectPackagesListFilter checked = requireNonNull(filter, FILTER);
        return sdk.gitLab().packages().list(id, checked).stream().map(item -> new ProjectPackage(this, item));
    }

    /**
     * Loads a package ({@code GET /projects/{id}/packages/{package_id}}).
     *
     * @param packageId the id of the package
     * @return the package, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ProjectPackage> projectPackage(final long packageId) throws IOException {
        return sdk.gitLab().packages().get(id, packageId).map(loaded -> new ProjectPackage(this, loaded));
    }

    /**
     * Lists the container registry repositories of the project ({@code GET /projects/{id}/registry/repositories}).
     * GitLab lists and loads a repository with the same record, so none is loaded again. Each repository answers
     * {@link RegistryRepository#project()} with this project without a request. Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the repositories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryRepository> registryRepositories() throws IOException {
        return sdk.gitLab().containerregistry().list(id).stream().map(item -> new RegistryRepository(this, item));
    }

    /**
     * Lists the container registry repositories of the project ({@code GET
     * /projects/{id}/registry/repositories?tags=...}), as described at {@link #registryRepositories()}.
     *
     * @param withTags whether GitLab should include the tags of each repository in the response
     * @return the repositories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryRepository> registryRepositories(final boolean withTags) throws IOException {
        return sdk
                .gitLab()
                .containerregistry()
                .list(
                        id,
                        GitLabProjectContainerRegistryListFilterBuilder.builder()
                                .tags(withTags)
                                .build())
                .stream()
                .map(item -> new RegistryRepository(this, item));
    }

    private DeployKey createDeployKey(
            final GitLabProjectDeployKeysCreatePayloadBuilder builder, final String title, final String key)
            throws IOException {
        final GitLabProjectDeployKeysCreatePayload payload = builder.title(requireNotBlank(title, "title"))
                .key(requireNotBlank(key, KEY))
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

    private Stream<Deployment> loadedDeployments(final Stream<GitLabDeployment> listed) {
        return listed.map(item -> Lookups.find(() -> deployment(requireNonNull(item.id(), "id"))))
                .flatMap(Optional::stream);
    }

    private PipelineTrigger createTrigger(final GitLabProjectPipelineTriggersCreatePayload payload) throws IOException {
        return new PipelineTrigger(this, sdk.gitLab().pipelinetriggers().create(id, payload));
    }

    private Optional<PipelineSchedule> loadedSchedule(final GitLabCiPipelineSchedule listed) {
        return Lookups.find(() -> pipelineSchedule(requireNonNull(listed.id(), "id")));
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
