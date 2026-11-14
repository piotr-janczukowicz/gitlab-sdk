package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link GroupVariable} and sends them in one request. Only the fields that were set are sent,
 * and nothing is sent until {@link #send()}. The variable is targeted by its key alone: GitLab's documentation
 * describes a scope filter, but the core does not offer it on the group update. When several variables share the key,
 * which one GitLab changes is not verified. {@link #environmentScope(String)} sets a new scope. The object has no
 * {@code toString()}, so a value set here is never printed.
 */
public final class GroupVariableUpdate {

    private final GroupVariable variable;
    private final GitLabGroupGroupLevelVariablesUpdatePayloadBuilder payload =
            GitLabGroupGroupLevelVariablesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param variable the variable to change
     * @throws NullPointerException if {@code variable} is {@code null}
     */
    public GroupVariableUpdate(final GroupVariable variable) {
        this.variable = requireNonNull(variable, "variable");
    }

    /**
     * Sets the value. The value is secret, do not log it; an empty value is sent as given.
     *
     * @param value the value
     * @return this update
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public GroupVariableUpdate value(final String value) {
        payload.value(requireNonNull(value, "value"));
        return changed();
    }

    /**
     * Sets whether the variable is protected.
     *
     * @param enabled {@code true} to protect
     * @return this update
     */
    public GroupVariableUpdate protectedValue(final boolean enabled) {
        payload.protectedValue(enabled);
        return changed();
    }

    /**
     * Sets whether the variable is masked in job logs.
     *
     * @param enabled {@code true} to mask
     * @return this update
     */
    public GroupVariableUpdate masked(final boolean enabled) {
        payload.masked(enabled);
        return changed();
    }

    /**
     * Sets whether the variable is raw: its value is not expanded.
     *
     * @param enabled {@code true} for raw
     * @return this update
     */
    public GroupVariableUpdate raw(final boolean enabled) {
        payload.raw(enabled);
        return changed();
    }

    /**
     * Sets the type.
     *
     * @param type the type
     * @return this update
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public GroupVariableUpdate type(final VariableType type) {
        payload.variableType(GitLabGroupGroupLevelVariablesUpdateVariableType.valueOf(
                requireNonNull(type, "type").name()));
        return changed();
    }

    /**
     * Sets a new environment scope. The variable is still looked up by key alone.
     *
     * @param environmentScope the new scope
     * @return this update
     * @throws NullPointerException if {@code environmentScope} is {@code null}
     * @throws IllegalArgumentException if {@code environmentScope} is blank
     */
    public GroupVariableUpdate environmentScope(final String environmentScope) {
        payload.environmentScope(requireNotBlank(environmentScope, "environmentScope"));
        return changed();
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GroupVariableUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/variables/{key}}). The variable this update was made from is not
     * changed.
     *
     * @return the variable as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupVariable send() throws IOException {
        state.requireChange();
        return variable.apply(payload.build());
    }

    private GroupVariableUpdate changed() {
        state.mark();
        return this;
    }
}
