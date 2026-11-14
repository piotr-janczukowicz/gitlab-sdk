package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateFile;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The snippets of one project. {@code Project.snippets()} is the usual way in. The SDK always sends the files of a
 * snippet as the {@code files} array, never the {@code content} and {@code file_name} parameters, which the GitLab REST
 * docs mark deprecated.
 */
public final class ProjectSnippets {

    private final Project project;
    private final ProjectSnippetGateway gateway;

    private ProjectSnippets(final Project project, final ProjectSnippetGateway gateway) {
        this.project = project;
        this.gateway = gateway;
    }

    /**
     * Returns the snippets of a project; no request is made.
     *
     * @param project the project
     * @return the snippets of the project
     * @throws NullPointerException if {@code project} is {@code null}
     */
    public static ProjectSnippets forProject(final Project project) {
        requireNonNull(project, "project");
        return new ProjectSnippets(project, new ProjectSnippetGateway(Internals.gitLab(project), project.id()));
    }

    /**
     * Lists the snippets of the project ({@code GET /projects/{id}/snippets}). The first page is requested at once;
     * further pages are fetched as the stream reaches them. The listed snippets are complete, so no snippet is loaded
     * again. The stream can be used once; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the snippets
     * @throws IOException if the list cannot be loaded
     */
    public Stream<Snippet> all() throws IOException {
        return gateway.list().map(listed -> new Snippet(this, listed));
    }

    /**
     * Loads one snippet ({@code GET /projects/{id}/snippets/{snippet_id}}).
     *
     * @param id the id of the snippet
     * @return the snippet, or empty when GitLab answers HTTP 404; empty also when snippets are not available
     * @throws IOException if the transport fails
     */
    public Optional<Snippet> find(final long id) throws IOException {
        return gateway.get(id).map(loaded -> new Snippet(this, loaded));
    }

    /**
     * Creates a snippet with one file ({@code POST /projects/{id}/snippets}), sent as a one-entry {@code files} array.
     *
     * @param title the title
     * @param visibility who can see the snippet
     * @param filePath the path of the file, such as {@code dir/add.rb}
     * @param content the content of the file, sent as given
     * @return the snippet as GitLab returned it
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code filePath} is blank
     * @throws IOException if the transport fails
     */
    public Snippet create(
            final String title, final SnippetVisibility visibility, final String filePath, final String content)
            throws IOException {
        final String checkedTitle = requireNotBlank(title, SnippetValues.TITLE);
        final SnippetVisibility checkedVisibility = requireNonNull(visibility, SnippetValues.VISIBILITY);
        final GitLabProjectProjectSnippetsCreateFile file = new GitLabProjectProjectSnippetsCreateFile(
                requireNotBlank(filePath, SnippetValues.FILE_PATH), requireNonNull(content, SnippetValues.CONTENT));
        return create(GitLabProjectProjectSnippetsCreatePayloadBuilder.builder()
                .title(checkedTitle)
                .visibility(GitLabProjectProjectSnippetsCreateVisibility.fromWire(checkedVisibility.value()))
                .files(List.of(file))
                .build());
    }

    /**
     * Creates a snippet from the core's payload ({@code POST /projects/{id}/snippets}), for several files or a
     * description. The payload is sent unchanged; its {@code content} and {@code fileName} are deprecated by GitLab in
     * favor of {@code files}.
     *
     * @param payload the core's payload
     * @return the snippet as GitLab returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Snippet create(final GitLabProjectProjectSnippetsCreatePayload payload) throws IOException {
        return new Snippet(this, gateway.create(requireNonNull(payload, "payload")));
    }

    Project project() {
        return project;
    }

    ProjectSnippetGateway gateway() {
        return gateway;
    }
}
