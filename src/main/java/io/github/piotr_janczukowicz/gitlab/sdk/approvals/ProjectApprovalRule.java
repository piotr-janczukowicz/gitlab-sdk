package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsUpdateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * An approval rule of a {@link Project}, as GitLab returned it. This is the project-level rule, not the rule of a
 * single merge request ({@link MergeRequestApprovalRule}). Per GitLab's API description, approval rules are a Premium
 * feature: on a lower tier the core's {@code GitLabTierException} passes through. The object is immutable, and
 * {@link #response()} gives the complete response, with the approvers and the protected branches.
 */
public final class ProjectApprovalRule {

    private final Project project;
    private final GitLabProjectApprovalRule response;

    /**
     * Wraps a response. Applications normally get this object from the project; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ProjectApprovalRule(final Project project, final GitLabProjectApprovalRule response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the rule belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
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
     * Returns the kind of the rule, such as {@code regular} or {@code any_approver}.
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
     * Tells whether the rule names groups the caller cannot see.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean containsHiddenGroups() {
        return Boolean.TRUE.equals(response.containsHiddenGroups());
    }

    /**
     * Tells whether the rule applies to every protected branch.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean appliesToAllProtectedBranches() {
        return Boolean.TRUE.equals(response.appliesToAllProtectedBranches());
    }

    /**
     * Returns the report type of a rule that GitLab made from a policy, such as {@code code_coverage}.
     *
     * @return the report type, or empty when GitLab does not return one
     */
    public Optional<String> reportType() {
        return Optional.ofNullable(response.reportType());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link ProjectApprovalRuleUpdate#send()}.
     *
     * @return the update builder
     */
    public ProjectApprovalRuleUpdate update() {
        return new ProjectApprovalRuleUpdate(this);
    }

    /**
     * Deletes the rule ({@code DELETE /projects/{id}/approval_rules/{approval_rule_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().projects().project().deleteApprovalRule(project.id(), id());
    }

    /**
     * Loads the rule again ({@code GET /projects/{id}/approval_rules/{approval_rule_id}}). This object is not changed.
     *
     * @return the rule as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the rule
     * @throws IOException if the transport fails
     */
    public ProjectApprovalRule refresh() throws IOException {
        return project.approvalRule(id())
                .orElseThrow(() -> new IllegalStateException("Approval rule " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabProjectApprovalRule response() {
        return response;
    }

    ProjectApprovalRule apply(final GitLabProjectProjectsUpdateApprovalRulePayload payload) throws IOException {
        return new ProjectApprovalRule(
                project, gitLab().projects().project().updateApprovalRule(project.id(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
