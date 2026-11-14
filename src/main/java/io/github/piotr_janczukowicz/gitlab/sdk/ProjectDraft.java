package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.io.IOException;

/** Collects the fields of a new project and creates it in one request when {@link #send()} is called. */
public final class ProjectDraft {

    private final GitLabSdk sdk;
    private final ManagementForms form = new ManagementForms();

    ProjectDraft(final GitLabSdk sdk, final String name) {
        this.sdk = requireNonNull(sdk, "sdk");
        form.put("name", requireNotBlank(name, "name"));
    }

    /**
     * Sets the path (the URL slug); GitLab derives it from the name when it is not set.
     *
     * @param path the path
     * @return this draft
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public ProjectDraft path(final String path) {
        form.put("path", requireNotBlank(path, "path"));
        return this;
    }

    /**
     * Sets the namespace (user or group) to create the project in; GitLab uses the namespace of the signed-in account
     * when it is not set.
     *
     * @param namespaceId the numeric id of the namespace
     * @return this draft
     */
    public ProjectDraft namespaceId(final long namespaceId) {
        form.put("namespace_id", String.valueOf(namespaceId));
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this draft
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public ProjectDraft description(final String description) {
        form.put("description", requireNonNull(description, "description"));
        return this;
    }

    /**
     * Sets the visibility.
     *
     * @param visibility the visibility
     * @return this draft
     * @throws NullPointerException if {@code visibility} is {@code null}
     */
    public ProjectDraft visibility(final Visibility visibility) {
        form.put("visibility", requireNonNull(visibility, "visibility").value());
        return this;
    }

    /**
     * Sets the name of the default branch.
     *
     * @param branch the branch name
     * @return this draft
     * @throws NullPointerException if {@code branch} is {@code null}
     * @throws IllegalArgumentException if {@code branch} is blank
     */
    public ProjectDraft defaultBranch(final String branch) {
        form.put("default_branch", requireNotBlank(branch, "branch"));
        return this;
    }

    /**
     * Sets whether GitLab creates an initial commit with a README.
     *
     * @param initialize {@code true} to create the README
     * @return this draft
     */
    public ProjectDraft initializeWithReadme(final boolean initialize) {
        form.put("initialize_with_readme", String.valueOf(initialize));
        return this;
    }

    /**
     * Creates the project ({@code POST /projects}) and loads it again ({@code GET /projects/{id}}), because GitLab
     * answers the creation with a shorter record.
     *
     * @return the created project as GitLab returns it afterwards
     * @throws IllegalStateException if the project is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Project send() throws IOException {
        final String created = String.valueOf(requireNonNull(
                sdk.gitLab().projects().project().create(form.build()).id(), "id"));
        return sdk.project(created)
                .fetch()
                .orElseThrow(() -> new IllegalStateException("Project " + created + " is gone"));
    }
}
