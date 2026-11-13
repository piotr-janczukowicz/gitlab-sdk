package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMember;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A member of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. A member
 * is addressed by the numeric id of its user. A member listed by {@link Project#allMembers()} may hold its membership
 * only through a parent group: {@link #refresh()} then loads it from the list with inherited members, while
 * {@link #update()} and {@link #remove()} act on direct memberships only and fail for such a member.
 */
public final class Member {

    static {
        Internals.registerAllMembersAccess(member -> new Member(member.project, member.response, true));
    }

    private final Project project;
    private final GitLabMember response;
    private final boolean listedWithInherited;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Member(final Project project, final GitLabMember response) {
        this(project, response, false);
    }

    private Member(final Project project, final GitLabMember response, final boolean listedWithInherited) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
        this.listedWithInherited = listedWithInherited;
    }

    /**
     * Returns the project the member belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
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
     * @throws NullPointerException if GitLab omits {@code access_level}
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
     * Starts a change of the access level or the expiry date (set or cleared). Nothing is sent until
     * {@link MemberUpdate#send()}. The change acts on the direct membership only: for a user who is a member only
     * through a parent group (listed by {@code allMembers()}), {@code send()} fails with the core's exception for
     * GitLab's HTTP 404.
     *
     * @return the update builder
     */
    public MemberUpdate update() {
        return new MemberUpdate(this);
    }

    /**
     * Removes the member from the project ({@code DELETE /projects/{id}/members/{user_id}}). This acts on the direct
     * membership only: for a user who is a member only through a parent group (listed by {@code allMembers()}), it
     * fails with the core's exception for GitLab's HTTP 404.
     *
     * @throws IOException if the transport fails
     */
    public void remove() throws IOException {
        gitLab().members().project().delete(project.id(), userId());
    }

    /**
     * Loads the member again. This object is not changed. A member built with the public constructor, or returned by
     * {@code member(userId)}, {@code members()}, {@code addMember(...)} or {@code update().send()}, is loaded as a
     * direct member ({@code GET /projects/{id}/members/{user_id}}). A member listed by {@code allMembers()} may be
     * inherited from a parent group, so it is loaded from the list with inherited members ({@code GET
     * /projects/{id}/members/all/{user_id}}), and the returned member is loaded that way again.
     *
     * @return the member as GitLab holds it now
     * @throws IllegalStateException if the user is no longer a member
     * @throws IOException if the transport fails
     */
    public Member refresh() throws IOException {
        final Optional<Member> loaded = listedWithInherited
                ? gitLab().members()
                        .project()
                        .getAll(project.id(), userId())
                        .map(found -> new Member(project, found, true))
                : project.member(userId());
        return loaded.orElseThrow(() -> new IllegalStateException("Member " + userId() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMember response() {
        return response;
    }

    Member apply(final GitLabProjectMembersUpdatePayload payload) throws IOException {
        return new Member(project, gitLab().members().project().update(project.id(), userId(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
