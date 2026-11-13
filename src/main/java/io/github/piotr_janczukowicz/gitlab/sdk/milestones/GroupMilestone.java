package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A milestone of a {@link Group}, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * GitLab lists, loads, creates and updates a milestone with the same record, so none of them needs an extra request.
 * Burndown events are not wrapped; use the core client for them.
 */
public final class GroupMilestone {

    private final Group group;
    private final GitLabMilestone response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupMilestone(final Group group, final GitLabMilestone response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the milestone belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the id of the milestone, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return MilestoneValues.id(response);
    }

    /**
     * Returns the group-internal id of the milestone.
     *
     * @return the iid
     */
    public long iid() {
        return MilestoneValues.iid(response);
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
    }

    /**
     * Returns the description.
     *
     * @return the description, or empty when GitLab sent none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the state.
     *
     * @return the state
     * @throws IllegalArgumentException if GitLab returned a state the SDK does not know; see {@link #stateName()}
     */
    public MilestoneState state() {
        return MilestoneValues.state(response);
    }

    /**
     * Returns the state exactly as GitLab spelled it, such as {@code active}.
     *
     * @return the raw state name
     */
    public String stateName() {
        return MilestoneValues.stateName(response);
    }

    /**
     * Returns the due date.
     *
     * @return the date, or empty when GitLab sent none or a blank one
     * @throws IllegalStateException if GitLab sent a value that is not an ISO date; the message names the value
     */
    public Optional<LocalDate> dueDate() {
        return MilestoneValues.date(response.dueDate(), "due date");
    }

    /**
     * Returns the start date.
     *
     * @return the date, or empty when GitLab sent none or a blank one
     * @throws IllegalStateException if GitLab sent a value that is not an ISO date; the message names the value
     */
    public Optional<LocalDate> startDate() {
        return MilestoneValues.date(response.startDate(), "start date");
    }

    /**
     * Tells whether the milestone is expired: its due date has passed.
     *
     * @return {@code true} if GitLab reports the milestone as expired; {@code false} also when the flag is absent
     */
    public boolean isExpired() {
        return Boolean.TRUE.equals(response.expired());
    }

    /**
     * Starts a change of the title, description, due date or start date. Nothing is sent until
     * {@link GroupMilestoneUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupMilestoneUpdate update() {
        return new GroupMilestoneUpdate(this);
    }

    /**
     * Closes the milestone ({@code PUT /groups/{id}/milestones/{milestone_id}} with state event {@code close}).
     *
     * @return the milestone as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public GroupMilestone close() throws IOException {
        return stateEvent(GitLabGroupGroupMilestonesUpdateStateEvent.CLOSE);
    }

    /**
     * Reopens the milestone ({@code PUT /groups/{id}/milestones/{milestone_id}} with state event {@code activate}).
     *
     * @return the milestone as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public GroupMilestone reopen() throws IOException {
        return stateEvent(GitLabGroupGroupMilestonesUpdateStateEvent.ACTIVATE);
    }

    /**
     * Lists the issues of the milestone ({@code GET /groups/{id}/milestones/{milestone_id}/issues}). The issues belong
     * to different projects. GitLab lists a summary of each issue, so each one is loaded ({@code GET
     * /projects/{id}/issues/{issue_iid}}) as the stream reaches it, through the project GitLab names in the summary
     * ({@link Issue#project()} is a handle to it). An issue that is gone by then is skipped, and so is one whose
     * summary names no project. Transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return gitLab().groupmilestones().listIssues(group.id(), id()).stream()
                .flatMap(listed -> Lookups.find(() -> loadIssue(listed.projectId(), listed.iid())).stream());
    }

    /**
     * Lists the merge requests of the milestone ({@code GET /groups/{id}/milestones/{milestone_id}/merge_requests}).
     * The merge requests belong to different projects. GitLab lists a summary of each merge request, so each one is
     * loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, through the
     * project GitLab names in the summary ({@link MergeRequest#project()} is a handle to it). A merge request that is
     * gone by then is skipped, and so is one whose summary names no project. Transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return gitLab().groupmilestones().listMergeRequests(group.id(), id()).stream()
                .flatMap(listed -> Lookups.find(() -> loadMergeRequest(listed.projectId(), listed.iid())).stream());
    }

    /**
     * Deletes the milestone ({@code DELETE /groups/{id}/milestones/{milestone_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().groupmilestones().delete(group.id(), id());
    }

    /**
     * Loads the milestone again ({@code GET /groups/{id}/milestones/{milestone_id}}). This object is not changed.
     *
     * @return the milestone as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the milestone
     * @throws IOException if the transport fails
     */
    public GroupMilestone refresh() throws IOException {
        return group.milestone(id())
                .orElseThrow(() -> new IllegalStateException("Group milestone " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMilestone response() {
        return response;
    }

    GroupMilestone apply(final GitLabGroupGroupMilestonesUpdatePayload payload) throws IOException {
        return new GroupMilestone(group, gitLab().groupmilestones().update(group.id(), id(), payload));
    }

    private GroupMilestone stateEvent(final GitLabGroupGroupMilestonesUpdateStateEvent event) throws IOException {
        return apply(GitLabGroupGroupMilestonesUpdatePayloadBuilder.builder()
                .stateEvent(event)
                .build());
    }

    private Optional<Issue> loadIssue(final @Nullable Long projectId, final @Nullable Long iid) throws IOException {
        if (projectId == null) {
            return Optional.empty();
        }
        return Internals.project(group, String.valueOf(projectId)).issue(requireNonNull(iid, "iid"));
    }

    private Optional<MergeRequest> loadMergeRequest(final @Nullable Long projectId, final @Nullable Long iid)
            throws IOException {
        if (projectId == null) {
            return Optional.empty();
        }
        return Internals.project(group, String.valueOf(projectId)).mergeRequest(requireNonNull(iid, "iid"));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
