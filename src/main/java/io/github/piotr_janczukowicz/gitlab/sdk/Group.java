package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerWithUuid;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesCreateMergeAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesCreatePushAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListRunnersFilter;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsShareGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsSharePayload;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.GroupCreatedDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.GroupDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.GroupLabel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupAccessRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupMember;
import io.github.piotr_janczukowicz.gitlab.sdk.milestones.GroupMilestone;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.GroupProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.RegistrationToken;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.GroupVariable;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.GroupWebhook;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhookEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A group, identified by its numeric id or plain full path (the core encodes it); it may be nested. A handle from
 * {@link GitLabSdk#group(String)} holds no data from GitLab; {@link #fetch()} returns a loaded group. The object is
 * immutable: every operation returns a new group built from GitLab's answer.
 */
public final class Group {

    private static final String IS_GONE = " is gone";
    private static final String NAME = "name";
    private static final String FILTER = "filter";

    static {
        Internals.registerGroupAccess(group -> group.sdk);
    }

    private final GitLabSdk sdk;
    private final String id;
    private final @Nullable GitLabGroupDetail response;

    Group(final GitLabSdk sdk, final String id, final @Nullable GitLabGroupDetail response) {
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
     * Loads the group ({@code GET /groups/{id}}). This object is not changed.
     *
     * @return the loaded group, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Group> fetch() throws IOException {
        return sdk.gitLab().groups().get(id).map(loaded -> new Group(sdk, id, loaded));
    }

    /**
     * Reloads the group ({@code GET /groups/{id}}). This object is not changed.
     *
     * @return the reloaded group
     * @throws IllegalStateException if GitLab no longer has the group
     * @throws IOException if the transport fails
     */
    public Group refresh() throws IOException {
        return fetch().orElseThrow(() -> new IllegalStateException("Group " + id + IS_GONE));
    }

    /**
     * Returns the complete response of a loaded group.
     *
     * @return the response record
     * @throws IllegalStateException if this is a handle that was not loaded with {@link #fetch()}
     */
    public GitLabGroupDetail response() {
        if (response == null) {
            throw new IllegalStateException("Group " + id + " is not loaded; call fetch()");
        }
        return response;
    }

    /**
     * Starts a change of the name, path, description or visibility. Nothing is sent until {@link GroupUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupUpdate update() {
        return new GroupUpdate(sdk, id);
    }

    /**
     * Archives the group ({@code POST /groups/{id}/archive}) and loads it again.
     *
     * @return the archived group as GitLab returns it afterwards
     * @throws IllegalStateException if the group is gone when it is loaded again after the change; a group that is
     *     already gone fails the {@code POST} first, with the core's exception
     * @throws IOException if the transport fails
     */
    public Group archive() throws IOException {
        sdk.gitLab().groups().archive(id);
        return refresh();
    }

    /**
     * Unarchives the group ({@code POST /groups/{id}/unarchive}) and loads it again.
     *
     * @return the unarchived group as GitLab returns it afterwards
     * @throws IllegalStateException if the group is gone when it is loaded again after the change; a group that is
     *     already gone fails the {@code POST} first, with the core's exception
     * @throws IOException if the transport fails
     */
    public Group unarchive() throws IOException {
        sdk.gitLab().groups().unarchive(id);
        return refresh();
    }

    /**
     * Deletes the group ({@code DELETE /groups/{id}}). GitLab may only mark it for deletion, depending on the instance
     * settings.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        sdk.gitLab().groups().delete(id);
    }

    /**
     * Transfers the group to another parent group ({@code POST /groups/{id}/transfer}) and loads it again ({@code GET
     * /groups/{id}}), because GitLab answers the transfer with no body. A transfer changes the full path of the group,
     * so the group is addressed by its numeric id from then on: the returned group has a numeric {@link #id()}. For a
     * group that is not loaded (a handle) this costs one extra {@code GET /groups/{id}} before the transfer, to learn
     * the numeric id.
     *
     * @param newParentGroupId the id of the new parent group
     * @return the moved group as GitLab returns it afterwards
     * @throws IllegalStateException if the group is gone before the transfer, or gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Group transfer(final long newParentGroupId) throws IOException {
        final long numericId =
                requireNonNull((response != null ? this : refresh()).response().id(), "id");
        sdk.gitLab().groups().transfer(id, new GitLabGroupGroupsTransferPayload(newParentGroupId));
        final String movedId = String.valueOf(numericId);
        return sdk.group(movedId).fetch().orElseThrow(() -> new IllegalStateException("Group " + movedId + IS_GONE));
    }

    /**
     * Shares the group with another group ({@code POST /groups/{id}/share}). GitLab answers with this group, so the
     * returned group is built from the answer without another request.
     *
     * @param groupId the id of the group to share with
     * @param level the access level the other group's members get
     * @return this group as GitLab answers the sharing
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IOException if the transport fails
     */
    public Group share(final long groupId, final AccessLevel level) throws IOException {
        final GitLabGroupGroupsSharePayload payload = new GitLabGroupGroupsSharePayload(
                groupId,
                GitLabGroupGroupsShareGroupAccess.fromWire(
                        requireNonNull(level, "level").value()),
                null,
                null);
        return new Group(sdk, id, sdk.gitLab().groups().share(id, payload));
    }

    /**
     * Stops sharing the group with another group ({@code DELETE /groups/{id}/share/{group_id}}).
     *
     * @param groupId the id of the group the sharing is removed for
     * @throws IOException if the transport fails
     */
    public void unshare(final long groupId) throws IOException {
        sdk.gitLab().groups().deleteShare(id, groupId);
    }

    /**
     * Lists the direct members of the group ({@code GET /groups/{id}/members}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupMember> members() throws IOException {
        return sdk.gitLab().members().group().list(id).stream().map(listed -> new GroupMember(this, listed));
    }

    /**
     * Lists the members of the group including those inherited from its parent groups ({@code GET
     * /groups/{id}/members/all}). Pages are fetched only as the stream is consumed; transport failures during iteration
     * are {@link java.io.UncheckedIOException}.
     *
     * <p>A listed member may be a member only through a parent group. Its {@code refresh()} loads it from this list
     * ({@code GET /groups/{id}/members/all/{user_id}}); its {@code update()} and {@code remove()} act on direct
     * memberships only and fail with the core's exception for GitLab's HTTP 404 when the membership is inherited.
     *
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupMember> allMembers() throws IOException {
        return sdk.gitLab().members().group().listAll(id).stream()
                .map(listed -> Internals.fromAllGroupMembers(new GroupMember(this, listed)));
    }

    /**
     * Loads a direct member ({@code GET /groups/{id}/members/{user_id}}).
     *
     * @param userId the numeric user id
     * @return the member, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupMember> member(final long userId) throws IOException {
        return sdk.gitLab().members().group().get(id, userId).map(loaded -> new GroupMember(this, loaded));
    }

    /**
     * Adds a user as a member ({@code POST /groups/{id}/members}).
     *
     * @param userId the numeric user id
     * @param level the access level to grant
     * @return the new member
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupMember addMember(final long userId, final AccessLevel level) throws IOException {
        return createMember(String.valueOf(userId), null, level);
    }

    /**
     * Adds a user as a member by username ({@code POST /groups/{id}/members}).
     *
     * @param username the username
     * @param level the access level to grant
     * @return the new member
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code username} is blank
     * @throws IOException if the transport fails
     */
    public GroupMember addMember(final String username, final AccessLevel level) throws IOException {
        return createMember(null, requireNotBlank(username, "username"), level);
    }

    /**
     * Requests access to the group for the user of the client ({@code POST /groups/{id}/access_requests}).
     *
     * @return the request
     * @throws IOException if the transport fails
     */
    public GroupAccessRequest requestAccess() throws IOException {
        return new GroupAccessRequest(
                this, sdk.gitLab().accessrequests().group().create(id));
    }

    /**
     * Lists the pending access requests of the group ({@code GET /groups/{id}/access_requests}). Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupAccessRequest> accessRequests() throws IOException {
        return sdk.gitLab().accessrequests().group().list(id).stream()
                .map(listed -> new GroupAccessRequest(this, listed));
    }

    /**
     * Lists the projects of the group ({@code GET /groups/{id}/projects}). The list holds only summaries, so each
     * project is loaded as the stream reaches it ({@code GET /projects/{id}}) and one that was deleted in the meantime
     * is skipped. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the loaded projects
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Project> projects() throws IOException {
        return sdk.gitLab().groups().listProjects(id, GitLabPageRequest.byPage()).stream()
                .map(listed -> String.valueOf(requireNonNull(listed.id(), "id")))
                .flatMap(projectId -> Lookups.find(() -> sdk.project(projectId).fetch()).stream());
    }

    /**
     * Lists the direct subgroups, one level down ({@code GET /groups/{id}/subgroups}); deeper descendants are not
     * included (the core's {@code listDescendantGroups} lists them). The list holds only summaries, so each group is
     * loaded as the stream reaches it ({@code GET /groups/{id}}) and one that was deleted in the meantime is skipped.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the loaded groups
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Group> subgroups() throws IOException {
        return sdk.gitLab().groups().listGroup(id, GitLabPageRequest.byPage()).stream()
                .map(listed -> String.valueOf(requireNonNull(listed.id(), "id")))
                .flatMap(groupId -> Lookups.find(() -> sdk.group(groupId).fetch()).stream());
    }

    /**
     * Lists the webhooks of the group ({@code GET /groups/{id}/hooks}). Premium. Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the webhooks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupWebhook> webhooks() throws IOException {
        return sdk.gitLab().groupwebhooks().list(id).stream().map(listed -> new GroupWebhook(this, listed));
    }

    /**
     * Loads a webhook ({@code GET /groups/{id}/hooks/{hook_id}}). Premium.
     *
     * @param hookId the id of the webhook
     * @return the webhook, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupWebhook> webhook(final long hookId) throws IOException {
        return sdk.gitLab().groupwebhooks().get(id, hookId).map(loaded -> new GroupWebhook(this, loaded));
    }

    /**
     * Creates a webhook that sends to a URL ({@code POST /groups/{id}/hooks}). Premium. GitLab's defaults decide which
     * events it is switched on for; change them with {@link GroupWebhook#update()}.
     *
     * @param url the URL to send the events to
     * @return the new webhook
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     * @throws IOException if the transport fails
     */
    public GroupWebhook createWebhook(final String url) throws IOException {
        return createWebhook(GitLabGroupGroupWebhooksCreatePayloadBuilder.builder()
                .url(requireNotBlank(url, "url"))
                .build());
    }

    /**
     * Creates a webhook from the core's payload ({@code POST /groups/{id}/hooks}). Premium. For the fields the SDK does
     * not wrap, such as the events outside {@link WebhookEvent}, URL variables and custom headers.
     *
     * @param payload the request body
     * @return the new webhook
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupWebhook createWebhook(final GitLabGroupGroupWebhooksCreatePayload payload) throws IOException {
        return new GroupWebhook(this, sdk.gitLab().groupwebhooks().create(id, requireNonNull(payload, "payload")));
    }

    /**
     * Lists the deploy tokens of the group ({@code GET /groups/{id}/deploy_tokens}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the deploy tokens
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupDeployToken> deployTokens() throws IOException {
        return sdk.gitLab().deploytokens().group().list(id).stream().map(listed -> new GroupDeployToken(this, listed));
    }

    /**
     * Loads a deploy token ({@code GET /groups/{id}/deploy_tokens/{token_id}}).
     *
     * @param tokenId the id of the token
     * @return the token, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupDeployToken> deployToken(final long tokenId) throws IOException {
        return sdk.gitLab().deploytokens().group().get(id, tokenId).map(loaded -> new GroupDeployToken(this, loaded));
    }

    /**
     * Creates a deploy token ({@code POST /groups/{id}/deploy_tokens}). The secret of the token is in the answer only;
     * GitLab never returns it again. GitLab has no group-level deploy keys.
     *
     * @param name the name of the token
     * @param scopes what the token may be used for
     * @return the new token with its secret
     * @throws NullPointerException if {@code name} or a scope is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank, or no scope is given
     * @throws IOException if the transport fails
     */
    public GroupCreatedDeployToken createDeployToken(final String name, final DeployTokenScope @Nullable ... scopes)
            throws IOException {
        requireNotBlank(name, NAME);
        if (scopes == null || scopes.length == 0) {
            throw new IllegalArgumentException("At least one scope is required");
        }
        final GitLabGroupDeployTokensCreatePayload payload = GitLabGroupDeployTokensCreatePayloadBuilder.builder()
                .name(name)
                .scopes(Stream.of(scopes)
                        .map(scope -> GitLabGroupDeployTokensCreateScopes.valueOf(
                                requireNonNull(scope, "scope").name()))
                        .toList())
                .build();
        return new GroupCreatedDeployToken(
                this, sdk.gitLab().deploytokens().group().create(id, payload));
    }

    /**
     * Lists the protected branches of the group ({@code GET /groups/{id}/protected_branches}). Premium. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the protected branches
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupProtectedBranch> protectedBranches() throws IOException {
        return sdk.gitLab().grouplevelprotectedbranches().list(id).stream()
                .map(listed -> new GroupProtectedBranch(this, listed));
    }

    /**
     * Loads a protected branch ({@code GET /groups/{id}/protected_branches/{name}}). Premium.
     *
     * @param name the branch name or wildcard, which may contain slashes; the core encodes it
     * @return the protected branch, or empty when GitLab answers HTTP 404
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if the name is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<GroupProtectedBranch> protectedBranch(final String name) throws IOException {
        return sdk.gitLab()
                .grouplevelprotectedbranches()
                .get(id, requireNotBlank(name, NAME))
                .map(loaded -> new GroupProtectedBranch(this, loaded));
    }

    /**
     * Protects a branch or wildcard in every project of the group with the default rules of GitLab ({@code POST
     * /groups/{id}/protected_branches}); only the name is sent. Premium.
     *
     * @param name the branch name or wildcard, which may contain slashes; the core encodes it
     * @return the protected branch
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if the name is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupProtectedBranch protectBranch(final String name) throws IOException {
        final GitLabGroupGroupLevelProtectedBranchesCreatePayloadBuilder builder =
                GitLabGroupGroupLevelProtectedBranchesCreatePayloadBuilder.builder()
                        .name(requireNotBlank(name, NAME));
        return new GroupProtectedBranch(
                this, sdk.gitLab().grouplevelprotectedbranches().create(id, builder.build()));
    }

    /**
     * Protects a branch or wildcard in every project of the group and says who may push and merge ({@code POST
     * /groups/{id}/protected_branches}). Premium.
     *
     * @param name the branch name or wildcard, which may contain slashes; the core encodes it
     * @param push who may push
     * @param merge who may merge
     * @return the protected branch
     * @throws IllegalArgumentException if the name is blank
     * @throws NullPointerException if an argument is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupProtectedBranch protectBranch(
            final String name, final ProtectedAccess push, final ProtectedAccess merge) throws IOException {
        requireNotBlank(name, NAME);
        final GitLabGroupGroupLevelProtectedBranchesCreatePayloadBuilder builder =
                GitLabGroupGroupLevelProtectedBranchesCreatePayloadBuilder.builder()
                        .name(name)
                        .pushAccessLevel(GitLabGroupGroupLevelProtectedBranchesCreatePushAccessLevel.fromWire(
                                requireNonNull(push, "push").value()))
                        .mergeAccessLevel(GitLabGroupGroupLevelProtectedBranchesCreateMergeAccessLevel.fromWire(
                                requireNonNull(merge, "merge").value()));
        return new GroupProtectedBranch(
                this, sdk.gitLab().grouplevelprotectedbranches().create(id, builder.build()));
    }

    /**
     * Lists the milestones of the group ({@code GET /groups/{id}/milestones}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the milestones
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupMilestone> milestones() throws IOException {
        return sdk.gitLab().groupmilestones().list(id).stream().map(listed -> new GroupMilestone(this, listed));
    }

    /**
     * Lists the milestones of the group that match a filter ({@code GET /groups/{id}/milestones}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (state, iids, title, search, dates, sort, parent milestones, descendants)
     * @return the milestones
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupMilestone> milestones(final GitLabGroupGroupMilestonesListFilter filter) throws IOException {
        return sdk.gitLab().groupmilestones().list(id, requireNonNull(filter, "filter")).stream()
                .map(listed -> new GroupMilestone(this, listed));
    }

    /**
     * Loads a milestone ({@code GET /groups/{id}/milestones/{milestone_id}}).
     *
     * @param milestoneId the id of the milestone (not its iid)
     * @return the milestone, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupMilestone> milestone(final long milestoneId) throws IOException {
        return sdk.gitLab().groupmilestones().get(id, milestoneId).map(loaded -> new GroupMilestone(this, loaded));
    }

    /**
     * Creates a milestone with a title ({@code POST /groups/{id}/milestones}).
     *
     * @param title the title
     * @return the created milestone
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     */
    public GroupMilestone createMilestone(final String title) throws IOException {
        return createMilestone(GitLabGroupGroupMilestonesCreatePayloadBuilder.builder()
                .title(requireNotBlank(title, "title"))
                .build());
    }

    /**
     * Creates a milestone from the core's payload ({@code POST /groups/{id}/milestones}), for the fields the SDK does
     * not wrap, such as the description and the dates.
     *
     * @param payload the request body
     * @return the created milestone
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupMilestone createMilestone(final GitLabGroupGroupMilestonesCreatePayload payload) throws IOException {
        return new GroupMilestone(this, sdk.gitLab().groupmilestones().create(id, requireNonNull(payload, "payload")));
    }

    /**
     * Lists the labels of the group ({@code GET /groups/{id}/labels}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the labels
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupLabel> labels() throws IOException {
        return sdk.gitLab().grouplabels().list(id).stream().map(listed -> new GroupLabel(this, listed));
    }

    /**
     * Lists the labels of the group that match a filter ({@code GET /groups/{id}/labels}). Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the core's filter (counts, ancestor and descendant groups, group labels only, archived, search)
     * @return the labels
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupLabel> labels(final GitLabGroupGroupLabelsListFilter filter) throws IOException {
        return sdk.gitLab().grouplabels().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new GroupLabel(this, listed));
    }

    /**
     * Loads a label by its name ({@code GET /groups/{id}/labels/{name}}). A name with a slash or a space reaches the
     * core unchanged.
     *
     * @param name the label name
     * @return the label, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GroupLabel> label(final String name) throws IOException {
        return sdk.gitLab()
                .grouplabels()
                .get(id, requireNotBlank(name, NAME))
                .map(loaded -> new GroupLabel(this, loaded));
    }

    /**
     * Creates a label ({@code POST /groups/{id}/labels}).
     *
     * @param name the label name
     * @param color the color, as a hexadecimal value with a leading {@code #} or a CSS color name
     * @return the created label
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public GroupLabel createLabel(final String name, final String color) throws IOException {
        final GitLabGroupGroupLabelsCreatePayload payload = GitLabGroupGroupLabelsCreatePayloadBuilder.builder()
                .name(requireNotBlank(name, NAME))
                .color(requireNotBlank(color, "color"))
                .build();
        return new GroupLabel(this, sdk.gitLab().grouplabels().create(id, payload));
    }

    /**
     * Lists the CI/CD variables of the group ({@code GET /groups/{id}/variables}). Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}. The values are
     * secrets, do not log them.
     *
     * @return the variables
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupVariable> variables() throws IOException {
        return sdk.gitLab().grouplevelvariables().list(id).stream().map(listed -> new GroupVariable(this, listed));
    }

    /**
     * Loads a variable by its key ({@code GET /groups/{id}/variables/{key}}). A key with special characters reaches the
     * core unchanged. GitLab answers with one variable even if several share the key; the core's group API cannot name
     * an environment scope.
     *
     * @param key the key of the variable
     * @return the variable, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public Optional<GroupVariable> variable(final String key) throws IOException {
        return sdk.gitLab()
                .grouplevelvariables()
                .get(id, requireNotBlank(key, "key"))
                .map(loaded -> new GroupVariable(this, loaded));
    }

    /**
     * Creates a variable with a key and a value ({@code POST /groups/{id}/variables}). GitLab's defaults decide the
     * type, the scope and the flags; an empty value is sent as given. Use
     * {@link #createVariable(GitLabGroupGroupLevelVariablesCreatePayload)} for the other fields, such as the
     * environment scope, masking and the hidden flag.
     *
     * @param key the key of the variable
     * @param value the value; it is secret, do not log it
     * @return the new variable
     * @throws NullPointerException if {@code key} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public GroupVariable createVariable(final String key, final String value) throws IOException {
        return createVariable(GitLabGroupGroupLevelVariablesCreatePayloadBuilder.builder()
                .key(requireNotBlank(key, "key"))
                .value(requireNonNull(value, "value"))
                .build());
    }

    /**
     * Creates a variable from the core's payload ({@code POST /groups/{id}/variables}), for the fields the SDK does not
     * wrap, such as the environment scope, the type, masking and {@code maskedAndHidden}.
     *
     * @param payload the request body
     * @return the new variable
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupVariable createVariable(final GitLabGroupGroupLevelVariablesCreatePayload payload) throws IOException {
        return new GroupVariable(
                this, sdk.gitLab().grouplevelvariables().create(id, requireNonNull(payload, "payload")));
    }

    /**
     * Lists the runners of the group ({@code GET /groups/{id}/runners}). GitLab lists a summary of each runner, so each
     * one is loaded ({@code GET /runners/{id}}) as the stream reaches it, and one that is gone by then is skipped.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the runners
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Runner> runners() throws IOException {
        return loadedRunners(sdk.gitLab().groups().listRunners(id).stream());
    }

    /**
     * Lists the runners of the group that match a filter ({@code GET /groups/{id}/runners}), loaded one by one as
     * described at {@link #runners()}.
     *
     * @param filter the query parameters
     * @return the runners
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Runner> runners(final GitLabGroupGroupsListRunnersFilter filter) throws IOException {
        final GitLabGroupGroupsListRunnersFilter checked = requireNonNull(filter, FILTER);
        return loadedRunners(sdk.gitLab().groups().listRunners(id, checked).stream());
    }

    /**
     * Resets the runner registration token of the group ({@code POST /groups/{id}/runners/reset_registration_token}).
     * The old token stops working, and the new one is secret, do not log it.
     *
     * @return the new token
     * @throws IOException if the transport fails
     */
    public RegistrationToken resetRunnerRegistrationToken() throws IOException {
        return new RegistrationToken(sdk.gitLab().groups().resetRegistrationTokenRunner(id));
    }

    private Stream<Runner> loadedRunners(final Stream<GitLabCiRunnerWithUuid> listed) {
        return listed.map(item -> Lookups.find(() -> sdk.runner(requireNonNull(item.id(), "id"))))
                .flatMap(Optional::stream);
    }

    private GroupMember createMember(
            final @Nullable String userId, final @Nullable String username, final AccessLevel level)
            throws IOException {
        final GitLabGroupMembersCreatePayload payload = GitLabGroupMembersCreatePayloadBuilder.builder()
                .userId(userId)
                .username(username)
                .accessLevel(requireNonNull(level, "level").value())
                .build();
        return new GroupMember(this, sdk.gitLab().members().group().create(id, payload));
    }
}
