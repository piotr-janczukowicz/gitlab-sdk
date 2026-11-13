package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Collects changes to a {@link GroupMember} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class GroupMemberUpdate {

    private final GroupMember member;
    private final GitLabGroupMembersUpdatePayloadBuilder payload = GitLabGroupMembersUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param member the member to change
     * @throws NullPointerException if {@code member} is {@code null}
     */
    public GroupMemberUpdate(final GroupMember member) {
        this.member = requireNonNull(member, "member");
    }

    /**
     * Sets the access level.
     *
     * @param level the new level
     * @return this update
     * @throws NullPointerException if {@code level} is {@code null}
     */
    public GroupMemberUpdate accessLevel(final AccessLevel level) {
        payload.accessLevel(requireNonNull(level, "level").value());
        return changed();
    }

    /**
     * Sets the date the membership expires, sent as the start of that day in UTC. The core's payload leaves out an
     * absent value, so an expiry cannot be cleared through the SDK.
     *
     * @param date the expiry date
     * @return this update
     * @throws NullPointerException if {@code date} is {@code null}
     */
    public GroupMemberUpdate expiresAt(final LocalDate date) {
        payload.expiresAt(MemberValues.startOfDayUtc(date));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/members/{user_id}}). The member this update was made from is not
     * changed.
     *
     * @return the member as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupMember send() throws IOException {
        state.requireChange();
        return member.apply(payload.build());
    }

    private GroupMemberUpdate changed() {
        state.mark();
        return this;
    }
}
