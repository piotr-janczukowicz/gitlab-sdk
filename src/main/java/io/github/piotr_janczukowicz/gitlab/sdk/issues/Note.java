package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateIssuesNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateIssuesNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

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
     * Deletes the note ({@code DELETE /projects/{id}/issues/{issue_iid}/notes/{note_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().notes().project().deleteIssuesNotes(issue.project().id(), issue.iid(), id());
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
