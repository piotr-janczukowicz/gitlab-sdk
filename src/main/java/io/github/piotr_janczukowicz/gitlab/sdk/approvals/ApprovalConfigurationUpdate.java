package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateApprovalsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to an {@link ApprovalConfiguration} and sends them in one request. Only the fields that were set are
 * sent, and nothing is sent until {@link #send()}. The two settings that GitLab's API description marks as deprecated
 * are not offered.
 */
public final class ApprovalConfigurationUpdate {

    private final ApprovalConfiguration configuration;
    private final GitLabProjectMergeRequestApprovalsCreateApprovalsPayloadBuilder payload =
            GitLabProjectMergeRequestApprovalsCreateApprovalsPayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param configuration the configuration to change
     * @throws NullPointerException if {@code configuration} is {@code null}
     */
    public ApprovalConfigurationUpdate(final ApprovalConfiguration configuration) {
        this.configuration = requireNonNull(configuration, "configuration");
    }

    /**
     * Sets whether a push removes the approvals of a merge request.
     *
     * @param enabled {@code true} to remove them
     * @return this update
     */
    public ApprovalConfigurationUpdate resetApprovalsOnPush(final boolean enabled) {
        payload.resetApprovalsOnPush(enabled);
        return changed();
    }

    /**
     * Sets whether a change to a file removes only the approvals of its code owners.
     *
     * @param enabled {@code true} to remove only theirs
     * @return this update
     */
    public ApprovalConfigurationUpdate selectiveCodeOwnerRemovals(final boolean enabled) {
        payload.selectiveCodeOwnerRemovals(enabled);
        return changed();
    }

    /**
     * Sets whether the approvers of a single merge request cannot be changed.
     *
     * @param enabled {@code true} to forbid changing them
     * @return this update
     */
    public ApprovalConfigurationUpdate disableOverridingApproversPerMergeRequest(final boolean enabled) {
        payload.disableOverridingApproversPerMergeRequest(enabled);
        return changed();
    }

    /**
     * Sets whether the author of a merge request may approve it.
     *
     * @param enabled {@code true} to allow it
     * @return this update
     */
    public ApprovalConfigurationUpdate mergeRequestsAuthorApproval(final boolean enabled) {
        payload.mergeRequestsAuthorApproval(enabled);
        return changed();
    }

    /**
     * Sets whether a committer may not approve the merge request.
     *
     * @param enabled {@code true} to forbid it
     * @return this update
     */
    public ApprovalConfigurationUpdate mergeRequestsDisableCommittersApproval(final boolean enabled) {
        payload.mergeRequestsDisableCommittersApproval(enabled);
        return changed();
    }

    /**
     * Sets whether an approval needs the user to authenticate again.
     *
     * @param enabled {@code true} to require it
     * @return this update
     */
    public ApprovalConfigurationUpdate requireReauthenticationToApprove(final boolean enabled) {
        payload.requireReauthenticationToApprove(enabled);
        return changed();
    }

    /**
     * Sends the changes ({@code POST /projects/{id}/approvals}). The configuration this update was made from is not
     * changed.
     *
     * @return the configuration as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public ApprovalConfiguration send() throws IOException {
        state.requireChange();
        return configuration.apply(payload.build());
    }

    private ApprovalConfigurationUpdate changed() {
        state.mark();
        return this;
    }
}
