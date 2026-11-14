package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link Group} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class GroupUpdate {

    private final GitLabSdk sdk;
    private final String id;
    private final ManagementForms form = new ManagementForms();
    private final UpdateState state = new UpdateState();

    GroupUpdate(final GitLabSdk sdk, final String id) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.id = requireNotBlank(id, "id");
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public GroupUpdate name(final String name) {
        return put("name", requireNotBlank(name, "name"));
    }

    /**
     * Sets the path; this changes the URL of the group.
     *
     * @param path the new path
     * @return this update
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public GroupUpdate path(final String path) {
        return put("path", requireNotBlank(path, "path"));
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GroupUpdate description(final String description) {
        return put("description", requireNonNull(description, "description"));
    }

    /**
     * Sets the visibility.
     *
     * @param visibility the new visibility
     * @return this update
     * @throws NullPointerException if {@code visibility} is {@code null}
     */
    public GroupUpdate visibility(final Visibility visibility) {
        return put("visibility", requireNonNull(visibility, "visibility").value());
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}}) and loads the group again ({@code GET /groups/{id}}), addressed by
     * its numeric id, so a changed path does not matter. The group this update was made from is not changed.
     *
     * @return the group as GitLab returns it afterwards
     * @throws IllegalStateException if nothing was set, or if the group is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Group send() throws IOException {
        state.requireChange();
        final String updated = String.valueOf(
                requireNonNull(sdk.gitLab().groups().update(id, form.build()).id(), "id"));
        return sdk.group(updated).fetch().orElseThrow(() -> new IllegalStateException("Group " + updated + " is gone"));
    }

    private GroupUpdate put(final String field, final String value) {
        form.put(field, value);
        state.mark();
        return this;
    }
}
