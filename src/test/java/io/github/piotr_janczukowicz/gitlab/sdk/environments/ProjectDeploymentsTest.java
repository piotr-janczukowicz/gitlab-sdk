package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.deployment;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.listedDeployment;
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
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsCreateStatus;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectDeploymentsTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void deployments_shouldLoadEachItem_whenListReturnsSmallerRecord() throws IOException {
        when(gitLab.deployments().list(ID).stream()).thenReturn(Stream.of(listedDeployment(1), listedDeployment(2)));
        when(gitLab.deployments().get(ID, 1L)).thenReturn(Optional.of(deployment(1, "success")));
        when(gitLab.deployments().get(ID, 2L)).thenReturn(Optional.of(deployment(2, "failed")));

        final List<Deployment> result = project.deployments().toList();

        assertThat(result).extracting(Deployment::id).containsExactly(1L, 2L);
        assertThat(result).extracting(Deployment::sha).containsOnly("abc123");
    }

    @Test
    void deployments_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.deployments().list(ID).stream())
                .thenReturn(Stream.of(listedDeployment(1), listedDeployment(2), listedDeployment(3)));
        when(gitLab.deployments().get(ID, 1L)).thenReturn(Optional.of(deployment(1, "success")));
        when(gitLab.deployments().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.deployments().get(ID, 3L)).thenReturn(Optional.of(deployment(3, "success")));

        final List<Deployment> result = project.deployments().toList();

        assertThat(result).extracting(Deployment::id).containsExactly(1L, 3L);
    }

    @Test
    void deployments_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.deployments().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return listedDeployment(id);
                }));
        when(gitLab.deployments().get(eq(ID), any(Long.class))).thenReturn(Optional.of(deployment(1, "success")));

        final Stream<Deployment> stream = project.deployments();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void deployments_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectDeploymentsListFilter filter = GitLabProjectDeploymentsListFilterBuilder.builder()
                .environment("prod")
                .build();
        when(gitLab.deployments().list(ID, filter).stream()).thenReturn(Stream.of(listedDeployment(1)));
        when(gitLab.deployments().get(ID, 1L)).thenReturn(Optional.of(deployment(1, "success")));

        final List<Deployment> result = project.deployments(filter).toList();

        assertThat(result).hasSize(1);
    }

    @Test
    void deployments_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> project.deployments(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.deployments());
    }

    @Test
    void deployment_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.deployments().get(ID, 9L)).thenReturn(Optional.empty());

        assertThat(project.deployment(9L)).isEmpty();
    }

    @Test
    void deployment_shouldReturnDeployment_whenPresent() throws IOException {
        when(gitLab.deployments().get(ID, 9L)).thenReturn(Optional.of(deployment(9, "running")));

        assertThat(project.deployment(9L)).get().satisfies(d -> {
            assertThat(d.id()).isEqualTo(9L);
            assertThat(d.project()).isSameAs(project);
        });
    }

    @Test
    void createDeployment_shouldSendStatus_whenStatusIsSettable() throws IOException {
        when(gitLab.deployments().create(eq(ID), any(GitLabProjectDeploymentsCreatePayload.class)))
                .thenReturn(deployment(10, "running"));

        final Deployment result = project.createDeployment("production", "abc123", "main", DeploymentStatus.RUNNING);

        final ArgumentCaptor<GitLabProjectDeploymentsCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeploymentsCreatePayload.class);
        verify(gitLab.deployments()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().environment()).isEqualTo("production");
            softly.assertThat(payload.getValue().sha()).isEqualTo("abc123");
            softly.assertThat(payload.getValue().ref()).isEqualTo("main");
            softly.assertThat(payload.getValue().tag()).isNull();
            softly.assertThat(payload.getValue().status()).isEqualTo(GitLabProjectDeploymentsCreateStatus.RUNNING);
            softly.assertThat(result.status()).isEqualTo(DeploymentStatus.RUNNING);
        });
    }

    @Test
    void createDeployment_shouldRejectUnsupportedStatus_withoutCoreCall() {
        final Throwable thrown = catchThrowable(
                () -> project.createDeployment("production", "abc123", "main", DeploymentStatus.BLOCKED));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("running");
        verifyNoInteractions(gitLab);
    }

    @Test
    void createDeployment_shouldThrowBeforeCoreCall_whenAnArgumentIsBlankOrNull() {
        final Throwable environment =
                catchThrowable(() -> project.createDeployment(" ", "abc123", "main", DeploymentStatus.RUNNING));
        final Throwable sha =
                catchThrowable(() -> project.createDeployment("production", "", "main", DeploymentStatus.RUNNING));
        final Throwable ref =
                catchThrowable(() -> project.createDeployment("production", "abc123", " ", DeploymentStatus.RUNNING));
        final Throwable nullEnvironment =
                catchThrowable(() -> project.createDeployment(null, "abc123", "main", DeploymentStatus.RUNNING));
        final Throwable nullStatus =
                catchThrowable(() -> project.createDeployment("production", "abc123", "main", null));

        assertSoftly(softly -> {
            softly.assertThat(environment).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sha).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(ref).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullEnvironment).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullStatus).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
