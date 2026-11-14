package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsApi;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateFile;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateFileAction;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippetBuilder;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabSnippet;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The snippet calls for the signed-in user's personal snippets. */
final class PersonalSnippetGateway {

    private final GitLab gitLab;

    PersonalSnippetGateway(final GitLab gitLab) {
        this.gitLab = gitLab;
    }

    /**
     * Lists the snippets ({@code GET /snippets}). The list answers {@code GitLabSnippet} records, which have the same
     * components as {@code GitLabPersonalSnippet}; each is copied component by component, without a request.
     */
    Stream<GitLabPersonalSnippet> list() throws IOException {
        return api().list().stream().map(PersonalSnippetGateway::personal);
    }

    Optional<GitLabPersonalSnippet> get(final long snippetId) throws IOException {
        return api().get(snippetId);
    }

    GitLabPersonalSnippet create(final GitLabCodeSnippetsCreatePayload payload) throws IOException {
        return api().create(payload);
    }

    GitLabPersonalSnippet update(final long snippetId, final SnippetValues.Changes changes) throws IOException {
        final SnippetVisibility visibility = changes.visibility();
        final List<SnippetValues.FileChange> files = changes.files();
        return api().update(
                        snippetId,
                        GitLabCodeSnippetsUpdatePayloadBuilder.builder()
                                .title(changes.title())
                                .description(changes.description())
                                .visibility(
                                        visibility == null
                                                ? null
                                                : GitLabCodeSnippetsUpdateVisibility.fromWire(visibility.value()))
                                .files(
                                        files.isEmpty()
                                                ? null
                                                : files.stream()
                                                        .map(PersonalSnippetGateway::file)
                                                        .toList())
                                .build());
    }

    void delete(final long snippetId) throws IOException {
        api().delete(snippetId);
    }

    String raw(final long snippetId) throws IOException {
        return api().getRaw(snippetId);
    }

    String fileRaw(final long snippetId, final String ref, final String filePath) throws IOException {
        return api().getFilesRaw(String.valueOf(snippetId), ref, filePath);
    }

    private static GitLabPersonalSnippet personal(final GitLabSnippet listed) {
        return GitLabPersonalSnippetBuilder.builder()
                .id(listed.id())
                .title(listed.title())
                .description(listed.description())
                .visibility(listed.visibility())
                .author(listed.author())
                .createdAt(listed.createdAt())
                .updatedAt(listed.updatedAt())
                .projectId(listed.projectId())
                .webUrl(listed.webUrl())
                .rawUrl(listed.rawUrl())
                .sshUrlToRepo(listed.sshUrlToRepo())
                .httpUrlToRepo(listed.httpUrlToRepo())
                .fileName(listed.fileName())
                .files(listed.files())
                .imported(listed.imported())
                .importedFrom(listed.importedFrom())
                .repositoryStorage(listed.repositoryStorage())
                .build();
    }

    private static GitLabCodeSnippetsUpdateFile file(final SnippetValues.FileChange change) {
        return new GitLabCodeSnippetsUpdateFile(
                GitLabCodeSnippetsUpdateFileAction.fromWire(change.action()),
                change.content(),
                change.path(),
                change.previousPath());
    }

    private GitLabCodeSnippetsApi api() {
        return gitLab.codesnippets();
    }
}
