package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabMergeRequestApprovalRuleBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class MergeRequestApprovalRuleTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;
    private static final long RULE_ID = 5L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest mergeRequest = new MergeRequest(
            GitLabSdk.of(gitLab).project(ID),
            GitLabMergeRequestBuilder.builder()
                    .iid(IID)
                    .title("t")
                    .state("opened")
                    .build());
    private final GitLabMergeRequestApprovalRule response = GitLabMergeRequestApprovalRuleBuilder.builder()
            .id(RULE_ID)
            .name("Security")
            .ruleType("regular")
            .approvalsRequired(2)
            .build();
    private final MergeRequestApprovalRule sut = new MergeRequestApprovalRule(mergeRequest, response);

    @Test
    void accessors_shouldReturnResponseValues_whenTheRuleIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(RULE_ID);
            softly.assertThat(sut.name()).contains("Security");
            softly.assertThat(sut.ruleType()).contains("regular");
            softly.assertThat(sut.approvalsRequired()).contains(2);
            softly.assertThat(sut.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabDoesNotSayAnything() {
        final MergeRequestApprovalRule bare = new MergeRequestApprovalRule(
                mergeRequest, GitLabMergeRequestApprovalRuleBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(bare.name()).isEmpty();
            softly.assertThat(bare.ruleType()).isEmpty();
            softly.assertThat(bare.approvalsRequired()).isEmpty();
        });
    }

    @Test
    void id_shouldThrowNullPointerException_whenGitLabReturnsNoId() {
        final MergeRequestApprovalRule bare = new MergeRequestApprovalRule(
                mergeRequest, GitLabMergeRequestApprovalRuleBuilder.builder().build());

        final Throwable thrown = catchThrowable(bare::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new MergeRequestApprovalRule(null, response)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("mergeRequest");
            softly.assertThat(catchThrowable(() -> new MergeRequestApprovalRule(mergeRequest, null)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("response");
        });
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.mergerequestapprovals()).deleteMergeRequestsApprovalRule(ID, IID, RULE_ID);
    }

    @Test
    void delete_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier = new GitLabTierException("op", GitLabTier.PREMIUM, GitLabTier.FREE);
        final var approvals = gitLab.mergerequestapprovals();
        doThrow(tier).when(approvals).deleteMergeRequestsApprovalRule(ID, IID, RULE_ID);

        final Throwable thrown = catchThrowable(sut::delete);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void refresh_shouldReturnTheRuleAsGitLabHoldsItNow_whenItExists() throws IOException {
        final GitLabMergeRequestApprovalRule newer = GitLabMergeRequestApprovalRuleBuilder.builder(response)
                .approvalsRequired(3)
                .build();
        when(gitLab.mergerequestapprovals().getMergeRequestsApprovalRule(ID, IID, RULE_ID))
                .thenReturn(Optional.of(newer));

        final MergeRequestApprovalRule refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed.response()).isSameAs(newer);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenTheRuleIsGone() throws IOException {
        when(gitLab.mergerequestapprovals().getMergeRequestsApprovalRule(ID, IID, RULE_ID))
                .thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Approval rule 5 is gone");
    }
}
