package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link Project} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. Archiving stays with {@link Project#archive()}.
 */
public final class ProjectUpdate {

    private final GitLabSdk sdk;
    private final String id;
    private final ManagementForms form = new ManagementForms();
    private final UpdateState state = new UpdateState();

    ProjectUpdate(final GitLabSdk sdk, final String id) {
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
    public ProjectUpdate name(final String name) {
        return put("name", requireNotBlank(name, "name"));
    }

    /**
     * Sets the path; this changes the URL of the project.
     *
     * @param path the new path
     * @return this update
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public ProjectUpdate path(final String path) {
        return put("path", requireNotBlank(path, "path"));
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public ProjectUpdate description(final String description) {
        return put("description", requireNonNull(description, "description"));
    }

    /**
     * Sends an empty string as the description, the same as {@code description("")}. Whether GitLab then clears the
     * description is not verified against a live instance.
     *
     * @return this update
     */
    public ProjectUpdate clearDescription() {
        return put("description", "");
    }

    /**
     * Sets the visibility.
     *
     * @param visibility the new visibility
     * @return this update
     * @throws NullPointerException if {@code visibility} is {@code null}
     */
    public ProjectUpdate visibility(final Visibility visibility) {
        return put("visibility", requireNonNull(visibility, "visibility").wireValue());
    }

    /**
     * Sets the default branch.
     *
     * @param branch the branch name
     * @return this update
     * @throws NullPointerException if {@code branch} is {@code null}
     * @throws IllegalArgumentException if {@code branch} is blank
     */
    public ProjectUpdate defaultBranch(final String branch) {
        return put("default_branch", requireNotBlank(branch, "branch"));
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}}) and loads the project again ({@code GET /projects/{id}}),
     * addressed by its numeric id, so a changed path does not matter. The project this update was made from is not
     * changed.
     *
     * @return the project as GitLab returns it afterwards
     * @throws IllegalStateException if nothing was set, or if the project is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Project send() throws IOException {
        state.requireChange();
        final String updated = String.valueOf(requireNonNull(
                sdk.gitLab().projects().project().update(id, form.build()).id(), "id"));
        return sdk.project(updated)
                .fetch()
                .orElseThrow(() -> new IllegalStateException("Project " + updated + " is gone"));
    }

    private ProjectUpdate put(final String field, final String value) {
        form.put(field, value);
        state.mark();
        return this;
    }
}
