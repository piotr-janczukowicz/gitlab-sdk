package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.branchWithRules;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.groupBranchWithRules;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.rule;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.tagWithRules;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedRefAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Only plain role rules appear in the access lists; rules of a user, group, deploy key or member role do not. */
@UnitTest
class ProtectedRuleFilteringTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    static Stream<GitLabProtectedRefAccess> notPlainRoleRules() {
        return Stream.of(
                rule(40, 5L, null, null, null),
                rule(40, null, 6L, null, null),
                rule(40, null, null, 7L, null),
                rule(40, null, null, null, 8L),
                rule(null, null, null, null, null),
                rule(null, 5L, null, null, null));
    }

    @ParameterizedTest
    @MethodSource("notPlainRoleRules")
    void pushAndMergeAccess_shouldSkipEntry_whenProjectBranchRuleIsNotPlainRole(final GitLabProtectedRefAccess rule) {
        final ProtectedBranch sut = new ProtectedBranch(
                GitLabSdk.of(gitLab).project("my-group/demo"), branchWithRules(rule, rule(30, null, null, null, null)));

        assertSoftly(softly -> {
            softly.assertThat(sut.pushAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.pushAccessValues()).containsExactly(30);
            softly.assertThat(sut.mergeAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.mergeAccessValues()).containsExactly(30);
        });
    }

    @ParameterizedTest
    @MethodSource("notPlainRoleRules")
    void pushAndMergeAccess_shouldSkipEntry_whenGroupBranchRuleIsNotPlainRole(final GitLabProtectedRefAccess rule) {
        final GroupProtectedBranch sut = new GroupProtectedBranch(
                GitLabSdk.of(gitLab).group("my-group"), groupBranchWithRules(rule, rule(30, null, null, null, null)));

        assertSoftly(softly -> {
            softly.assertThat(sut.pushAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.pushAccessValues()).containsExactly(30);
            softly.assertThat(sut.mergeAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.mergeAccessValues()).containsExactly(30);
        });
    }

    @ParameterizedTest
    @MethodSource("notPlainRoleRules")
    void createAccess_shouldSkipEntry_whenTagRuleIsNotPlainRole(final GitLabProtectedRefAccess rule) {
        final ProtectedTag sut = new ProtectedTag(
                GitLabSdk.of(gitLab).project("my-group/demo"), tagWithRules(rule, rule(30, null, null, null, null)));

        assertSoftly(softly -> {
            softly.assertThat(sut.createAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.createAccessValues()).containsExactly(30);
        });
    }

    @Test
    void access_shouldKeepEntry_whenRuleIsPlainRoleOnly() {
        final ProtectedBranch sut = new ProtectedBranch(
                GitLabSdk.of(gitLab).project("my-group/demo"), branchWithRules(rule(40, null, null, null, null)));

        assertSoftly(softly -> {
            softly.assertThat(sut.pushAccess()).containsExactly(ProtectedAccess.MAINTAINER);
            softly.assertThat(sut.pushAccessValues()).containsExactly(40);
        });
    }
}
