package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLink;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A link of a {@link Release}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response.
 */
public final class ReleaseLink {

    private final Release release;
    private final GitLabReleaseLink response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param release the release the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ReleaseLink(final Release release, final GitLabReleaseLink response) {
        this.release = requireNonNull(release, "release");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the release the link belongs to.
     *
     * @return the release
     */
    public Release release() {
        return release;
    }

    /**
     * Returns the id of the link.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the name of the link.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the URL of the link.
     *
     * @return the URL
     */
    public String url() {
        return requireNonNull(response.url(), "url");
    }

    /**
     * Returns the permanent URL GitLab builds from the direct asset path of the link.
     *
     * @return the URL, or empty when the link has no direct asset path
     */
    public Optional<String> directAssetUrl() {
        return Optional.ofNullable(response.directAssetUrl());
    }

    /**
     * Returns the type of the link.
     *
     * @return the type; {@link ReleaseLinkType#OTHER}, GitLab's default, when the response has none
     * @throws IllegalArgumentException if the SDK does not know the type in the response; {@link #typeName()} gives the
     *     raw name
     */
    public ReleaseLinkType type() {
        final String name = response.linkType();
        return name == null ? ReleaseLinkType.OTHER : ReleaseLinkType.from(name);
    }

    /**
     * Returns the type of the link as GitLab spelled it, whether or not the SDK knows it.
     *
     * @return the raw type, or empty when the response has none
     */
    public Optional<String> typeName() {
        return Optional.ofNullable(response.linkType());
    }

    /**
     * Starts a change of the name, URL or type. Nothing is sent until {@link ReleaseLinkUpdate#send()}.
     *
     * @return the update builder
     */
    public ReleaseLinkUpdate update() {
        return new ReleaseLinkUpdate(this);
    }

    /**
     * Deletes the link ({@code DELETE /projects/{id}/releases/{tag_name}/assets/links/{link_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().releaselinks().delete(release.project().id(), release.tagName(), id());
    }

    /**
     * Loads the link again ({@code GET /projects/{id}/releases/{tag_name}/assets/links/{link_id}}). This object is not
     * changed.
     *
     * @return the link as GitLab holds it now
     * @throws IllegalStateException if the link is gone
     * @throws IOException if the transport fails
     */
    public ReleaseLink refresh() throws IOException {
        return release.link(id()).orElseThrow(() -> new IllegalStateException("Release link " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabReleaseLink response() {
        return response;
    }

    ReleaseLink apply(final GitLabProjectReleaseLinksUpdatePayload payload) throws IOException {
        return new ReleaseLink(
                release, gitLab().releaselinks().update(release.project().id(), release.tagName(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(release.project());
    }
}
