package io.github.piotr_janczukowicz.gitlab.sdk.discussions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussion;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsUpdateIssuesDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsUpdateIssuesDiscussionsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesDiscussionsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesDiscussionsNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Note;
import java.io.IOException;
import java.util.List;

/**
 * A discussion (thread) on an {@link Issue}, as GitLab returned it. The object is immutable, and {@link #response()}
 * gives the complete response for every field the accessors here do not cover. Editing or deleting a reply uses the
 * {@link Note} objects of {@link #notes()}.
 */
public final class IssueDiscussion {

    private final Issue issue;
    private final GitLabDiscussion response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param issue the issue the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public IssueDiscussion(final Issue issue, final GitLabDiscussion response) {
        this.issue = requireNonNull(issue, "issue");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the issue the discussion belongs to.
     *
     * @return the issue
     */
    public Issue issue() {
        return issue;
    }

    /**
     * Returns the discussion id.
     *
     * @return the id
     */
    public String id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Tells whether the discussion is a standalone note, not a thread.
     *
     * @return {@code true} for an individual note; {@code false} otherwise or when GitLab does not return the flag
     */
    public boolean isIndividualNote() {
        return Boolean.TRUE.equals(response.individualNote());
    }

    /**
     * Tells whether the discussion can be resolved.
     *
     * @return {@code true} when resolvable; {@code false} otherwise or when GitLab does not return the flag
     */
    public boolean isResolvable() {
        return Boolean.TRUE.equals(response.resolvable());
    }

    /**
     * Tells whether the discussion is resolved.
     *
     * @return {@code true} when resolved; {@code false} otherwise or when GitLab does not return the flag
     */
    public boolean isResolved() {
        return Boolean.TRUE.equals(response.resolved());
    }

    /**
     * Returns the notes of the thread, taken from the response (no request).
     *
     * @return an unmodifiable list of the notes, each belonging to {@link #issue()}
     */
    public List<Note> notes() {
        final List<GitLabNote> notes = response.notes();
        if (notes == null) {
            return List.of();
        }
        return notes.stream().map(note -> new Note(issue, note)).toList();
    }

    /**
     * Adds a reply to the thread ({@code POST /projects/{id}/issues/{issue_iid}/discussions/{discussion_id}/notes}).
     * When the discussion is gone, the core's exception passes through.
     *
     * @param body the reply text in Markdown
     * @return the created note
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public Note reply(final String body) throws IOException {
        final GitLabProjectNotesCreateIssuesDiscussionsNotesPayload payload =
                GitLabProjectNotesCreateIssuesDiscussionsNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new Note(
                issue,
                gitLab().notes()
                        .project()
                        .createIssuesDiscussionsNotes(issue.project().id(), issue.iid(), id(), payload));
    }

    /**
     * Resolves the thread ({@code PUT /projects/{id}/issues/{issue_iid}/discussions/{discussion_id}} with
     * {@code resolved=true}). GitLab refuses a discussion that cannot be resolved; its exception passes through. This
     * object is not changed.
     *
     * @return the discussion as GitLab answered
     * @throws IOException if the transport fails
     */
    public IssueDiscussion resolve() throws IOException {
        return setResolved(true);
    }

    /**
     * Reopens the thread ({@code PUT /projects/{id}/issues/{issue_iid}/discussions/{discussion_id}} with
     * {@code resolved=false}). GitLab refuses a discussion that cannot be resolved; its exception passes through. This
     * object is not changed.
     *
     * @return the discussion as GitLab answered
     * @throws IOException if the transport fails
     */
    public IssueDiscussion unresolve() throws IOException {
        return setResolved(false);
    }

    /**
     * Loads the discussion again ({@code GET /projects/{id}/issues/{issue_iid}/discussions/{discussion_id}}). This
     * object is not changed.
     *
     * @return the discussion as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the discussion
     * @throws IOException if the transport fails
     */
    public IssueDiscussion refresh() throws IOException {
        return issue.discussion(id()).orElseThrow(() -> new IllegalStateException("Discussion " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabDiscussion response() {
        return response;
    }

    private IssueDiscussion setResolved(final boolean resolved) throws IOException {
        final GitLabProjectDiscussionsUpdateIssuesDiscussionsPayload payload =
                GitLabProjectDiscussionsUpdateIssuesDiscussionsPayloadBuilder.builder()
                        .resolved(resolved)
                        .build();
        return new IssueDiscussion(
                issue,
                gitLab().discussions()
                        .project()
                        .updateIssuesDiscussions(issue.project().id(), issue.iid(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(issue.project());
    }
}
