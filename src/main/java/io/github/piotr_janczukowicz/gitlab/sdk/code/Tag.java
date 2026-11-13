package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTag;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTagSignature;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.releases.Release;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * A tag of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. GitLab
 * lists and loads a tag with the same record, so a listed tag needs no extra request.
 */
public final class Tag {

    private final Project project;
    private final GitLabTag response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Tag(final Project project, final GitLabTag response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the tag belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the tag name, which may contain slashes such as {@code release/1.x}.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the message of an annotated tag.
     *
     * @return the message, or empty when GitLab sent none
     */
    public Optional<String> message() {
        return Optional.ofNullable(response.message());
    }

    /**
     * Returns the target of the tag: the SHA of the tag object of an annotated tag, or of the commit of a lightweight
     * tag.
     *
     * @return the target
     */
    public String target() {
        return requireNonNull(response.target(), "target");
    }

    /**
     * Tells whether the tag is protected.
     *
     * @return {@code true} if GitLab reports the tag as protected; {@code false} also when the flag is absent
     */
    public boolean isProtected() {
        return Boolean.TRUE.equals(response.protectedValue());
    }

    /**
     * Returns the time the tag was created.
     *
     * @return the creation time, or empty when GitLab sent none
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(response.createdAt());
    }

    /**
     * Loads the commit the tag points to ({@code GET /projects/{id}/repository/commits/{sha}}, one extra request) with
     * the SHA of the commit in the tag's response. No request is made when the response has no commit.
     *
     * @return the commit, or empty when the response has no commit or GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Commit> commit() throws IOException {
        final GitLabCommit commit = response.commit();
        final String sha = commit == null ? null : commit.id();
        return sha == null ? Optional.empty() : project.commit(sha);
    }

    /**
     * Loads the release of the tag ({@code GET /projects/{id}/releases/{tag_name}}, one extra request) with the name of
     * the tag.
     *
     * @return the release, or empty when the tag has none (GitLab answers HTTP 404)
     * @throws IOException if the transport fails
     */
    public Optional<Release> release() throws IOException {
        return project.release(name());
    }

    /**
     * Loads the signature of the tag ({@code GET /projects/{id}/repository/tags/{tag_name}/signature}).
     *
     * @return the core's signature record, or empty when GitLab answers HTTP 404 (for example for an unsigned tag)
     * @throws IOException if the transport fails
     */
    public Optional<GitLabTagSignature> signature() throws IOException {
        return gitLab().tags().getSignature(project.id(), name());
    }

    /**
     * Deletes the tag ({@code DELETE /projects/{id}/repository/tags/{tag_name}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().tags().delete(project.id(), name());
    }

    /**
     * Loads the tag again ({@code GET /projects/{id}/repository/tags/{tag_name}}). This object is not changed.
     *
     * @return the tag as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the tag
     * @throws IOException if the transport fails
     */
    public Tag refresh() throws IOException {
        return project.tag(name()).orElseThrow(() -> new IllegalStateException("Tag " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabTag response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
