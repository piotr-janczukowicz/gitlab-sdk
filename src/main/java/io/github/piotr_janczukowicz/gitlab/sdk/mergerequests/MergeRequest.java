package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A merge request of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment
 * it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. A
 * merge request is addressed by its {@code iid}, the number within its project.
 */
public final class MergeRequest {

    private final Project project;
    private final GitLabMergeRequest response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public MergeRequest(final Project project, final GitLabMergeRequest response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the merge request belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the merge request number within its project.
     *
     * @return the {@code iid}
     */
    public long iid() {
        return requireNonNull(response.iid(), "iid");
    }

    /**
     * Returns the merge request title.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
    }

    /**
     * Returns the merge request description in Markdown.
     *
     * @return the description, or empty when GitLab does not return one
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the merge request state.
     *
     * @return the state
     * @throws IllegalArgumentException if GitLab returns a state other than {@code opened}, {@code closed},
     *     {@code locked} or {@code merged}
     */
    public MergeRequestState state() {
        return MergeRequestState.from(requireNonNull(response.state(), "state"));
    }

    /**
     * Returns the name of the branch the merge request merges into.
     *
     * @return the target branch, or empty when GitLab does not return one
     */
    public Optional<String> targetBranch() {
        return Optional.ofNullable(response.targetBranch());
    }

    /**
     * Returns the name of the branch the merge request merges from.
     *
     * @return the source branch, or empty when GitLab does not return one
     */
    public Optional<String> sourceBranch() {
        return Optional.ofNullable(response.sourceBranch());
    }

    /**
     * Returns the names of the labels of the merge request.
     *
     * @return the label names, empty when there are none
     */
    public List<String> labels() {
        return Optional.ofNullable(response.labels()).orElse(List.of());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link MergeRequestUpdate#send()}.
     *
     * @return the update builder
     */
    public MergeRequestUpdate update() {
        return new MergeRequestUpdate(this);
    }

    /**
     * Closes the merge request ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}} with
     * {@code state_event=close}). This object is not changed.
     *
     * @return the merge request as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public MergeRequest close() throws IOException {
        return changeState(GitLabProjectMergeRequestsUpdateStateEvent.CLOSE);
    }

    /**
     * Reopens the merge request ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}} with
     * {@code state_event=reopen}). This object is not changed.
     *
     * @return the merge request as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public MergeRequest reopen() throws IOException {
        return changeState(GitLabProjectMergeRequestsUpdateStateEvent.REOPEN);
    }

    /**
     * Merges the merge request with GitLab's default settings ({@code PUT
     * /projects/{id}/merge_requests/{merge_request_iid}/merge}). This object is not changed.
     *
     * @return the merge request as GitLab returned it after the merge
     * @throws IOException if the transport fails, or the core's {@code GitLabApiException} when GitLab refuses, for
     *     example because the merge request is not mergeable; it passes through unchanged
     */
    public MergeRequest merge() throws IOException {
        return new MergeRequest(
                project,
                gitLab().mergerequests()
                        .project()
                        .merge(
                                project.id(),
                                iid(),
                                GitLabProjectMergeRequestsMergePayloadBuilder.builder()
                                        .build()));
    }

    /**
     * Approves the merge request as the authenticated user ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/approve}).
     *
     * @return the approvals as GitLab returned them after the approval
     * @throws IOException if the transport fails
     */
    public Approvals approve() throws IOException {
        return new Approvals(
                this,
                gitLab().mergerequests()
                        .project()
                        .approve(
                                project.id(),
                                iid(),
                                GitLabProjectMergeRequestsApprovePayloadBuilder.builder()
                                        .build()));
    }

    /**
     * Withdraws the approval of the authenticated user ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/unapprove}).
     *
     * @return the approvals as GitLab returned them after the change
     * @throws IOException if the transport fails
     */
    public Approvals unapprove() throws IOException {
        return new Approvals(this, gitLab().mergerequests().project().unapprove(project.id(), iid()));
    }

    /**
     * Loads the approvals of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/approvals}).
     *
     * @return the approvals, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Approvals> approvals() throws IOException {
        return gitLab().mergerequestapprovals()
                .getRequestsApprovals(project.id(), iid())
                .map(loaded -> new Approvals(this, loaded));
    }

    /**
     * Adds a note to the merge request ({@code POST /projects/{id}/merge_requests/{merge_request_iid}/notes}). The note
     * path names the merge request by its {@code iid}.
     *
     * @param body the note text in Markdown
     * @return the created note
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public MergeRequestNote comment(final String body) throws IOException {
        final GitLabProjectNotesCreateRequestsNotesPayload payload =
                GitLabProjectNotesCreateRequestsNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new MergeRequestNote(this, gitLab().notes().project().createRequestsNotes(project.id(), iid(), payload));
    }

    /**
     * Lists the notes of the merge request ({@code GET /projects/{id}/merge_requests/{merge_request_iid}/notes}),
     * system notes included. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the notes
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequestNote> notes() throws IOException {
        return gitLab().notes().project().listRequestsNotes(project.id(), iid()).stream()
                .map(note -> new MergeRequestNote(this, note));
    }

    /**
     * Lists the pipelines of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/pipelines}). GitLab lists a summary of each pipeline, so each
     * one is loaded ({@code GET /projects/{id}/pipelines/{pipeline_id}}) as the stream reaches it, and one that is gone
     * by then is skipped. Transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the pipelines
     * @throws IOException if the list cannot be loaded
     */
    public Stream<Pipeline> pipelines() throws IOException {
        return gitLab().mergerequests().project().listPipelines(project.id(), iid()).stream()
                .map(listed -> Lookups.find(() -> project.pipeline(requireNonNull(listed.id(), "id"))))
                .flatMap(Optional::stream);
    }

    /**
     * Deletes the merge request ({@code DELETE /projects/{id}/merge_requests/{merge_request_iid}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().mergerequests().project().delete(project.id(), iid());
    }

    /**
     * Loads the merge request again ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}). This object is not
     * changed.
     *
     * @return the merge request as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the merge request
     * @throws IOException if the transport fails
     */
    public MergeRequest refresh() throws IOException {
        return project.mergeRequest(iid())
                .orElseThrow(() -> new IllegalStateException("Merge request " + iid() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMergeRequest response() {
        return response;
    }

    MergeRequest apply(final GitLabProjectMergeRequestsUpdatePayload payload) throws IOException {
        return new MergeRequest(project, gitLab().mergerequests().project().update(project.id(), iid(), payload));
    }

    private MergeRequest changeState(final GitLabProjectMergeRequestsUpdateStateEvent event) throws IOException {
        return apply(GitLabProjectMergeRequestsUpdatePayloadBuilder.builder()
                .stateEvent(event)
                .build());
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
