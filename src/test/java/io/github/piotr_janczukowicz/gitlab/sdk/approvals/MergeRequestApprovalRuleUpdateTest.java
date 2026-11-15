package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRuleBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestApprovalRuleUpdateTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;
    private static final long RULE_ID = 5L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequestApprovalRule rule = rule(gitLab);

    private static MergeRequestApprovalRule rule(final GitLab client) {
        final MergeRequest mergeRequest = new MergeRequest(
                GitLabSdk.of(client).project(ID),
                GitLabMergeRequestBuilder.builder()
                        .iid(IID)
                        .title("t")
                        .state("opened")
                        .build());
        return new MergeRequestApprovalRule(
                mergeRequest,
                GitLabMergeRequestApprovalRuleBuilder.builder()
                        .id(RULE_ID)
                        .name("old")
                        .build());
    }

    @Test
    void send_shouldSendOnlySetFields_whenNameAndApprovalsRequiredAreSet() throws IOException {
        final GitLabMergeRequestApprovalRule updated = GitLabMergeRequestApprovalRuleBuilder.builder()
                .id(RULE_ID)
                .name("new")
                .build();
        when(gitLab.mergerequestapprovals()
                        .updateMergeRequestsApprovalRule(
                                eq(ID),
                                eq(IID),
                                eq(RULE_ID),
                                any(GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload.class)))
                .thenReturn(updated);

        final MergeRequestApprovalRule result =
                rule.update().name("new").approvalsRequired(2).send();

        final ArgumentCaptor<GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload.class);
        verify(gitLab.mergerequestapprovals())
                .updateMergeRequestsApprovalRule(eq(ID), eq(IID), eq(RULE_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("new");
            softly.assertThat(payload.getValue().approvalsRequired()).isEqualTo(2);
            softly.assertThat(payload.getValue().userIds()).isNull();
            softly.assertThat(payload.getValue().groupIds()).isNull();
            softly.assertThat(payload.getValue().usernames()).isNull();
            softly.assertThat(payload.getValue().removeHiddenGroups()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
        });
    }

    @Test
    void send_shouldSendApproversAndGroups_whenSet() throws IOException {
        when(gitLab.mergerequestapprovals()
                        .updateMergeRequestsApprovalRule(
                                eq(ID),
                                eq(IID),
                                eq(RULE_ID),
                                any(GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload.class)))
                .thenReturn(GitLabMergeRequestApprovalRuleBuilder.builder().build());

        rule.update()
                .users(List.of(1L))
                .groups(List.of(2L))
                .usernames(List.of("alice"))
                .removeHiddenGroups(true)
                .send();

        final ArgumentCaptor<GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload.class);
        verify(gitLab.mergerequestapprovals())
                .updateMergeRequestsApprovalRule(eq(ID), eq(IID), eq(RULE_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().userIds()).containsExactly(1L);
            softly.assertThat(payload.getValue().groupIds()).containsExactly(2L);
            softly.assertThat(payload.getValue().usernames()).containsExactly("alice");
            softly.assertThat(payload.getValue().removeHiddenGroups()).isTrue();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GitLab untouched = mock(GitLab.class);
        final MergeRequestApprovalRule other = rule(untouched);

        final Throwable thrown = catchThrowable(() -> other.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(untouched);
    }

    @Test
    void send_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier = new GitLabTierException("op", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.mergerequestapprovals()
                        .updateMergeRequestsApprovalRule(
                                eq(ID),
                                eq(IID),
                                eq(RULE_ID),
                                any(GitLabProjectMergeRequestApprovalsUpdateMergeRequestsApprovalRulePayload.class)))
                .thenThrow(tier);
        final MergeRequestApprovalRuleUpdate update = rule.update().name("new");

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void setters_shouldRejectBadArguments_whenGivenBlankOrNullOrNegativeValues() {
        final MergeRequestApprovalRuleUpdate update = rule.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> update.name(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> update.approvalsRequired(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> update.users(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.groups(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.usernames(List.of(" "))))
                    .isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRuleIsNull() {
        final Throwable thrown = catchThrowable(() -> new MergeRequestApprovalRuleUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("rule");
    }
}
