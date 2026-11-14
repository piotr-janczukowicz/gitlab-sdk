package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.io.IOException;

/**
 * Collects changes to a project {@link Snippet} and sends them in one request. Only the fields that were set are sent,
 * and nothing is sent until {@link #send()}. File changes are sent as the {@code files} array, in the order of the
 * calls; the {@code content} and {@code file_name} parameters, which the GitLab REST docs mark deprecated, are never
 * sent. The {@code toString()} is the default one, so it never prints a description or content.
 */
public final class SnippetUpdate {

    private final Snippet snippet;
    private final SnippetValues.Changes changes = new SnippetValues.Changes();

    /**
     * Starts a change of one snippet. Applications normally get it from {@link Snippet#update()}. Nothing is sent until
     * {@code send()}.
     *
     * @param snippet the snippet to change
     * @throws NullPointerException if {@code snippet} is {@code null}
     */
    public SnippetUpdate(final Snippet snippet) {
        this.snippet = requireNonNull(snippet, "snippet");
    }

    /**
     * Sets the title.
     *
     * @param title the title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public SnippetUpdate title(final String title) {
        changes.setTitle(title);
        return this;
    }

    /**
     * Sets the description, sent as given; an empty text is sent as such.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public SnippetUpdate description(final String description) {
        changes.setDescription(description);
        return this;
    }

    /**
     * Sets who can see the snippet.
     *
     * @param visibility the visibility
     * @return this update
     * @throws NullPointerException if {@code visibility} is {@code null}
     */
    public SnippetUpdate visibility(final SnippetVisibility visibility) {
        changes.setVisibility(visibility);
        return this;
    }

    /**
     * Adds a file (the file action {@code create}).
     *
     * @param path the path of the new file, such as {@code dir/add.rb}
     * @param content the content, sent as given
     * @return this update
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public SnippetUpdate addFile(final String path, final String content) {
        changes.addFile(path, content);
        return this;
    }

    /**
     * Replaces the content of a file (the file action {@code update}).
     *
     * @param path the path of the file
     * @param content the new content, sent as given
     * @return this update
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public SnippetUpdate updateFile(final String path, final String content) {
        changes.updateFile(path, content);
        return this;
    }

    /**
     * Deletes a file (the file action {@code delete}).
     *
     * @param path the path of the file
     * @return this update
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public SnippetUpdate deleteFile(final String path) {
        changes.deleteFile(path);
        return this;
    }

    /**
     * Moves a file (the file action {@code move}, with {@code previous_path}); no content is sent, so GitLab is
     * expected to keep the content of the file.
     *
     * @param previousPath the current path of the file
     * @param path the new path
     * @return this update
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     */
    public SnippetUpdate moveFile(final String previousPath, final String path) {
        changes.moveFile(previousPath, path);
        return this;
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/snippets/{snippet_id}}). The snippet this update was made from is
     * not changed.
     *
     * @return the snippet as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Snippet send() throws IOException {
        changes.requireChange();
        return snippet.apply(changes);
    }
}
