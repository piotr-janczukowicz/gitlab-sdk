package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksUpdateLinkType;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link ReleaseLink} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class ReleaseLinkUpdate {

    private final ReleaseLink link;
    private final GitLabProjectReleaseLinksUpdatePayloadBuilder payload =
            GitLabProjectReleaseLinksUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param link the link to change
     * @throws NullPointerException if {@code link} is {@code null}
     */
    public ReleaseLinkUpdate(final ReleaseLink link) {
        this.link = requireNonNull(link, "link");
    }

    /**
     * Sets the name.
     *
     * @param name the name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public ReleaseLinkUpdate name(final String name) {
        payload.name(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sets the URL.
     *
     * @param url the URL
     * @return this update
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     */
    public ReleaseLinkUpdate url(final String url) {
        payload.url(requireNotBlank(url, "url"));
        return changed();
    }

    /**
     * Sets the type.
     *
     * @param type the type
     * @return this update
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public ReleaseLinkUpdate type(final ReleaseLinkType type) {
        payload.linkType(GitLabProjectReleaseLinksUpdateLinkType.fromWire(
                requireNonNull(type, "type").value()));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/releases/{tag_name}/assets/links/{link_id}}). The link this update
     * was made from is not changed. GitLab answers with the link, so there is no extra request.
     *
     * @return the link as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public ReleaseLink send() throws IOException {
        state.requireChange();
        return link.apply(payload.build());
    }

    private ReleaseLinkUpdate changed() {
        state.mark();
        return this;
    }
}
