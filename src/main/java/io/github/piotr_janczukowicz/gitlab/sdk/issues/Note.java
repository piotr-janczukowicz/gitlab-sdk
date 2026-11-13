package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateIssuesNotesAwardEmojiPayload;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateIssuesNotesAwardEmojiPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateIssuesNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateIssuesNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.reactions.Reaction;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A note on an {@link Issue}, as GitLab returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Note {

    private final Issue issue;
    private final GitLabNote response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param issue the issue the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Note(final Issue issue, final GitLabNote response) {
        this.issue = requireNonNull(issue, "issue");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the issue the note belongs to.
     *
     * @return the issue
     */
    public Issue issue() {
        return issue;
    }

    /**
     * Returns the note id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the note text in Markdown.
     *
     * @return the body, or empty when GitLab does not return one
     */
    public Optional<String> body() {
        return Optional.ofNullable(response.body());
    }

    /**
     * Changes the note text ({@code PUT /projects/{id}/issues/{issue_iid}/notes/{note_id}}). This object is not
     * changed.
     *
     * @param body the new note text in Markdown
     * @return the note as GitLab returned it after the change
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public Note update(final String body) throws IOException {
        final GitLabProjectNotesUpdateIssuesNotesPayload payload =
                GitLabProjectNotesUpdateIssuesNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new Note(
                issue,
                gitLab().notes().project().updateIssuesNotes(issue.project().id(), issue.iid(), id(), payload));
    }

    /**
     * Lists the emoji reactions on the note ({@code GET
     * /projects/{id}/issues/{issue_iid}/notes/{note_id}/award_emoji}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        return gitLab()
                .emojireactions()
                .listIssuesNotesAwardEmoji(issue.project().id(), issue.iid(), id())
                .stream()
                .map(reaction -> new Reaction(this, reaction));
    }

    /**
     * Reacts to the note with an emoji ({@code POST /projects/{id}/issues/{issue_iid}/notes/{note_id}/award_emoji}).
     * When you already reacted with the same emoji, GitLab's error passes through.
     *
     * @param name the emoji name without colons, for example {@code thumbsup}
     * @return the created reaction
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Reaction react(final String name) throws IOException {
        final GitLabProjectEmojiReactionsCreateIssuesNotesAwardEmojiPayload payload =
                GitLabProjectEmojiReactionsCreateIssuesNotesAwardEmojiPayloadBuilder.builder()
                        .name(requireNotBlank(name, "name"))
                        .build();
        return new Reaction(
                this,
                gitLab().emojireactions()
                        .createIssuesNotesAwardEmoji(issue.project().id(), issue.iid(), id(), payload));
    }

    /**
     * Deletes the note ({@code DELETE /projects/{id}/issues/{issue_iid}/notes/{note_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().notes().project().deleteIssuesNotes(issue.project().id(), issue.iid(), id());
    }

    /**
     * Loads the note again ({@code GET /projects/{id}/issues/{issue_iid}/notes/{note_id}}). This object is not changed.
     *
     * @return the note as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the note
     * @throws IOException if the transport fails
     */
    public Note refresh() throws IOException {
        return gitLab().notes()
                .project()
                .getIssuesNotes(issue.project().id(), issue.iid(), id())
                .map(loaded -> new Note(issue, loaded))
                .orElseThrow(() -> new IllegalStateException("Note " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabNote response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(issue.project());
    }
}
