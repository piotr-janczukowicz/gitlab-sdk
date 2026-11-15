package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsUpdateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * An approval rule of a {@link Group}, as GitLab returned it. Per GitLab's API description, group approval rules are a
 * Premium feature: on a lower tier the core's {@code GitLabTierException} passes through. The object is immutable, and
 * {@link #response()} gives the complete response. It has no {@code delete()} and no {@code refresh()}: the core has no
 * operation to delete or to load a single group rule.
 */
public final class GroupApprovalRule {

    private final Group group;
    private final GitLabGroupApprovalRule response;

    /**
     * Wraps a response. Applications normally get this object from the group; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupApprovalRule(final Group group, final GitLabGroupApprovalRule response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the rule belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
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
     * Returns the report type of a rule that GitLab made from a policy.
     *
     * @return the report type, or empty when GitLab does not return one
     */
    public Optional<String> reportType() {
        return Optional.ofNullable(response.reportType());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link GroupApprovalRuleUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupApprovalRuleUpdate update() {
        return new GroupApprovalRuleUpdate(this);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabGroupApprovalRule response() {
        return response;
    }

    GroupApprovalRule apply(final GitLabGroupGroupsUpdateApprovalRulePayload payload) throws IOException {
        return new GroupApprovalRule(
                group, Internals.gitLab(group).groups().updateApprovalRule(group.id(), id(), payload));
    }
}
