package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.approvals;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestApprovals;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestApprovalsBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ApprovalsTest {

    private final MergeRequest mergeRequest =
            new MergeRequest(GitLabSdk.of(mock(GitLab.class)).project("42"), mergeRequest(7L, "Fix", "opened"));

    @Test
    void accessors_shouldReturnResponseValues_whenApprovalsAreLoaded() {
        final GitLabMergeRequestApprovals response = GitLabMergeRequestApprovalsBuilder.builder()
                .approved(true)
                .approvalsLeft(0)
                .approvalsRequired(2)
                .userHasApproved(true)
                .build();

        final Approvals sut = new Approvals(mergeRequest, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.approved()).contains(true);
            softly.assertThat(sut.approvalsLeft()).contains(0);
            softly.assertThat(sut.approvalsRequired()).contains(2);
            softly.assertThat(sut.userHasApproved()).contains(true);
            softly.assertThat(sut.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabDoesNotSayAnything() {
        final Approvals sut = new Approvals(
                mergeRequest, GitLabMergeRequestApprovalsBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.approved()).isEmpty();
            softly.assertThat(sut.approvalsLeft()).isEmpty();
            softly.assertThat(sut.approvalsRequired()).isEmpty();
            softly.assertThat(sut.userHasApproved()).isEmpty();
        });
    }

    @Test
    void accessors_shouldReflectApprovalLeft_whenNotYetApproved() {
        final Approvals sut = new Approvals(mergeRequest, approvals(false, 1));

        assertSoftly(softly -> {
            softly.assertThat(sut.approved()).contains(false);
            softly.assertThat(sut.approvalsLeft()).contains(1);
        });
    }
}
