package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdateVariablePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.VariableType;
import java.io.IOException;
import java.util.Optional;

/**
 * A variable of a {@link PipelineSchedule}, as GitLab returned it. A variable is addressed by its key within its
 * schedule. The object is immutable: it keeps the data of the moment it was loaded, and {@link #response()} gives the
 * complete response. The value is a secret: no method of this object, and no {@code toString()}, prints it.
 */
public final class ScheduleVariable {

    private final PipelineSchedule schedule;
    private final GitLabCiVariable response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param schedule the schedule the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ScheduleVariable(final PipelineSchedule schedule, final GitLabCiVariable response) {
        this.schedule = requireNonNull(schedule, "schedule");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the schedule the variable belongs to.
     *
     * @return the schedule
     */
    public PipelineSchedule schedule() {
        return schedule;
    }

    /**
     * Returns the key of the variable, which addresses it within the schedule.
     *
     * @return the key
     */
    public String key() {
        return requireNonNull(response.key(), "key");
    }

    /**
     * Returns the value of the variable. The value is secret, do not log it.
     *
     * @return the value, or empty when GitLab sent none
     */
    public Optional<String> value() {
        return Optional.ofNullable(response.value());
    }

    /**
     * Returns the type of the variable.
     *
     * @return the type
     * @throws IllegalArgumentException if GitLab sent a type the SDK does not know; {@link #typeName()} still works
     */
    public VariableType type() {
        return VariableType.from(typeName());
    }

    /**
     * Returns the type of the variable as GitLab spells it, such as {@code env_var}.
     *
     * @return the raw type name
     */
    public String typeName() {
        return requireNonNull(response.variableType(), "variableType");
    }

    /**
     * Starts a change of the variable. Nothing is sent until {@link ScheduleVariableUpdate#send()}.
     *
     * @return the update builder
     */
    public ScheduleVariableUpdate update() {
        return new ScheduleVariableUpdate(this);
    }

    /**
     * Deletes the variable ({@code DELETE /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/variables/{key}}).
     * What the core returns for the deleted variable is not passed on.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.gitLab(schedule.project())
                .pipelineschedules()
                .deleteVariable(schedule.project().id(), schedule.id(), key());
    }

    /**
     * Loads the variable again ({@code GET /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/variables/{key}}).
     * This object is not changed.
     *
     * @return the variable as GitLab holds it now
     * @throws IllegalStateException if the variable is gone
     * @throws IOException if the transport fails
     */
    public ScheduleVariable refresh() throws IOException {
        return schedule.variable(key()).orElseThrow(() -> new IllegalStateException("Variable " + key() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from. It holds the secret value; the record prints it
     * redacted.
     *
     * @return the response record
     */
    public GitLabCiVariable response() {
        return response;
    }

    ScheduleVariable apply(final GitLabProjectPipelineSchedulesUpdateVariablePayload payload) throws IOException {
        return new ScheduleVariable(
                schedule,
                Internals.gitLab(schedule.project())
                        .pipelineschedules()
                        .updateVariable(schedule.project().id(), schedule.id(), key(), payload));
    }
}
