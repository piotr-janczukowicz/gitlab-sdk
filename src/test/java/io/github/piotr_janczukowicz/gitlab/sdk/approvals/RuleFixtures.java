package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupApprovalRuleBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabApprovalSettings;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabApprovalSettingsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectApprovalRuleBuilder;

/** Builds the core records for the tests of the project and group approval rule objects. */
final class RuleFixtures {

    private RuleFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabProjectApprovalRule projectRule(final long id) {
        return GitLabProjectApprovalRuleBuilder.builder().id(id).build();
    }

    static GitLabProjectApprovalRule fullProjectRule(final long id) {
        return GitLabProjectApprovalRuleBuilder.builder()
                .id(id)
                .name("security")
                .ruleType("regular")
                .approvalsRequired(2)
                .containsHiddenGroups(true)
                .appliesToAllProtectedBranches(true)
                .reportType("code_coverage")
                .build();
    }

    static GitLabGroupApprovalRule groupRule(final long id) {
        return GitLabGroupApprovalRuleBuilder.builder().id(id).build();
    }

    static GitLabGroupApprovalRule fullGroupRule(final long id) {
        return GitLabGroupApprovalRuleBuilder.builder()
                .id(id)
                .name("security")
                .ruleType("regular")
                .approvalsRequired(2)
                .containsHiddenGroups(true)
                .appliesToAllProtectedBranches(true)
                .reportType("code_coverage")
                .build();
    }

    static GitLabApprovalSettings settings() {
        return GitLabApprovalSettingsBuilder.builder().build();
    }

    static GitLabApprovalSettings fullSettings() {
        return GitLabApprovalSettingsBuilder.builder()
                .approvalsBeforeMerge(3)
                .resetApprovalsOnPush(true)
                .selectiveCodeOwnerRemovals(true)
                .disableOverridingApproversPerMergeRequest(true)
                .mergeRequestsAuthorApproval(true)
                .mergeRequestsDisableCommittersApproval(true)
                .requirePasswordToApprove(true)
                .requireReauthenticationToApprove(true)
                .build();
    }
}
