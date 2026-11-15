package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * An approval rule of a {@link MergeRequest}, as GitLab returned it. The object is immutable, and {@link #response()}
 * gives the complete response for every field the accessors here do not cover. Approval rules are a Premium feature: on
 * a lower tier the core's {@code GitLabTierException} passes through.
 */
public final class MergeRequestApprovalRule {

    private final MergeRequest mergeRequest;
    private final GitLabMergeRequestApprovalRule response;

    /**
     * Wraps a response. Applications normally get this object from the merge request; call the constructor only to wrap
     * a response that was loaded some other way.
     *
     * @param mergeRequest the merge request the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public MergeRequestApprovalRule(final MergeRequest mergeRequest, final GitLabMergeRequestApprovalRule response) {
        this.mergeRequest = requireNonNull(mergeRequest, "mergeRequest");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the merge request the rule belongs to.
     *
     * @return the merge request
     */
    public MergeRequest mergeRequest() {
        return mergeRequest;
    }

    /**
     * Returns the rule id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the rule name.
     *
     * @return the name, or empty when GitLab does not return one
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the kind of the rule, such as {@code regular} or {@code code_owner}.
     *
     * @return the rule type, or empty when GitLab does not return one
     */
    public Optional<String> ruleType() {
        return Optional.ofNullable(response.ruleType());
    }

    /**
     * Returns the number of approvals the rule needs.
     *
     * @return the number required, or empty when GitLab does not return it
     */
    public Optional<Integer> approvalsRequired() {
        return Optional.ofNullable(response.approvalsRequired());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link MergeRequestApprovalRuleUpdate#send()}.
     *
     * @return the update builder
     */
    public MergeRequestApprovalRuleUpdate update() {
        return new MergeRequestApprovalRuleUpdate(this);
    }

    /**
     * Deletes the rule ({@code DELETE /projects/{id}/merge_requests/{merge_request_iid}/approval_rules/{rule_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().mergerequestapprovals()
                .deleteMergeRequestsApprovalRule(mergeRequest.project().id(), mergeRequest.iid(), id());
    }

    /**
     * Loads the rule again ({@code GET /projects/{id}/merge_requests/{merge_request_iid}/approval_rules/{rule_id}}).
     * This object is not changed.
     *
     * @return the rule as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the rule
     * @throws IOException if the transport fails
     */
    public MergeRequestApprovalRule refresh() throws IOException {
        return mergeRequest
                .approvalRule(id())
                .orElseThrow(() -> new IllegalStateException("Approval rule " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMergeRequestApprovalRule response() {
        return response;
    }

    MergeRequestApprovalRule apply(
            final GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload payload) throws IOException {
        return new MergeRequestApprovalRule(
                mergeRequest,
                gitLab().mergerequestapprovals()
                        .updateMergeRequestsApprovalRule(
                                mergeRequest.project().id(), mergeRequest.iid(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(mergeRequest.project());
    }
}
