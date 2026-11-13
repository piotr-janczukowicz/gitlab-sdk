package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestApprovals;
import java.util.Optional;

/**
 * The approval status of a {@link MergeRequest}, as GitLab returned it. The object is immutable: it keeps the data of
 * the moment it was loaded, and {@link #response()} gives the complete response for every field the accessors here do
 * not cover.
 */
public final class Approvals {

    private final MergeRequest mergeRequest;
    private final GitLabMergeRequestApprovals response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param mergeRequest the merge request the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Approvals(final MergeRequest mergeRequest, final GitLabMergeRequestApprovals response) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the merge request the approvals belong to.
     *
     * @return the merge request
     */
    public MergeRequest mergeRequest() {
        return mergeRequest;
    }

    /**
     * Tells whether the merge request has all the approvals it needs.
     *
     * @return {@code true} when approved, empty when GitLab does not say
     */
    public Optional<Boolean> approved() {
        return Optional.ofNullable(response.approved());
    }

    /**
     * Returns the number of approvals the merge request still needs.
     *
     * @return the number left, or empty when GitLab does not say
     */
    public Optional<Integer> approvalsLeft() {
        return Optional.ofNullable(response.approvalsLeft());
    }

    /**
     * Returns the number of approvals the merge request needs in total.
     *
     * @return the number required, or empty when GitLab does not say
     */
    public Optional<Integer> approvalsRequired() {
        return Optional.ofNullable(response.approvalsRequired());
    }

    /**
     * Tells whether the authenticated user has approved the merge request.
     *
     * @return {@code true} when approved by the user, empty when GitLab does not say
     */
    public Optional<Boolean> userHasApproved() {
        return Optional.ofNullable(response.userHasApproved());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMergeRequestApprovals response() {
        return response;
    }
}
