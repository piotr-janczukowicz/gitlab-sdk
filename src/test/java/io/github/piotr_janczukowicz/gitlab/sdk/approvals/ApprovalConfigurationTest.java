package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullSettings;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.settings;
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
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabApprovalSettings;
import io.github.piotr_janczukowicz.gitlab.api.mergerequestapprovals.GitLabProjectMergeRequestApprovalsCreateApprovalsPayload;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ApprovalConfigurationTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private ApprovalConfiguration sut(final GitLabApprovalSettings response) {
        return new ApprovalConfiguration(GitLabSdk.of(gitLab).project(ID), response);
    }

    private GitLabProjectMergeRequestApprovalsCreateApprovalsPayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabProjectMergeRequestApprovalsCreateApprovalsPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestApprovalsCreateApprovalsPayload.class);
        verify(gitLab.mergerequestapprovals()).createApprovals(eq(ID), payload.capture());
        return payload.getValue();
    }

    @Test
    void accessors_shouldReadResponse_whenAllFlagsAreSet() {
        final ApprovalConfiguration sut = sut(fullSettings());

        assertSoftly(softly -> {
            softly.assertThat(sut.project().id()).isEqualTo(ID);
            softly.assertThat(sut.resetApprovalsOnPush()).isTrue();
            softly.assertThat(sut.selectiveCodeOwnerRemovals()).isTrue();
            softly.assertThat(sut.disableOverridingApproversPerMergeRequest()).isTrue();
            softly.assertThat(sut.mergeRequestsAuthorApproval()).isTrue();
            softly.assertThat(sut.mergeRequestsDisableCommittersApproval()).isTrue();
            softly.assertThat(sut.requireReauthenticationToApprove()).isTrue();
        });
    }

    @Test
    void send_shouldSendOnlySetFieldsAndNeverTheDeprecatedOnes_whenOneFlagIsSet() throws IOException {
        when(gitLab.mergerequestapprovals()
                        .createApprovals(eq(ID), any(GitLabProjectMergeRequestApprovalsCreateApprovalsPayload.class)))
                .thenReturn(fullSettings());

        final ApprovalConfiguration result =
                sut(settings()).update().resetApprovalsOnPush(true).send();

        final GitLabProjectMergeRequestApprovalsCreateApprovalsPayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.resetApprovalsOnPush()).isTrue();
            softly.assertThat(payload.selectiveCodeOwnerRemovals()).isNull();
            softly.assertThat(payload.disableOverridingApproversPerMergeRequest())
                    .isNull();
            softly.assertThat(payload.mergeRequestsAuthorApproval()).isNull();
            softly.assertThat(payload.mergeRequestsDisableCommittersApproval()).isNull();
            softly.assertThat(payload.requireReauthenticationToApprove()).isNull();
            softly.assertThat(payload.approvalsBeforeMerge()).isNull();
            softly.assertThat(payload.requirePasswordToApprove()).isNull();
            softly.assertThat(result.resetApprovalsOnPush()).isTrue();
        });
    }

    @Test
    void send_shouldSendEveryFlag_whenAllAreSet() throws IOException {
        when(gitLab.mergerequestapprovals()
                        .createApprovals(eq(ID), any(GitLabProjectMergeRequestApprovalsCreateApprovalsPayload.class)))
                .thenReturn(settings());

        sut(settings())
                .update()
                .resetApprovalsOnPush(false)
                .selectiveCodeOwnerRemovals(true)
                .disableOverridingApproversPerMergeRequest(false)
                .mergeRequestsAuthorApproval(true)
                .mergeRequestsDisableCommittersApproval(false)
                .requireReauthenticationToApprove(true)
                .send();

        final GitLabProjectMergeRequestApprovalsCreateApprovalsPayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.resetApprovalsOnPush()).isFalse();
            softly.assertThat(payload.selectiveCodeOwnerRemovals()).isTrue();
            softly.assertThat(payload.disableOverridingApproversPerMergeRequest())
                    .isFalse();
            softly.assertThat(payload.mergeRequestsAuthorApproval()).isTrue();
            softly.assertThat(payload.mergeRequestsDisableCommittersApproval()).isFalse();
            softly.assertThat(payload.requireReauthenticationToApprove()).isTrue();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final ApprovalConfigurationUpdate update = sut(settings()).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void refresh_shouldReturnLoadedConfiguration_whenPresent() throws IOException {
        when(gitLab.mergerequestapprovals().getApprovals(ID)).thenReturn(fullSettings());

        final ApprovalConfiguration refreshed = sut(settings()).refresh();

        assertThat(refreshed.selectiveCodeOwnerRemovals()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.mergerequestapprovals().getApprovals(ID)).thenThrow(mock(GitLabNotFoundException.class));
        final ApprovalConfiguration sut = sut(settings());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void response_shouldReturnRecordWithTheDeprecatedFields_whenCalled() {
        final GitLabApprovalSettings record = fullSettings();

        assertThat(sut(record).response()).isSameAs(record);
    }
}
