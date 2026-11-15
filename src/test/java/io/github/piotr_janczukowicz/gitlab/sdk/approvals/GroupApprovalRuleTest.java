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
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsUpdateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupApprovalRuleTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private GroupApprovalRule sut(final GitLabGroupApprovalRule response) {
        return new GroupApprovalRule(GitLabSdk.of(gitLab).group(ID), response);
    }

    private GitLabGroupGroupsUpdateApprovalRulePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupsUpdateApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupsUpdateApprovalRulePayload.class);
        verify(gitLab.groups()).updateApprovalRule(eq(ID), eq(3L), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.groups().updateApprovalRule(eq(ID), eq(3L), any(GitLabGroupGroupsUpdateApprovalRulePayload.class)))
                .thenReturn(fullGroupRule(3L));
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final GroupApprovalRule sut = sut(fullGroupRule(3L));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(3L);
            softly.assertThat(sut.group().id()).isEqualTo(ID);
            softly.assertThat(sut.name()).contains("security");
            softly.assertThat(sut.ruleType()).contains("regular");
            softly.assertThat(sut.approvalsRequired()).contains(2);
            softly.assertThat(sut.containsHiddenGroups()).isTrue();
            softly.assertThat(sut.appliesToAllProtectedBranches()).isTrue();
            softly.assertThat(sut.reportType()).contains("code_coverage");
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenResponseLeavesFieldsOut() {
        final GroupApprovalRule sut = sut(groupRule(3L));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.ruleType()).isEmpty();
            softly.assertThat(sut.approvalsRequired()).isEmpty();
            softly.assertThat(sut.containsHiddenGroups()).isFalse();
            softly.assertThat(sut.appliesToAllProtectedBranches()).isFalse();
            softly.assertThat(sut.reportType()).isEmpty();
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenNameAndCountAreSet() throws IOException {
        stubUpdate();

        final GroupApprovalRule result =
                sut(groupRule(3L)).update().name("sec").approvalsRequired(1).send();

        final GitLabGroupGroupsUpdateApprovalRulePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("sec");
            softly.assertThat(payload.approvalsRequired()).isEqualTo(1);
            softly.assertThat(payload.userIds()).isNull();
            softly.assertThat(payload.groupIds()).isNull();
            softly.assertThat(payload.ruleType()).isNull();
            softly.assertThat(result.id()).isEqualTo(3L);
        });
    }

    @Test
    void send_shouldSendApprovers_whenUsersAndGroupsAreSet() throws IOException {
        stubUpdate();

        sut(groupRule(3L)).update().userIds(List.of(1L, 2L)).groupIds(List.of()).send();

        final GitLabGroupGroupsUpdateApprovalRulePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.userIds()).containsExactly(1L, 2L);
            softly.assertThat(payload.groupIds()).isEmpty();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GroupApprovalRuleUpdate update = sut(groupRule(3L)).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void setters_shouldThrowOnBadArguments_whenNullBlankOrNegative() {
        final GroupApprovalRuleUpdate update = sut(groupRule(3L)).update();

        final Throwable blank = catchThrowable(() -> update.name(" "));
        final Throwable negative = catchThrowable(() -> update.approvalsRequired(-1));
        final Throwable nullIds = catchThrowable(() -> update.groupIds(null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(negative).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullIds).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabGroupApprovalRule record = groupRule(3L);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
