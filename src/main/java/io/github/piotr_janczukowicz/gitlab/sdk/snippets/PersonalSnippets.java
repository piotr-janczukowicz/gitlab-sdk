package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreateFile;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The personal snippets of the signed-in user, which belong to no project. {@code GitLabSdk.snippets()} is the usual
 * way in. The SDK always sends the files of a snippet as the {@code files} array, never the {@code content} and
 * {@code file_name} parameters, which the GitLab REST docs mark deprecated.
 */
public final class PersonalSnippets {

    private final PersonalSnippetGateway gateway;

    private PersonalSnippets(final PersonalSnippetGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the personal snippets of the account the SDK signs in as; no request is made.
     *
     * @param sdk the SDK
     * @return the personal snippets
     * @throws NullPointerException if {@code sdk} is {@code null}
     */
    public static PersonalSnippets forSdk(final GitLabSdk sdk) {
        requireNonNull(sdk, "sdk");
        return new PersonalSnippets(new PersonalSnippetGateway(Internals.gitLab(sdk)));
    }

    /**
     * Lists the snippets of the signed-in user ({@code GET /snippets}). The first page is requested at once; further
     * pages are fetched as the stream reaches them. The core decodes the list as {@code GitLabSnippet}, which has the
     * same fields as {@code GitLabPersonalSnippet}, so each is copied into a {@code GitLabPersonalSnippet} without a
     * request. The stream can be used once; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the snippets
     * @throws IOException if the list cannot be loaded
     */
    public Stream<PersonalSnippet> all() throws IOException {
        return gateway.list().map(listed -> new PersonalSnippet(this, listed));
    }

    /**
     * Loads one snippet ({@code GET /snippets/{id}}): GitLab is expected to return any snippet the signed-in user can
     * see, not only their own. That can be a project snippet ({@code response().projectId()} set); its {@code update()}
     * and {@code delete()} still go to {@code /snippets/{id}}, which is not verified for a project snippet.
     *
     * @param id the id of the snippet
     * @return the snippet, or empty when GitLab answers HTTP 404; a 403 passes through
     * @throws IOException if the transport fails
     */
    public Optional<PersonalSnippet> find(final long id) throws IOException {
        return gateway.get(id).map(loaded -> new PersonalSnippet(this, loaded));
    }

    /**
     * Creates a personal snippet with one file ({@code POST /snippets}), sent as a one-entry {@code files} array.
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
    public PersonalSnippet create(
            final String title, final SnippetVisibility visibility, final String filePath, final String content)
            throws IOException {
        final String checkedTitle = requireNotBlank(title, SnippetValues.TITLE);
        final SnippetVisibility checkedVisibility = requireNonNull(visibility, SnippetValues.VISIBILITY);
        final GitLabCodeSnippetsCreateFile file = new GitLabCodeSnippetsCreateFile(
                requireNotBlank(filePath, SnippetValues.FILE_PATH), requireNonNull(content, SnippetValues.CONTENT));
        return create(GitLabCodeSnippetsCreatePayloadBuilder.builder()
                .title(checkedTitle)
                .visibility(GitLabCodeSnippetsCreateVisibility.fromWire(checkedVisibility.value()))
                .files(List.of(file))
                .build());
    }

    /**
     * Creates a personal snippet from the core's payload ({@code POST /snippets}), for several files or a description.
     * The payload is sent unchanged; its {@code content} and {@code fileName} are deprecated by GitLab in favor of
     * {@code files}.
     *
     * @param payload the core's payload
     * @return the snippet as GitLab returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PersonalSnippet create(final GitLabCodeSnippetsCreatePayload payload) throws IOException {
        return new PersonalSnippet(this, gateway.create(requireNonNull(payload, "payload")));
    }

    PersonalSnippetGateway gateway() {
        return gateway;
    }
}
