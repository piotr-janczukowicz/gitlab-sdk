package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedBranchBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedRefAccess;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedRefAccessBuilder;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupProtectedBranchBuilder;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProtectedTag;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProtectedTagBuilder;
import java.util.List;

/** Builds the core records for the tests of the protection objects. */
final class ProtectionFixtures {

    private ProtectionFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabProtectedRefAccess access(final int level) {
        return GitLabProtectedRefAccessBuilder.builder().accessLevel(level).build();
    }

    /** An entry with every field that decides its kind; null leaves the field out. */
    static GitLabProtectedRefAccess rule(
            final Integer level,
            final Long userId,
            final Long groupId,
            final Long deployKeyId,
            final Long memberRoleId) {
        return GitLabProtectedRefAccessBuilder.builder()
                .accessLevel(level)
                .userId(userId)
                .groupId(groupId)
                .deployKeyId(deployKeyId)
                .memberRoleId(memberRoleId)
                .build();
    }

    static GitLabProtectedBranch branch(final String name) {
        return GitLabProtectedBranchBuilder.builder().id(1L).name(name).build();
    }

    static GitLabProtectedBranch branch(final String name, final int push, final int merge) {
        return GitLabProtectedBranchBuilder.builder()
                .id(1L)
                .name(name)
                .pushAccessLevels(List.of(access(push)))
                .mergeAccessLevels(List.of(access(merge)))
                .allowForcePush(false)
                .codeOwnerApprovalRequired(true)
                .build();
    }

    static GitLabProtectedBranch branchWithRules(final GitLabProtectedRefAccess... rules) {
        return GitLabProtectedBranchBuilder.builder()
                .id(1L)
                .name("main")
                .pushAccessLevels(List.of(rules))
                .mergeAccessLevels(List.of(rules))
                .build();
    }

    static GitLabGroupProtectedBranch groupBranchWithRules(final GitLabProtectedRefAccess... rules) {
        return GitLabGroupProtectedBranchBuilder.builder()
                .id(1L)
                .name("main")
                .pushAccessLevels(List.of(rules))
                .mergeAccessLevels(List.of(rules))
                .build();
    }

    static GitLabProtectedTag tagWithRules(final GitLabProtectedRefAccess... rules) {
        return GitLabProtectedTagBuilder.builder()
                .name("v*")
                .createAccessLevels(List.of(rules))
                .build();
    }

    static GitLabProtectedBranch branchWithForcePush(final String name, final boolean allowForcePush) {
        return GitLabProtectedBranchBuilder.builder()
                .id(1L)
                .name(name)
                .allowForcePush(allowForcePush)
                .build();
    }

    static GitLabProtectedBranch branchWithCodeOwners(final String name, final boolean required) {
        return GitLabProtectedBranchBuilder.builder()
                .id(1L)
                .name(name)
                .codeOwnerApprovalRequired(required)
                .build();
    }

    static GitLabGroupProtectedBranch groupBranch(final String name) {
        return GitLabGroupProtectedBranchBuilder.builder().id(1L).name(name).build();
    }

    static GitLabGroupProtectedBranch groupBranch(final String name, final int push, final int merge) {
        return GitLabGroupProtectedBranchBuilder.builder()
                .id(1L)
                .name(name)
                .pushAccessLevels(List.of(access(push)))
                .mergeAccessLevels(List.of(access(merge)))
                .allowForcePush(true)
                .codeOwnerApprovalRequired(false)
                .build();
    }

    static GitLabGroupProtectedBranch groupBranchWithForcePush(final String name, final boolean allowForcePush) {
        return GitLabGroupProtectedBranchBuilder.builder()
                .id(1L)
                .name(name)
                .allowForcePush(allowForcePush)
                .build();
    }

    static GitLabGroupProtectedBranch groupBranchWithCodeOwners(final String name, final boolean required) {
        return GitLabGroupProtectedBranchBuilder.builder()
                .id(1L)
                .name(name)
                .codeOwnerApprovalRequired(required)
                .build();
    }

    static GitLabProtectedTag tag(final String name) {
        return GitLabProtectedTagBuilder.builder().name(name).build();
    }

    static GitLabProtectedTag tag(final String name, final int create) {
        return GitLabProtectedTagBuilder.builder()
                .name(name)
                .createAccessLevels(List.of(access(create)))
                .build();
    }
}
