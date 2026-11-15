package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.List;

/**
 * Collects changes to a {@link MergeRequest} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class MergeRequestUpdate {

    private static final String USER_IDS = "userIds";

    private final MergeRequest mergeRequest;
    private final GitLabProjectMergeRequestsUpdatePayloadBuilder payload =
            GitLabProjectMergeRequestsUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param mergeRequest the merge request to change
     * @throws NullPointerException if {@code mergeRequest} is {@code null}
     */
    public MergeRequestUpdate(final MergeRequest mergeRequest) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
    }

    /**
     * Sets the title.
     *
     * @param title the new title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public MergeRequestUpdate title(final String title) {
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
    public MergeRequestUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets the target branch.
     *
     * @param targetBranch the name of the branch to merge into
     * @return this update
     * @throws NullPointerException if {@code targetBranch} is {@code null}
     * @throws IllegalArgumentException if {@code targetBranch} is blank
     */
    public MergeRequestUpdate targetBranch(final String targetBranch) {
        payload.targetBranch(requireNotBlank(targetBranch, "targetBranch"));
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
    public MergeRequestUpdate labels(final List<String> names) {
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
    public MergeRequestUpdate assignees(final List<Long> userIds) {
        payload.assigneeIds(List.copyOf(requireNonNull(userIds, USER_IDS)));
        return changed();
    }

    /**
     * Replaces the reviewers. An empty list removes all reviewers.
     *
     * @param userIds the numeric user ids
     * @return this update
     * @throws NullPointerException if {@code userIds} or an id is {@code null}
     */
    public MergeRequestUpdate reviewers(final List<Long> userIds) {
        payload.reviewerIds(List.copyOf(requireNonNull(userIds, USER_IDS)));
        return changed();
    }

    /**
     * Sets the milestone. The core offers no way to remove a milestone, so this SDK has none either.
     *
     * @param milestoneId the numeric id of the milestone
     * @return this update
     */
    public MergeRequestUpdate milestone(final long milestoneId) {
        payload.milestoneId(milestoneId);
        return changed();
    }

    /**
     * Sets whether the commits are squashed when the merge request merges.
     *
     * @param squash {@code true} to squash
     * @return this update
     */
    public MergeRequestUpdate squash(final boolean squash) {
        payload.squash(squash);
        return changed();
    }

    /**
     * Sets whether the source branch is removed when the merge request merges.
     *
     * @param removeSourceBranch {@code true} to remove the source branch
     * @return this update
     */
    public MergeRequestUpdate removeSourceBranch(final boolean removeSourceBranch) {
        payload.removeSourceBranch(removeSourceBranch);
        return changed();
    }

    /**
     * Locks or unlocks the discussion, so that only project members can comment while it is locked.
     *
     * @param discussionLocked {@code true} to lock the discussion
     * @return this update
     */
    public MergeRequestUpdate discussionLocked(final boolean discussionLocked) {
        payload.discussionLocked(discussionLocked);
        return changed();
    }

    /**
     * Sets whether members of the target project can push to the source branch.
     *
     * @param allowCollaboration {@code true} to allow it
     * @return this update
     */
    public MergeRequestUpdate allowCollaboration(final boolean allowCollaboration) {
        payload.allowCollaboration(allowCollaboration);
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/merge_requests/{merge_request_iid}}). The merge request this update
     * was made from is not changed.
     *
     * @return the merge request as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public MergeRequest send() throws IOException {
        state.requireChange();
        return mergeRequest.apply(payload.build());
    }

    private MergeRequestUpdate changed() {
        state.mark();
        return this;
    }
}
