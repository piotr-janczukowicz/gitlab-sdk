package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabDraftNote;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A draft note of a {@link MergeRequest}: a review comment only its author sees until it is published. The object is
 * immutable, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 *
 * <p>After {@link #publish()} the draft note no longer exists as a draft, so {@link #refresh()} then fails with
 * {@link IllegalStateException} and {@link #delete()} with the core's exception for a missing draft note.
 */
public final class DraftNote {

    private final MergeRequest mergeRequest;
    private final GitLabDraftNote response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param mergeRequest the merge request the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public DraftNote(final MergeRequest mergeRequest, final GitLabDraftNote response) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the merge request the draft note belongs to.
     *
     * @return the merge request
     */
    public MergeRequest mergeRequest() {
        return mergeRequest;
    }

    /**
     * Returns the draft note id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the text of the draft note in Markdown.
     *
     * @return the text, or empty when GitLab does not return one
     */
    public Optional<String> note() {
        return Optional.ofNullable(response.note());
    }

    /**
     * Tells whether publishing the draft note resolves the discussion it replies to.
     *
     * @return {@code true} when GitLab says so; {@code false} when it says otherwise or does not return the flag
     */
    public boolean isResolvingDiscussion() {
        return Boolean.TRUE.equals(response.resolveDiscussion());
    }

    /**
     * Starts a change of the draft note. Nothing is sent until {@link DraftNoteUpdate#send()}.
     *
     * @return the update
     */
    public DraftNoteUpdate update() {
        return new DraftNoteUpdate(this);
    }

    /**
     * Publishes the draft note ({@code PUT
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/{draft_note_id}/publish}). It no longer exists as a
     * draft afterwards: {@link #refresh()} then throws {@link IllegalStateException} and {@link #delete()} fails with
     * the core's exception.
     *
     * @throws IOException if the transport fails
     */
    public void publish() throws IOException {
        gitLab().draftnotes().publish(mergeRequest.project().id(), mergeRequest.iid(), id());
    }

    /**
     * Deletes the draft note ({@code DELETE
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/{draft_note_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().draftnotes().delete(mergeRequest.project().id(), mergeRequest.iid(), id());
    }

    /**
     * Loads the draft note again ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/{draft_note_id}}). This object is not changed.
     *
     * @return the draft note as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the draft note, for example after it was published
     * @throws IOException if the transport fails
     */
    public DraftNote refresh() throws IOException {
        return mergeRequest
                .draftNote(id())
                .orElseThrow(() -> new IllegalStateException("Draft note " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabDraftNote response() {
        return response;
    }

    DraftNote apply(final GitLabProjectDraftNotesUpdatePayload payload) throws IOException {
        return new DraftNote(
                mergeRequest,
                gitLab().draftnotes().update(mergeRequest.project().id(), mergeRequest.iid(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(mergeRequest.project());
    }
}
