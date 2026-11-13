package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateRequestsNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A note on a {@link MergeRequest}, as GitLab returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class MergeRequestNote {

    private final MergeRequest mergeRequest;
    private final GitLabNote response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param mergeRequest the merge request the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public MergeRequestNote(final MergeRequest mergeRequest, final GitLabNote response) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the merge request the note belongs to.
     *
     * @return the merge request
     */
    public MergeRequest mergeRequest() {
        return mergeRequest;
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
     * Changes the note text ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}/notes/{note_id}}). This
     * object is not changed.
     *
     * @param body the new note text in Markdown
     * @return the note as GitLab returned it after the change
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public MergeRequestNote update(final String body) throws IOException {
        final GitLabProjectNotesUpdateRequestsNotesPayload payload =
                GitLabProjectNotesUpdateRequestsNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new MergeRequestNote(
                mergeRequest,
                gitLab().notes()
                        .project()
                        .updateRequestsNotes(mergeRequest.project().id(), mergeRequest.iid(), id(), payload));
    }

    /**
     * Deletes the note ({@code DELETE /projects/{id}/merge_requests/{merge_request_iid}/notes/{note_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().notes().project().deleteRequestsNotes(mergeRequest.project().id(), mergeRequest.iid(), id());
    }

    /**
     * Loads the note again ({@code GET /projects/{id}/merge_requests/{merge_request_iid}/notes/{note_id}}). This object
     * is not changed.
     *
     * @return the note as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the note
     * @throws IOException if the transport fails
     */
    public MergeRequestNote refresh() throws IOException {
        return gitLab().notes()
                .project()
                .getRequestsNotes(mergeRequest.project().id(), mergeRequest.iid(), id())
                .map(loaded -> new MergeRequestNote(mergeRequest, loaded))
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
        return Internals.gitLab(mergeRequest.project());
    }
}
