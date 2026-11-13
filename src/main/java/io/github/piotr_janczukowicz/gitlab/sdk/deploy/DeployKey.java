package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabDeployKeysProject;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * A deploy key of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response, including the public key, which has no accessor of
 * its own. GitLab has no group-level deploy keys.
 */
public final class DeployKey {

    private final Project project;
    private final GitLabDeployKeysProject response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public DeployKey(final Project project, final GitLabDeployKeysProject response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the key belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the key.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the title of the key.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
    }

    /**
     * Tells whether the key may push to the repository.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean canPush() {
        return Boolean.TRUE.equals(response.canPush());
    }

    /**
     * Returns when the key expires.
     *
     * @return the expiry, or empty when the key does not expire
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(response.expiresAt());
    }

    /**
     * Starts a change of the key. Nothing is sent until {@link DeployKeyUpdate#send()}.
     *
     * @return the update builder
     */
    public DeployKeyUpdate update() {
        return new DeployKeyUpdate(this);
    }

    /**
     * Deletes the key from the project ({@code DELETE /projects/{id}/deploy_keys/{key_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().deploykeys().project().delete(project.id(), id());
    }

    /**
     * Loads the key again ({@code GET /projects/{id}/deploy_keys/{key_id}}). This object is not changed.
     *
     * @return the key as GitLab holds it now
     * @throws IllegalStateException if the key is gone
     * @throws IOException if the transport fails
     */
    public DeployKey refresh() throws IOException {
        return project.deployKey(id())
                .orElseThrow(() -> new IllegalStateException("Deploy key " + id() + DeployValues.IS_GONE));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabDeployKeysProject response() {
        return response;
    }

    /**
     * Sends an update and loads the key again: the core's update answers with a different record than the one that
     * lists and loads the key.
     */
    DeployKey apply(final GitLabProjectDeployKeysUpdatePayload payload) throws IOException {
        gitLab().deploykeys().project().update(project.id(), id(), payload);
        return refresh();
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
