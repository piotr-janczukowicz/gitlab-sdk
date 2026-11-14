package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link Variable} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The variable is targeted by its key and its own environment scope;
 * {@link #environmentScope(String)} sets a new scope. The object has no {@code toString()}, so a value set here is
 * never printed.
 */
public final class VariableUpdate {

    private final Variable variable;
    private final GitLabProjectProjectLevelVariablesUpdatePayloadBuilder payload =
            GitLabProjectProjectLevelVariablesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param variable the variable to change
     * @throws NullPointerException if {@code variable} is {@code null}
     */
    public VariableUpdate(final Variable variable) {
        this.variable = requireNonNull(variable, "variable");
    }

    /**
     * Sets the value. The value is secret, do not log it; an empty value is sent as given.
     *
     * @param value the value
     * @return this update
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public VariableUpdate value(final String value) {
        payload.value(requireNonNull(value, "value"));
        return changed();
    }

    /**
     * Sets whether the variable is protected.
     *
     * @param enabled {@code true} to protect
     * @return this update
     */
    public VariableUpdate protectedValue(final boolean enabled) {
        payload.protectedValue(enabled);
        return changed();
    }

    /**
     * Sets whether the variable is masked in job logs.
     *
     * @param enabled {@code true} to mask
     * @return this update
     */
    public VariableUpdate masked(final boolean enabled) {
        payload.masked(enabled);
        return changed();
    }

    /**
     * Sets whether the variable is raw: its value is not expanded.
     *
     * @param enabled {@code true} for raw
     * @return this update
     */
    public VariableUpdate raw(final boolean enabled) {
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
    public VariableUpdate type(final VariableType type) {
        payload.variableType(GitLabProjectProjectLevelVariablesUpdateVariableType.valueOf(
                requireNonNull(type, "type").name()));
        return changed();
    }

    /**
     * Sets a new environment scope. This is not the scope the variable is looked up by: that stays the variable's own
     * scope.
     *
     * @param environmentScope the new scope
     * @return this update
     * @throws NullPointerException if {@code environmentScope} is {@code null}
     * @throws IllegalArgumentException if {@code environmentScope} is blank
     */
    public VariableUpdate environmentScope(final String environmentScope) {
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
    public VariableUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/variables/{key}}) with the variable's own environment scope as
     * {@code filter[environment_scope]} when it has one. The variable this update was made from is not changed.
     *
     * @return the variable as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Variable send() throws IOException {
        state.requireChange();
        return variable.apply(payload);
    }

    private VariableUpdate changed() {
        state.mark();
        return this;
    }
}
