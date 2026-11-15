package io.github.piotr_janczukowicz.gitlab.sdk.badges;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBadge;
import io.github.piotr_janczukowicz.gitlab.api.groupbadges.GitLabGroupGroupBadgesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A badge of a {@link Group}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class GroupBadge {

    private final Group group;
    private final GitLabBadge response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupBadge(final Group group, final GitLabBadge response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the badge belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the id of the badge.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the name of the badge.
     *
     * @return the name, or empty when it has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the link URL as it was stored, with its placeholders such as {@code %{project_path}}.
     *
     * @return the link URL
     */
    public String linkUrl() {
        return requireNonNull(response.linkUrl(), "linkUrl");
    }

    /**
     * Returns the image URL as it was stored, with its placeholders.
     *
     * @return the image URL
     */
    public String imageUrl() {
        return requireNonNull(response.imageUrl(), "imageUrl");
    }

    /**
     * Returns the link URL with the placeholders replaced.
     *
     * @return the rendered link URL, or empty when the response has none
     */
    public Optional<String> renderedLinkUrl() {
        return Optional.ofNullable(response.renderedLinkUrl());
    }

    /**
     * Returns the image URL with the placeholders replaced.
     *
     * @return the rendered image URL, or empty when the response has none
     */
    public Optional<String> renderedImageUrl() {
        return Optional.ofNullable(response.renderedImageUrl());
    }

    /**
     * Starts a change of the badge. Nothing is sent until {@link GroupBadgeUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupBadgeUpdate update() {
        return new GroupBadgeUpdate(this);
    }

    /**
     * Deletes the badge ({@code DELETE /groups/{id}/badges/{badge_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().groupbadges().delete(group.id(), id());
    }

    /**
     * Loads the badge again ({@code GET /groups/{id}/badges/{badge_id}}). This object is not changed.
     *
     * @return the badge as GitLab holds it now
     * @throws IllegalStateException if the badge is gone
     * @throws IOException if the transport fails
     */
    public GroupBadge refresh() throws IOException {
        return group.badge(id()).orElseThrow(() -> new IllegalStateException("Badge " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabBadge response() {
        return response;
    }

    GroupBadge apply(final GitLabGroupGroupBadgesUpdatePayload payload) throws IOException {
        return new GroupBadge(group, gitLab().groupbadges().update(group.id(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
