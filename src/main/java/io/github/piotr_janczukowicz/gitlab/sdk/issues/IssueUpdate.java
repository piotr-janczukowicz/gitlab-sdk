package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Collects changes to an {@link Issue} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class IssueUpdate {

    private final Issue issue;
    private final GitLabProjectIssuesUpdatePayloadBuilder payload = GitLabProjectIssuesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param issue the issue to change
     * @throws NullPointerException if {@code issue} is {@code null}
     */
    public IssueUpdate(final Issue issue) {
        this.issue = requireNonNull(issue, "issue");
    }

    /**
     * Sets the title.
     *
     * @param title the new title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public IssueUpdate title(final String title) {
        payload.title(requireNotBlank(title, "title"));
        return changed();
    }

    /**
     * Sets the description.
     *
     * @param description the new description in Markdown
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public IssueUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sends an empty string as the description, the same as {@code description("")}. Whether GitLab then clears the
     * description is not verified against a live instance.
     *
     * @return this update
     */
    public IssueUpdate clearDescription() {
        payload.description("");
        return changed();
    }

    /**
     * Replaces the labels. An empty list removes all labels.
     *
     * @param names the label names
     * @return this update
     * @throws NullPointerException if {@code names} or a name is {@code null}
     * @throws IllegalArgumentException if a name is blank
     */
    public IssueUpdate labels(final List<String> names) {
        payload.labels(Arguments.requireNotBlankElements(names, "names"));
        return changed();
    }

    /**
     * Replaces the assignees. An empty list removes all assignees.
     *
     * @param userIds the numeric user ids
     * @return this update
     * @throws NullPointerException if {@code userIds} or an id is {@code null}
     */
    public IssueUpdate assignees(final List<Long> userIds) {
        payload.assigneeIds(List.copyOf(requireNonNull(userIds, "userIds")));
        return changed();
    }

    /**
     * Makes the issue confidential or public.
     *
     * @param confidential {@code true} for a confidential issue
     * @return this update
     */
    public IssueUpdate confidential(final boolean confidential) {
        payload.confidential(confidential);
        return changed();
    }

    /**
     * Sets the milestone. The core offers no way to remove a milestone, so this SDK has none either.
     *
     * @param milestoneId the numeric id of the milestone
     * @return this update
     */
    public IssueUpdate milestone(final long milestoneId) {
        payload.milestoneId(milestoneId);
        return changed();
    }

    /**
     * Sets the due date. The core offers no way to remove a due date, so this SDK has none either.
     *
     * @param dueDate the new due date
     * @return this update
     * @throws NullPointerException if {@code dueDate} is {@code null}
     */
    public IssueUpdate dueDate(final LocalDate dueDate) {
        payload.dueDate(requireNonNull(dueDate, "dueDate").toString());
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/issues/{issue_iid}}). The issue this update was made from is not
     * changed.
     *
     * @return the issue as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Issue send() throws IOException {
        state.requireChange();
        return issue.apply(payload.build());
    }

    private IssueUpdate changed() {
        state.mark();
        return this;
    }
}
