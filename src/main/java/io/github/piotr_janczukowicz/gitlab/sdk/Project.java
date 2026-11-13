package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
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
     * Lists the issues of the project ({@code GET /projects/{id}/issues}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return issues(GitLabProjectIssuesListFilter.empty());
    }

    /**
     * Lists the issues of the project that match a filter ({@code GET /projects/{id}/issues}). Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the query parameters of the core
     * @return the issues
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues(final GitLabProjectIssuesListFilter filter) throws IOException {
        return sdk.gitLab().issues().project().list(id, filter).stream().map(listed -> new Issue(this, listed));
    }

    /**
     * Loads an issue ({@code GET /projects/{id}/issues/{issue_iid}}).
     *
     * @param iid the issue number within the project
     * @return the issue, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Issue> issue(final long iid) throws IOException {
        return sdk.gitLab().issues().project().get(id, iid).map(loaded -> new Issue(this, loaded));
    }

    /**
     * Creates an issue with a title ({@code POST /projects/{id}/issues}).
     *
     * @param title the issue title
     * @return the created issue
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final String title) throws IOException {
        return createIssue(GitLabProjectIssuesCreatePayloadBuilder.builder()
                .title(requireNotBlank(title, "title"))
                .build());
    }

    /**
     * Creates an issue from the payload of the core ({@code POST /projects/{id}/issues}).
     *
     * @param payload the payload
     * @return the created issue
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final GitLabProjectIssuesCreatePayload payload) throws IOException {
        return new Issue(this, sdk.gitLab().issues().project().create(id, payload));
    }

    /**
     * Lists the labels of the project ({@code GET /projects/{id}/labels}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the labels
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Label> labels() throws IOException {
        return sdk.gitLab().labels().list(id).stream().map(listed -> new Label(this, listed));
    }

    /**
     * Creates a label ({@code POST /projects/{id}/labels}).
     *
     * @param name the label name
     * @param color the color, as a hexadecimal value with a leading {@code #} or a CSS color name
     * @return the created label
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public Label createLabel(final String name, final String color) throws IOException {
        final GitLabProjectLabelsCreatePayload payload = GitLabProjectLabelsCreatePayloadBuilder.builder()
                .name(requireNotBlank(name, "name"))
                .color(requireNotBlank(color, "color"))
                .build();
        return new Label(this, sdk.gitLab().labels().create(id, payload));
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
