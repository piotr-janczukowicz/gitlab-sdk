package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalState;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalStateRule;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import java.util.List;

/**
 * The approval state of a {@link MergeRequest}, as GitLab returned it: the rules that apply to it and whether they are
 * approved. The object is immutable, and {@link #response()} gives the complete response. The approval state is a
 * Premium feature.
 */
public final class ApprovalState {

    private final MergeRequest mergeRequest;
    private final GitLabMergeRequestApprovalState response;

    /**
     * Wraps a response. Applications normally get this object from the merge request; call the constructor only to wrap
     * a response that was loaded some other way.
     *
     * @param mergeRequest the merge request the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ApprovalState(final MergeRequest mergeRequest, final GitLabMergeRequestApprovalState response) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the merge request the approval state belongs to.
     *
     * @return the merge request
     */
    public MergeRequest mergeRequest() {
        return mergeRequest;
    }

    /**
     * Tells whether the merge request has its own approval rules instead of the ones of the project.
     *
     * @return {@code true} when GitLab says so; {@code false} when it says otherwise or does not return the flag
     */
    public boolean approvalRulesOverwritten() {
        return Boolean.TRUE.equals(response.approvalRulesOverwritten());
    }

    /**
     * Returns the rules that apply to the merge request, with who approved each of them, as the core describes them.
     *
     * @return the rules as an unmodifiable list, empty when there are none
     */
    public List<GitLabMergeRequestApprovalStateRule> rules() {
        final List<GitLabMergeRequestApprovalStateRule> rules = response.rules();
        return rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMergeRequestApprovalState response() {
        return response;
    }
}
