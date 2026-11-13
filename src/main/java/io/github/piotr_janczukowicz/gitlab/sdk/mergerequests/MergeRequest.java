package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.discussions.MergeRequestDiscussion;
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
     * @return the label names as an unmodifiable list, empty when there are none
     */
    public List<String> labels() {
        final List<String> labels = response.labels();
        return labels == null ? List.of() : List.copyOf(labels);
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
        return merge(GitLabProjectMergeRequestsMergePayloadBuilder.builder().build());
    }

    /**
     * Merges the merge request with the given options ({@code PUT
     * /projects/{id}/merge_requests/{merge_request_iid}/merge}), for example squashing, the commit messages, removing
     * the source branch or merging when the pipeline succeeds. This object is not changed.
     *
     * @param options the merge options of the core client; a field left unset is not sent
     * @return the merge request as GitLab returned it after the merge
     * @throws NullPointerException if {@code options} is {@code null}
     * @throws IOException if the transport fails, or the core's {@code GitLabApiException} when GitLab refuses, for
     *     example because the merge request is not mergeable; it passes through unchanged
     */
    public MergeRequest merge(final GitLabProjectMergeRequestsMergePayload options) throws IOException {
        return new MergeRequest(
                project,
                gitLab().mergerequests().project().merge(project.id(), iid(), requireNonNull(options, "options")));
    }

    /**
     * Approves the merge request as the authenticated user ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/approve}).
     *
     * @return the approvals as GitLab returned them after the approval
     * @throws IOException if the transport fails
     */
    public Approvals approve() throws IOException {
        return approve(GitLabProjectMergeRequestsApprovePayloadBuilder.builder().build());
    }

    /**
     * Approves the merge request as the authenticated user with the given options ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/approve}), for example the {@code sha} the approval is for or
     * whether to publish a pending review.
     *
     * @param options the approval options of the core client; a field left unset is not sent
     * @return the approvals as GitLab returned them after the approval
     * @throws NullPointerException if {@code options} is {@code null}
     * @throws IOException if the transport fails
     */
    public Approvals approve(final GitLabProjectMergeRequestsApprovePayload options) throws IOException {
        return new Approvals(
                this,
                gitLab().mergerequests().project().approve(project.id(), iid(), requireNonNull(options, "options")));
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
     * Lists the discussions (threads) of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/discussions}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the discussions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequestDiscussion> discussions() throws IOException {
        return gitLab().discussions().project().listRequestsDiscussions(project.id(), iid()).stream()
                .map(discussion -> new MergeRequestDiscussion(this, discussion));
    }

    /**
     * Looks up a discussion of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/discussions/{discussion_id}}).
     *
     * @param id the discussion id
     * @return the discussion, or empty when GitLab answers 404
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     * @throws IOException if the transport fails
     */
    public Optional<MergeRequestDiscussion> discussion(final String id) throws IOException {
        return gitLab().discussions()
                .project()
                .getRequestsDiscussions(project.id(), iid(), requireNotBlank(id, "id"))
                .map(discussion -> new MergeRequestDiscussion(this, discussion));
    }

    /**
     * Starts a discussion on the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/discussions}).
     *
     * @param body the text of the first note in Markdown
     * @return the created discussion
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public MergeRequestDiscussion startDiscussion(final String body) throws IOException {
        return startDiscussion(GitLabProjectDiscussionsCreateRequestsDiscussionsPayloadBuilder.builder()
                .body(requireNotBlank(body, "body"))
                .build());
    }

    /**
     * Starts a discussion on the merge request from the core's payload, which carries the {@code position} of a diff
     * comment ({@code POST /projects/{id}/merge_requests/{merge_request_iid}/discussions}). The SDK adds no wrapper for
     * the position.
     *
     * @param payload the core payload with body, optional creation time and optional position
     * @return the created discussion
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public MergeRequestDiscussion startDiscussion(
            final GitLabProjectDiscussionsCreateRequestsDiscussionsPayload payload) throws IOException {
        return new MergeRequestDiscussion(
                this,
                gitLab().discussions()
                        .project()
                        .createRequestsDiscussions(project.id(), iid(), requireNonNull(payload, "payload")));
    }

    /**
     * Lists the pipelines of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/pipelines}). GitLab lists a summary of each pipeline, so each
     * one is loaded ({@code GET /projects/{id}/pipelines/{pipeline_id}}) as the stream reaches it, and one that is gone
     * by then is skipped. Each pipeline is loaded from the project it ran in, which for a merge request from a fork can
     * be the fork, so {@link Pipeline#project()} is a handle to that project; when GitLab does not name the project, or
     * names the target project, it is {@link #project()}. Transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the pipelines
     * @throws IOException if the list cannot be loaded
     */
    public Stream<Pipeline> pipelines() throws IOException {
        return gitLab().mergerequests().project().listPipelines(project.id(), iid()).stream()
                .map(listed -> Lookups.find(() -> projectOf(listed).pipeline(requireNonNull(listed.id(), "id"))))
                .flatMap(Optional::stream);
    }

    /**
     * Lists the commits of the merge request ({@code GET /projects/{id}/merge_requests/{merge_request_iid}/commits}).
     * GitLab lists a summary of each commit, so each one is loaded ({@code GET
     * /projects/{id}/repository/commits/{sha}}) as the stream reaches it, and one that is gone by then is skipped.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the commits
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits() throws IOException {
        return gitLab().mergerequests().project().listCommits(project.id(), iid()).stream()
                .map(listed -> Lookups.find(() -> project.commit(requireNonNull(listed.id(), "id"))))
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

    private Project projectOf(final GitLabCiPipelineBasic listed) {
        final Long ranIn = listed.projectId();
        if (ranIn == null
                || ranIn.equals(response.projectId())
                || ranIn.equals(response.targetProjectId())
                || project.id().equals(String.valueOf(ranIn))) {
            return project;
        }
        return Internals.project(project, String.valueOf(ranIn));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
