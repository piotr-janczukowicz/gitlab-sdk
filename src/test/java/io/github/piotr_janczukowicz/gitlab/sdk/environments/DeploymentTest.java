package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.approval;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.deployment;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.mergeRequestBasic;
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
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentApproval;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentExtended;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsApprovalPayload;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsApprovalStatus;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsUpdateStatus;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class DeploymentTest {

    private static final String ID = "my-group/demo";
    private static final long DEPLOYMENT_ID = 9L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabDeploymentExtended response = deployment(DEPLOYMENT_ID, "running");
    private final Deployment sut = new Deployment(project, response);

    @Test
    void accessors_shouldReturnValues_whenResponseIsFull() {
        assertSoftly(softly -> {
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.id()).isEqualTo(DEPLOYMENT_ID);
            softly.assertThat(sut.iid()).isEqualTo(109L);
            softly.assertThat(sut.ref()).isEqualTo("main");
            softly.assertThat(sut.sha()).isEqualTo("abc123");
            softly.assertThat(sut.status()).isEqualTo(DeploymentStatus.RUNNING);
            softly.assertThat(sut.statusName()).isEqualTo("running");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void status_shouldThrowIllegalArgumentException_whenUnknown() {
        final Deployment odd = new Deployment(project, deployment(DEPLOYMENT_ID, "paused"));

        final Throwable thrown = catchThrowable(odd::status);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        assertThat(odd.statusName()).isEqualTo("paused");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new Deployment(null, response)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new Deployment(project, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void refresh_shouldReturnNewDeployment_whenGitLabHasIt() throws IOException {
        when(gitLab.deployments().get(ID, DEPLOYMENT_ID)).thenReturn(Optional.of(deployment(DEPLOYMENT_ID, "success")));

        final Deployment result = sut.refresh();

        assertThat(result.status()).isEqualTo(DeploymentStatus.SUCCESS);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.deployments().get(ID, DEPLOYMENT_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("9");
    }

    @Test
    void updateStatus_shouldSendStatusAndReturnAnswer_whenStatusIsSettable() throws IOException {
        when(gitLab.deployments().update(eq(ID), eq(DEPLOYMENT_ID), any(GitLabProjectDeploymentsUpdatePayload.class)))
                .thenReturn(deployment(DEPLOYMENT_ID, "success"));

        final Deployment result = sut.updateStatus(DeploymentStatus.SUCCESS);

        final ArgumentCaptor<GitLabProjectDeploymentsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeploymentsUpdatePayload.class);
        verify(gitLab.deployments()).update(eq(ID), eq(DEPLOYMENT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().status()).isEqualTo(GitLabProjectDeploymentsUpdateStatus.SUCCESS);
            softly.assertThat(result.status()).isEqualTo(DeploymentStatus.SUCCESS);
        });
    }

    @Test
    void refresh_shouldLoadTheDeployment_whenChainedAfterUpdateStatus() throws IOException {
        when(gitLab.deployments().update(eq(ID), eq(DEPLOYMENT_ID), any(GitLabProjectDeploymentsUpdatePayload.class)))
                .thenReturn(deployment(DEPLOYMENT_ID, "success"));
        when(gitLab.deployments().get(ID, DEPLOYMENT_ID)).thenReturn(Optional.of(deployment(DEPLOYMENT_ID, "failed")));

        final Deployment result = sut.updateStatus(DeploymentStatus.SUCCESS).refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(DEPLOYMENT_ID);
            softly.assertThat(result.status()).isEqualTo(DeploymentStatus.FAILED);
        });
        verify(gitLab.deployments()).get(ID, DEPLOYMENT_ID);
    }

    @Test
    void updateStatus_shouldRejectUnsupportedStatus_withoutCoreCall() {
        final Throwable created = catchThrowable(() -> sut.updateStatus(DeploymentStatus.CREATED));
        final Throwable skipped = catchThrowable(() -> sut.updateStatus(DeploymentStatus.SKIPPED));
        final Throwable blocked = catchThrowable(() -> sut.updateStatus(DeploymentStatus.BLOCKED));
        final Throwable nothing = catchThrowable(() -> sut.updateStatus(null));

        assertSoftly(softly -> {
            softly.assertThat(created).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(skipped).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blocked).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nothing).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void approve_shouldSendApprovedWithComment_whenCommentGiven() throws IOException {
        final GitLabDeploymentApproval answer = approval("approved");
        when(gitLab.deployments()
                        .approval(eq(ID), eq(DEPLOYMENT_ID), any(GitLabProjectDeploymentsApprovalPayload.class)))
                .thenReturn(answer);

        final GitLabDeploymentApproval result = sut.approve("ship it");

        final ArgumentCaptor<GitLabProjectDeploymentsApprovalPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeploymentsApprovalPayload.class);
        verify(gitLab.deployments()).approval(eq(ID), eq(DEPLOYMENT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().status()).isEqualTo(GitLabProjectDeploymentsApprovalStatus.APPROVED);
            softly.assertThat(payload.getValue().comment()).isEqualTo("ship it");
            softly.assertThat(result).isSameAs(answer);
        });
    }

    @Test
    void reject_shouldSendRejectedWithoutComment_whenCommentIsNull() throws IOException {
        final GitLabDeploymentApproval answer = approval("rejected");
        when(gitLab.deployments()
                        .approval(eq(ID), eq(DEPLOYMENT_ID), any(GitLabProjectDeploymentsApprovalPayload.class)))
                .thenReturn(answer);

        final GitLabDeploymentApproval result = sut.reject(null);

        final ArgumentCaptor<GitLabProjectDeploymentsApprovalPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeploymentsApprovalPayload.class);
        verify(gitLab.deployments()).approval(eq(ID), eq(DEPLOYMENT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().status()).isEqualTo(GitLabProjectDeploymentsApprovalStatus.REJECTED);
            softly.assertThat(payload.getValue().comment()).isNull();
            softly.assertThat(result).isSameAs(answer);
        });
    }

    @Test
    void approve_shouldThrowIllegalArgumentException_whenCommentIsBlank() {
        final Throwable approve = catchThrowable(() -> sut.approve(" "));
        final Throwable reject = catchThrowable(() -> sut.reject(""));

        assertSoftly(softly -> {
            softly.assertThat(approve).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(reject).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void approve_shouldPassTierException_whenCoreRefuses() throws IOException {
        final GitLabTierException tier = new GitLabTierException("approval", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.deployments()
                        .approval(eq(ID), eq(DEPLOYMENT_ID), any(GitLabProjectDeploymentsApprovalPayload.class)))
                .thenThrow(tier);

        final Throwable thrown = catchThrowable(() -> sut.approve("ok"));

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void mergeRequests_shouldLoadThroughOwnProject_whenSummaryNamesNoProjectOrTheOwningOne() throws IOException {
        when(gitLab.deployments().listMergeRequests(ID, DEPLOYMENT_ID).stream())
                .thenReturn(
                        Stream.of(mergeRequestBasic(1, null), mergeRequestBasic(2, null), mergeRequestBasic(3, null)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(1L), any())).thenReturn(Optional.of(mergeRequest(1)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(2L), any())).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get(eq(ID), eq(3L), any())).thenReturn(Optional.of(mergeRequest(3)));
        final Project numeric = GitLabSdk.of(gitLab).project("42");
        final Deployment inNumeric = new Deployment(numeric, response);
        when(gitLab.deployments().listMergeRequests("42", DEPLOYMENT_ID).stream())
                .thenReturn(Stream.of(mergeRequestBasic(2, 42L)));
        when(gitLab.mergerequests().project().get(eq("42"), eq(2L), any())).thenReturn(Optional.of(mergeRequest(2)));

        final List<MergeRequest> result = sut.mergeRequests().toList();
        final List<MergeRequest> viaNumeric = inNumeric.mergeRequests().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 3L);
            softly.assertThat(viaNumeric).singleElement().satisfies(found -> {
                assertThat(found.iid()).isEqualTo(2L);
                assertThat(found.project()).isSameAs(numeric);
            });
        });
    }

    @Test
    void mergeRequests_shouldLoadThroughOtherProject_whenSummaryNamesAnotherProject() throws IOException {
        when(gitLab.deployments().listMergeRequests(ID, DEPLOYMENT_ID).stream())
                .thenReturn(Stream.of(mergeRequestBasic(5, 99L)));
        when(gitLab.mergerequests().project().get(eq("99"), eq(5L), any())).thenReturn(Optional.of(mergeRequest(5)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result)
                .singleElement()
                .satisfies(found -> assertThat(found.project().id()).isEqualTo("99"));
    }

    @Test
    void mergeRequests_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.deployments().listMergeRequests(ID, DEPLOYMENT_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return mergeRequestBasic(iid, null);
                }));
        when(gitLab.mergerequests().project().get(eq(ID), any(Long.class), any()))
                .thenReturn(Optional.of(mergeRequest(1)));

        final Stream<MergeRequest> stream = sut.mergeRequests();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.deployments()).delete(ID, DEPLOYMENT_ID);
    }
}
