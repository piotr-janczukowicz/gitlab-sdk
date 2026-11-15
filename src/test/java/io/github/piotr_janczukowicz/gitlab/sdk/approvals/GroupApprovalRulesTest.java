package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullGroupRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.groupRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsCreateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupApprovalRulesTest {

    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(GROUP_ID);

    private GitLabGroupGroupsCreateApprovalRulePayload sentCreate() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupsCreateApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupsCreateApprovalRulePayload.class);
        verify(gitLab.groups()).createApprovalRule(eq(GROUP_ID), payload.capture());
        return payload.getValue();
    }

    private void stubCreate() throws IOException {
        when(gitLab.groups().createApprovalRule(eq(GROUP_ID), any(GitLabGroupGroupsCreateApprovalRulePayload.class)))
                .thenReturn(fullGroupRule(4L));
    }

    @Test
    void approvalRules_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listApprovalRules(GROUP_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return groupRule(id);
                }));

        final Stream<GroupApprovalRule> rules = group.approvalRules();

        assertThat(consumed).hasValue(0);
        assertThat(rules.limit(1).toList())
                .singleElement()
                .satisfies(r -> assertThat(r.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
    }

    @Test
    void createApprovalRule_shouldSendNameAndCountOnly_whenNoApproversAreGiven() throws IOException {
        stubCreate();

        final GroupApprovalRule result = group.createApprovalRule("security", 2);

        final GitLabGroupGroupsCreateApprovalRulePayload payload = sentCreate();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("security");
            softly.assertThat(payload.approvalsRequired()).isEqualTo(2);
            softly.assertThat(payload.ruleType()).isNull();
            softly.assertThat(payload.userIds()).isNull();
            softly.assertThat(payload.groupIds()).isNull();
            softly.assertThat(result.id()).isEqualTo(4L);
        });
    }

    @Test
    void createApprovalRule_shouldSendTheApprovers_whenApproversAreGiven() throws IOException {
        stubCreate();

        group.createApprovalRule("owners", 1, List.of(7L, 8L), List.of(9L));

        final GitLabGroupGroupsCreateApprovalRulePayload payload = sentCreate();
        assertSoftly(softly -> {
            softly.assertThat(payload.userIds()).containsExactly(7L, 8L);
            softly.assertThat(payload.groupIds()).containsExactly(9L);
        });
    }

    @Test
    void createApprovalRule_shouldThrowOnBadArguments_whenNullBlankOrNegative() {
        final Throwable blank = catchThrowable(() -> group.createApprovalRule(" ", 1));
        final Throwable negative = catchThrowable(() -> group.createApprovalRule("a", -1));
        final Throwable nullIds = catchThrowable(() -> group.createApprovalRule("a", 1, null, List.of()));
        final Throwable nullGroups = catchThrowable(() -> group.createApprovalRule("a", 1, List.of(), null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(negative).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullIds).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullGroups).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
