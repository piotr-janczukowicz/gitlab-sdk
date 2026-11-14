package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdateVariablePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.VariableType;
import java.io.IOException;

/**
 * Collects changes to a {@link ScheduleVariable} and sends them in one request. Only the fields that were set are sent,
 * and nothing is sent until {@link #send()}. The object has no {@code toString()}, so a value set here is never
 * printed.
 */
public final class ScheduleVariableUpdate {

    private final ScheduleVariable variable;
    private final GitLabProjectPipelineSchedulesUpdateVariablePayloadBuilder payload =
            GitLabProjectPipelineSchedulesUpdateVariablePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param variable the variable to change
     * @throws NullPointerException if {@code variable} is {@code null}
     */
    public ScheduleVariableUpdate(final ScheduleVariable variable) {
        this.variable = requireNonNull(variable, "variable");
    }

    /**
     * Sets the value. The value is secret, do not log it; an empty value is sent as given.
     *
     * @param value the value
     * @return this update
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public ScheduleVariableUpdate value(final String value) {
        payload.value(requireNonNull(value, "value"));
        return changed();
    }

    /**
     * Sets the type.
     *
     * @param type the type
     * @return this update
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public ScheduleVariableUpdate type(final VariableType type) {
        payload.variableType(GitLabProjectPipelineSchedulesUpdateVariableType.valueOf(
                requireNonNull(type, "type").name()));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/variables/{key}}). The
     * variable this update was made from is not changed.
     *
     * @return the variable as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public ScheduleVariable send() throws IOException {
        state.requireChange();
        return variable.apply(payload.build());
    }

    private ScheduleVariableUpdate changed() {
        state.mark();
        return this;
    }
}
