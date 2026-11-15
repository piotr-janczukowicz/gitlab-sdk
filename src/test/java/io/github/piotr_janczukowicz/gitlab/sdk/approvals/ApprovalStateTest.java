package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalState;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalStateBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalStateRule;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestApprovalStateRuleBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ApprovalStateTest {

    private final MergeRequest mergeRequest = new MergeRequest(
            GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)).project("p"),
            GitLabMergeRequestBuilder.builder()
                    .iid(1L)
                    .title("t")
                    .state("opened")
                    .build());

    @Test
    void accessors_shouldReturnResponseValues_whenTheStateIsLoaded() {
        final GitLabMergeRequestApprovalStateRule rule =
                GitLabMergeRequestApprovalStateRuleBuilder.builder().id(3L).build();
        final GitLabMergeRequestApprovalState response = GitLabMergeRequestApprovalStateBuilder.builder()
                .approvalRulesOverwritten(true)
                .rules(List.of(rule))
                .build();

        final ApprovalState sut = new ApprovalState(mergeRequest, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.approvalRulesOverwritten()).isTrue();
            softly.assertThat(sut.rules()).containsExactly(rule);
            softly.assertThat(sut.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldHaveNoRulesAndNoOverride_whenGitLabDoesNotSayAnything() {
        final ApprovalState sut = new ApprovalState(
                mergeRequest, GitLabMergeRequestApprovalStateBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.approvalRulesOverwritten()).isFalse();
            softly.assertThat(sut.rules()).isEmpty();
        });
    }

    @Test
    void rules_shouldReturnAnUnmodifiableCopy_whenTheResponseListIsMutable() {
        final List<GitLabMergeRequestApprovalStateRule> rules = new ArrayList<>(List.of(
                GitLabMergeRequestApprovalStateRuleBuilder.builder().id(1L).build()));
        final ApprovalState sut = new ApprovalState(
                mergeRequest,
                GitLabMergeRequestApprovalStateBuilder.builder().rules(rules).build());

        final List<GitLabMergeRequestApprovalStateRule> copy = sut.rules();
        rules.clear();

        assertSoftly(softly -> {
            softly.assertThat(copy).hasSize(1);
            softly.assertThatThrownBy(copy::clear).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        final GitLabMergeRequestApprovalState response =
                GitLabMergeRequestApprovalStateBuilder.builder().build();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new ApprovalState(null, response)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("mergeRequest");
            softly.assertThat(catchThrowable(() -> new ApprovalState(mergeRequest, null)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("response");
        });
    }
}
