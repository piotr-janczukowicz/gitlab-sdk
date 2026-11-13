package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
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
     * /groups/{id}}), because GitLab answers the transfer with no body.
     *
     * @param newParentGroupId the id of the new parent group
     * @return the moved group as GitLab returns it afterwards
     * @throws IllegalStateException if the group is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Group transfer(final long newParentGroupId) throws IOException {
        sdk.gitLab().groups().transfer(id, new GitLabGroupGroupsTransferPayload(newParentGroupId));
        return refresh();
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
}
