package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabProjectPullMirroringUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabPullMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * The pull mirror of a {@link Project}, as GitLab returned it. Per GitLab's API description, pull mirroring is a
 * Premium feature, and the operation that changes its settings is marked experimental there: on a lower tier the core's
 * {@code GitLabTierException} passes through. The object is immutable, and {@link #response()} gives the complete
 * response. The address may carry credentials. This object has no {@code toString()} of its own, so it never prints it;
 * whether GitLab masks the credentials in its answers is not verified live.
 */
public final class PullMirror {

    private final Project project;
    private final GitLabPullMirror response;

    /**
     * Wraps a response. Applications normally get this object from the project; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public PullMirror(final Project project, final GitLabPullMirror response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the mirror belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the mirror.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the address GitLab pulls from, which may carry credentials.
     *
     * @return the address, or empty when the response has none
     */
    public Optional<String> url() {
        return Optional.ofNullable(response.url());
    }

    /**
     * Tells whether the mirror is switched on.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(response.enabled());
    }

    /**
     * Returns the state of the last update, as GitLab spells it.
     *
     * @return the status, or empty when the response has none
     */
    public Optional<String> updateStatus() {
        return Optional.ofNullable(response.updateStatus());
    }

    /**
     * Returns the error of the last update.
     *
     * @return the error, or empty when the response has none
     */
    public Optional<String> lastError() {
        return Optional.ofNullable(response.lastError());
    }

    /**
     * Returns when the last update ended.
     *
     * @return the time, or empty when the response has none
     */
    public Optional<Instant> lastUpdateAt() {
        return Optional.ofNullable(response.lastUpdateAt());
    }

    /**
     * Returns when the last update started.
     *
     * @return the time, or empty when the response has none
     */
    public Optional<Instant> lastUpdateStartedAt() {
        return Optional.ofNullable(response.lastUpdateStartedAt());
    }

    /**
     * Returns when the last successful update ended.
     *
     * @return the time, or empty when the response has none
     */
    public Optional<Instant> lastSuccessfulUpdateAt() {
        return Optional.ofNullable(response.lastSuccessfulUpdateAt());
    }

    /**
     * Tells whether a pull starts the pipelines of the project.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean mirrorTriggerBuilds() {
        return Boolean.TRUE.equals(response.mirrorTriggerBuilds());
    }

    /**
     * Tells whether only protected branches are mirrored.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean onlyMirrorProtectedBranches() {
        return Boolean.TRUE.equals(response.onlyMirrorProtectedBranches());
    }

    /**
     * Tells whether a pull overwrites branches that diverged.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean mirrorOverwritesDivergedBranches() {
        return Boolean.TRUE.equals(response.mirrorOverwritesDivergedBranches());
    }

    /**
     * Returns the regular expression that selects the branches to mirror.
     *
     * @return the expression, or empty when the response has none
     */
    public Optional<String> mirrorBranchRegex() {
        return Optional.ofNullable(response.mirrorBranchRegex());
    }

    /**
     * Starts a change of the mirror settings. Nothing is sent until {@link PullMirrorUpdate#send()}.
     *
     * @return the update builder
     */
    public PullMirrorUpdate update() {
        return new PullMirrorUpdate(this);
    }

    /**
     * Loads the mirror again ({@code GET /projects/{id}/mirror/pull}). This object is not changed.
     *
     * @return the mirror as GitLab holds it now
     * @throws IllegalStateException if the project no longer has a pull mirror
     * @throws IOException if the transport fails
     */
    public PullMirror refresh() throws IOException {
        return project.pullMirror().orElseThrow(() -> new IllegalStateException("The pull mirror is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabPullMirror response() {
        return response;
    }

    PullMirror apply(final GitLabProjectPullMirroringUpdatePayload payload) throws IOException {
        return new PullMirror(project, gitLab().pullmirroring().update(project.id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
