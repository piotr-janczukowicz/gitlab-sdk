package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
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
 * A milestone of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * GitLab lists, loads, creates and updates a milestone with the same record, so none of them needs an extra request.
 * Burndown events are not wrapped; use the core client for them.
 */
public final class Milestone {

    private final Project project;
    private final GitLabMilestone response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Milestone(final Project project, final GitLabMilestone response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the milestone belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
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
     * Returns the project-internal id of the milestone.
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
     * {@link MilestoneUpdate#send()}.
     *
     * @return the update builder
     */
    public MilestoneUpdate update() {
        return new MilestoneUpdate(this);
    }

    /**
     * Closes the milestone ({@code PUT /projects/{id}/milestones/{milestone_id}} with state event {@code close}).
     *
     * @return the milestone as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public Milestone close() throws IOException {
        return stateEvent(GitLabProjectProjectMilestonesUpdateStateEvent.CLOSE);
    }

    /**
     * Reopens the milestone ({@code PUT /projects/{id}/milestones/{milestone_id}} with state event {@code activate}).
     *
     * @return the milestone as GitLab returned it; this object is not changed
     * @throws IOException if the transport fails
     */
    public Milestone reopen() throws IOException {
        return stateEvent(GitLabProjectProjectMilestonesUpdateStateEvent.ACTIVATE);
    }

    /**
     * Promotes the milestone to a group milestone ({@code POST /projects/{id}/milestones/{milestone_id}/promote}).
     * GitLab answers with no body, so nothing is returned; load the group milestone through the group.
     *
     * @throws IOException if the transport fails
     */
    public void promote() throws IOException {
        gitLab().projectmilestones().promote(project.id(), id());
    }

    /**
     * Lists the issues of the milestone ({@code GET /projects/{id}/milestones/{milestone_id}/issues}). GitLab lists a
     * summary of each issue, so each one is loaded ({@code GET /projects/{id}/issues/{issue_iid}}) as the stream
     * reaches it, and one that is gone by then is skipped. An issue is loaded from the project GitLab names in the
     * summary; when it names none, from {@link #project()}. Transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return gitLab().projectmilestones().listIssues(project.id(), id()).stream()
                .map(listed ->
                        Lookups.find(() -> projectOf(listed.projectId()).issue(requireNonNull(listed.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Lists the merge requests of the milestone ({@code GET /projects/{id}/milestones/{milestone_id}/merge_requests}).
     * GitLab lists a summary of each merge request, so each one is loaded ({@code GET
     * /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, and one that is gone by then is
     * skipped. A merge request is loaded from the project GitLab names in the summary; when it names none, from
     * {@link #project()}. Transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return gitLab().projectmilestones().listMergeRequests(project.id(), id()).stream()
                .map(listed -> Lookups.find(
                        () -> projectOf(listed.projectId()).mergeRequest(requireNonNull(listed.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Deletes the milestone ({@code DELETE /projects/{id}/milestones/{milestone_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().projectmilestones().delete(project.id(), id());
    }

    /**
     * Loads the milestone again ({@code GET /projects/{id}/milestones/{milestone_id}}). This object is not changed.
     *
     * @return the milestone as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the milestone
     * @throws IOException if the transport fails
     */
    public Milestone refresh() throws IOException {
        return project.milestone(id()).orElseThrow(() -> new IllegalStateException("Milestone " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMilestone response() {
        return response;
    }

    Milestone apply(final GitLabProjectProjectMilestonesUpdatePayload payload) throws IOException {
        return new Milestone(project, gitLab().projectmilestones().update(project.id(), id(), payload));
    }

    private Milestone stateEvent(final GitLabProjectProjectMilestonesUpdateStateEvent event) throws IOException {
        return apply(GitLabProjectProjectMilestonesUpdatePayloadBuilder.builder()
                .stateEvent(event)
                .build());
    }

    private Project projectOf(final @Nullable Long listedIn) {
        if (listedIn == null
                || listedIn.equals(response.projectId())
                || project.id().equals(String.valueOf(listedIn))) {
            return project;
        }
        return Internals.project(project, String.valueOf(listedIn));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
