package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the file changes of a commit and creates the commit in one request when {@link #send()} is called
 * ({@code POST /projects/{id}/repository/commits}, the changes sent as {@code actions[][...]} form fields in the order
 * they were added). Adding a change makes no request. The core sends the {@code actions[][...]} fields as multipart
 * form fields; GitLab documents JSON and form bodies, so the multipart encoding is covered by unit tests only and has
 * not been verified against a live GitLab.
 */
public final class CommitDraft {

    private final Project project;
    private final String branch;
    private final String message;
    private final List<CommitAction> actions = new ArrayList<>();

    /**
     * Starts a commit. Applications normally call {@link Project#createCommit(String, String)}.
     *
     * @param project the project to commit to
     * @param branch the branch to commit to
     * @param message the commit message
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code branch} or {@code message} is blank
     */
    public CommitDraft(final Project project, final String branch, final String message) {
        this.project = requireNonNull(project, "project");
        this.branch = requireNotBlank(branch, "branch");
        this.message = requireNotBlank(message, "message");
    }

    /**
     * Adds a new file with text content, sent as it is.
     *
     * @param path the path of the file within the repository
     * @param content the content; may be empty
     * @return this draft
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public CommitDraft create(final String path, final String content) {
        actions.add(CommitAction.create(path, content));
        return this;
    }

    /**
     * Adds a new file with binary content, sent Base64-encoded with {@code encoding=base64}.
     *
     * @param path the path of the file within the repository
     * @param content the content; may be empty
     * @return this draft
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public CommitDraft create(final String path, final byte[] content) {
        actions.add(CommitAction.create(path, content));
        return this;
    }

    /**
     * Replaces the content of an existing file with text, sent as it is.
     *
     * @param path the path of the file within the repository
     * @param content the new content; may be empty
     * @return this draft
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public CommitDraft update(final String path, final String content) {
        actions.add(CommitAction.update(path, content));
        return this;
    }

    /**
     * Replaces the content of an existing file with binary content, sent Base64-encoded with {@code encoding=base64}.
     *
     * @param path the path of the file within the repository
     * @param content the new content; may be empty
     * @return this draft
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public CommitDraft update(final String path, final byte[] content) {
        actions.add(CommitAction.update(path, content));
        return this;
    }

    /**
     * Deletes a file.
     *
     * @param path the path of the file within the repository
     * @return this draft
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} is blank
     */
    public CommitDraft delete(final String path) {
        actions.add(CommitAction.delete(path));
        return this;
    }

    /**
     * Moves (renames) a file without changing its content.
     *
     * @param previousPath the current path of the file
     * @param path the new path of the file
     * @return this draft
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is blank
     */
    public CommitDraft move(final String previousPath, final String path) {
        actions.add(CommitAction.move(previousPath, path));
        return this;
    }

    /**
     * Creates the commit ({@code POST /projects/{id}/repository/commits}). The draft is not changed, so each call of
     * {@code send()} creates a new commit from the same actions. The core sends the actions as multipart form fields;
     * that is covered by unit tests only and has not been verified against a live GitLab.
     *
     * @return the new commit, as GitLab returned it for the creation
     * @throws IllegalStateException if no change was added (no request is made), or GitLab answers without the id of
     *     the commit
     * @throws IOException if the transport fails
     */
    public Commit send() throws IOException {
        if (actions.isEmpty()) {
            throw new IllegalStateException("A commit needs at least one action");
        }
        final MultipartFormData.Builder form =
                MultipartFormData.builder().text("branch", branch).text("commit_message", message);
        actions.forEach(each -> each.writeTo(form));
        final GitLabCommitDetail created =
                Internals.gitLab(project).commits().createCommits(project.id(), form.build());
        if (created.id() == null) {
            throw new IllegalStateException("GitLab answered without the id of the new commit");
        }
        return new Commit(project, created);
    }
}
