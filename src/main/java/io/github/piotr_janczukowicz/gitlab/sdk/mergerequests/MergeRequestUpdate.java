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
