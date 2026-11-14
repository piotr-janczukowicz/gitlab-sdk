package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A CI/CD variable of a {@link Group}, as GitLab returned it. GitLab lets a group hold several variables with one key
 * and different environment scopes, but the core's group API has no way to name the scope when it loads, changes or
 * deletes a variable: {@link #refresh()}, {@link #update()} and {@link #delete()} address the variable by key alone, so
 * same-key group variables of different scopes cannot be told apart. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response. The value is a secret: no method of this
 * object, and no {@code toString()}, prints it. Instance-level variables are not wrapped; use the core client for them.
 */
public final class GroupVariable {

    private final Group group;
    private final GitLabCiVariable response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupVariable(final Group group, final GitLabCiVariable response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the variable belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the key of the variable.
     *
     * @return the key
     */
    public String key() {
        return requireNonNull(response.key(), "key");
    }

    /**
     * Returns the value of the variable. The value is secret, do not log it.
     *
     * @return the value, or empty when GitLab sent none, as for a hidden variable
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
     * Tells whether the variable is protected: only pipelines on protected branches and tags get it.
     *
     * @return {@code true} if GitLab reports it as protected; {@code false} also when the flag is absent
     */
    public boolean isProtected() {
        return Boolean.TRUE.equals(response.protectedValue());
    }

    /**
     * Tells whether the variable is masked in job logs.
     *
     * @return {@code true} if GitLab reports it as masked; {@code false} also when the flag is absent
     */
    public boolean isMasked() {
        return Boolean.TRUE.equals(response.masked());
    }

    /**
     * Tells whether the variable is raw: its value is not expanded.
     *
     * @return {@code true} if GitLab reports it as raw; {@code false} also when the flag is absent
     */
    public boolean isRaw() {
        return Boolean.TRUE.equals(response.raw());
    }

    /**
     * Tells whether the variable is hidden: GitLab never shows its value again.
     *
     * @return {@code true} if GitLab reports it as hidden; {@code false} also when the flag is absent
     */
    public boolean isHidden() {
        return Boolean.TRUE.equals(response.hidden());
    }

    /**
     * Returns the environment scope of the variable. The SDK does not assume GitLab's default {@code *}.
     *
     * @return the scope, or empty when GitLab sent none
     */
    public Optional<String> environmentScope() {
        return Optional.ofNullable(response.environmentScope());
    }

    /**
     * Returns the description.
     *
     * @return the description, or empty when GitLab sent none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Starts a change of the variable. Nothing is sent until {@link GroupVariableUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupVariableUpdate update() {
        return new GroupVariableUpdate(this);
    }

    /**
     * Deletes the variable ({@code DELETE /groups/{id}/variables/{key}}), by key alone.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().grouplevelvariables().delete(group.id(), key());
    }

    /**
     * Loads the variable again ({@code GET /groups/{id}/variables/{key}}), by key alone. This object is not changed.
     *
     * @return the variable as GitLab holds it now
     * @throws IllegalStateException if the variable is gone
     * @throws IOException if the transport fails
     */
    public GroupVariable refresh() throws IOException {
        return group.variable(key()).orElseThrow(() -> new IllegalStateException("Variable " + key() + " is gone"));
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

    GroupVariable apply(final GitLabGroupGroupLevelVariablesUpdatePayload payload) throws IOException {
        return new GroupVariable(group, gitLab().grouplevelvariables().update(group.id(), key(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
