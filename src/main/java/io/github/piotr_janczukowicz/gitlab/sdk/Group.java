package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsShareGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsSharePayload;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupAccessRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupMember;
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

    static {
        Internals.registerGroupAccess(group -> group.sdk.gitLab());
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
     * Stops sharing the group with another group ({@code DELETE /groups/{id}/share/{group_id}}).
     *
     * @param groupId the id of the group the sharing is removed for
     * @throws IOException if the transport fails
     */
    public void unshare(final long groupId) throws IOException {
        sdk.gitLab().groups().deleteShare(id, groupId);
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
     * @return the members
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GroupMember> allMembers() throws IOException {
        return sdk.gitLab().members().group().listAll(id).stream().map(listed -> new GroupMember(this, listed));
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
