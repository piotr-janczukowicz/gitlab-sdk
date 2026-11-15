package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipeline;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesBulkPublishPayload;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesBulkPublishPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsAddSpentTimePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsAddSpentTimePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePipelinePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListDiffsFilter;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsRebasePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsRebasePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsTimeEstimatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsTimeEstimatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.ApprovalState;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.MergeRequestApprovalRule;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.discussions.MergeRequestDiscussion;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.TimeStats;
import io.github.piotr_janczukowicz.gitlab.sdk.reactions.Reaction;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A merge request of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment
 * it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. A
 * merge request is addressed by its {@code iid}, the number within its project.
 */
public final class MergeRequest {

    private static final String PAYLOAD = "payload";
    private static final String OPTIONS = "options";

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
     *     {@code locked} or {@code merged}; {@link #stateName()} still returns it
     */
    public MergeRequestState state() {
        return MergeRequestState.from(stateName());
    }

    /**
     * Returns the state exactly as GitLab spelled it, such as {@code opened}. Use it when {@link #state()} throws
     * {@link IllegalArgumentException} because GitLab returned a state this SDK does not know yet.
     *
     * @return the raw state name
     */
    public String stateName() {
        return requireNonNull(response.state(), "state");
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
                gitLab().mergerequests().project().merge(project.id(), iid(), requireNonNull(options, OPTIONS)));
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
                gitLab().mergerequests().project().approve(project.id(), iid(), requireNonNull(options, OPTIONS)));
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
                        .createRequestsDiscussions(project.id(), iid(), requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists your draft notes on the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes}). GitLab answers with one list in a single
     * request, so the stream is made over that list and fetches nothing more.
     *
     * @return the draft notes
     * @throws IOException if the transport fails
     */
    public Stream<DraftNote> draftNotes() throws IOException {
        return gitLab().draftnotes().list(project.id(), iid()).stream().map(draft -> new DraftNote(this, draft));
    }

    /**
     * Looks up a draft note of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/{draft_note_id}}).
     *
     * @param id the draft note id
     * @return the draft note, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<DraftNote> draftNote(final long id) throws IOException {
        return gitLab().draftnotes().get(project.id(), iid(), id).map(draft -> new DraftNote(this, draft));
    }

    /**
     * Creates a draft note on the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes}). Calling it with a literal {@code null} needs a
     * cast, because {@link #createDraftNote(GitLabProjectDraftNotesCreatePayload)} is an overload.
     *
     * @param note the text in Markdown
     * @return the created draft note
     * @throws NullPointerException if {@code note} is {@code null}
     * @throws IllegalArgumentException if {@code note} is blank
     * @throws IOException if the transport fails
     */
    public DraftNote createDraftNote(final String note) throws IOException {
        return createDraftNote(GitLabProjectDraftNotesCreatePayloadBuilder.builder()
                .note(requireNotBlank(note, "note"))
                .build());
    }

    /**
     * Creates a draft note from the core's payload, which also carries the discussion to reply to, the commit, whether
     * to resolve the discussion and a {@code position} ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes}). The SDK adds no wrapper for the position.
     *
     * @param payload the core payload
     * @return the created draft note
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public DraftNote createDraftNote(final GitLabProjectDraftNotesCreatePayload payload) throws IOException {
        return new DraftNote(this, gitLab().draftnotes().create(project.id(), iid(), requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Publishes all your draft notes on the merge request with GitLab's defaults: an empty payload ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/bulk_publish}). The draft notes no longer exist as
     * drafts afterwards.
     *
     * @throws IOException if the transport fails
     */
    public void publishDraftNotes() throws IOException {
        publishDraftNotes(
                GitLabProjectDraftNotesBulkPublishPayloadBuilder.builder().build());
    }

    /**
     * Publishes all your draft notes on the merge request from the core's payload, which can carry a reviewer state, a
     * summary note and whether it is internal ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/draft_notes/bulk_publish}). This method is an overload of
     * {@link #publishDraftNotes()}, so a literal {@code null} needs a cast.
     *
     * @param payload the core payload
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public void publishDraftNotes(final GitLabProjectDraftNotesBulkPublishPayload payload) throws IOException {
        gitLab().draftnotes().bulkPublish(project.id(), iid(), requireNonNull(payload, PAYLOAD));
    }

    /**
     * Lists the emoji reactions on the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/award_emoji}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        return gitLab().emojireactions().listRequestsAwardEmoji(project.id(), iid()).stream()
                .map(reaction -> new Reaction(this, reaction));
    }

    /**
     * Reacts to the merge request with an emoji ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/award_emoji}). When you already reacted with the same emoji,
     * GitLab's error passes through.
     *
     * @param name the emoji name without colons, for example {@code thumbsup}
     * @return the created reaction
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Reaction react(final String name) throws IOException {
        final GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload payload =
                GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayloadBuilder.builder()
                        .name(requireNotBlank(name, "name"))
                        .build();
        return new Reaction(this, gitLab().emojireactions().createRequestsAwardEmoji(project.id(), iid(), payload));
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
                .map(listed ->
                        Lookups.find(() -> projectOf(listed.projectId()).pipeline(requireNonNull(listed.id(), "id"))))
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
     * Tells whether GitLab is rebasing the merge request. A rebase runs in the background, so this reads the moment the
     * merge request was loaded: call {@link #refresh()} after {@link #rebase()} to see it.
     *
     * @return {@code true} when GitLab says a rebase is in progress; {@code false} when it says otherwise or does not
     *     return the flag
     */
    public boolean isRebaseInProgress() {
        return Boolean.TRUE.equals(response.rebaseInProgress());
    }

    /**
     * Cancels merging the merge request when its pipeline succeeds ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/cancel_merge_when_pipeline_succeeds}). This object is not
     * changed.
     *
     * @return the merge request as GitLab returned it after the change
     * @throws IOException if the transport fails, or the core's {@code GitLabApiException} when GitLab refuses, for
     *     example because no automatic merge is set; it passes through unchanged
     */
    public MergeRequest cancelAutoMerge() throws IOException {
        return new MergeRequest(project, gitLab().mergerequests().project().cancelAutoMerge(project.id(), iid()));
    }

    /**
     * Starts rebasing the merge request onto its target branch ({@code PUT
     * /projects/{id}/merge_requests/{merge_request_iid}/rebase}). The rebase is asynchronous: this method returns as
     * soon as GitLab accepted the request, before the rebase has finished. Use {@link #refresh()} and
     * {@link #isRebaseInProgress()} to follow it; a failed rebase shows in the {@code merge_error} field of
     * {@link #response()}.
     *
     * @throws IOException if the transport fails, or the core's {@code GitLabApiException} when GitLab refuses; it
     *     passes through unchanged
     */
    public void rebase() throws IOException {
        rebase(GitLabProjectMergeRequestsRebasePayloadBuilder.builder().build());
    }

    /**
     * Starts rebasing the merge request with the given options ({@code PUT
     * /projects/{id}/merge_requests/{merge_request_iid}/rebase}), for example to skip the CI pipeline. The rebase is
     * asynchronous, as for {@link #rebase()}.
     *
     * @param options the rebase options of the core client; a field left unset is not sent
     * @throws NullPointerException if {@code options} is {@code null}
     * @throws IOException if the transport fails, or the core's {@code GitLabApiException} when GitLab refuses; it
     *     passes through unchanged
     */
    public void rebase(final GitLabProjectMergeRequestsRebasePayload options) throws IOException {
        gitLab().mergerequests().project().rebase(project.id(), iid(), requireNonNull(options, OPTIONS));
    }

    /**
     * Creates a pipeline for the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/pipelines}) and loads it ({@code GET
     * /projects/{id}/pipelines/{pipeline_id}}), because GitLab answers the creation with a smaller record. The pipeline
     * is loaded from the project GitLab names for it, as for {@link #pipelines()}.
     *
     * @return the created pipeline
     * @throws NullPointerException if GitLab's answer has no pipeline id
     * @throws IllegalStateException if GitLab no longer has the pipeline after creating it
     * @throws IOException if the transport fails
     */
    public Pipeline createPipeline() throws IOException {
        final GitLabCiPipeline created = gitLab().mergerequests()
                .project()
                .createPipeline(
                        project.id(),
                        iid(),
                        GitLabProjectMergeRequestsCreatePipelinePayloadBuilder.builder()
                                .build());
        final long pipelineId = requireNonNull(created.id(), "id");
        return projectOf(created.projectId())
                .pipeline(pipelineId)
                .orElseThrow(() -> new IllegalStateException("Pipeline " + pipelineId + " is gone"));
    }

    /**
     * Lists the changed files of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/diffs}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the diffs as the core describes them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabDiff> diffs() throws IOException {
        return gitLab().mergerequests().project().listDiffs(project.id(), iid()).stream();
    }

    /**
     * Lists the changed files of the merge request that match a filter ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/diffs}), for example in the unified diff format. Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param filter the query parameters of the core
     * @return the diffs as the core describes them
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabDiff> diffs(final GitLabProjectMergeRequestsListDiffsFilter filter) throws IOException {
        return gitLab()
                .mergerequests()
                .project()
                .listDiffs(project.id(), iid(), requireNonNull(filter, "filter"))
                .stream();
    }

    /**
     * Lists the versions of the diff of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/versions}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the versions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<DiffVersion> versions() throws IOException {
        return gitLab().mergerequests().project().listVersions(project.id(), iid()).stream()
                .map(DiffVersion::new);
    }

    /**
     * Loads one version of the diff with its commits and changed files ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/versions/{version_id}}).
     *
     * @param versionId the id of the version, from {@link DiffVersion#id()}
     * @return the version, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<MergeRequestDiff> version(final long versionId) throws IOException {
        return gitLab().mergerequests()
                .project()
                .getVersion(project.id(), iid(), versionId)
                .map(MergeRequestDiff::new);
    }

    /**
     * Lists the users taking part in the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/participants}). GitLab answers in one piece, so the stream only
     * walks the list.
     *
     * @return the participants as the core describes them
     * @throws IOException if the transport fails
     */
    public Stream<GitLabUserBasic> participants() throws IOException {
        return gitLab().mergerequests().project().listParticipants(project.id(), iid()).stream();
    }

    /**
     * Lists the issues the merge request closes when it merges ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/closes_issues}). GitLab lists a summary of each issue, so each
     * one is loaded ({@code GET /projects/{id}/issues/{issue_iid}}) as the stream reaches it, and one that is gone by
     * then is skipped. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> closesIssues() throws IOException {
        return gitLab().mergerequests().project().listClosesIssues(project.id(), iid()).stream()
                .map(listed ->
                        Lookups.find(() -> projectOf(listed.projectId()).issue(requireNonNull(listed.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Loads the approval state of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/approval_state}). The approval state is a Premium feature: on a
     * lower tier the core's {@code GitLabTierException} passes through.
     *
     * @return the approval state, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<ApprovalState> approvalState() throws IOException {
        return gitLab().mergerequests()
                .project()
                .getApprovalState(project.id(), iid())
                .map(loaded -> new ApprovalState(this, loaded));
    }

    /**
     * Lists the approval rules of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/approval_rules}). Approval rules are a Premium feature: on a
     * lower tier the core's {@code GitLabTierException} passes through. Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the rules
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequestApprovalRule> approvalRules() throws IOException {
        return gitLab().mergerequestapprovals().listMergeRequestsApprovalRules(project.id(), iid()).stream()
                .map(rule -> new MergeRequestApprovalRule(this, rule));
    }

    /**
     * Looks up an approval rule of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/approval_rules/{rule_id}}). Approval rules are a Premium
     * feature: on a lower tier the core's {@code GitLabTierException} passes through.
     *
     * @param id the rule id
     * @return the rule, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<MergeRequestApprovalRule> approvalRule(final long id) throws IOException {
        return gitLab().mergerequestapprovals()
                .getMergeRequestsApprovalRule(project.id(), iid(), id)
                .map(rule -> new MergeRequestApprovalRule(this, rule));
    }

    /**
     * Creates an approval rule on the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/approval_rules}). The rule has no approvers yet; use
     * {@link MergeRequestApprovalRule#update()} or {@link #createApprovalRule(
     * GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload)} to name them. Approval rules are a
     * Premium feature: on a lower tier the core's {@code GitLabTierException} passes through.
     *
     * @param name the rule name
     * @param approvalsRequired the number of approvals the rule needs
     * @return the created rule
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank or {@code approvalsRequired} is negative
     * @throws IOException if the transport fails
     */
    public MergeRequestApprovalRule createApprovalRule(final String name, final int approvalsRequired)
            throws IOException {
        if (approvalsRequired < 0) {
            throw new IllegalArgumentException("approvalsRequired must not be negative");
        }
        return createApprovalRule(
                GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayloadBuilder.builder()
                        .name(requireNotBlank(name, "name"))
                        .approvalsRequired(approvalsRequired)
                        .build());
    }

    /**
     * Creates an approval rule from the core's payload, which also carries the approving users and groups ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/approval_rules}). Approval rules are a Premium feature: on a
     * lower tier the core's {@code GitLabTierException} passes through.
     *
     * @param payload the core payload
     * @return the created rule
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public MergeRequestApprovalRule createApprovalRule(
            final GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload payload) throws IOException {
        return new MergeRequestApprovalRule(
                this,
                gitLab().mergerequestapprovals()
                        .createMergeRequestsApprovalRule(project.id(), iid(), requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Loads the time tracking of the merge request ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}/time_stats}).
     *
     * @return the time statistics, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<TimeStats> timeStats() throws IOException {
        return gitLab().mergerequests()
                .project()
                .getTimeStats(project.id(), iid())
                .map(TimeStats::new);
    }

    /**
     * Sets the time estimate of the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/time_estimate}). Whole seconds are sent; this object is not
     * changed.
     *
     * @param estimate the estimate
     * @return the time statistics as GitLab returned them after the change
     * @throws NullPointerException if {@code estimate} is {@code null}
     * @throws IllegalArgumentException if {@code estimate} is negative; nothing is sent
     * @throws IOException if the transport fails
     */
    public TimeStats estimateTime(final Duration estimate) throws IOException {
        final GitLabProjectMergeRequestsTimeEstimatePayload payload =
                GitLabProjectMergeRequestsTimeEstimatePayloadBuilder.builder()
                        .duration(Arguments.seconds(estimate, "estimate"))
                        .build();
        return new TimeStats(gitLab().mergerequests().project().timeEstimate(project.id(), iid(), payload));
    }

    /**
     * Removes the time estimate of the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/reset_time_estimate}).
     *
     * @return the time statistics as GitLab returned them after the change
     * @throws IOException if the transport fails
     */
    public TimeStats resetEstimate() throws IOException {
        return new TimeStats(gitLab().mergerequests().project().resetTimeEstimate(project.id(), iid()));
    }

    /**
     * Adds spent time to the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/add_spent_time}). Whole seconds are sent; this object is not
     * changed.
     *
     * @param spent the time spent
     * @return the time statistics as GitLab returned them after the change
     * @throws NullPointerException if {@code spent} is {@code null}
     * @throws IllegalArgumentException if {@code spent} is negative; nothing is sent
     * @throws IOException if the transport fails
     */
    public TimeStats addSpentTime(final Duration spent) throws IOException {
        final GitLabProjectMergeRequestsAddSpentTimePayload payload =
                GitLabProjectMergeRequestsAddSpentTimePayloadBuilder.builder()
                        .duration(Arguments.seconds(spent, "spent"))
                        .build();
        return new TimeStats(gitLab().mergerequests().project().addSpentTime(project.id(), iid(), payload));
    }

    /**
     * Removes the spent time of the merge request ({@code POST
     * /projects/{id}/merge_requests/{merge_request_iid}/reset_spent_time}).
     *
     * @return the time statistics as GitLab returned them after the change
     * @throws IOException if the transport fails
     */
    public TimeStats resetSpentTime() throws IOException {
        return new TimeStats(gitLab().mergerequests().project().resetSpentTime(project.id(), iid()));
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

    private Project projectOf(final @Nullable Long ranIn) {
        if (ranIn == null
                || ranIn.equals(response.projectId())
                || ranIn.equals(response.targetProjectId())
                || project.id().equals(String.valueOf(ranIn))) {
            return project;
        }
        return Internals.sdk(project).project(String.valueOf(ranIn));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
