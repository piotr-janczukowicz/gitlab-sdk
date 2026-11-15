package io.github.piotr_janczukowicz.gitlab.sdk.badges;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projectbadges.GitLabProjectProjectBadgesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link Badge} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The core's payload has no way to send a null, so a field cannot be cleared.
 */
public final class BadgeUpdate {

    private final Badge badge;
    private final GitLabProjectProjectBadgesUpdatePayloadBuilder payload =
            GitLabProjectProjectBadgesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param badge the badge to change
     * @throws NullPointerException if {@code badge} is {@code null}
     */
    public BadgeUpdate(final Badge badge) {
        this.badge = requireNonNull(badge, "badge");
    }

    /**
     * Sets the URL the badge links to. It may hold placeholders such as {@code %{project_path}}.
     *
     * @param linkUrl the link URL
     * @return this update
     * @throws NullPointerException if {@code linkUrl} is {@code null}
     * @throws IllegalArgumentException if {@code linkUrl} is blank
     */
    public BadgeUpdate linkUrl(final String linkUrl) {
        payload.linkUrl(requireNotBlank(linkUrl, "linkUrl"));
        return changed();
    }

    /**
     * Sets the URL of the badge image. It may hold placeholders.
     *
     * @param imageUrl the image URL
     * @return this update
     * @throws NullPointerException if {@code imageUrl} is {@code null}
     * @throws IllegalArgumentException if {@code imageUrl} is blank
     */
    public BadgeUpdate imageUrl(final String imageUrl) {
        payload.imageUrl(requireNotBlank(imageUrl, "imageUrl"));
        return changed();
    }

    /**
     * Sets the name of the badge.
     *
     * @param name the name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public BadgeUpdate name(final String name) {
        payload.name(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/badges/{badge_id}}). The badge this update was made from is not
     * changed.
     *
     * @return the badge as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Badge send() throws IOException {
        state.requireChange();
        return badge.apply(payload.build());
    }

    private BadgeUpdate changed() {
        state.mark();
        return this;
    }
}
