package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabRelease;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A release of a {@link Project}, as GitLab returned it. A release is addressed by the name of its tag, which may
 * contain slashes such as {@code v1.0/rc}; the core encodes it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not
 * cover, such as the milestones, the assets and the evidences. GitLab lists, loads, creates and updates a release with
 * the same record, so none of them needs an extra request. Group releases, the latest-release permalinks and release
 * asset sources are not wrapped; use the core client for them.
 */
public final class Release {

    private final Project project;
    private final GitLabRelease response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Release(final Project project, final GitLabRelease response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the release belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the name of the tag the release is made for, which may contain slashes.
     *
     * @return the tag name
     */
    public String tagName() {
        return requireNonNull(response.tagName(), "tagName");
    }

    /**
     * Returns the name of the release.
     *
     * @return the name, or empty when GitLab sent none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the description (the release notes).
     *
     * @return the description, or empty when GitLab sent none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the time the release is or was published.
     *
     * @return the time, or empty when GitLab sent none
     */
    public Optional<Instant> releasedAt() {
        return Optional.ofNullable(response.releasedAt());
    }

    /**
     * Tells whether the release is an upcoming one: its release time lies in the future.
     *
     * @return {@code true} if GitLab reports the release as upcoming; {@code false} also when the flag is absent
     */
    public boolean isUpcoming() {
        return Boolean.TRUE.equals(response.upcomingRelease());
    }

    /**
     * Starts a change of the name, description, release time or milestones. Nothing is sent until
     * {@link ReleaseUpdate#send()}.
     *
     * @return the update builder
     */
    public ReleaseUpdate update() {
        return new ReleaseUpdate(this);
    }

    /**
     * Asks GitLab to collect new evidence for the release ({@code POST /projects/{id}/releases/{tag_name}/evidence}).
     * GitLab answers with the release.
     *
     * @return the release as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public Release collectEvidence() throws IOException {
        return new Release(project, gitLab().releases().evidence(project.id(), tagName()));
    }

    /**
     * Lists the links of the release ({@code GET /projects/{id}/releases/{tag_name}/assets/links}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the links
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<ReleaseLink> links() throws IOException {
        return gitLab().releaselinks().list(project.id(), tagName()).stream()
                .map(listed -> new ReleaseLink(this, listed));
    }

    /**
     * Loads a link of the release ({@code GET /projects/{id}/releases/{tag_name}/assets/links/{link_id}}).
     *
     * @param linkId the id of the link
     * @return the link, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ReleaseLink> link(final long linkId) throws IOException {
        return gitLab().releaselinks()
                .get(project.id(), tagName(), linkId)
                .map(loaded -> new ReleaseLink(this, loaded));
    }

    /**
     * Adds a link to the release ({@code POST /projects/{id}/releases/{tag_name}/assets/links}). GitLab gives the link
     * its default type; change it with {@link ReleaseLink#update()}.
     *
     * @param name the name of the link
     * @param url the URL of the link
     * @return the new link
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public ReleaseLink addLink(final String name, final String url) throws IOException {
        return addLink(GitLabProjectReleaseLinksCreatePayloadBuilder.builder()
                .name(requireNotBlank(name, "name"))
                .url(requireNotBlank(url, "url"))
                .build());
    }

    /**
     * Adds a link to the release from the core's payload ({@code POST
     * /projects/{id}/releases/{tag_name}/assets/links}), for the fields the SDK does not wrap, such as the type, the
     * direct asset path and the file path.
     *
     * @param payload the request body
     * @return the new link
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public ReleaseLink addLink(final GitLabProjectReleaseLinksCreatePayload payload) throws IOException {
        return new ReleaseLink(
                this, gitLab().releaselinks().create(project.id(), tagName(), requireNonNull(payload, "payload")));
    }

    /**
     * Deletes the release ({@code DELETE /projects/{id}/releases/{tag_name}}). The tag stays.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().releases().delete(project.id(), tagName());
    }

    /**
     * Loads the release again ({@code GET /projects/{id}/releases/{tag_name}}). This object is not changed.
     *
     * @return the release as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the release
     * @throws IOException if the transport fails
     */
    public Release refresh() throws IOException {
        return project.release(tagName())
                .orElseThrow(() -> new IllegalStateException("Release " + tagName() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabRelease response() {
        return response;
    }

    Release apply(final GitLabProjectReleasesUpdatePayload payload) throws IOException {
        return new Release(project, gitLab().releases().update(project.id(), tagName(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
