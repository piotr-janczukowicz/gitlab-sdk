package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranch;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesProtectPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;

/**
 * A branch of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Branch {

    private final Project project;
    private final GitLabBranch response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Branch(final Project project, final GitLabBranch response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the branch belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the branch name.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Tells whether the branch is protected.
     *
     * @return {@code true} if GitLab reports the branch as protected
     */
    public boolean isProtected() {
        return Boolean.TRUE.equals(response.protectedValue());
    }

    /**
     * Tells whether the branch is the default branch of the project.
     *
     * @return {@code true} if GitLab reports the branch as the default branch
     */
    public boolean isDefault() {
        return Boolean.TRUE.equals(response.defaultValue());
    }

    /**
     * Protects the branch with the default rules ({@code PUT /projects/{id}/repository/branches/{branch}/protect}).
     * This object is not changed.
     *
     * @return the branch as GitLab returns it afterwards
     * @throws IOException if the transport fails
     */
    public Branch protect() throws IOException {
        return new Branch(
                project,
                gitLab().branches()
                        .protect(
                                project.id(),
                                name(),
                                GitLabProjectBranchesProtectPayloadBuilder.builder()
                                        .build()));
    }

    /**
     * Removes the protection of the branch ({@code PUT /projects/{id}/repository/branches/{branch}/unprotect}). This
     * object is not changed.
     *
     * @return the branch as GitLab returns it afterwards
     * @throws IOException if the transport fails
     */
    public Branch unprotect() throws IOException {
        return new Branch(project, gitLab().branches().unprotect(project.id(), name()));
    }

    /**
     * Deletes the branch ({@code DELETE /projects/{id}/repository/branches/{branch}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().branches().delete(project.id(), name());
    }

    /**
     * Loads the branch again ({@code GET /projects/{id}/repository/branches/{branch}}). This object is not changed.
     *
     * @return the branch as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the branch
     * @throws IOException if the transport fails
     */
    public Branch refresh() throws IOException {
        return project.branch(name()).orElseThrow(() -> new IllegalStateException("Branch " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabBranch response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
