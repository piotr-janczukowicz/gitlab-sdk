package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabProjectPipelineTriggersUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link PipelineTrigger} and sends them in one request. Nothing is sent until {@link #send()}.
 * GitLab lets a trigger's description change; its token and expiry stay as they are.
 */
public final class PipelineTriggerUpdate {

    private final PipelineTrigger trigger;
    private final GitLabProjectPipelineTriggersUpdatePayloadBuilder payload =
            GitLabProjectPipelineTriggersUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param trigger the trigger to change
     * @throws NullPointerException if {@code trigger} is {@code null}
     */
    public PipelineTriggerUpdate(final PipelineTrigger trigger) {
        this.trigger = requireNonNull(trigger, "trigger");
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     * @throws IllegalArgumentException if {@code description} is blank
     */
    public PipelineTriggerUpdate description(final String description) {
        payload.description(requireNotBlank(description, "description"));
        state.mark();
        return this;
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/triggers/{trigger_id}}). The trigger this update was made from is
     * not changed.
     *
     * @return the trigger as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public PipelineTrigger send() throws IOException {
        state.requireChange();
        return trigger.apply(payload.build());
    }
}
