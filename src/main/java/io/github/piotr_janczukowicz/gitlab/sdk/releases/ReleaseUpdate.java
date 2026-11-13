package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Collects changes to a {@link Release} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. {@code null} cannot be sent; an empty string, where one is accepted, is sent
 * as given, and whether GitLab then clears the field is not verified against a live instance.
 */
public final class ReleaseUpdate {

    private final Release release;
    private final GitLabProjectReleasesUpdatePayloadBuilder payload =
            GitLabProjectReleasesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param release the release to change
     * @throws NullPointerException if {@code release} is {@code null}
     */
    public ReleaseUpdate(final Release release) {
        this.release = requireNonNull(release, "release");
    }

    /**
     * Sets the name.
     *
     * @param name the name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public ReleaseUpdate name(final String name) {
        payload.name(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sets the description (the release notes).
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public ReleaseUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets the time the release is published.
     *
     * @param releasedAt the release time
     * @return this update
     * @throws NullPointerException if {@code releasedAt} is {@code null}
     */
    public ReleaseUpdate releasedAt(final Instant releasedAt) {
        payload.releasedAt(requireNonNull(releasedAt, "releasedAt"));
        return changed();
    }

    /**
     * Sets the milestones the release is associated with, by title. The list is sent as given, an empty one included;
     * that an empty list removes the milestones is not confirmed by the core's documentation, so do not rely on it.
     *
     * @param milestones the titles of the milestones
     * @return this update
     * @throws NullPointerException if {@code milestones} is {@code null}
     */
    public ReleaseUpdate milestones(final List<String> milestones) {
        payload.milestones(requireNonNull(milestones, "milestones"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/releases/{tag_name}}). The release this update was made from is not
     * changed. GitLab answers with the release, so there is no extra request.
     *
     * @return the release as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Release send() throws IOException {
        state.requireChange();
        return release.apply(payload.build());
    }

    private ReleaseUpdate changed() {
        state.mark();
        return this;
    }
}
