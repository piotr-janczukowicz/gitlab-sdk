package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsUpdateApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.List;

/**
 * Collects changes to a {@link GroupApprovalRule} and sends them in one request. Only the fields that were set are
 * sent, and nothing is sent until {@link #send()}. The rule type is not offered, because GitLab's API description says
 * not to use it when building rules from the API.
 */
public final class GroupApprovalRuleUpdate {

    private final GroupApprovalRule rule;
    private final GitLabGroupGroupsUpdateApprovalRulePayloadBuilder payload =
            GitLabGroupGroupsUpdateApprovalRulePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param rule the rule to change
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    public GroupApprovalRuleUpdate(final GroupApprovalRule rule) {
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
    public GroupApprovalRuleUpdate name(final String name) {
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
    public GroupApprovalRuleUpdate approvalsRequired(final int approvalsRequired) {
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
    public GroupApprovalRuleUpdate userIds(final List<Long> userIds) {
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
    public GroupApprovalRuleUpdate groupIds(final List<Long> groupIds) {
        payload.groupIds(List.copyOf(requireNonNull(groupIds, "groupIds")));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/approval_rules/{approval_rule_id}}). The rule this update was made
     * from is not changed.
     *
     * @return the rule as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupApprovalRule send() throws IOException {
        state.requireChange();
        return rule.apply(payload.build());
    }

    private GroupApprovalRuleUpdate changed() {
        state.mark();
        return this;
    }
}
