package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabMirrorHostKey;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabRemoteMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A remote (push) mirror of a {@link Project}, as GitLab returned it. The object is immutable, and {@link #response()}
 * gives the complete response. The address of a mirror may carry credentials. This object has no {@code toString()} of
 * its own, so it never prints the address; {@link #url()} and {@link #response()} return it as the core decoded it, and
 * whether GitLab masks the credentials in its answers is not verified live.
 */
public final class RemoteMirror {

    private final Project project;
    private final GitLabRemoteMirror response;

    /**
     * Wraps a response. Applications normally get this object from the project; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public RemoteMirror(final Project project, final GitLabRemoteMirror response) {
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
     * Returns the address of the mirror, which may carry credentials.
     *
     * @return the address
     */
    public String url() {
        return requireNonNull(response.url(), "url");
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
     * Returns the error of the last update.
     *
     * @return the error, or empty when the response has none
     */
    public Optional<String> lastError() {
        return Optional.ofNullable(response.lastError());
    }

    /**
     * Tells whether only protected branches are mirrored.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean onlyProtectedBranches() {
        return Boolean.TRUE.equals(response.onlyProtectedBranches());
    }

    /**
     * Tells whether refs that diverged are kept on the target.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean keepDivergentRefs() {
        return Boolean.TRUE.equals(response.keepDivergentRefs());
    }

    /**
     * Returns how GitLab authenticates to the target, as GitLab spells it. Use {@link MirrorAuthMethod#from(String)} to
     * turn it into a value.
     *
     * @return the method, or empty when the response has none
     */
    public Optional<String> authMethod() {
        return Optional.ofNullable(response.authMethod());
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
     * Returns the SHA-256 fingerprints of the host keys GitLab holds for the target.
     *
     * @return the fingerprints; empty when the response has none
     */
    public List<String> hostKeyFingerprints() {
        final List<GitLabMirrorHostKey> keys = response.hostKeys();
        return keys == null
                ? List.of()
                : keys.stream()
                        .map(GitLabMirrorHostKey::fingerprintSha256)
                        .filter(java.util.Objects::nonNull)
                        .toList();
    }

    /**
     * Starts a change of the mirror. Nothing is sent until {@link RemoteMirrorUpdate#send()}.
     *
     * @return the update builder
     */
    public RemoteMirrorUpdate update() {
        return new RemoteMirrorUpdate(this);
    }

    /**
     * Asks GitLab to update the mirror now ({@code POST /projects/{id}/remote_mirrors/{mirror_id}/sync}). The update
     * runs after this method returns.
     *
     * @throws IOException if the transport fails
     */
    public void sync() throws IOException {
        gitLab().remotemirrors().sync(project.id(), String.valueOf(id()));
    }

    /**
     * Deletes the mirror ({@code DELETE /projects/{id}/remote_mirrors/{mirror_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().remotemirrors().delete(project.id(), String.valueOf(id()));
    }

    /**
     * Loads the mirror again ({@code GET /projects/{id}/remote_mirrors/{mirror_id}}). This object is not changed.
     *
     * @return the mirror as GitLab holds it now
     * @throws IllegalStateException if the mirror is gone
     * @throws IOException if the transport fails
     */
    public RemoteMirror refresh() throws IOException {
        return project.remoteMirror(id())
                .orElseThrow(() -> new IllegalStateException("Remote mirror " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabRemoteMirror response() {
        return response;
    }

    RemoteMirror apply(final GitLabProjectRemoteMirrorsUpdatePayload payload) throws IOException {
        return new RemoteMirror(project, gitLab().remotemirrors().update(project.id(), String.valueOf(id()), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
