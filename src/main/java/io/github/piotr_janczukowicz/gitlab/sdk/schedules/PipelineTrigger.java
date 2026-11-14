package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabProjectPipelineTriggersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTrigger;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * A pipeline trigger of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response. The token is a secret: no method of this
 * object, and no {@code toString()}, prints it. Firing a trigger ({@code POST /projects/{id}/trigger/pipeline}) is not
 * wrapped; use the core client for it.
 */
public final class PipelineTrigger {

    private final Project project;
    private final GitLabTrigger response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public PipelineTrigger(final Project project, final GitLabTrigger response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the trigger belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the trigger, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
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
     * Returns the token that fires the trigger, as GitLab returns it; for triggers owned by someone else GitLab may
     * return a shortened token (not verified). The token is secret, do not log it.
     *
     * @return the token
     */
    public String token() {
        return requireNonNull(response.token(), "token");
    }

    /**
     * Returns the time the trigger was last used.
     *
     * @return the time, or empty when it was never used
     */
    public Optional<Instant> lastUsed() {
        return Optional.ofNullable(response.lastUsed());
    }

    /**
     * Returns the time the trigger expires.
     *
     * @return the time, or empty when it does not expire
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(response.expiresAt());
    }

    /**
     * Returns the owner of the trigger.
     *
     * @return the owner, or empty when GitLab sent none
     */
    public Optional<GitLabUserBasic> owner() {
        return Optional.ofNullable(response.owner());
    }

    /**
     * Starts a change of the trigger. Nothing is sent until {@link PipelineTriggerUpdate#send()}.
     *
     * @return the update builder
     */
    public PipelineTriggerUpdate update() {
        return new PipelineTriggerUpdate(this);
    }

    /**
     * Deletes the trigger ({@code DELETE /projects/{id}/triggers/{trigger_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().pipelinetriggers().delete(project.id(), id());
    }

    /**
     * Loads the trigger again ({@code GET /projects/{id}/triggers/{trigger_id}}). This object is not changed.
     *
     * @return the trigger as GitLab holds it now
     * @throws IllegalStateException if the trigger is gone
     * @throws IOException if the transport fails
     */
    public PipelineTrigger refresh() throws IOException {
        return project.pipelineTrigger(id())
                .orElseThrow(() -> new IllegalStateException("Pipeline trigger " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from. It holds the secret token; the record prints it
     * redacted.
     *
     * @return the response record
     */
    public GitLabTrigger response() {
        return response;
    }

    PipelineTrigger apply(final GitLabProjectPipelineTriggersUpdatePayload payload) throws IOException {
        return new PipelineTrigger(project, gitLab().pipelinetriggers().update(project.id(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
