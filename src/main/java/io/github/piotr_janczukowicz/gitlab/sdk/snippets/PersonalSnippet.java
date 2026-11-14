package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A personal snippet, as GitLab returned it. The object is immutable: it keeps the data of the moment it was loaded,
 * and {@link #response()} gives the complete response. It has no parent navigation: a personal snippet belongs to its
 * author, not to a project. Its {@code toString()} is the default one, so it never prints the content or the
 * description.
 */
public final class PersonalSnippet {

    private final PersonalSnippets snippets;
    private final GitLabPersonalSnippet response;

    /**
     * Wraps a response. Applications normally get this object from {@link PersonalSnippets}; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param snippets the personal snippets of the SDK that makes the requests
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public PersonalSnippet(final PersonalSnippets snippets, final GitLabPersonalSnippet response) {
        this.snippets = requireNonNull(snippets, "snippets");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), SnippetValues.TITLE);
    }

    /**
     * Returns the description.
     *
     * @return the description, or empty when GitLab sent none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns who can see the snippet.
     *
     * @return the visibility, or empty when GitLab sent none
     * @throws IllegalArgumentException if the SDK does not know the visibility; {@link #visibilityName()} gives the raw
     *     name
     */
    public Optional<SnippetVisibility> visibility() {
        return SnippetValues.visibility(response.visibility());
    }

    /**
     * Returns the visibility as GitLab spells it, such as {@code private}.
     *
     * @return the raw name, or empty when GitLab sent none
     */
    public Optional<String> visibilityName() {
        return Optional.ofNullable(response.visibility());
    }

    /**
     * Returns the author.
     *
     * @return the core's user record, or empty when GitLab sent none
     */
    public Optional<GitLabUserBasic> author() {
        return Optional.ofNullable(response.author());
    }

    /**
     * Returns the URL of the snippet in the GitLab web interface.
     *
     * @return the URL, or empty when GitLab sent none
     */
    public Optional<String> webUrl() {
        return Optional.ofNullable(response.webUrl());
    }

    /**
     * Returns the files of the snippet, from its {@code files}. A legacy single-file response without {@code files}
     * gives an empty list (its {@code file_name} stays in {@link #response()}); an entry without a path is skipped.
     *
     * @return the files, immutable
     */
    public List<SnippetFile> files() {
        return SnippetValues.files(response.files());
    }

    /**
     * Reads the content of the snippet as text ({@code GET /snippets/{id}/raw}). Binary content comes back decoded as
     * UTF-8. A snippet that is gone is the core's {@code GitLabNotFoundException}, not an empty result.
     *
     * @return the content
     * @throws IOException if the transport fails
     */
    public String content() throws IOException {
        return snippets.gateway().raw(id());
    }

    /**
     * Reads the content of one file of the snippet as text ({@code GET /snippets/{id}/files/{ref}/{file_path}/raw}).
     * The core declares the snippet id of this request as text, so the id is passed as {@code String.valueOf(id())}.
     * The ref and the path are passed to the core unchanged, not URL-encoded: {@code dir/add.rb} stays as it is. Binary
     * content comes back decoded as UTF-8. A missing file or ref is the core's {@code GitLabNotFoundException}, not an
     * empty result.
     *
     * @param ref the branch, tag or commit of the snippet repository, such as {@code main}
     * @param path the path of the file
     * @return the content of the file
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     * @throws IOException if the transport fails
     */
    public String fileContent(final String ref, final String path) throws IOException {
        final String checkedRef = requireNotBlank(ref, "ref");
        return snippets.gateway().fileRaw(id(), checkedRef, requireNotBlank(path, "path"));
    }

    /**
     * Loads the snippet again by its id ({@code GET /snippets/{id}}). This object is not changed.
     *
     * @return the snippet as GitLab holds it now
     * @throws IllegalStateException if the snippet is gone
     * @throws IOException if the transport fails
     */
    public PersonalSnippet refresh() throws IOException {
        final long id = id();
        return snippets.find(id).orElseThrow(() -> new IllegalStateException("Snippet " + id + SnippetValues.IS_GONE));
    }

    /**
     * Starts a change of the snippet. Nothing is sent until {@link PersonalSnippetUpdate#send()}.
     *
     * @return the update builder
     */
    public PersonalSnippetUpdate update() {
        return new PersonalSnippetUpdate(this);
    }

    /**
     * Deletes the snippet ({@code DELETE /snippets/{id}}). The core may return a body; it is discarded.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        snippets.gateway().delete(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabPersonalSnippet response() {
        return response;
    }

    PersonalSnippet apply(final SnippetValues.Changes changes) throws IOException {
        return new PersonalSnippet(snippets, snippets.gateway().update(id(), changes));
    }
}
