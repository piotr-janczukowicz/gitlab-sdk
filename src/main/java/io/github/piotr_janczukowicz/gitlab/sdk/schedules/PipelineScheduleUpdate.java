package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link PipelineSchedule} and sends them in one request. Only the fields that were set are sent,
 * and nothing is sent until {@link #send()}. The cron expression and the time zone are sent as given: the SDK does not
 * validate them. The schedule's inputs are not offered here; use the core client for them.
 */
public final class PipelineScheduleUpdate {

    private final PipelineSchedule schedule;
    private final GitLabProjectPipelineSchedulesUpdatePayloadBuilder payload =
            GitLabProjectPipelineSchedulesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param schedule the schedule to change
     * @throws NullPointerException if {@code schedule} is {@code null}
     */
    public PipelineScheduleUpdate(final PipelineSchedule schedule) {
        this.schedule = requireNonNull(schedule, "schedule");
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     */
    public PipelineScheduleUpdate description(final String description) {
        payload.description(requireNotBlank(description, "description"));
        return changed();
    }

    /**
     * Sets the branch or tag the schedule runs for.
     *
     * @param ref the branch or tag name
     * @return this update
     * @throws NullPointerException if {@code ref} is {@code null}
     * @throws IllegalArgumentException if {@code ref} is blank
     */
    public PipelineScheduleUpdate ref(final String ref) {
        payload.ref(requireNotBlank(ref, "ref"));
        return changed();
    }

    /**
     * Sets the cron expression, sent as given.
     *
     * @param cron the cron expression
     * @return this update
     * @throws NullPointerException if {@code cron} is {@code null}
     * @throws IllegalArgumentException if {@code cron} is blank
     */
    public PipelineScheduleUpdate cron(final String cron) {
        payload.cron(requireNotBlank(cron, "cron"));
        return changed();
    }

    /**
     * Sets the time zone of the cron expression, sent as given.
     *
     * @param cronTimezone the time zone name, such as {@code Europe/Warsaw}
     * @return this update
     * @throws NullPointerException if {@code cronTimezone} is {@code null}
     * @throws IllegalArgumentException if {@code cronTimezone} is blank
     */
    public PipelineScheduleUpdate cronTimezone(final String cronTimezone) {
        payload.cronTimezone(requireNotBlank(cronTimezone, "cronTimezone"));
        return changed();
    }

    /**
     * Sets whether the schedule is active.
     *
     * @param active {@code true} to activate, {@code false} to deactivate
     * @return this update
     */
    public PipelineScheduleUpdate active(final boolean active) {
        payload.active(active);
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/pipeline_schedules/{pipeline_schedule_id}}). The schedule this
     * update was made from is not changed.
     *
     * @return the schedule as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public PipelineSchedule send() throws IOException {
        state.requireChange();
        return schedule.apply(payload.build());
    }

    private PipelineScheduleUpdate changed() {
        state.mark();
        return this;
    }
}
