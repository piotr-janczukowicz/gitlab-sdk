package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.accessrequests.GitLabAccessRequester;
import io.github.piotr_janczukowicz.gitlab.api.accessrequests.GitLabGroupAccessRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;

/**
 * A pending request of a user for access to a {@link Group}, as GitLab returned it. The object is immutable;
 * {@link #response()} gives the complete response. A request is addressed by the numeric id of its user.
 */
public final class GroupAccessRequest {

    private final Group group;
    private final GitLabAccessRequester response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupAccessRequest(final Group group, final GitLabAccessRequester response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the request is for.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the numeric id of the requesting user.
     *
     * @return the user id
     */
    public long userId() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the username of the requesting user.
     *
     * @return the username
     */
    public String username() {
        return requireNonNull(response.username(), "username");
    }

    /**
     * Approves the request ({@code PUT /groups/{id}/access_requests/{user_id}/approve}).
     *
     * @param level the access level to grant
     * @return the new member as GitLab returned it
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IOException if the transport fails
     */
    public GroupMember approve(final AccessLevel level) throws IOException {
        final GitLabGroupAccessRequestsApprovePayload payload = new GitLabGroupAccessRequestsApprovePayload(
                requireNonNull(level, "level").value());
        return new GroupMember(group, gitLab().accessrequests().group().approve(group.id(), userId(), payload));
    }

    /**
     * Denies the request ({@code DELETE /groups/{id}/access_requests/{user_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void deny() throws IOException {
        gitLab().accessrequests().group().delete(group.id(), userId());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabAccessRequester response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
