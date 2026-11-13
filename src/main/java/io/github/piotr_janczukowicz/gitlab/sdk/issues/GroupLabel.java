package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsUpdateByNamePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A label of a {@link Group}, as GitLab returned it. A label is addressed by its name. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover. GitLab lists, loads,
 * creates and updates a group label with the same record, so none of them needs an extra request. A group label has no
 * priority.
 */
public final class GroupLabel {

    private final Group group;
    private final GitLabGroupLabel response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupLabel(final Group group, final GitLabGroupLabel response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the label belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the label name.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the label color.
     *
     * @return the color, for example {@code #FF0000}
     */
    public String color() {
        return requireNonNull(response.color(), "color");
    }

    /**
     * Returns the label description.
     *
     * @return the description, or empty when GitLab does not return one
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Tells whether the signed-in user is subscribed to the label.
     *
     * @return {@code true} if GitLab reports a subscription; {@code false} also when the flag is absent
     */
    public boolean isSubscribed() {
        return Boolean.TRUE.equals(response.subscribed());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link GroupLabelUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupLabelUpdate update() {
        return new GroupLabelUpdate(this);
    }

    /**
     * Subscribes the signed-in user to the label ({@code POST /groups/{id}/labels/{name}/subscribe}).
     *
     * @return the label as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public GroupLabel subscribe() throws IOException {
        return new GroupLabel(group, gitLab().grouplabels().subscribe(group.id(), name()));
    }

    /**
     * Unsubscribes the signed-in user from the label ({@code POST /groups/{id}/labels/{name}/unsubscribe}).
     *
     * @return the label as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public GroupLabel unsubscribe() throws IOException {
        return new GroupLabel(group, gitLab().grouplabels().unsubscribe(group.id(), name()));
    }

    /**
     * Deletes the label ({@code DELETE /groups/{id}/labels/{name}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().grouplabels().deleteByName(group.id(), name());
    }

    /**
     * Loads the label again ({@code GET /groups/{id}/labels/{name}}). This object is not changed.
     *
     * @return the label as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the label
     * @throws IOException if the transport fails
     */
    public GroupLabel refresh() throws IOException {
        return group.label(name()).orElseThrow(() -> new IllegalStateException("Group label " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabGroupLabel response() {
        return response;
    }

    GroupLabel apply(final GitLabGroupGroupLabelsUpdateByNamePayload payload) throws IOException {
        return new GroupLabel(group, gitLab().grouplabels().updateByName(group.id(), name(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
