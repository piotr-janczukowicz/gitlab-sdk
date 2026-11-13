package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Collects changes to a {@link Milestone} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. A field cannot be cleared, the dates included: the core's payload fields are
 * plain optional values and GitLab's documentation does not say that a missing or empty value clears them. To close or
 * reopen the milestone use {@link Milestone#close()} and {@link Milestone#reopen()}.
 */
public final class MilestoneUpdate {

    private final Milestone milestone;
    private final GitLabProjectProjectMilestonesUpdatePayloadBuilder payload =
            GitLabProjectProjectMilestonesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param milestone the milestone to change
     * @throws NullPointerException if {@code milestone} is {@code null}
     */
    public MilestoneUpdate(final Milestone milestone) {
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
    public MilestoneUpdate title(final String title) {
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
    public MilestoneUpdate description(final String description) {
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
    public MilestoneUpdate dueDate(final LocalDate dueDate) {
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
    public MilestoneUpdate startDate(final LocalDate startDate) {
        payload.startDate(MilestoneValues.isoDate(startDate, "startDate"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/milestones/{milestone_id}}). The milestone this update was made from
     * is not changed. GitLab answers with the milestone, so there is no extra request.
     *
     * @return the milestone as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Milestone send() throws IOException {
        state.requireChange();
        return milestone.apply(payload.build());
    }

    private MilestoneUpdate changed() {
        state.mark();
        return this;
    }
}
