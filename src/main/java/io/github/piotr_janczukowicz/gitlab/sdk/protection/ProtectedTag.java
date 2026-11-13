package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProtectedTag;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;

/**
 * A protected tag of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment
 * it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * The tag is addressed by its name, which may be a wildcard; the name reaches the core client unchanged and the core
 * encodes it. GitLab cannot change a protected tag in place: remove the protection and protect the tag again. User-,
 * group-, deploy-key- and member-role-based rules are not wrapped: they are left out of the access lists and stay
 * available from {@link #response()}.
 */
public final class ProtectedTag {

    private final Project project;
    private final GitLabProtectedTag response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ProtectedTag(final Project project, final GitLabProtectedTag response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the tag belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the name or wildcard of the protected tag.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the access levels allowed to create the tag. Only role-based rules are listed; rules of a user, a group,
     * a deploy key or a member role are skipped and stay available from {@link #response()}.
     *
     * @return the levels, empty when GitLab returned none
     * @throws IllegalArgumentException if GitLab returns a level the SDK does not know; {@link #createAccessValues()}
     *     still gives the numbers
     */
    public List<ProtectedAccess> createAccess() {
        return ProtectedAccesses.levels(response.createAccessLevels());
    }

    /**
     * Returns the access level numbers allowed to create the tag as GitLab returned them, an escape hatch for a level
     * {@link ProtectedAccess} does not know. Only role-based rules are listed; rules of a user, a group, a deploy key
     * or a member role are skipped and stay available from {@link #response()}.
     *
     * @return the numbers, empty when GitLab returned none
     */
    public List<Integer> createAccessValues() {
        return ProtectedAccesses.values(response.createAccessLevels());
    }

    /**
     * Removes the protection ({@code DELETE /projects/{id}/protected_tags/{name}}). The tags stay.
     *
     * @throws IOException if the transport fails
     */
    public void unprotect() throws IOException {
        gitLab().protectedtags().delete(project.id(), name());
    }

    /**
     * Loads the protected tag again ({@code GET /projects/{id}/protected_tags/{name}}). This object is not changed.
     *
     * @return the protected tag as GitLab holds it now
     * @throws IllegalStateException if the tag is no longer protected
     * @throws IOException if the transport fails
     */
    public ProtectedTag refresh() throws IOException {
        return project.protectedTag(name())
                .orElseThrow(() -> new IllegalStateException("Protected tag " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabProtectedTag response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
