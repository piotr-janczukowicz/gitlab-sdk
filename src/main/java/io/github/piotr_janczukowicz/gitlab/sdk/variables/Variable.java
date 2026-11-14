package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesDeleteFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdateFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A CI/CD variable of a {@link Project}, as GitLab returned it. A variable is addressed by its key and its environment
 * scope: several variables of one project may share a key and differ in the scope. This object targets GitLab with its
 * own scope in {@link #refresh()}, {@link #update()} and {@link #delete()}; a variable the answer gave no scope for is
 * targeted by key alone. A scope that is stale (the variable was moved to another scope since this object was loaded)
 * may make {@link #update()} change a different variable of the same key: GitLab's documentation says that when the
 * scope of the filter does not exist, the endpoint updates a variable with the same key but a different scope (not
 * verified live). Call {@link #refresh()} first when the scope may have changed. The object is immutable: it keeps the
 * data of the moment it was loaded, and {@link #response()} gives the complete response. The value is a secret: no
 * method of this object, and no {@code toString()}, prints it. Instance-level and pipeline variables are not wrapped;
 * use the core client for them.
 */
public final class Variable {

    private final Project project;
    private final GitLabCiVariable response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Variable(final Project project, final GitLabCiVariable response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the variable belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
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
     * Starts a change of the variable. Nothing is sent until {@link VariableUpdate#send()}.
     *
     * @return the update builder
     */
    public VariableUpdate update() {
        return new VariableUpdate(this);
    }

    /**
     * Deletes the variable ({@code DELETE /projects/{id}/variables/{key}}), narrowed by the variable's own environment
     * scope with {@code filter[environment_scope]} when it has one.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final Optional<String> scope = environmentScope();
        if (scope.isPresent()) {
            gitLab().projectlevelvariables()
                    .delete(
                            project.id(),
                            key(),
                            GitLabProjectProjectLevelVariablesDeleteFilterBuilder.builder()
                                    .filterEnvironmentScope(scope.get())
                                    .build());
        } else {
            gitLab().projectlevelvariables().delete(project.id(), key());
        }
    }

    /**
     * Loads the variable again ({@code GET /projects/{id}/variables/{key}}), narrowed by the variable's own environment
     * scope when it has one. This object is not changed.
     *
     * @return the variable as GitLab holds it now
     * @throws IllegalStateException if the variable is gone
     * @throws IOException if the transport fails
     */
    public Variable refresh() throws IOException {
        final Optional<String> scope = environmentScope();
        final Optional<Variable> loaded =
                scope.isPresent() ? project.variable(key(), scope.get()) : project.variable(key());
        return loaded.orElseThrow(() -> new IllegalStateException("Variable " + key() + " is gone"));
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

    Variable apply(final GitLabProjectProjectLevelVariablesUpdatePayloadBuilder payload) throws IOException {
        environmentScope()
                .ifPresent(scope -> payload.filter(GitLabProjectProjectLevelVariablesUpdateFilterBuilder.builder()
                        .environmentScope(scope)
                        .build()));
        return new Variable(project, gitLab().projectlevelvariables().update(project.id(), key(), payload.build()));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
