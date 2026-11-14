package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.deployment;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.environment;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.fullEnvironment;
import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.listedDeployment;
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
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabEnvironment;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsStopPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class EnvironmentTest {

    private static final String ID = "my-group/demo";
    private static final long ENV_ID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabEnvironment response = fullEnvironment();
    private final Environment sut = new Environment(project, response);

    @Test
    void accessors_shouldReturnValues_whenResponseIsFull() {
        assertSoftly(softly -> {
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.id()).isEqualTo(ENV_ID);
            softly.assertThat(sut.name()).isEqualTo("production");
            softly.assertThat(sut.slug()).isEqualTo("production-slug");
            softly.assertThat(sut.state()).isEqualTo("available");
            softly.assertThat(sut.externalUrl()).contains("https://example.com");
            softly.assertThat(sut.description()).contains("The live one");
            softly.assertThat(sut.kubernetesNamespace()).contains("prod-ns");
            softly.assertThat(sut.fluxResourcePath()).contains("path/to/flux");
            softly.assertThat(sut.tier()).contains(EnvironmentTier.PRODUCTION);
            softly.assertThat(sut.tierName()).contains("production");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenOptionalValuesAreAbsent() {
        final Environment bare = new Environment(project, environment(ENV_ID, "dev", null));

        assertSoftly(softly -> {
            softly.assertThat(bare.externalUrl()).isEmpty();
            softly.assertThat(bare.description()).isEmpty();
            softly.assertThat(bare.kubernetesNamespace()).isEmpty();
            softly.assertThat(bare.fluxResourcePath()).isEmpty();
            softly.assertThat(bare.tier()).isEmpty();
            softly.assertThat(bare.tierName()).isEmpty();
        });
    }

    @Test
    void tier_shouldThrowIllegalArgumentException_whenUnknown() {
        final Environment odd = new Environment(project, environment(ENV_ID, "dev", "qa"));

        final Throwable thrown = catchThrowable(odd::tier);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        assertThat(odd.tierName()).contains("qa");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new Environment(null, response)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new Environment(project, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void refresh_shouldReturnNewEnvironment_whenGitLabHasIt() throws IOException {
        when(gitLab.environments().get(ID, ENV_ID)).thenReturn(Optional.of(environment(ENV_ID, "production", "other")));

        final Environment result = sut.refresh();

        assertThat(result.tier()).contains(EnvironmentTier.OTHER);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.environments().get(ID, ENV_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void stop_shouldSendNoForce_whenCalledWithoutArgument() throws IOException {
        when(gitLab.environments().stop(eq(ID), eq(ENV_ID), any(GitLabProjectEnvironmentsStopPayload.class)))
                .thenReturn(environment(ENV_ID, "production", "production"));

        final Environment result = sut.stop();

        final ArgumentCaptor<GitLabProjectEnvironmentsStopPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectEnvironmentsStopPayload.class);
        verify(gitLab.environments()).stop(eq(ID), eq(ENV_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().force()).isNull();
            softly.assertThat(result.id()).isEqualTo(ENV_ID);
        });
    }

    @Test
    void stop_shouldSendForce_whenForceIsGiven() throws IOException {
        when(gitLab.environments().stop(eq(ID), eq(ENV_ID), any(GitLabProjectEnvironmentsStopPayload.class)))
                .thenReturn(environment(ENV_ID, "production", "production"));

        sut.stop(true);
        sut.stop(false);

        final ArgumentCaptor<GitLabProjectEnvironmentsStopPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectEnvironmentsStopPayload.class);
        verify(gitLab.environments(), org.mockito.Mockito.times(2)).stop(eq(ID), eq(ENV_ID), payload.capture());
        assertThat(payload.getAllValues())
                .extracting(GitLabProjectEnvironmentsStopPayload::force)
                .containsExactly(true, false);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        when(gitLab.environments().delete(ID, ENV_ID)).thenReturn(Optional.of(response));

        sut.delete();

        verify(gitLab.environments()).delete(ID, ENV_ID);
    }

    @Test
    void deployments_shouldFilterByNameAndLoadEachItem_whenListReturnsSmallerRecord() throws IOException {
        when(gitLab.deployments().list(eq(ID), any(GitLabProjectDeploymentsListFilter.class)).stream())
                .thenReturn(Stream.of(listedDeployment(1), listedDeployment(2)));
        when(gitLab.deployments().get(ID, 1L)).thenReturn(Optional.of(deployment(1, "success")));
        when(gitLab.deployments().get(ID, 2L)).thenReturn(Optional.of(deployment(2, "failed")));

        final List<Deployment> result = sut.deployments().toList();

        final ArgumentCaptor<GitLabProjectDeploymentsListFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectDeploymentsListFilter.class);
        verify(gitLab.deployments()).list(eq(ID), filter.capture());
        assertSoftly(softly -> {
            softly.assertThat(filter.getValue().environment()).isEqualTo("production");
            softly.assertThat(result).extracting(Deployment::id).containsExactly(1L, 2L);
            softly.assertThat(result)
                    .extracting(Deployment::status)
                    .containsExactly(DeploymentStatus.SUCCESS, DeploymentStatus.FAILED);
        });
    }
}
