package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsUpdateByNamePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link GroupLabel} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. {@code null} cannot be sent; an empty description is sent as given, and
 * whether GitLab then clears it is not verified against a live instance.
 */
public final class GroupLabelUpdate {

    private final GroupLabel label;
    private final GitLabGroupGroupLabelsUpdateByNamePayloadBuilder payload =
            GitLabGroupGroupLabelsUpdateByNamePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param label the label to change
     * @throws NullPointerException if {@code label} is {@code null}
     */
    public GroupLabelUpdate(final GroupLabel label) {
        this.label = requireNonNull(label, "label");
    }

    /**
     * Sets the new label name.
     *
     * @param name the new value
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public GroupLabelUpdate name(final String name) {
        payload.newName(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sets the new color, as a hexadecimal value with a leading {@code #} or a CSS color name.
     *
     * @param color the new value
     * @return this update
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code color} is blank
     */
    public GroupLabelUpdate color(final String color) {
        payload.color(requireNotBlank(color, "color"));
        return changed();
    }

    /**
     * Sets the new description.
     *
     * @param description the new value
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GroupLabelUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Archives or unarchives the label.
     *
     * @param archived {@code true} to archive the label, {@code false} to unarchive it
     * @return this update
     */
    public GroupLabelUpdate archived(final boolean archived) {
        payload.archived(archived);
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/labels/{name}}). The label this update was made from is not changed.
     * After a rename, address the new label by the new name: the returned object does.
     *
     * @return the label as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupLabel send() throws IOException {
        state.requireChange();
        return label.apply(payload.build());
    }

    private GroupLabelUpdate changed() {
        state.mark();
        return this;
    }
}
