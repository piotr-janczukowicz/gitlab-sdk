package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateIssuesDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateIssuesDiscussionsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateIssuesAwardEmojiPayload;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateIssuesAwardEmojiPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLink;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabProjectIssueLinksCreateLinkType;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabProjectIssueLinksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabProjectIssueLinksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabRelatedIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesAddSpentTimePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesAddSpentTimePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesMovePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesMovePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesTimeEstimatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesTimeEstimatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.discussions.IssueDiscussion;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.reactions.Reaction;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * An issue of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. An issue
 * is addressed by its {@code iid}, the number within its project.
 */
public final class Issue {

    private final Project project;
    private final GitLabIssue response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Issue(final Project project, final GitLabIssue response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the issue belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the issue number within its project.
     *
     * @return the {@code iid}
     */
    public long iid() {
        return requireNonNull(response.iid(), "iid");
    }

    /**
     * Returns the issue title.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
    }

    /**
     * Returns the issue description in Markdown.
     *
     * @return the description, or empty when GitLab does not return one
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the issue state.
     *
     * @return the state
     * @throws IllegalArgumentException if GitLab returns a state other than {@code opened} or {@code closed};
     *     {@link #stateName()} still returns it
     */
    public IssueState state() {
        return IssueState.from(stateName());
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
     * Returns the names of the labels of the issue.
     *
     * @return the label names as an unmodifiable list, empty when there are none
     */
    public List<String> labels() {
        final List<String> labels = response.labels();
        return labels == null ? List.of() : List.copyOf(labels);
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link IssueUpdate#send()}.
     *
     * @return the update builder
     */
    public IssueUpdate update() {
        return new IssueUpdate(this);
    }

    /**
     * Closes the issue ({@code PUT /projects/{id}/issues/{issue_iid}} with {@code state_event=close}). This object is
     * not changed.
     *
     * @return the issue as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Issue close() throws IOException {
        return changeState(GitLabProjectIssuesUpdateStateEvent.CLOSE);
    }

    /**
     * Reopens the issue ({@code PUT /projects/{id}/issues/{issue_iid}} with {@code state_event=reopen}). This object is
     * not changed.
     *
     * @return the issue as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Issue reopen() throws IOException {
        return changeState(GitLabProjectIssuesUpdateStateEvent.REOPEN);
    }

    /**
     * Adds a note to the issue ({@code POST /projects/{id}/issues/{issue_iid}/notes}). The note path names the issue by
     * its {@code iid}, not by its global id.
     *
     * @param body the note text in Markdown
     * @return the created note
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public Note comment(final String body) throws IOException {
        final GitLabProjectNotesCreateIssuesNotesPayload payload =
                GitLabProjectNotesCreateIssuesNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new Note(this, gitLab().notes().project().createIssuesNotes(project.id(), iid(), payload));
    }

    /**
     * Lists the notes of the issue ({@code GET /projects/{id}/issues/{issue_iid}/notes}), system notes included. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the notes
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Note> notes() throws IOException {
        return gitLab().notes().project().listIssuesNotes(project.id(), iid()).stream()
                .map(note -> new Note(this, note));
    }

    /**
     * Lists the discussions (threads) of the issue ({@code GET /projects/{id}/issues/{issue_iid}/discussions}). Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the discussions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<IssueDiscussion> discussions() throws IOException {
        return gitLab().discussions().project().listIssuesDiscussions(project.id(), iid()).stream()
                .map(discussion -> new IssueDiscussion(this, discussion));
    }

    /**
     * Looks up a discussion of the issue ({@code GET /projects/{id}/issues/{issue_iid}/discussions/{discussion_id}}).
     *
     * @param id the discussion id
     * @return the discussion, or empty when GitLab answers 404
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     * @throws IOException if the transport fails
     */
    public Optional<IssueDiscussion> discussion(final String id) throws IOException {
        return gitLab().discussions()
                .project()
                .getIssuesDiscussions(project.id(), iid(), requireNotBlank(id, "id"))
                .map(discussion -> new IssueDiscussion(this, discussion));
    }

    /**
     * Starts a discussion on the issue ({@code POST /projects/{id}/issues/{issue_iid}/discussions}).
     *
     * @param body the text of the first note in Markdown
     * @return the created discussion
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public IssueDiscussion startDiscussion(final String body) throws IOException {
        final GitLabProjectDiscussionsCreateIssuesDiscussionsPayload payload =
                GitLabProjectDiscussionsCreateIssuesDiscussionsPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new IssueDiscussion(
                this, gitLab().discussions().project().createIssuesDiscussions(project.id(), iid(), payload));
    }

    /**
     * Lists the emoji reactions on the issue ({@code GET /projects/{id}/issues/{issue_iid}/award_emoji}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the reactions
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Reaction> reactions() throws IOException {
        return gitLab().emojireactions().listIssuesAwardEmoji(project.id(), iid()).stream()
                .map(reaction -> new Reaction(this, reaction));
    }

    /**
     * Reacts to the issue with an emoji ({@code POST /projects/{id}/issues/{issue_iid}/award_emoji}). When you already
     * reacted with the same emoji, GitLab's error passes through.
     *
     * @param name the emoji name without colons, for example {@code thumbsup}
     * @return the created reaction
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the transport fails
     */
    public Reaction react(final String name) throws IOException {
        final GitLabProjectEmojiReactionsCreateIssuesAwardEmojiPayload payload =
                GitLabProjectEmojiReactionsCreateIssuesAwardEmojiPayloadBuilder.builder()
                        .name(requireNotBlank(name, "name"))
                        .build();
        return new Reaction(this, gitLab().emojireactions().createIssuesAwardEmoji(project.id(), iid(), payload));
    }

    /**
     * Loads the time tracking of the issue ({@code GET /projects/{id}/issues/{issue_iid}/time_stats}).
     *
     * @return the time statistics, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<TimeStats> timeStats() throws IOException {
        return gitLab().issues().project().getTimeStats(project.id(), iid()).map(TimeStats::new);
    }

    /**
     * Sets the time estimate of the issue ({@code POST /projects/{id}/issues/{issue_iid}/time_estimate}). Whole seconds
     * are sent; this object is not changed.
     *
     * @param estimate the estimate
     * @return the time statistics as GitLab returned them after the change
     * @throws NullPointerException if {@code estimate} is {@code null}
     * @throws IllegalArgumentException if {@code estimate} is negative; nothing is sent
     * @throws IOException if the transport fails
     */
    public TimeStats estimateTime(final Duration estimate) throws IOException {
        final GitLabProjectIssuesTimeEstimatePayload payload = GitLabProjectIssuesTimeEstimatePayloadBuilder.builder()
                .duration(Arguments.seconds(estimate, "estimate"))
                .build();
        return new TimeStats(gitLab().issues().project().timeEstimate(project.id(), iid(), payload));
    }

    /**
     * Removes the time estimate of the issue ({@code POST /projects/{id}/issues/{issue_iid}/reset_time_estimate}).
     *
     * @return the time statistics as GitLab returned them after the change
     * @throws IOException if the transport fails
     */
    public TimeStats resetEstimate() throws IOException {
        return new TimeStats(gitLab().issues().project().resetTimeEstimate(project.id(), iid()));
    }

    /**
     * Adds spent time to the issue ({@code POST /projects/{id}/issues/{issue_iid}/add_spent_time}). Whole seconds are
     * sent; this object is not changed.
     *
     * @param spent the time spent
     * @return the time statistics as GitLab returned them after the change
     * @throws NullPointerException if {@code spent} is {@code null}
     * @throws IllegalArgumentException if {@code spent} is negative; nothing is sent
     * @throws IOException if the transport fails
     */
    public TimeStats addSpentTime(final Duration spent) throws IOException {
        final GitLabProjectIssuesAddSpentTimePayload payload = GitLabProjectIssuesAddSpentTimePayloadBuilder.builder()
                .duration(Arguments.seconds(spent, "spent"))
                .build();
        return new TimeStats(gitLab().issues().project().addSpentTime(project.id(), iid(), payload));
    }

    /**
     * Removes the spent time of the issue ({@code POST /projects/{id}/issues/{issue_iid}/reset_spent_time}).
     *
     * @return the time statistics as GitLab returned them after the change
     * @throws IOException if the transport fails
     */
    public TimeStats resetSpentTime() throws IOException {
        return new TimeStats(gitLab().issues().project().resetSpentTime(project.id(), iid()));
    }

    /**
     * Lists the links of the issue to other issues ({@code GET /projects/{id}/issues/{issue_iid}/links}). GitLab lists
     * a summary of each linked issue, so each link is loaded ({@code GET /projects/{id}/issues/{issue_iid}/links/{id}})
     * as the stream reaches it, and one that is gone by then is skipped. Transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the links
     * @throws IOException if the list cannot be loaded
     */
    public Stream<IssueLink> links() throws IOException {
        return gitLab().issuelinks().list(project.id(), iid()).stream()
                .map(listed -> Lookups.find(() -> link(requireNonNull(listed.issueLinkId(), "issueLinkId"))))
                .flatMap(Optional::stream);
    }

    /**
     * Looks up a link of the issue ({@code GET /projects/{id}/issues/{issue_iid}/links/{issue_link_id}}).
     *
     * @param linkId the id of the link
     * @return the link, or empty when GitLab answers 404
     * @throws IOException if the transport fails
     */
    public Optional<IssueLink> link(final long linkId) throws IOException {
        return gitLab().issuelinks()
                .get(project.id(), iid(), String.valueOf(linkId))
                .map(loaded -> new IssueLink(this, loaded));
    }

    /**
     * Links the issue to another issue ({@code POST /projects/{id}/issues/{issue_iid}/links}). The target may be in
     * another project. The link type is read from this issue: {@link IssueLinkType#BLOCKS} means this issue blocks the
     * target. GitLab's answer to the create request carries no link id, so when it has none this method makes one extra
     * request ({@code GET /projects/{id}/issues/{issue_iid}/links}) and takes the id from the entry of the target; the
     * returned link therefore always has a usable {@link IssueLink#id()}. This is the one exception to one core
     * operation per method.
     *
     * @param target the issue to link to
     * @param type the kind of link
     * @return the created link
     * @throws NullPointerException if {@code target} or {@code type} is {@code null}
     * @throws IllegalStateException if the answer has no link id and the list does not show the new link
     * @throws IOException if the transport fails
     */
    public IssueLink linkTo(final Issue target, final IssueLinkType type) throws IOException {
        final Issue linked = requireNonNull(target, "target");
        final GitLabProjectIssueLinksCreatePayload payload = GitLabProjectIssueLinksCreatePayloadBuilder.builder()
                .targetProjectId(linked.project().id())
                .targetIssueIid(String.valueOf(linked.iid()))
                .linkType(GitLabProjectIssueLinksCreateLinkType.fromWire(
                        requireNonNull(type, "type").value()))
                .build();
        final GitLabIssueLink created = gitLab().issuelinks().create(project.id(), iid(), payload);
        if (created.id() != null) {
            return new IssueLink(this, created);
        }
        return new IssueLink(
                this,
                GitLabIssueLinkBuilder.builder(created).id(findLinkId(linked)).build());
    }

    /**
     * Lists the users taking part in the issue ({@code GET /projects/{id}/issues/{issue_iid}/participants}). GitLab
     * answers in one piece, so the stream only walks the list.
     *
     * @return the participants as the core describes them
     * @throws IOException if the transport fails
     */
    public Stream<GitLabUserBasic> participants() throws IOException {
        return gitLab().issues().project().listParticipants(project.id(), iid()).stream();
    }

    /**
     * Lists the merge requests that close the issue when they merge ({@code GET
     * /projects/{id}/issues/{issue_iid}/closed_by}). GitLab lists a summary of each merge request, so each one is
     * loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, and one that is
     * gone by then is skipped. Transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the list cannot be loaded
     */
    public Stream<MergeRequest> closedByMergeRequests() throws IOException {
        return mergeRequests(gitLab().issues().project().listClosedBy(project.id(), iid()));
    }

    /**
     * Lists the merge requests that mention the issue ({@code GET
     * /projects/{id}/issues/{issue_iid}/related_merge_requests}). GitLab lists a summary of each merge request, so each
     * one is loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, and one
     * that is gone by then is skipped. Transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the list cannot be loaded
     */
    public Stream<MergeRequest> relatedMergeRequests() throws IOException {
        return mergeRequests(gitLab().issues().project().listRelatedMergeRequests(project.id(), iid()));
    }

    /**
     * Moves the issue to another project ({@code POST /projects/{id}/issues/{issue_iid}/move}). The issue gets a new
     * {@code iid} there; this object is not changed.
     *
     * @param targetProjectId the numeric id of the project to move the issue to
     * @return the issue as GitLab returned it, in the target project
     * @throws IOException if the transport fails
     */
    public Issue move(final long targetProjectId) throws IOException {
        final GitLabProjectIssuesMovePayload payload = GitLabProjectIssuesMovePayloadBuilder.builder()
                .toProjectId(targetProjectId)
                .build();
        final GitLabIssue moved = gitLab().issues().project().move(project.id(), iid(), payload);
        return new Issue(Internals.sdk(project).project(String.valueOf(targetProjectId)), moved);
    }

    /**
     * Deletes the issue ({@code DELETE /projects/{id}/issues/{issue_iid}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().issues().project().delete(project.id(), iid());
    }

    /**
     * Loads the issue again ({@code GET /projects/{id}/issues/{issue_iid}}). This object is not changed.
     *
     * @return the issue as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the issue
     * @throws IOException if the transport fails
     */
    public Issue refresh() throws IOException {
        return project.issue(iid()).orElseThrow(() -> new IllegalStateException("Issue " + iid() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabIssue response() {
        return response;
    }

    Issue apply(final GitLabProjectIssuesUpdatePayload payload) throws IOException {
        return new Issue(project, gitLab().issues().project().update(project.id(), iid(), payload));
    }

    private Issue changeState(final GitLabProjectIssuesUpdateStateEvent event) throws IOException {
        return apply(GitLabProjectIssuesUpdatePayloadBuilder.builder()
                .stateEvent(event)
                .build());
    }

    private Long findLinkId(final Issue linked) throws IOException {
        final Long targetProjectId = linked.response().projectId();
        for (final GitLabRelatedIssue related : gitLab().issuelinks().list(project.id(), iid())) {
            if (Objects.equals(related.iid(), linked.iid())
                    && related.issueLinkId() != null
                    && (targetProjectId == null || targetProjectId.equals(related.projectId()))) {
                return related.issueLinkId();
            }
        }
        throw new IllegalStateException(
                "GitLab created the link but its id was not found in the links of issue " + iid());
    }

    private Stream<MergeRequest> mergeRequests(final List<GitLabMergeRequestBasic> listed) {
        return listed.stream()
                .map(summary -> Lookups.find(
                        () -> projectOf(summary.projectId()).mergeRequest(requireNonNull(summary.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    private Project projectOf(final @Nullable Long listedIn) {
        if (listedIn == null || project.id().equals(String.valueOf(listedIn))) {
            return project;
        }
        return Internals.sdk(project).project(String.valueOf(listedIn));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
