package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A project, identified by its numeric id or URL-encoded path. A handle from {@link GitLabSdk#project(String)} holds no
 * data from GitLab; {@link #fetch()} returns a loaded project. It is the starting point for the operations that belong
 * to a project. The object is immutable: every operation returns a new project built from GitLab's answer.
 */
public final class Project {

    private static final String IS_GONE = " is gone";

    static {
        Internals.registerProjectAccess(project -> project.sdk.gitLab());
    }

    private final GitLabSdk sdk;
    private final String id;
    private final @Nullable GitLabProjectWithAccessAndCatalog response;

    Project(final GitLabSdk sdk, final String id, final @Nullable GitLabProjectWithAccessAndCatalog response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.id = requireNotBlank(id, "id");
        this.response = response;
    }

    /**
     * Returns the id this object was addressed with.
     *
     * @return the numeric id or the URL-encoded path
     */
    public String id() {
        return id;
    }

    /**
     * Loads the project ({@code GET /projects/{id}}). This object is not changed.
     *
     * @return the loaded project, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Project> fetch() throws IOException {
        return sdk.gitLab().projects().project().get(id).map(loaded -> new Project(sdk, id, loaded));
    }

    /**
     * Reloads the project ({@code GET /projects/{id}}). This object is not changed.
     *
     * @return the reloaded project
     * @throws IllegalStateException if GitLab no longer has the project
     * @throws IOException if the transport fails
     */
    public Project refresh() throws IOException {
        return fetch().orElseThrow(() -> new IllegalStateException("Project " + id + IS_GONE));
    }

    /**
     * Archives the project ({@code POST /projects/{id}/archive}) and loads it again.
     *
     * @return the archived project as GitLab returns it afterwards
     * @throws IllegalStateException if GitLab no longer has the project
     * @throws IOException if the transport fails
     */
    public Project archive() throws IOException {
        sdk.gitLab().projects().project().archive(id);
        return refresh();
    }

    /**
     * Unarchives the project ({@code POST /projects/{id}/unarchive}) and loads it again.
     *
     * @return the unarchived project as GitLab returns it afterwards
     * @throws IllegalStateException if GitLab no longer has the project
     * @throws IOException if the transport fails
     */
    public Project unarchive() throws IOException {
        sdk.gitLab().projects().project().unarchive(id);
        return refresh();
    }

    /**
     * Deletes the project ({@code DELETE /projects/{id}}). GitLab may only mark it for deletion, depending on the
     * instance settings.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        sdk.gitLab().projects().project().delete(id);
    }

    /**
     * Returns the complete response of a loaded project.
     *
     * @return the response record
     * @throws IllegalStateException if this is a handle that was not loaded with {@link #fetch()}
     */
    public GitLabProjectWithAccessAndCatalog response() {
        if (response == null) {
            throw new IllegalStateException("Project " + id + " is not loaded; call fetch()");
        }
        return response;
    }
}
