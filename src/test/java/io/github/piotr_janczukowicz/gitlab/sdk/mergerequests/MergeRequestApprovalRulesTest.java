package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRuleBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalState;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalStateBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.ApprovalState;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.MergeRequestApprovalRule;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestApprovalRulesTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final MergeRequest sut = new MergeRequest(project, mergeRequest(IID, "Fix", "opened"));

    @Test
    void approvalState_shouldWrapResponse_whenGitLabHasIt() throws IOException {
        final GitLabMergeRequestApprovalState state =
                GitLabMergeRequestApprovalStateBuilder.builder().build();
        when(gitLab.mergerequests().project().getApprovalState(ID, IID)).thenReturn(Optional.of(state));

        final Optional<ApprovalState> found = sut.approvalState();

        assertThat(found)
                .hasValueSatisfying(value -> assertSoftly(softly -> {
                    softly.assertThat(value.response()).isSameAs(state);
                    softly.assertThat(value.mergeRequest()).isSameAs(sut);
                }));
    }

    @Test
    void approvalState_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequests().project().getApprovalState(ID, IID)).thenReturn(Optional.empty());

        final Optional<ApprovalState> found = sut.approvalState();

        assertThat(found).isEmpty();
    }

    @Test
    void approvalState_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier = tierException();
        when(gitLab.mergerequests().project().getApprovalState(ID, IID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(sut::approvalState);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void approvalRules_shouldWrapEachRuleWithItsMergeRequest_whenRulesExist() throws IOException {
        when(gitLab.mergerequestapprovals().listMergeRequestsApprovalRules(ID, IID).stream())
                .thenReturn(Stream.of(rule(1L, "a"), rule(2L, "b")));

        final List<MergeRequestApprovalRule> rules = sut.approvalRules().toList();

        assertSoftly(softly -> {
            softly.assertThat(rules).extracting(MergeRequestApprovalRule::id).containsExactly(1L, 2L);
            softly.assertThat(rules)
                    .extracting(MergeRequestApprovalRule::mergeRequest)
                    .containsOnly(sut);
        });
    }

    @Test
    void approvalRules_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.mergerequestapprovals().listMergeRequestsApprovalRules(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return rule(id, "r");
                }));

        final Stream<MergeRequestApprovalRule> rules = sut.approvalRules();

        assertThat(consumed).hasValue(0);
        assertThat(rules.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void approvalRules_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier = tierException();
        when(gitLab.mergerequestapprovals().listMergeRequestsApprovalRules(ID, IID))
                .thenThrow(tier);

        final Throwable thrown = catchThrowable(sut::approvalRules);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void approvalRule_shouldWrapResponse_whenGitLabHasIt() throws IOException {
        final GitLabMergeRequestApprovalRule rule = rule(5L, "a");
        when(gitLab.mergerequestapprovals().getMergeRequestsApprovalRule(ID, IID, 5L))
                .thenReturn(Optional.of(rule));

        final Optional<MergeRequestApprovalRule> found = sut.approvalRule(5L);

        assertThat(found)
                .hasValueSatisfying(value -> assertThat(value.response()).isSameAs(rule));
    }

    @Test
    void approvalRule_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequestapprovals().getMergeRequestsApprovalRule(ID, IID, 5L))
                .thenReturn(Optional.empty());

        final Optional<MergeRequestApprovalRule> found = sut.approvalRule(5L);

        assertThat(found).isEmpty();
    }

    @Test
    void approvalRule_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier = tierException();
        when(gitLab.mergerequestapprovals().getMergeRequestsApprovalRule(ID, IID, 5L))
                .thenThrow(tier);

        final Throwable thrown = catchThrowable(() -> sut.approvalRule(5L));

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void createApprovalRule_shouldSendNameAndApprovalsRequired_whenGivenBoth() throws IOException {
        final GitLabMergeRequestApprovalRule created = rule(9L, "Security");
        when(gitLab.mergerequestapprovals()
                        .createMergeRequestsApprovalRule(
                                eq(ID),
                                eq(IID),
                                any(GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload.class)))
                .thenReturn(created);

        final MergeRequestApprovalRule result = sut.createApprovalRule("Security", 2);

        final ArgumentCaptor<GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload.class);
        verify(gitLab.mergerequestapprovals()).createMergeRequestsApprovalRule(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("Security");
            softly.assertThat(payload.getValue().approvalsRequired()).isEqualTo(2);
            softly.assertThat(payload.getValue().userIds()).isNull();
            softly.assertThat(result.response()).isSameAs(created);
        });
    }

    @Test
    void createApprovalRule_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createApprovalRule(" ", 1));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void createApprovalRule_shouldThrowIllegalArgumentException_whenApprovalsRequiredIsNegative() {
        final Throwable thrown = catchThrowable(() -> sut.createApprovalRule("Security", -1));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("approvalsRequired");
    }

    @Test
    void createApprovalRule_shouldSendThePayload_whenGivenTheCorePayload() throws IOException {
        final GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload payload =
                GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayloadBuilder.builder()
                        .name("Security")
                        .approvalsRequired(1)
                        .userIds(List.of(3L))
                        .build();
        final GitLabMergeRequestApprovalRule created = rule(9L, "Security");
        when(gitLab.mergerequestapprovals().createMergeRequestsApprovalRule(ID, IID, payload))
                .thenReturn(created);

        final MergeRequestApprovalRule result = sut.createApprovalRule(payload);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void createApprovalRule_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createApprovalRule(
                (GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("payload");
    }

    @Test
    void createApprovalRule_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier = tierException();
        when(gitLab.mergerequestapprovals()
                        .createMergeRequestsApprovalRule(
                                eq(ID),
                                eq(IID),
                                any(GitLabProjectMergeRequestApprovalsCreateMergeRequestsApprovalRulePayload.class)))
                .thenThrow(tier);

        final Throwable thrown = catchThrowable(() -> sut.createApprovalRule("Security", 1));

        assertThat(thrown).isSameAs(tier);
    }

    private static GitLabMergeRequestApprovalRule rule(final long id, final String name) {
        return GitLabMergeRequestApprovalRuleBuilder.builder().id(id).name(name).build();
    }

    private static GitLabTierException tierException() {
        return new GitLabTierException("approvalRules", GitLabTier.PREMIUM, GitLabTier.FREE);
    }
}
