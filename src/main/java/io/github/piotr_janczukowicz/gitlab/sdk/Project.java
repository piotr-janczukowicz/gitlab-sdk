package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.FILTER;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.IS_GONE;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.NAME;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.loadedRunners;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensListFilter;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBasicBadgeDetails;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDeployment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLicense;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabTemplateList;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
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
import io.github.piotr_janczukowicz.gitlab.api.events.GitLabEvent;
import io.github.piotr_janczukowicz.gitlab.api.events.GitLabProjectEventsListFilter;
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
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsGetFilter;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsGetFilterBuilder;
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
import io.github.piotr_janczukowicz.gitlab.api.projectbadges.GitLabProjectProjectBadgesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesGetFilter;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesGetFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectProjectPushRulesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsCreateApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsHousekeepingPayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsHousekeepingTask;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsPipelineRefTriggerPayloadBuilder;
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
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabProjectPullMirroringUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabProjectPullMirroringUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesGetChangelogFilter;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesGetCompareFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetRawFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersListFilter;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsListFilter;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.ApprovalConfiguration;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.ProjectApprovalRule;
import io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditEvents;
import io.github.piotr_janczukowicz.gitlab.sdk.badges.Badge;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Job;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.code.CommitDraft;
import io.github.piotr_janczukowicz.gitlab.sdk.code.RepositoryFile;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Tag;
import io.github.piotr_janczukowicz.gitlab.sdk.dependencies.Dependencies;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.CreatedDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployKey;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.Deployment;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.DeploymentStatus;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.Environment;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.FileForms;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.RepositoryRequests;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.TokenRequests;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.members.Member;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.milestones.Milestone;
import io.github.piotr_janczukowicz.gitlab.sdk.mirrors.PullMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.mirrors.RemoteMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.packages.ProjectPackage;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedTag;
import io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRules;
import io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryRepository;
import io.github.piotr_janczukowicz.gitlab.sdk.releases.Release;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.Comparison;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.Contributor;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.TreeEntry;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnerToken;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.PipelineSchedule;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.PipelineTrigger;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.ProjectSnippets;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.AccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.AccessTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.CreatedAccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.Variable;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.Webhook;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhookEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.wikis.Wiki;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final int BAD_REQUEST = 400;
    private static final GitLabProjectMergeRequestsGetFilter WITH_REBASE_STATE =
            GitLabProjectMergeRequestsGetFilterBuilder.builder()
                    .includeRebaseInProgress(true)
                    .build();

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
     * Lists the groups invited into the project, that is, the groups it is shared with ({@code GET
     * /projects/{id}/invited_groups}; the same relation as {@link Group#invitedGroups()} for a group); these are the
     * records of the core, which carry the group's id, name and path. Per GitLab's API description, unauthenticated
     * requests get only public groups. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}. The stream is single-use. Earlier versions of this method
     * called {@code GET /projects/{id}/groups}, which lists the ancestor groups; {@link #groups()} lists those now.
     *
     * @return the groups, as the core returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabPublicGroupDetails> sharedGroups() throws IOException {
        return sdk.gitLab().projects().project().listInvitedGroups(id).stream();
    }

    /**
     * Lists the users with access to the project ({@code GET /projects/{id}/users}); these are the records of the core.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the users, as the core returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabUserBasic> users() throws IOException {
        return sdk.gitLab().projects().project().listUsers(id).stream();
    }

    /**
     * Lists the ancestor groups of the project ({@code GET /projects/{id}/groups}); these are the records of the core,
     * which carry the group's id, name and path. Pages are fetched only as the stream is consumed; transport failures
     * during iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the groups, as the core returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabPublicGroupDetails> groups() throws IOException {
        return sdk.gitLab().projects().project().listGroups(id).stream();
    }

    /**
     * Lists the visible events of the project ({@code GET /projects/{id}/events}); these are the records of the core.
     * Per GitLab's API description the default order is newest first. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the events, as the core returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabEvent> events() throws IOException {
        return sdk.gitLab().events().project().list(id).stream();
    }

    /**
     * Lists the events of the project that match a filter ({@code GET /projects/{id}/events}), as described at
     * {@link #events()}.
     *
     * @param filter the query parameters of the core ({@code action}, {@code target_type}, {@code before},
     *     {@code after} and {@code sort})
     * @return the events, as the core returns them
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabEvent> events(final GitLabProjectEventsListFilter filter) throws IOException {
        return sdk.gitLab().events().project().list(id, requireNonNull(filter, FILTER)).stream();
    }

    /**
     * Stars the project for the account ({@code POST /projects/{id}/star}) and loads it again. The core's OpenAPI
     * description lists HTTP 304 for this call; how the core's client treats it is not verified live, so a project that
     * is already starred may fail with the core's exception.
     *
     * @return the project as GitLab returns it afterwards
     * @throws IllegalStateException if the project is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Project star() throws IOException {
        sdk.gitLab().projectstarring().createStar(id);
        return refresh();
    }

    /**
     * Removes the account's star from the project ({@code POST /projects/{id}/unstar}) and loads it again. GitLab's API
     * description names HTTP 304 for a project that is not starred; how the core's client treats it is not verified
     * live, so such a call may fail with the core's exception.
     *
     * @return the project as GitLab returns it afterwards
     * @throws IllegalStateException if the project is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Project unstar() throws IOException {
        sdk.gitLab().projectstarring().createUnstar(id);
        return refresh();
    }

    /**
     * Returns the share of each programming language in the project ({@code GET /projects/{id}/languages}).
     *
     * @return the language names with their percentage, as an unmodifiable copy
     * @throws IOException if the transport fails
     */
    public Map<String, Double> languages() throws IOException {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(sdk.gitLab().projects().project().getLanguages(id)));
    }

    /**
     * Starts the housekeeping of the repository ({@code POST /projects/{id}/housekeeping}) with GitLab's default task;
     * GitLab runs it asynchronously. It runs maintenance on the real repository, so it is never run by the smoke tests.
     *
     * @throws IOException if the transport fails
     */
    public void housekeeping() throws IOException {
        housekeeping(null);
    }

    /**
     * Starts a specific housekeeping task of the repository ({@code POST /projects/{id}/housekeeping}); GitLab runs it
     * asynchronously. It runs maintenance on the real repository, so it is never run by the smoke tests.
     *
     * @param task the task ({@code EAGER} or {@code PRUNE}), or {@code null} for GitLab's default
     * @throws IOException if the transport fails
     */
    public void housekeeping(final @Nullable GitLabProjectProjectsHousekeepingTask task) throws IOException {
        sdk.gitLab().projects().project().housekeeping(id, new GitLabProjectProjectsHousekeepingPayload(task));
    }

    /**
     * Lists the templates of a kind that the project can use ({@code GET /projects/{id}/templates/{type}}); these are
     * the records of the core, with the key and name of each. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @param type the kind of template
     * @return the templates, as the core returns them
     * @throws NullPointerException if {@code type} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabTemplateList> templates(final TemplateType type) throws IOException {
        return sdk
                .gitLab()
                .projecttemplates()
                .list(id, requireNonNull(type, "type").toListType())
                .stream();
    }

    /**
     * Loads one template ({@code GET /projects/{id}/templates/{type}/{name}}). The core types the answer of every kind
     * as its license record: {@code name} and {@code content} are filled for every kind, the other fields only for
     * licenses.
     *
     * @param type the kind of template
     * @param name the name (key) of the template, as {@link #templates(TemplateType)} lists it
     * @return the template, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code type} or {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank; no request is made
     * @throws IOException if the transport fails
     */
    public Optional<GitLabLicense> template(final TemplateType type, final String name) throws IOException {
        return sdk.gitLab()
                .projecttemplates()
                .get(id, requireNonNull(type, "type").toGetType(), requireNotBlank(name, "name"));
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
     * Lists the merge requests of the project ({@code GET /projects/{id}/merge_requests}). GitLab's {@code scope}
     * defaults to {@code all}. GitLab lists a summary of each merge request, so each one is loaded ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, and one that is gone by then is
     * skipped. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
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
     * Loads a merge request ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}), asking GitLab for the
     * rebase state ({@code include_rebase_in_progress=true}; per docs.gitlab.com/api/merge_requests, GitLab includes
     * whether a rebase is in progress only then), so that {@code isRebaseInProgress()} of the result can be true.
     *
     * @param iid the merge request number within the project
     * @return the merge request, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<MergeRequest> mergeRequest(final long iid) throws IOException {
        return sdk.gitLab()
                .mergerequests()
                .project()
                .get(id, iid, WITH_REBASE_STATE)
                .map(loaded -> new MergeRequest(this, loaded));
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
     * Starts a pipeline for a branch or tag with a pipeline trigger token ({@code POST
     * /projects/{id}/ref/{ref}/trigger/pipeline}), without variables, and loads it. See {@link #triggerPipeline(String,
     * String, Map)}.
     *
     * @param token the trigger token; secret, do not log it
     * @param ref the branch or tag to run the pipeline for
     * @return the started pipeline
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IllegalStateException if the pipeline cannot be loaded after the trigger succeeded: the pipeline HAS been
     *     started (its id is in the message), but the follow-up load uses the client's own authentication, not the
     *     trigger token, and GitLab answered HTTP 404, for example because that account cannot read the project's
     *     pipelines or the pipeline is already gone; do not trigger again on this exception
     * @throws IOException if the transport fails
     */
    public Pipeline triggerPipeline(final String token, final String ref) throws IOException {
        return startTriggered(token, ref, null);
    }

    /**
     * Starts a pipeline for a branch or tag with a pipeline trigger token ({@code POST
     * /projects/{id}/ref/{ref}/trigger/pipeline}) and loads it ({@code GET /projects/{id}/pipelines/{pipeline_id}}),
     * because GitLab answers the trigger with a smaller record. The ref is part of the path, as per GitLab's API
     * description, which also lists no deprecation for this operation (the {@code ref}-less operation has no way to
     * name a ref in the core). The token is a secret: it is sent in the request body, and neither this method nor an
     * exception of the SDK prints it. Use the token of a trigger from {@link #createPipelineTrigger(String)}. The
     * variable values are sent as given and may be secret too; the SDK never prints them. A ref with a slash reaches
     * the core unchanged. Not verified live: the smoke tests only read.
     *
     * @param token the trigger token; secret, do not log it
     * @param ref the branch or tag to run the pipeline for
     * @param variables the pipeline variables by key; empty sends none
     * @return the started pipeline
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code token} or {@code ref} is blank
     * @throws IllegalStateException if the pipeline cannot be loaded after the trigger succeeded: the pipeline HAS been
     *     started (its id is in the message), but the follow-up load uses the client's own authentication, not the
     *     trigger token, and GitLab answered HTTP 404, for example because that account cannot read the project's
     *     pipelines or the pipeline is already gone; do not trigger again on this exception
     * @throws IOException if the transport fails
     */
    public Pipeline triggerPipeline(final String token, final String ref, final Map<String, String> variables)
            throws IOException {
        return startTriggered(token, ref, requireNonNull(variables, "variables"));
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
     * Streams the raw content of a file at a ref into a stream ({@code GET
     * /projects/{id}/repository/files/{file_path}/raw?ref=...}), with the core's streaming download; nothing is held in
     * memory. The path is passed to the core as it is; the core encodes it. The stream is the caller's: the SDK never
     * closes it, and it may hold a partial body when the transfer fails. Not verified live.
     *
     * @param path the path of the file within the repository
     * @param ref the branch, tag or commit to read the file at
     * @param out the stream to write to
     * @return the number of bytes written
     * @throws IllegalArgumentException if {@code path} or {@code ref} is blank
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or the stream cannot be written
     */
    public long rawFile(final String path, final String ref, final OutputStream out) throws IOException {
        requireNotBlank(path, "path");
        requireNotBlank(ref, REF);
        requireNonNull(out, "out");
        return sdk.gitLab()
                .repositoryfiles()
                .getRaw(
                        id,
                        path,
                        GitLabProjectRepositoryFilesGetRawFilterBuilder.builder()
                                .ref(ref)
                                .build(),
                        out);
    }

    /**
     * Writes the raw content of a file at a ref to a local file ({@code GET
     * /projects/{id}/repository/files/{file_path}/raw?ref=...}), with the core's download into a {@link Path}; the
     * guarantees of that download (for example whether a failed transfer leaves a partial file) are the core's and not
     * verified live here. The path is passed to the core as it is; the core encodes it.
     *
     * @param path the path of the file within the repository
     * @param ref the branch, tag or commit to read the file at
     * @param target the local file to write
     * @return {@code target}, as the core returns it
     * @throws IllegalArgumentException if {@code path} or {@code ref} is blank
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IOException if the transport fails or the file cannot be written
     */
    public Path rawFile(final String path, final String ref, final Path target) throws IOException {
        requireNotBlank(path, "path");
        requireNotBlank(ref, REF);
        requireNonNull(target, "target");
        return sdk.gitLab()
                .repositoryfiles()
                .getRaw(
                        id,
                        path,
                        GitLabProjectRepositoryFilesGetRawFilterBuilder.builder()
                                .ref(ref)
                                .build(),
                        target);
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
     * Lists the commits of the project that match a filter ({@code GET /projects/{id}/repository/commits}), loading
     * each one as for {@link #commits()}. Per GitLab's API description, {@code ref_name} "Uses the default branch if
     * not given" and the {@code order} is reverse chronological unless {@code topo} is asked for. Every commit is
     * loaded with {@code GET /projects/{id}/repository/commits/{sha}}, which includes the commit statistics by default,
     * so the filter's {@code with_stats} changes only what GitLab lists, not the commits returned.
     *
     * @param filter the query parameters of the core
     * @return the commits
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits(final GitLabProjectCommitsListFilter filter) throws IOException {
        return sdk.gitLab().commits().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> Lookups.find(() -> commit(requireNonNull(listed.id(), "id"))))
                .flatMap(Optional::stream);
    }

    /**
     * Starts a commit of several file changes to a branch. Nothing is sent until {@link CommitDraft#send()}.
     *
     * @param branch the branch to commit to
     * @param message the commit message
     * @return the draft, with no change yet
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     */
    public CommitDraft createCommit(final String branch, final String message) {
        return new CommitDraft(this, branch, message);
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
     * Lists the root of the repository at the default branch, without nested directories ({@code GET
     * /projects/{id}/repository/tree?recursive=false}). Only {@code recursive=false} is sent, which is GitLab's
     * documented default; the {@code ref} default is the default branch of the project (per the GitLab docs, "If not
     * specified, uses the default branch"), and the path is the root. A page has 20 entries. Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the entries
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<TreeEntry> tree() throws IOException {
        return tree(null, null, false);
    }

    /**
     * Lists a directory of the repository ({@code GET /projects/{id}/repository/tree?path=...&ref=...&recursive=...}).
     * A {@code null} {@code path} or {@code ref} is not sent, so GitLab's default applies: the root for {@code path},
     * the default branch for {@code ref}. {@code recursive} is always sent; {@code false} lists only the direct entries
     * of the directory. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param path the directory within the repository, or {@code null} for the root
     * @param ref the branch, tag or commit, or {@code null} for the default branch
     * @param recursive whether to list nested directories too
     * @return the entries
     * @throws IllegalArgumentException if {@code path} or {@code ref} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<TreeEntry> tree(final @Nullable String path, final @Nullable String ref, final boolean recursive)
            throws IOException {
        return sdk.gitLab().repositories().listTree(id, RepositoryRequests.treeFilter(path, ref, recursive)).stream()
                .map(TreeEntry::new);
    }

    /**
     * Compares two refs ({@code GET /projects/{id}/repository/compare}). Without {@code straight} GitLab compares the
     * merge base of {@code from} and {@code to} with {@code to}; with it, the two refs directly. The {@code straight}
     * parameter is always sent.
     *
     * @param from the branch, tag or commit to compare from
     * @param to the branch, tag or commit to compare to
     * @param straight {@code true} to compare the refs directly instead of from their merge base
     * @return the comparison
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Comparison compare(final String from, final String to, final boolean straight) throws IOException {
        return new Comparison(sdk.gitLab()
                .repositories()
                .getCompare(
                        id,
                        requireNotBlank(from, "from"),
                        requireNotBlank(to, "to"),
                        GitLabProjectRepositoriesGetCompareFilterBuilder.builder()
                                .straight(straight)
                                .build()));
    }

    /**
     * Lists the contributors of the repository at the default branch ({@code GET
     * /projects/{id}/repository/contributors}). No {@code order_by} or {@code sort} is sent. Per the GitLab docs (<a
     * href="https://docs.gitlab.com/api/repositories/">docs.gitlab.com/api/repositories</a>, "Get contributor list"),
     * "If not specified, contributors are ordered by commit date" and the sort "Default is {@code asc}"; this is not
     * verified live. The commit counts do not include merge commits. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the contributors
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Contributor> contributors() throws IOException {
        return sdk.gitLab().repositories().listContributors(id).stream().map(Contributor::new);
    }

    /**
     * Finds the merge base, the common ancestor, of two or more refs ({@code GET /projects/{id}/repository/merge_base})
     * and loads it ({@code GET /projects/{id}/repository/commits/{sha}}). The result is empty when GitLab answers HTTP
     * 404 (no merge base, or an unknown ref), and also when the found commit is gone by the time it is loaded; other
     * errors are the core's exceptions.
     *
     * @param refs at least two branches, tags or commits
     * @return the merge base commit, or empty
     * @throws NullPointerException if {@code refs} or an element is {@code null}
     * @throws IllegalArgumentException if fewer than two refs are given or one is blank
     * @throws IOException if the transport fails
     */
    public Optional<Commit> mergeBase(final String... refs) throws IOException {
        final List<String> checked = RepositoryRequests.mergeBaseRefs(refs);
        final GitLabCommit found;
        try {
            found = sdk.gitLab().repositories().getMergeBase(id, checked);
        } catch (final GitLabNotFoundException e) {
            return Optional.empty();
        }
        return commit(requireNonNull(found.id(), "id"));
    }

    /**
     * Asks GitLab to delete all branches that are merged into the default branch ({@code DELETE
     * /projects/{id}/repository/merged_branches}). Protected branches are not deleted. GitLab answers HTTP 202, so the
     * branches are deleted after this method returns; the answer carries nothing to wait for.
     *
     * @throws IOException if the transport fails
     */
    public void deleteMergedBranches() throws IOException {
        sdk.gitLab().branches().deleteMergedBranches(id);
    }

    /**
     * Generates the changelog notes of a version from the commits of the repository without committing them ({@code GET
     * /projects/{id}/repository/changelog}). GitLab's defaults apply: the range starts at the latest stable version tag
     * before {@code version} and ends at the latest commit of the default branch.
     *
     * @param version the version, in semantic versioning format, with or without a {@code v} prefix
     * @return the notes in Markdown
     * @throws NullPointerException if {@code version} is {@code null}
     * @throws IllegalArgumentException if {@code version} is blank
     * @throws IOException if the transport fails
     */
    public String changelog(final String version) throws IOException {
        return requireNonNull(
                sdk.gitLab()
                        .repositories()
                        .getChangelog(id, requireNotBlank(version, "version"))
                        .notes(),
                "notes");
    }

    /**
     * Generates the changelog notes of a version without committing them ({@code GET
     * /projects/{id}/repository/changelog}), with the range, trailer and configuration of a filter.
     *
     * @param version the version, in semantic versioning format, with or without a {@code v} prefix
     * @param filter the core's filter (from, to, date, trailer, config file)
     * @return the notes in Markdown
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code version} is blank
     * @throws IOException if the transport fails
     */
    public String changelog(final String version, final GitLabProjectRepositoriesGetChangelogFilter filter)
            throws IOException {
        return requireNonNull(
                sdk.gitLab()
                        .repositories()
                        .getChangelog(id, requireNotBlank(version, "version"), requireNonNull(filter, FILTER))
                        .notes(),
                "notes");
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
     * @throws IllegalStateException if the runner was enabled but could not be loaded
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

    /**
     * Returns the wiki of the project: its pages and attachments. No request is made.
     *
     * @return the wiki
     */
    public Wiki wiki() {
        return Wiki.forProject(this);
    }

    /**
     * Returns the snippets of the project: list, load, create, read their content, update and delete. No request is
     * made.
     *
     * @return the snippets of the project
     */
    public ProjectSnippets snippets() {
        return ProjectSnippets.forProject(this);
    }

    /**
     * Returns the dependencies of the project (Ultimate): a read-only list. No request is made.
     *
     * @return the dependencies of the project
     */
    public Dependencies dependencies() {
        return Dependencies.forProject(this);
    }

    /**
     * Returns the audit events of the project (Premium): a read-only list, newest first, and a lookup by id. No request
     * is made.
     *
     * @return the audit events of the project
     */
    public AuditEvents auditEvents() {
        return AuditEvents.forProject(this);
    }

    /**
     * Lists the badges of the project ({@code GET /projects/{id}/badges}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the badges
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Badge> badges() throws IOException {
        return sdk.gitLab().projectbadges().list(id).stream().map(listed -> new Badge(this, listed));
    }

    /**
     * Loads a badge ({@code GET /projects/{id}/badges/{badge_id}}).
     *
     * @param badgeId the id of the badge
     * @return the badge, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Badge> badge(final long badgeId) throws IOException {
        return sdk.gitLab().projectbadges().get(id, badgeId).map(loaded -> new Badge(this, loaded));
    }

    /**
     * Adds a badge ({@code POST /projects/{id}/badges}). Both URLs may hold placeholders such as
     * {@code %{project_path}}.
     *
     * @param linkUrl the URL the badge links to
     * @param imageUrl the URL of the badge image
     * @return the new badge
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Badge addBadge(final String linkUrl, final String imageUrl) throws IOException {
        return createBadge(linkUrl, imageUrl, null);
    }

    /**
     * Adds a named badge ({@code POST /projects/{id}/badges}).
     *
     * @param linkUrl the URL the badge links to
     * @param imageUrl the URL of the badge image
     * @param name the name of the badge
     * @return the new badge
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Badge addBadge(final String linkUrl, final String imageUrl, final String name) throws IOException {
        return createBadge(linkUrl, imageUrl, requireNotBlank(name, NAME));
    }

    /**
     * Shows how a badge would look with its placeholders replaced, without creating it ({@code GET
     * /projects/{id}/badges/render}). The core's record is returned as it is.
     *
     * @param linkUrl the URL the badge would link to
     * @param imageUrl the URL of the badge image
     * @return the rendered URLs
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public GitLabBasicBadgeDetails previewBadge(final String linkUrl, final String imageUrl) throws IOException {
        final String link = requireNotBlank(linkUrl, "linkUrl");
        final String image = requireNotBlank(imageUrl, "imageUrl");
        return sdk.gitLab().projectbadges().getRender(id, link, image);
    }

    /**
     * Loads the push rules of the project ({@code GET /projects/{id}/push_rule}). Per GitLab's API description, push
     * rules are a Premium feature: on a lower tier the core's {@code GitLabTierException} passes through. The core
     * throws for HTTP 404, and the SDK turns that into an empty result. The GitLab API description only says "Not
     * Found" for this operation, so that a project without push rules is answered with 404 is an assumption, not
     * verified live; if GitLab answers 200 with a JSON {@code null} instead, the core throws
     * {@link IllegalStateException}, and the SDK does not catch it.
     *
     * @return the push rules, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PushRules> pushRules() throws IOException {
        try {
            return Optional.of(
                    new PushRules(this, sdk.gitLab().projectpushrules().get(id)));
        } catch (final GitLabNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates the push rules of the project from the core's payload ({@code POST /projects/{id}/push_rule}); change
     * them later with {@link PushRules#update()}. Per GitLab's API description, push rules are a Premium feature: on a
     * lower tier the core's {@code GitLabTierException} passes through.
     *
     * @param payload the request body
     * @return the new push rules
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PushRules createPushRules(final GitLabProjectProjectPushRulesCreatePayload payload) throws IOException {
        final GitLabProjectProjectPushRulesCreatePayload checked = requireNonNull(payload, PAYLOAD);
        return new PushRules(this, sdk.gitLab().projectpushrules().create(id, checked));
    }

    /**
     * Lists the access tokens of the project ({@code GET /projects/{id}/access_tokens}). The core's call returns the
     * whole list in one request, so the stream is a view of that list. A token with the Planner or the Security Manager
     * role cannot be decoded by the core's response type; that is a known gap of the core, not checked live.
     *
     * @return the access tokens
     * @throws IOException if the request fails
     */
    public Stream<AccessToken> accessTokens() throws IOException {
        return sdk.gitLab().accesstokens().project().list(id).stream().map(listed -> new AccessToken(this, listed));
    }

    /**
     * Lists the access tokens of the project that match a filter ({@code GET /projects/{id}/access_tokens}).
     *
     * @param filter the query parameters of the core
     * @return the access tokens
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the request fails
     */
    public Stream<AccessToken> accessTokens(final GitLabProjectAccessTokensListFilter filter) throws IOException {
        return sdk.gitLab().accesstokens().project().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new AccessToken(this, listed));
    }

    /**
     * Loads an access token ({@code GET /projects/{id}/access_tokens/{token_id}}).
     *
     * @param tokenId the id of the token
     * @return the token, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<AccessToken> accessToken(final long tokenId) throws IOException {
        return sdk.gitLab()
                .accesstokens()
                .project()
                .get(id, String.valueOf(tokenId))
                .map(loaded -> new AccessToken(this, loaded));
    }

    /**
     * Creates an access token ({@code POST /projects/{id}/access_tokens}). The secret of the token is in the answer
     * only; GitLab never returns it again. The role must be one of {@code GUEST}, {@code REPORTER}, {@code DEVELOPER},
     * {@code MAINTAINER} or {@code OWNER}: the core's response type has no value for the other roles, so the answer of
     * such a creation could not be decoded and the secret would be lost, and the SDK refuses them before the request.
     *
     * @param name the name of the token
     * @param scopes what the token may be used for
     * @param expiresAt the day the token expires, or {@code null} to leave it to GitLab
     * @param level the role of the token in the project
     * @return the new token with its secret
     * @throws NullPointerException if {@code name}, {@code scopes}, a scope or {@code level} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank, no scope is given, or the role is not supported
     * @throws IOException if the transport fails
     */
    public CreatedAccessToken createAccessToken(
            final String name,
            final Collection<AccessTokenScope> scopes,
            final @Nullable LocalDate expiresAt,
            final AccessLevel level)
            throws IOException {
        final GitLabProjectAccessTokensCreatePayloadBuilder payload =
                GitLabProjectAccessTokensCreatePayloadBuilder.builder()
                        .name(requireNotBlank(name, NAME))
                        .scopes(TokenRequests.requireScopes(scopes).stream()
                                .map(scope -> GitLabProjectAccessTokensCreateScopes.valueOf(scope.name()))
                                .toList())
                        .accessLevel(GitLabProjectAccessTokensCreateAccessLevel.fromWire(
                                TokenRequests.requireTokenLevel(level).value()))
                        .expiresAt(expiresAt);
        return new CreatedAccessToken(
                this, sdk.gitLab().accesstokens().project().create(id, payload.build()));
    }

    /**
     * Lists the remote (push) mirrors of the project ({@code GET /projects/{id}/remote_mirrors}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the remote mirrors
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RemoteMirror> remoteMirrors() throws IOException {
        return sdk.gitLab().remotemirrors().list(id).stream().map(listed -> new RemoteMirror(this, listed));
    }

    /**
     * Loads a remote mirror ({@code GET /projects/{id}/remote_mirrors/{mirror_id}}).
     *
     * @param mirrorId the id of the mirror
     * @return the mirror, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<RemoteMirror> remoteMirror(final long mirrorId) throws IOException {
        return sdk.gitLab()
                .remotemirrors()
                .get(id, String.valueOf(mirrorId))
                .map(loaded -> new RemoteMirror(this, loaded));
    }

    /**
     * Creates a remote mirror that pushes to an address ({@code POST /projects/{id}/remote_mirrors}). Only the address
     * is sent, so GitLab's defaults decide the rest; change them with {@link RemoteMirror#update()}. The address may
     * carry credentials; the SDK does not print it.
     *
     * @param url the address to push to
     * @return the new mirror
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     * @throws IOException if the transport fails
     */
    public RemoteMirror createRemoteMirror(final String url) throws IOException {
        return createRemoteMirror(GitLabProjectRemoteMirrorsCreatePayloadBuilder.builder()
                .url(requireNotBlank(url, "url"))
                .build());
    }

    /**
     * Creates a remote mirror from the core's payload ({@code POST /projects/{id}/remote_mirrors}), for the fields the
     * SDK does not wrap, such as the authentication method and the host keys.
     *
     * @param payload the request body
     * @return the new mirror
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public RemoteMirror createRemoteMirror(final GitLabProjectRemoteMirrorsCreatePayload payload) throws IOException {
        final GitLabProjectRemoteMirrorsCreatePayload checked = requireNonNull(payload, PAYLOAD);
        return new RemoteMirror(this, sdk.gitLab().remotemirrors().create(id, checked));
    }

    /**
     * Loads the pull mirror of the project ({@code GET /projects/{id}/mirror/pull}). Per GitLab's API description, pull
     * mirroring is a Premium feature, and GitLab answers HTTP 400 when the project is not mirrored. The result is empty
     * for HTTP 404 and for that HTTP 400; the 400 mapping follows the description and is not verified live. Other
     * errors, including the core's tier error, pass through.
     *
     * @return the pull mirror, or empty when the project has none
     * @throws IOException if the transport fails
     */
    public Optional<PullMirror> pullMirror() throws IOException {
        try {
            return Optional.of(new PullMirror(this, sdk.gitLab().pullmirroring().get(id)));
        } catch (final GitLabNotFoundException e) {
            return Optional.empty();
        } catch (final GitLabApiException e) {
            if (e.statusCode() == BAD_REQUEST) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Sets the address the project is pull-mirrored from ({@code PUT /projects/{id}/mirror/pull}). Only the address is
     * sent; the other settings, such as the credentials, follow with {@link PullMirror#update()}. Per GitLab's API
     * description, pull mirroring is a Premium feature and this operation is marked experimental there. It is not
     * verified live. Starting a pull ({@code POST /projects/{id}/mirror/pull}) is not offered.
     *
     * @param url the address to pull from
     * @return the pull mirror as GitLab answers
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     * @throws IOException if the transport fails
     */
    public PullMirror configurePullMirror(final String url) throws IOException {
        final GitLabProjectPullMirroringUpdatePayload payload = GitLabProjectPullMirroringUpdatePayloadBuilder.builder()
                .url(requireNotBlank(url, "url"))
                .build();
        return new PullMirror(this, sdk.gitLab().pullmirroring().update(id, payload));
    }

    /**
     * Lists the approval rules of the project ({@code GET /projects/{id}/approval_rules}). They are the project-level
     * rules, not those of a merge request. Per GitLab's API description, approval rules are a Premium feature: on a
     * lower tier the core's {@code GitLabTierException} passes through. Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the approval rules
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ProjectApprovalRule> approvalRules() throws IOException {
        return sdk.gitLab().projects().project().listApprovalRules(id).stream()
                .map(listed -> new ProjectApprovalRule(this, listed));
    }

    /**
     * Loads an approval rule of the project ({@code GET /projects/{id}/approval_rules/{approval_rule_id}}). Premium, as
     * above.
     *
     * @param ruleId the id of the rule
     * @return the rule, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ProjectApprovalRule> approvalRule(final long ruleId) throws IOException {
        return sdk.gitLab()
                .projects()
                .project()
                .getApprovalRule(id, ruleId)
                .map(loaded -> new ProjectApprovalRule(this, loaded));
    }

    /**
     * Creates an approval rule that has no approvers yet ({@code POST /projects/{id}/approval_rules}). Name the
     * approvers with {@link #createApprovalRule(String, int, List, List, List)} or later with
     * {@link ProjectApprovalRule#update()}. No rule type is sent, because GitLab's API description says not to use it.
     * Premium, as above.
     *
     * @param name the rule name
     * @param approvalsRequired the number of approvals the rule needs
     * @return the created rule
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank or {@code approvalsRequired} is negative
     * @throws IOException if the transport fails
     */
    public ProjectApprovalRule createApprovalRule(final String name, final int approvalsRequired) throws IOException {
        return createApprovalRule(name, approvalsRequired, List.of(), List.of(), List.of());
    }

    /**
     * Creates an approval rule with its approvers ({@code POST /projects/{id}/approval_rules}). An empty list sends
     * nothing for that field. Only the current approver fields are offered: the core's payload also has {@code users}
     * and {@code groups}, which GitLab's API description marks as deprecated. No rule type is sent. Premium, as above.
     *
     * @param name the rule name
     * @param approvalsRequired the number of approvals the rule needs
     * @param userIds the numeric ids of the approving users
     * @param groupIds the numeric ids of the groups whose members approve
     * @param usernames the user names of the approvers
     * @return the created rule
     * @throws NullPointerException if an argument, an id or a name is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank, {@code approvalsRequired} is negative or a user name
     *     is blank
     * @throws IOException if the transport fails
     */
    public ProjectApprovalRule createApprovalRule(
            final String name,
            final int approvalsRequired,
            final List<Long> userIds,
            final List<Long> groupIds,
            final List<String> usernames)
            throws IOException {
        if (approvalsRequired < 0) {
            throw new IllegalArgumentException("approvalsRequired must not be negative");
        }
        final GitLabProjectProjectsCreateApprovalRulePayloadBuilder builder =
                GitLabProjectProjectsCreateApprovalRulePayloadBuilder.builder()
                        .name(requireNotBlank(name, NAME))
                        .approvalsRequired(approvalsRequired);
        if (!requireNonNull(userIds, "userIds").isEmpty()) {
            builder.userIds(List.copyOf(userIds));
        }
        if (!requireNonNull(groupIds, "groupIds").isEmpty()) {
            builder.groupIds(List.copyOf(groupIds));
        }
        if (!requireNonNull(usernames, "usernames").isEmpty()) {
            builder.usernames(Arguments.requireNotBlankElements(usernames, "usernames"));
        }
        return new ProjectApprovalRule(this, sdk.gitLab().projects().project().createApprovalRule(id, builder.build()));
    }

    /**
     * Loads the approval configuration of the project ({@code GET /projects/{id}/approvals}). Per GitLab's API
     * description, it is a Premium feature: on a lower tier the core's {@code GitLabTierException} passes through. Two
     * of its settings are deprecated in that description and are not offered by the object; see
     * {@link ApprovalConfiguration}.
     *
     * @return the configuration, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ApprovalConfiguration> approvalConfiguration() throws IOException {
        try {
            return Optional.of(new ApprovalConfiguration(
                    this, sdk.gitLab().mergerequestapprovals().getApprovals(id)));
        } catch (final GitLabNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Downloads the avatar of the project to a stream ({@code GET /projects/{id}/avatar}). The stream is not closed. A
     * project without an avatar is answered with an error of the core. Uploading an avatar is not offered.
     *
     * @param out where to write the image
     * @return the number of bytes written
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or writing fails
     */
    public long avatar(final OutputStream out) throws IOException {
        final OutputStream checked = requireNonNull(out, "out");
        return sdk.gitLab().projects().project().getAvatar(id, checked);
    }

    /**
     * Downloads the avatar of the project to a file ({@code GET /projects/{id}/avatar}).
     *
     * @param target the file to write
     * @return the file that was written
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IOException if the transport fails or writing fails
     */
    public Path avatar(final Path target) throws IOException {
        final Path checked = requireNonNull(target, "target");
        return sdk.gitLab().projects().project().getAvatar(id, checked);
    }

    /**
     * Uploads a file to the project ({@code POST /projects/{id}/uploads}) and returns the Markdown that shows or links
     * it. The whole file is read into memory first, so a file that cannot be read fails before any request. The file is
     * sent as a multipart form with the content type {@code application/octet-stream}; whether GitLab accepts that for
     * every file type is not verified live.
     *
     * @param file the file to upload
     * @return the Markdown link GitLab returns
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IOException if the file cannot be read or the transport fails
     */
    public String upload(final Path file) throws IOException {
        final Path name = requireNonNull(file, "file").getFileName();
        final MultipartFormData form = MultipartFormData.builder()
                .file("file", String.valueOf(name), "application/octet-stream", Files.readAllBytes(file))
                .build();
        return requireNonNull(sdk.gitLab().markdownuploads().create(id, form).markdown(), "markdown");
    }

    private Pipeline startTriggered(final String token, final String ref, final Map<String, String> variables)
            throws IOException {
        requireNotBlank(token, "token");
        requireNotBlank(ref, REF);
        final long started = requireNonNull(
                sdk.gitLab()
                        .projects()
                        .project()
                        .pipelineRefTrigger(
                                id,
                                ref,
                                GitLabProjectProjectsPipelineRefTriggerPayloadBuilder.builder()
                                        .token(token)
                                        .variables(
                                                variables == null || variables.isEmpty() ? null : Map.copyOf(variables))
                                        .build())
                        .id(),
                "id");
        return pipeline(started)
                .orElseThrow(() -> new IllegalStateException("Pipeline " + started
                        + " was started by the trigger but cannot be loaded with this client's credentials"));
    }

    private Badge createBadge(final String linkUrl, final String imageUrl, final @Nullable String name)
            throws IOException {
        final GitLabProjectProjectBadgesCreatePayloadBuilder payload =
                GitLabProjectProjectBadgesCreatePayloadBuilder.builder()
                        .linkUrl(requireNotBlank(linkUrl, "linkUrl"))
                        .imageUrl(requireNotBlank(imageUrl, "imageUrl"))
                        .name(name);
        return new Badge(this, sdk.gitLab().projectbadges().create(id, payload.build()));
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
