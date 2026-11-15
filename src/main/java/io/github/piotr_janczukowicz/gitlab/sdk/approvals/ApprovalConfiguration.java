package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabApprovalSettings;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateApprovalsPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;

/**
 * The approval configuration of a {@link Project} ({@code GET /projects/{id}/approvals}), as GitLab returned it. Per
 * GitLab's API description, it is a Premium feature: on a lower tier the core's {@code GitLabTierException} passes
 * through. The object is immutable, and {@link #response()} gives the complete response. Two settings are not offered
 * because GitLab's API description marks them deprecated: {@code approvals_before_merge} (create an approval rule
 * instead) and {@code require_password_to_approve} (use {@code require_reauthentication_to_approve}); they stay
 * readable in {@link #response()}. A flag the response leaves out counts as {@code false}.
 */
public final class ApprovalConfiguration {

    private final Project project;
    private final GitLabApprovalSettings response;

    /**
     * Wraps a response. Applications normally get this object from the project; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ApprovalConfiguration(final Project project, final GitLabApprovalSettings response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the configuration belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Tells whether a push removes the approvals of a merge request.
     *
     * @return {@code true} when the response says so
     */
    public boolean resetApprovalsOnPush() {
        return Boolean.TRUE.equals(response.resetApprovalsOnPush());
    }

    /**
     * Tells whether a change to a file removes only the approvals of its code owners.
     *
     * @return {@code true} when the response says so
     */
    public boolean selectiveCodeOwnerRemovals() {
        return Boolean.TRUE.equals(response.selectiveCodeOwnerRemovals());
    }

    /**
     * Tells whether the approvers of a single merge request cannot be changed.
     *
     * @return {@code true} when the response says so
     */
    public boolean disableOverridingApproversPerMergeRequest() {
        return Boolean.TRUE.equals(response.disableOverridingApproversPerMergeRequest());
    }

    /**
     * Tells whether the author of a merge request may approve it.
     *
     * @return {@code true} when the response says so
     */
    public boolean mergeRequestsAuthorApproval() {
        return Boolean.TRUE.equals(response.mergeRequestsAuthorApproval());
    }

    /**
     * Tells whether a committer may not approve the merge request.
     *
     * @return {@code true} when the response says so
     */
    public boolean mergeRequestsDisableCommittersApproval() {
        return Boolean.TRUE.equals(response.mergeRequestsDisableCommittersApproval());
    }

    /**
     * Tells whether an approval needs the user to authenticate again.
     *
     * @return {@code true} when the response says so
     */
    public boolean requireReauthenticationToApprove() {
        return Boolean.TRUE.equals(response.requireReauthenticationToApprove());
    }

    /**
     * Starts a change of the configuration. Nothing is sent until {@link ApprovalConfigurationUpdate#send()}.
     *
     * @return the update builder
     */
    public ApprovalConfigurationUpdate update() {
        return new ApprovalConfigurationUpdate(this);
    }

    /**
     * Loads the configuration again ({@code GET /projects/{id}/approvals}). This object is not changed.
     *
     * @return the configuration as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has it
     * @throws IOException if the transport fails
     */
    public ApprovalConfiguration refresh() throws IOException {
        return project.approvalConfiguration()
                .orElseThrow(() -> new IllegalStateException("The approval configuration is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabApprovalSettings response() {
        return response;
    }

    ApprovalConfiguration apply(final GitLabProjectMergeRequestApprovalsCreateApprovalsPayload payload)
            throws IOException {
        return new ApprovalConfiguration(
                project, Internals.gitLab(project).mergerequestapprovals().createApprovals(project.id(), payload));
    }
}
