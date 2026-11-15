package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.FILTER;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.IS_GONE;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.NAME;
import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.loadedRunners;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensListFilter;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBasicBadgeDetails;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.groupbadges.GitLabGroupGroupBadgesCreatePayloadBuilder;
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
import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupGroupPushRulesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsCreateApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListPackagesFilter;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListRunnersFilter;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsShareGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsSharePayload;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabGroupIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksCreateGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksDeleteFilter;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksDeleteFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabGroupMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLDeleteFilter;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLDeleteFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLGetFilter;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLGetFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.GroupApprovalRule;
import io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditEvents;
import io.github.piotr_janczukowicz.gitlab.sdk.badges.GroupBadge;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.GroupCreatedDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.GroupDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.TokenRequests;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.GroupLabel;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupAccessRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupMember;
import io.github.piotr_janczukowicz.gitlab.sdk.members.LdapGroupLink;
import io.github.piotr_janczukowicz.gitlab.sdk.members.SamlGroupLink;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.milestones.GroupMilestone;
import io.github.piotr_janczukowicz.gitlab.sdk.packages.ProjectPackage;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.GroupProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.pushrules.GroupPushRules;
import io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryRepository;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnerToken;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.AccessTokenScope;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.CreatedGroupAccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.GroupAccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.GroupVariable;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.GroupWebhook;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhookEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.wikis.Wiki;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A group, identified by its numeric id or plain full path (the core encodes it); it may be nested. A handle from
 * {@link GitLabSdk#group(String)} holds no data from GitLab; {@link #fetch()} returns a loaded group. The object is
 * immutable: every operation returns a new group built from GitLab's answer.
 */
public final class Group {

    private static final String PROVIDER = "provider";
    private static final String LEVEL = "level";
    private static final String PAYLOAD = "payload";

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
                        requireNonNull(level, LEVEL).value()),
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
     * Lists all visible descendant groups, at any depth ({@code GET /groups/{id}/descendant_groups}). Per GitLab's API
     * description, unauthenticated requests get only public groups, {@code all_available} defaults to {@code false} for
     * a user, and the order is by name ascending. GitLab lists a summary of each group, so each one is loaded
     * ({@code GET /groups/{id}}) as the stream reaches it, and one that was deleted in the meantime is skipped. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the loaded groups
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Group> descendantGroups() throws IOException {
        return sdk.loadedGroups(sdk.gitLab().groups().listDescendantGroups(id).stream());
    }

    /**
     * Lists the groups GitLab's API description calls shared with this group ({@code GET /groups/{id}/groups/shared}),
     * ordered by name ascending; unauthenticated requests get only public groups. Which side of the sharing the listed
     * groups are on is not verified. GitLab lists a summary of each group, so each one is loaded ({@code GET
     * /groups/{id}}) as the stream reaches it, and one that was deleted in the meantime is skipped. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}. The
     * stream is single-use.
     *
     * @return the loaded groups
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Group> sharedGroups() throws IOException {
        return sdk.loadedGroups(sdk.gitLab().groups().listShared(id).stream());
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
     * @return the webhook, or empty when GitLab answers HTTP 404 (expected in some cases where the feature is not
     *     available); a 403 or the core's {@code GitLabTierException} passes through
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
        return new GroupWebhook(this, sdk.gitLab().groupwebhooks().create(id, requireNonNull(payload, PAYLOAD)));
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
     * @return the protected branch, or empty when GitLab answers HTTP 404 (expected in some cases where the feature is
     *     not available); a 403 or the core's {@code GitLabTierException} passes through
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
        return new GroupMilestone(this, sdk.gitLab().groupmilestones().create(id, requireNonNull(payload, PAYLOAD)));
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
        return new GroupVariable(this, sdk.gitLab().grouplevelvariables().create(id, requireNonNull(payload, PAYLOAD)));
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
        return loadedRunners(sdk, sdk.gitLab().groups().listRunners(id).stream());
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
        return loadedRunners(sdk, sdk.gitLab().groups().listRunners(id, checked).stream());
    }

    /**
     * Resets the runner registration token of the group ({@code POST /groups/{id}/runners/reset_registration_token}).
     * The old token stops working, and the new one is secret, do not log it.
     *
     * @return the new token
     * @throws IOException if the transport fails
     */
    public RunnerToken resetRunnerRegistrationToken() throws IOException {
        return new RunnerToken(sdk.gitLab().groups().resetRegistrationTokenRunner(id));
    }

    /**
     * Lists the packages of the group, including its subgroups ({@code GET /groups/{id}/packages}). Each package
     * belongs to its own project: {@link ProjectPackage#project()} returns a handle of the project named by its
     * {@code project_id}, without a request. Pages are fetched only as the stream is consumed; transport failures
     * during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the packages
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ProjectPackage> packages() throws IOException {
        return sdk.gitLab().groups().listPackages(id).stream().map(item -> new ProjectPackage(sdk, item));
    }

    /**
     * Lists the packages of the group that match a filter ({@code GET /groups/{id}/packages}), as described at
     * {@link #packages()}.
     *
     * @param filter the query parameters
     * @return the packages
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ProjectPackage> packages(final GitLabGroupGroupsListPackagesFilter filter) throws IOException {
        final GitLabGroupGroupsListPackagesFilter checked = requireNonNull(filter, FILTER);
        return sdk.gitLab().groups().listPackages(id, checked).stream().map(item -> new ProjectPackage(sdk, item));
    }

    /**
     * Lists the container registry repositories of the group ({@code GET /groups/{id}/registry/repositories}). Each
     * repository belongs to its own project: {@link RegistryRepository#project()} returns a handle of the project named
     * by its {@code project_id}, without a request. The core offers no filter for this list. Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the repositories
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryRepository> registryRepositories() throws IOException {
        return sdk.gitLab().groups().listRegistryRepositories(id).stream()
                .map(item -> new RegistryRepository(sdk, item));
    }

    /**
     * Returns the wiki of the group (Premium): its pages and attachments. No request is made.
     *
     * @return the wiki
     */
    public Wiki wiki() {
        return Wiki.forGroup(this);
    }

    /**
     * Returns the audit events of the group (Premium): a read-only list, newest first, and a lookup by id (which needs
     * the Owner role). No request is made.
     *
     * @return the audit events of the group
     */
    public AuditEvents auditEvents() {
        return AuditEvents.forGroup(this);
    }

    /**
     * Lists the issues of the group and its subgroups ({@code GET /groups/{id}/issues}). Unlike the instance list, this
     * one is not limited to the caller's own issues: GitLab's {@code scope} defaults to {@code all} (see <a
     * href="https://docs.gitlab.com/api/issues/">the GitLab docs</a>). Each issue is addressed through the project
     * GitLab names for it. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return issues(GitLabGroupIssuesListFilter.empty());
    }

    /**
     * Lists the issues of the group and its subgroups that match a filter ({@code GET /groups/{id}/issues}). Each issue
     * is addressed through the project GitLab names for it. Pages are fetched only as the stream is consumed; transport
     * failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the query parameters of the core
     * @return the issues
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues(final GitLabGroupIssuesListFilter filter) throws IOException {
        return sdk.gitLab().issues().group().list(id, requireNonNull(filter, FILTER)).stream()
                .map(sdk::issueOf);
    }

    /**
     * Lists the merge requests of the group and its subgroups ({@code GET /groups/{id}/merge_requests}). GitLab's
     * {@code scope} defaults to {@code all} (see <a href="https://docs.gitlab.com/api/merge_requests/">the GitLab
     * docs</a>). GitLab lists a summary of each merge request, so each one is loaded ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}}) through the project GitLab names for it as the stream reaches
     * it, and one that is gone by then is skipped. Pages are fetched only as the stream is consumed; transport failures
     * during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return mergeRequests(GitLabGroupMergeRequestsListFilter.empty());
    }

    /**
     * Lists the merge requests of the group and its subgroups that match a filter ({@code GET
     * /groups/{id}/merge_requests}); GitLab's {@code scope} defaults to {@code all}. Each one is loaded as the stream
     * reaches it, and one that is gone by then is skipped, as for {@link #mergeRequests()}.
     *
     * @param filter the query parameters of the core
     * @return the merge requests
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests(final GitLabGroupMergeRequestsListFilter filter) throws IOException {
        return sdk.loadMergeRequests(
                sdk.gitLab().mergerequests().group().list(id, requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Lists the badges of the group ({@code GET /groups/{id}/badges}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the badges
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupBadge> badges() throws IOException {
        return sdk.gitLab().groupbadges().list(id).stream().map(listed -> new GroupBadge(this, listed));
    }

    /**
     * Loads a badge ({@code GET /groups/{id}/badges/{badge_id}}).
     *
     * @param badgeId the id of the badge
     * @return the badge, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupBadge> badge(final long badgeId) throws IOException {
        return sdk.gitLab().groupbadges().get(id, badgeId).map(loaded -> new GroupBadge(this, loaded));
    }

    /**
     * Adds a badge ({@code POST /groups/{id}/badges}). Both URLs may hold placeholders.
     *
     * @param linkUrl the URL the badge links to
     * @param imageUrl the URL of the badge image
     * @return the new badge
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public GroupBadge addBadge(final String linkUrl, final String imageUrl) throws IOException {
        return createBadge(linkUrl, imageUrl, null);
    }

    /**
     * Adds a named badge ({@code POST /groups/{id}/badges}).
     *
     * @param linkUrl the URL the badge links to
     * @param imageUrl the URL of the badge image
     * @param name the name of the badge
     * @return the new badge
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public GroupBadge addBadge(final String linkUrl, final String imageUrl, final String name) throws IOException {
        return createBadge(linkUrl, imageUrl, requireNotBlank(name, NAME));
    }

    /**
     * Shows how a badge would look with its placeholders replaced, without creating it ({@code GET
     * /groups/{id}/badges/render}). The core's record is returned as it is.
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
        return sdk.gitLab().groupbadges().getRender(id, link, image);
    }

    /**
     * Loads the push rules of the group ({@code GET /groups/{id}/push_rule}). Per GitLab's API description, push rules
     * are a Premium feature: on a lower tier the core's {@code GitLabTierException} passes through. The core throws for
     * HTTP 404, and the SDK turns that into an empty result. The GitLab API description only says "Not Found" for this
     * operation, so that a group without push rules is answered with 404 is an assumption, not verified live; if GitLab
     * answers 200 with a JSON {@code null} instead, the core throws {@link IllegalStateException}, and the SDK does not
     * catch it.
     *
     * @return the push rules, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupPushRules> pushRules() throws IOException {
        try {
            return Optional.of(
                    new GroupPushRules(this, sdk.gitLab().grouppushrules().get(id)));
        } catch (final GitLabNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates the push rules of the group from the core's payload ({@code POST /groups/{id}/push_rule}); change them
     * later with {@link GroupPushRules#update()}. Premium, as above.
     *
     * @param payload the request body
     * @return the new push rules
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupPushRules createPushRules(final GitLabGroupGroupPushRulesCreatePayload payload) throws IOException {
        final GitLabGroupGroupPushRulesCreatePayload checked = requireNonNull(payload, PAYLOAD);
        return new GroupPushRules(this, sdk.gitLab().grouppushrules().create(id, checked));
    }

    /**
     * Lists the access tokens of the group ({@code GET /groups/{id}/access_tokens}). The core's call returns the whole
     * list in one request, so the stream is a view of that list. A token with the Planner or the Security Manager role
     * cannot be decoded by the core's response type; that is a known gap of the core, not checked live.
     *
     * @return the access tokens
     * @throws IOException if the request fails
     */
    public Stream<GroupAccessToken> accessTokens() throws IOException {
        return sdk.gitLab().accesstokens().group().list(id).stream().map(listed -> new GroupAccessToken(this, listed));
    }

    /**
     * Lists the access tokens of the group that match a filter ({@code GET /groups/{id}/access_tokens}).
     *
     * @param filter the query parameters of the core
     * @return the access tokens
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the request fails
     */
    public Stream<GroupAccessToken> accessTokens(final GitLabGroupAccessTokensListFilter filter) throws IOException {
        return sdk.gitLab().accesstokens().group().list(id, requireNonNull(filter, FILTER)).stream()
                .map(listed -> new GroupAccessToken(this, listed));
    }

    /**
     * Loads an access token ({@code GET /groups/{id}/access_tokens/{token_id}}).
     *
     * @param tokenId the id of the token
     * @return the token, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GroupAccessToken> accessToken(final long tokenId) throws IOException {
        return sdk.gitLab()
                .accesstokens()
                .group()
                .get(id, String.valueOf(tokenId))
                .map(loaded -> new GroupAccessToken(this, loaded));
    }

    /**
     * Creates an access token ({@code POST /groups/{id}/access_tokens}). The secret of the token is in the answer only;
     * GitLab never returns it again. The role must be one of {@code GUEST}, {@code REPORTER}, {@code DEVELOPER},
     * {@code MAINTAINER} or {@code OWNER}: the core's response type has no value for the other roles, so the answer of
     * such a creation could not be decoded and the secret would be lost, and the SDK refuses them before the request.
     *
     * @param name the name of the token
     * @param scopes what the token may be used for
     * @param expiresAt the day the token expires, or {@code null} to leave it to GitLab
     * @param level the role of the token in the group
     * @return the new token with its secret
     * @throws NullPointerException if {@code name}, {@code scopes}, a scope or {@code level} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank, no scope is given, or the role is not supported
     * @throws IOException if the transport fails
     */
    public CreatedGroupAccessToken createAccessToken(
            final String name,
            final Collection<AccessTokenScope> scopes,
            final @Nullable LocalDate expiresAt,
            final AccessLevel level)
            throws IOException {
        final GitLabGroupAccessTokensCreatePayloadBuilder payload =
                GitLabGroupAccessTokensCreatePayloadBuilder.builder()
                        .name(requireNotBlank(name, NAME))
                        .scopes(TokenRequests.requireScopes(scopes).stream()
                                .map(scope -> GitLabGroupAccessTokensCreateScopes.valueOf(scope.name()))
                                .toList())
                        .accessLevel(GitLabGroupAccessTokensCreateAccessLevel.fromWire(
                                TokenRequests.requireTokenLevel(level).value()))
                        .expiresAt(expiresAt);
        return new CreatedGroupAccessToken(
                this, sdk.gitLab().accesstokens().group().create(id, payload.build()));
    }

    /**
     * Lists the approval rules of the group ({@code GET /groups/{id}/approval_rules}). Per GitLab's API description,
     * they are a Premium feature: on a lower tier the core's {@code GitLabTierException} passes through. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}. The installed core types the protected branches of a group rule as one
     * object where GitLab's API description has a list, so a rule that has any may not decode until the core is
     * rebuilt; this is not checked live.
     *
     * @return the approval rules
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupApprovalRule> approvalRules() throws IOException {
        return sdk.gitLab().groups().listApprovalRules(id).stream().map(listed -> new GroupApprovalRule(this, listed));
    }

    /**
     * Creates an approval rule that has no approvers yet ({@code POST /groups/{id}/approval_rules}). Name the approvers
     * with {@link #createApprovalRule(String, int, List, List)} or later with {@link GroupApprovalRule#update()}. No
     * rule type is sent, because GitLab's API description says not to use it. Premium, as above.
     *
     * @param name the rule name
     * @param approvalsRequired the number of approvals the rule needs
     * @return the created rule
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank or {@code approvalsRequired} is negative
     * @throws IOException if the transport fails
     */
    public GroupApprovalRule createApprovalRule(final String name, final int approvalsRequired) throws IOException {
        return createApprovalRule(name, approvalsRequired, List.of(), List.of());
    }

    /**
     * Creates an approval rule with its approvers ({@code POST /groups/{id}/approval_rules}). An empty list sends
     * nothing for that field. Only the current approver fields are offered: the core's payload also has {@code users}
     * and {@code groups}, which GitLab's API description marks as deprecated. No rule type is sent. Premium, as above.
     *
     * @param name the rule name
     * @param approvalsRequired the number of approvals the rule needs
     * @param userIds the numeric ids of the approving users
     * @param groupIds the numeric ids of the groups whose members approve
     * @return the created rule
     * @throws NullPointerException if an argument or an id is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank or {@code approvalsRequired} is negative
     * @throws IOException if the transport fails
     */
    public GroupApprovalRule createApprovalRule(
            final String name, final int approvalsRequired, final List<Long> userIds, final List<Long> groupIds)
            throws IOException {
        if (approvalsRequired < 0) {
            throw new IllegalArgumentException("approvalsRequired must not be negative");
        }
        final GitLabGroupGroupsCreateApprovalRulePayloadBuilder builder =
                GitLabGroupGroupsCreateApprovalRulePayloadBuilder.builder()
                        .name(requireNotBlank(name, NAME))
                        .approvalsRequired(approvalsRequired);
        if (!requireNonNull(userIds, "userIds").isEmpty()) {
            builder.userIds(List.copyOf(userIds));
        }
        if (!requireNonNull(groupIds, "groupIds").isEmpty()) {
            builder.groupIds(List.copyOf(groupIds));
        }
        return new GroupApprovalRule(this, sdk.gitLab().groups().createApprovalRule(id, builder.build()));
    }

    /**
     * Downloads the avatar of the group to a stream ({@code GET /groups/{id}/avatar}). The stream is not closed. A
     * group without an avatar is answered with an error of the core. Uploading an avatar is not offered.
     *
     * @param out where to write the image
     * @return the number of bytes written
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or writing fails
     */
    public long avatar(final OutputStream out) throws IOException {
        final OutputStream checked = requireNonNull(out, "out");
        return sdk.gitLab().groups().getAvatar(id, checked);
    }

    /**
     * Downloads the avatar of the group to a file ({@code GET /groups/{id}/avatar}).
     *
     * @param target the file to write
     * @return the file that was written
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IOException if the transport fails or writing fails
     */
    public Path avatar(final Path target) throws IOException {
        final Path checked = requireNonNull(target, "target");
        return sdk.gitLab().groups().getAvatar(id, checked);
    }

    /**
     * Lists the LDAP group links of the group ({@code GET /groups/{id}/ldap_group_links}). Per GitLab's API
     * description, LDAP group links are a Premium feature: on a lower tier the core's {@code GitLabTierException}
     * passes through. The core's call returns the whole list in one request, so the stream is a view of that list.
     *
     * @return the links
     * @throws IOException if the request fails
     */
    public Stream<LdapGroupLink> ldapLinks() throws IOException {
        return sdk.gitLab().ldapgrouplinks().list(id).stream().map(listed -> new LdapGroupLink(this, listed));
    }

    /**
     * Links an LDAP group, named by its CN, to the group ({@code POST /groups/{id}/ldap_group_links}). Premium, as
     * above. The role can be any {@link AccessLevel}.
     *
     * @param provider the name of the LDAP provider, which GitLab requires
     * @param cn the CN of the LDAP group
     * @param level the role the members of the LDAP group get
     * @return the new link
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code provider} or {@code cn} is blank
     * @throws IOException if the transport fails
     */
    public LdapGroupLink addLdapLink(final String provider, final String cn, final AccessLevel level)
            throws IOException {
        return createLdapLink(
                GitLabGroupLDAPGroupLinksCreatePayloadBuilder.builder().cn(requireNotBlank(cn, "cn")), provider, level);
    }

    /**
     * Links an LDAP group, selected by an LDAP filter, to the group ({@code POST /groups/{id}/ldap_group_links}).
     * Premium, as above.
     *
     * @param provider the name of the LDAP provider, which GitLab requires
     * @param filter the LDAP filter
     * @param level the role the members of the LDAP group get
     * @return the new link
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code provider} or {@code filter} is blank
     * @throws IOException if the transport fails
     */
    public LdapGroupLink addLdapFilterLink(final String provider, final String filter, final AccessLevel level)
            throws IOException {
        return createLdapLink(
                GitLabGroupLDAPGroupLinksCreatePayloadBuilder.builder().filter(requireNotBlank(filter, "filter")),
                provider,
                level);
    }

    /**
     * Deletes the link of an LDAP group, named by its CN ({@code DELETE /groups/{id}/ldap_group_links} with the
     * provider and the CN). The core also has two calls that put the CN in the path; GitLab's API description marks
     * them deprecated, so the SDK does not use them. Premium, as above.
     *
     * @param provider the name of the LDAP provider, which GitLab requires
     * @param cn the CN of the LDAP group
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public void deleteLdapLink(final String provider, final String cn) throws IOException {
        final String checkedProvider = requireNotBlank(provider, PROVIDER);
        final GitLabGroupLDAPGroupLinksDeleteFilter filter = GitLabGroupLDAPGroupLinksDeleteFilterBuilder.builder()
                .cn(requireNotBlank(cn, "cn"))
                .build();
        sdk.gitLab().ldapgrouplinks().delete(id, checkedProvider, filter);
    }

    /**
     * Deletes the link of an LDAP group that was selected by a filter ({@code DELETE /groups/{id}/ldap_group_links}
     * with the provider and the filter). Premium, as above.
     *
     * @param provider the name of the LDAP provider, which GitLab requires
     * @param filter the LDAP filter of the link
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public void deleteLdapFilterLink(final String provider, final String filter) throws IOException {
        final String checkedProvider = requireNotBlank(provider, PROVIDER);
        final GitLabGroupLDAPGroupLinksDeleteFilter deleteFilter =
                GitLabGroupLDAPGroupLinksDeleteFilterBuilder.builder()
                        .filter(requireNotBlank(filter, "filter"))
                        .build();
        sdk.gitLab().ldapgrouplinks().delete(id, checkedProvider, deleteFilter);
    }

    /**
     * Lists the SAML group links of the group ({@code GET /groups/{id}/saml_group_links}). Per GitLab's API
     * description, SAML group links are a Premium feature: on a lower tier the core's {@code GitLabTierException}
     * passes through. The core's call returns the whole list in one request, so the stream is a view of that list.
     *
     * @return the links
     * @throws IOException if the request fails
     */
    public Stream<SamlGroupLink> samlLinks() throws IOException {
        return sdk.gitLab().saml().list(id).stream().map(listed -> new SamlGroupLink(this, listed));
    }

    /**
     * Loads a SAML group link ({@code GET /groups/{id}/saml_group_links/{saml_group_name}}). Premium, as above. When
     * the group has links of that name for several providers, GitLab answers with an error that asks for the provider;
     * use {@link #samlLink(String, String)}.
     *
     * @param name the name of the SAML group
     * @return the link, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Optional<SamlGroupLink> samlLink(final String name) throws IOException {
        final String checkedName = requireNotBlank(name, NAME);
        return sdk.gitLab().saml().get(id, checkedName).map(loaded -> new SamlGroupLink(this, loaded));
    }

    /**
     * Loads the SAML group link of a provider ({@code GET /groups/{id}/saml_group_links/{saml_group_name}} with the
     * provider). Premium, as above.
     *
     * @param name the name of the SAML group
     * @param provider the name of the SAML provider
     * @return the link, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Optional<SamlGroupLink> samlLink(final String name, final String provider) throws IOException {
        final String checkedName = requireNotBlank(name, NAME);
        final GitLabGroupSAMLGetFilter filter = GitLabGroupSAMLGetFilterBuilder.builder()
                .provider(requireNotBlank(provider, PROVIDER))
                .build();
        return sdk.gitLab().saml().get(id, checkedName, filter).map(loaded -> new SamlGroupLink(this, loaded));
    }

    /**
     * Links a SAML group to the group ({@code POST /groups/{id}/saml_group_links}). Premium, as above. The role can be
     * any {@link AccessLevel}.
     *
     * @param name the name of the SAML group
     * @param level the role the members of the SAML group get
     * @return the new link
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public SamlGroupLink addSamlLink(final String name, final AccessLevel level) throws IOException {
        return createSamlLink(GitLabGroupSAMLCreatePayloadBuilder.builder(), name, level);
    }

    /**
     * Links a SAML group of a provider to the group ({@code POST /groups/{id}/saml_group_links}). Premium, as above.
     *
     * @param name the name of the SAML group
     * @param level the role the members of the SAML group get
     * @param provider the name of the SAML provider the link applies to
     * @return the new link
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code provider} is blank
     * @throws IOException if the transport fails
     */
    public SamlGroupLink addSamlLink(final String name, final AccessLevel level, final String provider)
            throws IOException {
        return createSamlLink(
                GitLabGroupSAMLCreatePayloadBuilder.builder().provider(requireNotBlank(provider, PROVIDER)),
                name,
                level);
    }

    /**
     * Deletes a SAML group link ({@code DELETE /groups/{id}/saml_group_links/{saml_group_name}}). The core answers with
     * the deleted link, or with nothing when GitLab answers HTTP 404, and the SDK ignores that answer. Premium, as
     * above.
     *
     * @param name the name of the SAML group
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public void deleteSamlLink(final String name) throws IOException {
        final String checkedName = requireNotBlank(name, NAME);
        sdk.gitLab().saml().delete(id, checkedName);
    }

    /**
     * Deletes the SAML group link of a provider ({@code DELETE /groups/{id}/saml_group_links/{saml_group_name}} with
     * the provider). Premium, as above.
     *
     * @param name the name of the SAML group
     * @param provider the name of the SAML provider
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public void deleteSamlLink(final String name, final String provider) throws IOException {
        final String checkedName = requireNotBlank(name, NAME);
        final GitLabGroupSAMLDeleteFilter filter = GitLabGroupSAMLDeleteFilterBuilder.builder()
                .provider(requireNotBlank(provider, PROVIDER))
                .build();
        sdk.gitLab().saml().delete(id, checkedName, filter);
    }

    private GroupBadge createBadge(final String linkUrl, final String imageUrl, final @Nullable String name)
            throws IOException {
        final GitLabGroupGroupBadgesCreatePayloadBuilder payload = GitLabGroupGroupBadgesCreatePayloadBuilder.builder()
                .linkUrl(requireNotBlank(linkUrl, "linkUrl"))
                .imageUrl(requireNotBlank(imageUrl, "imageUrl"))
                .name(name);
        return new GroupBadge(this, sdk.gitLab().groupbadges().create(id, payload.build()));
    }

    private LdapGroupLink createLdapLink(
            final GitLabGroupLDAPGroupLinksCreatePayloadBuilder builder, final String provider, final AccessLevel level)
            throws IOException {
        final GitLabGroupLDAPGroupLinksCreatePayload payload = builder.provider(requireNotBlank(provider, PROVIDER))
                .groupAccess(GitLabGroupLDAPGroupLinksCreateGroupAccess.fromWire(
                        requireNonNull(level, LEVEL).value()))
                .build();
        return new LdapGroupLink(this, sdk.gitLab().ldapgrouplinks().create(id, payload));
    }

    private SamlGroupLink createSamlLink(
            final GitLabGroupSAMLCreatePayloadBuilder builder, final String name, final AccessLevel level)
            throws IOException {
        final GitLabGroupSAMLCreatePayload payload = builder.samlGroupName(requireNotBlank(name, NAME))
                .accessLevel(GitLabGroupSAMLCreateAccessLevel.fromWire(
                        requireNonNull(level, LEVEL).value()))
                .build();
        return new SamlGroupLink(this, sdk.gitLab().saml().create(id, payload));
    }

    private GroupMember createMember(
            final @Nullable String userId, final @Nullable String username, final AccessLevel level)
            throws IOException {
        final GitLabGroupMembersCreatePayload payload = GitLabGroupMembersCreatePayloadBuilder.builder()
                .userId(userId)
                .username(username)
                .accessLevel(requireNonNull(level, LEVEL).value())
                .build();
        return new GroupMember(this, sdk.gitLab().members().group().create(id, payload));
    }
}
