package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsApi;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateFile;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateFileAction;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippet;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The snippet calls for a project. */
final class ProjectSnippetGateway {

    private final GitLab gitLab;
    private final String id;

    ProjectSnippetGateway(final GitLab gitLab, final String id) {
        this.gitLab = gitLab;
        this.id = id;
    }

    Stream<GitLabProjectSnippet> list() throws IOException {
        return api().list(id).stream();
    }

    Optional<GitLabProjectSnippet> get(final long snippetId) throws IOException {
        return api().get(id, snippetId);
    }

    GitLabProjectSnippet create(final GitLabProjectProjectSnippetsCreatePayload payload) throws IOException {
        return api().create(id, payload);
    }

    GitLabProjectSnippet update(final long snippetId, final SnippetValues.Changes changes) throws IOException {
        final SnippetVisibility visibility = changes.visibility();
        final List<SnippetValues.FileChange> files = changes.files();
        return api().update(
                        id,
                        snippetId,
                        GitLabProjectProjectSnippetsUpdatePayloadBuilder.builder()
                                .title(changes.title())
                                .description(changes.description())
                                .visibility(
                                        visibility == null
                                                ? null
                                                : GitLabProjectProjectSnippetsUpdateVisibility.fromWire(
                                                        visibility.value()))
                                .files(
                                        files.isEmpty()
                                                ? null
                                                : files.stream()
                                                        .map(ProjectSnippetGateway::file)
                                                        .toList())
                                .build());
    }

    void delete(final long snippetId) throws IOException {
        api().delete(id, snippetId);
    }

    String raw(final long snippetId) throws IOException {
        return api().getRaw(id, snippetId);
    }

    String fileRaw(final long snippetId, final String ref, final String filePath) throws IOException {
        return api().getFilesRaw(id, snippetId, ref, filePath);
    }

    private static GitLabProjectProjectSnippetsUpdateFile file(final SnippetValues.FileChange change) {
        return new GitLabProjectProjectSnippetsUpdateFile(
                GitLabProjectProjectSnippetsUpdateFileAction.fromWire(change.action()),
                change.content(),
                change.path(),
                change.previousPath());
    }

    private GitLabProjectProjectSnippetsApi api() {
        return gitLab.projectsnippets();
    }
}
