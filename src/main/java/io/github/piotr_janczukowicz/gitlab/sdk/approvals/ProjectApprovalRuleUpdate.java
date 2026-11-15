package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsUpdateApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.List;

/**
 * Collects changes to a {@link ProjectApprovalRule} and sends them in one request. Only the fields that were set are
 * sent, and nothing is sent until {@link #send()}. The approvers are set by id or user name only: the core's payload
 * also has {@code users} and {@code groups}, which GitLab's API description marks as deprecated, and the SDK does not
 * offer them. The rule type is not offered either, because the description says not to use it when building rules from
 * the API.
 */
public final class ProjectApprovalRuleUpdate {

    private final ProjectApprovalRule rule;
    private final GitLabProjectProjectsUpdateApprovalRulePayloadBuilder payload =
            GitLabProjectProjectsUpdateApprovalRulePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param rule the rule to change
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    public ProjectApprovalRuleUpdate(final ProjectApprovalRule rule) {
        this.rule = requireNonNull(rule, "rule");
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public ProjectApprovalRuleUpdate name(final String name) {
        payload.name(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sets the number of approvals the rule needs.
     *
     * @param approvalsRequired the number required
     * @return this update
     * @throws IllegalArgumentException if {@code approvalsRequired} is negative
     */
    public ProjectApprovalRuleUpdate approvalsRequired(final int approvalsRequired) {
        if (approvalsRequired < 0) {
            throw new IllegalArgumentException("approvalsRequired must not be negative");
        }
        payload.approvalsRequired(approvalsRequired);
        return changed();
    }

    /**
     * Replaces the users who can approve. An empty list removes them all.
     *
     * @param userIds the numeric user ids
     * @return this update
     * @throws NullPointerException if {@code userIds} or an id is {@code null}
     */
    public ProjectApprovalRuleUpdate userIds(final List<Long> userIds) {
        payload.userIds(List.copyOf(requireNonNull(userIds, "userIds")));
        return changed();
    }

    /**
     * Replaces the groups whose members can approve. An empty list removes them all.
     *
     * @param groupIds the numeric group ids
     * @return this update
     * @throws NullPointerException if {@code groupIds} or an id is {@code null}
     */
    public ProjectApprovalRuleUpdate groupIds(final List<Long> groupIds) {
        payload.groupIds(List.copyOf(requireNonNull(groupIds, "groupIds")));
        return changed();
    }

    /**
     * Replaces the users who can approve, by user name. An empty list removes them all.
     *
     * @param usernames the user names
     * @return this update
     * @throws NullPointerException if {@code usernames} or a name is {@code null}
     * @throws IllegalArgumentException if a name is blank
     */
    public ProjectApprovalRuleUpdate usernames(final List<String> usernames) {
        payload.usernames(Arguments.requireNotBlankElements(usernames, "usernames"));
        return changed();
    }

    /**
     * Replaces the protected branches the rule applies to. An empty list removes them all.
     *
     * @param protectedBranchIds the ids of the protected branches
     * @return this update
     * @throws NullPointerException if {@code protectedBranchIds} or an id is {@code null}
     */
    public ProjectApprovalRuleUpdate protectedBranchIds(final List<Long> protectedBranchIds) {
        payload.protectedBranchIds(List.copyOf(requireNonNull(protectedBranchIds, "protectedBranchIds")));
        return changed();
    }

    /**
     * Sets whether the rule applies to every protected branch.
     *
     * @param appliesToAll {@code true} to apply it to every protected branch
     * @return this update
     */
    public ProjectApprovalRuleUpdate appliesToAllProtectedBranches(final boolean appliesToAll) {
        payload.appliesToAllProtectedBranches(appliesToAll);
        return changed();
    }

    /**
     * Sets whether groups the caller cannot see are removed from the rule.
     *
     * @param removeHiddenGroups {@code true} to remove hidden groups
     * @return this update
     */
    public ProjectApprovalRuleUpdate removeHiddenGroups(final boolean removeHiddenGroups) {
        payload.removeHiddenGroups(removeHiddenGroups);
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/approval_rules/{approval_rule_id}}). The rule this update was made
     * from is not changed.
     *
     * @return the rule as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public ProjectApprovalRule send() throws IOException {
        state.requireChange();
        return rule.apply(payload.build());
    }

    private ProjectApprovalRuleUpdate changed() {
        state.mark();
        return this;
    }
}
