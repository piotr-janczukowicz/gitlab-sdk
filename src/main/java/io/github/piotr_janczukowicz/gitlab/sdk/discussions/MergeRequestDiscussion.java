package io.github.piotr_janczukowicz.gitlab.sdk.discussions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussion;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsUpdateRequestsDiscussionsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsDiscussionsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsDiscussionsNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestNote;
import java.io.IOException;
import java.util.List;

/**
 * A discussion (thread) on an {@link MergeRequest}, as GitLab returned it. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover. Editing or deleting
 * a reply uses the {@link MergeRequestNote} objects of {@link #notes()}.
 */
public final class MergeRequestDiscussion {

    private final MergeRequest mergeRequest;
    private final GitLabDiscussion response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param mergeRequest the mergeRequest the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public MergeRequestDiscussion(final MergeRequest mergeRequest, final GitLabDiscussion response) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the mergeRequest the discussion belongs to.
     *
     * @return the mergeRequest
     */
    public MergeRequest mergeRequest() {
        return mergeRequest;
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
     * Tells whether the discussion is a single note that cannot be replied to as a thread.
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
     * @return an unmodifiable list of the notes, each belonging to {@link #mergeRequest()}
     */
    public List<MergeRequestNote> notes() {
        final List<GitLabNote> notes = response.notes();
        if (notes == null) {
            return List.of();
        }
        return notes.stream()
                .map(note -> new MergeRequestNote(mergeRequest, note))
                .toList();
    }

    /**
     * Adds a reply to the thread ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/discussions/{discussion_id}/notes}). When the discussion is
     * gone, the core's exception passes through.
     *
     * @param body the reply text in Markdown
     * @return the created note
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public MergeRequestNote reply(final String body) throws IOException {
        final GitLabProjectNotesCreateRequestsDiscussionsNotesPayload payload =
                GitLabProjectNotesCreateRequestsDiscussionsNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new MergeRequestNote(
                mergeRequest,
                gitLab().notes()
                        .project()
                        .createRequestsDiscussionsNotes(
                                mergeRequest.project().id(), mergeRequest.iid(), id(), payload));
    }

    /**
     * Resolves the thread ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}/discussions/{discussion_id}}
     * with {@code resolved=true}). GitLab refuses a discussion that cannot be resolved; its exception passes through.
     * This object is not changed.
     *
     * @return the discussion as GitLab answered
     * @throws IOException if the transport fails
     */
    public MergeRequestDiscussion resolve() throws IOException {
        return setResolved(true);
    }

    /**
     * Reopens the thread ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}/discussions/{discussion_id}}
     * with {@code resolved=false}). GitLab refuses a discussion that cannot be resolved; its exception passes through.
     * This object is not changed.
     *
     * @return the discussion as GitLab answered
     * @throws IOException if the transport fails
     */
    public MergeRequestDiscussion unresolve() throws IOException {
        return setResolved(false);
    }

    /**
     * Loads the discussion again ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/discussions/{discussion_id}}). This object is not changed.
     *
     * @return the discussion as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the discussion
     * @throws IOException if the transport fails
     */
    public MergeRequestDiscussion refresh() throws IOException {
        return mergeRequest
                .discussion(id())
                .orElseThrow(() -> new IllegalStateException("Discussion " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabDiscussion response() {
        return response;
    }

    private MergeRequestDiscussion setResolved(final boolean resolved) throws IOException {
        final GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload payload =
                GitLabProjectDiscussionsUpdateRequestsDiscussionsPayloadBuilder.builder()
                        .resolved(resolved)
                        .build();
        return new MergeRequestDiscussion(
                mergeRequest,
                gitLab().discussions()
                        .project()
                        .updateRequestsDiscussions(mergeRequest.project().id(), mergeRequest.iid(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(mergeRequest.project());
    }
}
