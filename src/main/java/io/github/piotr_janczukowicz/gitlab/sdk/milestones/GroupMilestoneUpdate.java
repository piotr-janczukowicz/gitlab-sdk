package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Collects changes to a {@link GroupMilestone} and sends them in one request. Only the fields that were set are sent,
 * and nothing is sent until {@link #send()}. {@code null} cannot be sent, so a date cannot be cleared; an empty
 * description is sent as given, and whether GitLab then clears it is not verified against a live instance. To close or
 * reopen the milestone use {@link GroupMilestone#close()} and {@link GroupMilestone#reopen()}.
 */
public final class GroupMilestoneUpdate {

    private final GroupMilestone milestone;
    private final GitLabGroupGroupMilestonesUpdatePayloadBuilder payload =
            GitLabGroupGroupMilestonesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param milestone the milestone to change
     * @throws NullPointerException if {@code milestone} is {@code null}
     */
    public GroupMilestoneUpdate(final GroupMilestone milestone) {
        this.milestone = requireNonNull(milestone, "milestone");
    }

    /**
     * Sets the title.
     *
     * @param title the title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public GroupMilestoneUpdate title(final String title) {
        payload.title(requireNotBlank(title, "title"));
        return changed();
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GroupMilestoneUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets the due date, sent as {@code yyyy-MM-dd}.
     *
     * @param dueDate the due date
     * @return this update
     * @throws NullPointerException if {@code dueDate} is {@code null}
     */
    public GroupMilestoneUpdate dueDate(final LocalDate dueDate) {
        payload.dueDate(MilestoneValues.isoDate(dueDate, "dueDate"));
        return changed();
    }

    /**
     * Sets the start date, sent as {@code yyyy-MM-dd}.
     *
     * @param startDate the start date
     * @return this update
     * @throws NullPointerException if {@code startDate} is {@code null}
     */
    public GroupMilestoneUpdate startDate(final LocalDate startDate) {
        payload.startDate(MilestoneValues.isoDate(startDate, "startDate"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/milestones/{milestone_id}}). The milestone this update was made from
     * is not changed. GitLab answers with the milestone, so there is no extra request.
     *
     * @return the milestone as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupMilestone send() throws IOException {
        state.requireChange();
        return milestone.apply(payload.build());
    }

    private GroupMilestoneUpdate changed() {
        state.mark();
        return this;
    }
}
