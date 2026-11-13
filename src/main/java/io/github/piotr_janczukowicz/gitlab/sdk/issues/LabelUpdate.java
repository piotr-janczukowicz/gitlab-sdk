package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsUpdateByNamePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link Label} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class LabelUpdate {

    private final Label label;
    private final GitLabProjectLabelsUpdateByNamePayloadBuilder payload =
            GitLabProjectLabelsUpdateByNamePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param label the label to change
     * @throws NullPointerException if {@code label} is {@code null}
     */
    public LabelUpdate(final Label label) {
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
    public LabelUpdate name(final String name) {
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
    public LabelUpdate color(final String color) {
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
    public LabelUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/labels/{name}}). The label this update was made from is not changed.
     *
     * @return the label as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Label send() throws IOException {
        state.requireChange();
        return label.apply(payload.build());
    }

    private LabelUpdate changed() {
        state.mark();
        return this;
    }
}
