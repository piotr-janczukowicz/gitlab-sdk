package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link DraftNote} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class DraftNoteUpdate {

    private final DraftNote draftNote;
    private final GitLabProjectDraftNotesUpdatePayloadBuilder payload =
            GitLabProjectDraftNotesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param draftNote the draft note to change
     * @throws NullPointerException if {@code draftNote} is {@code null}
     */
    public DraftNoteUpdate(final DraftNote draftNote) {
        this.draftNote = requireNonNull(draftNote, "draftNote");
    }

    /**
     * Sets the text.
     *
     * @param note the new text in Markdown
     * @return this update
     * @throws NullPointerException if {@code note} is {@code null}
     * @throws IllegalArgumentException if {@code note} is blank
     */
    public DraftNoteUpdate note(final String note) {
        payload.note(requireNotBlank(note, "note"));
        state.mark();
        return this;
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/{draft_note_id}}).
     * The draft note this update was made from is not changed.
     *
     * @return the draft note as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public DraftNote send() throws IOException {
        state.requireChange();
        return draftNote.apply(payload.build());
    }
}
