package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMember;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A member of a {@link Group}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. A member
 * is addressed by the numeric id of its user.
 */
public final class GroupMember {

    private final Group group;
    private final GitLabMember response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupMember(final Group group, final GitLabMember response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the member belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the numeric id of the user.
     *
     * @return the user id
     */
    public long userId() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the username of the user.
     *
     * @return the username
     */
    public String username() {
        return requireNonNull(response.username(), "username");
    }

    /**
     * Returns the access level.
     *
     * @return the level
     * @throws IllegalArgumentException if GitLab returns a level the SDK does not know; {@link #accessLevelValue()}
     *     still gives the number
     */
    public AccessLevel accessLevel() {
        return AccessLevel.from(accessLevelValue());
    }

    /**
     * Returns the access level as the raw number GitLab returned, an escape hatch for a level {@link AccessLevel} does
     * not know.
     *
     * @return the access level number
     */
    public int accessLevelValue() {
        return MemberValues.accessLevelValue(response.accessLevel());
    }

    /**
     * Returns the date the membership expires.
     *
     * @return the date, or empty when the membership does not expire
     * @throws IllegalStateException if GitLab returns a value that is not an ISO date
     */
    public Optional<LocalDate> expiresAt() {
        return MemberValues.expiresAt(response.expiresAt());
    }

    /**
     * Starts a change of the access level or the expiry date. Nothing is sent until {@link GroupMemberUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupMemberUpdate update() {
        return new GroupMemberUpdate(this);
    }

    /**
     * Removes the member from the group ({@code DELETE /groups/{id}/members/{user_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void remove() throws IOException {
        gitLab().members().group().delete(group.id(), userId());
    }

    /**
     * Loads the member again ({@code GET /groups/{id}/members/{user_id}}). This object is not changed.
     *
     * @return the member as GitLab holds it now
     * @throws IllegalStateException if the user is no longer a member
     * @throws IOException if the transport fails
     */
    public GroupMember refresh() throws IOException {
        return group.member(userId()).orElseThrow(() -> new IllegalStateException("Member " + userId() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMember response() {
        return response;
    }

    GroupMember apply(final GitLabGroupMembersUpdatePayload payload) throws IOException {
        return new GroupMember(group, gitLab().members().group().update(group.id(), userId(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
