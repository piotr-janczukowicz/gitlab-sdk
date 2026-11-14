package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.environment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsUpdateTier;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class EnvironmentUpdateTest {

    private static final String ID = "my-group/demo";
    private static final long ENV_ID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Environment sut =
            new Environment(GitLabSdk.of(gitLab).project(ID), environment(ENV_ID, "production", "production"));

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final EnvironmentUpdate update = sut.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void send_shouldSendOnlySetFields_whenOnlyTierIsSet() throws IOException {
        when(gitLab.environments().update(eq(ID), eq(ENV_ID), any(GitLabProjectEnvironmentsUpdatePayload.class)))
                .thenReturn(environment(ENV_ID, "production", "staging"));

        final Environment result = sut.update().tier(EnvironmentTier.STAGING).send();

        final ArgumentCaptor<GitLabProjectEnvironmentsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectEnvironmentsUpdatePayload.class);
        verify(gitLab.environments()).update(eq(ID), eq(ENV_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().tier()).isEqualTo(GitLabProjectEnvironmentsUpdateTier.STAGING);
            softly.assertThat(payload.getValue().externalUrl()).isNull();
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(payload.getValue().kubernetesNamespace()).isNull();
            softly.assertThat(payload.getValue().fluxResourcePath()).isNull();
            softly.assertThat(payload.getValue().clusterAgentId()).isNull();
            softly.assertThat(payload.getValue().autoStopSetting()).isNull();
            softly.assertThat(result.tier()).contains(EnvironmentTier.STAGING);
        });
        verify(gitLab.environments(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void send_shouldSendEveryField_whenAllAreSet() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload = send(u -> u.externalUrl("https://e.example")
                .description("d")
                .tier(EnvironmentTier.TESTING)
                .kubernetesNamespace("ns")
                .fluxResourcePath("flux")
                .clusterAgentId(5L));

        assertSoftly(softly -> {
            softly.assertThat(payload.externalUrl()).isEqualTo("https://e.example");
            softly.assertThat(payload.description()).isEqualTo("d");
            softly.assertThat(payload.tier()).isEqualTo(GitLabProjectEnvironmentsUpdateTier.TESTING);
            softly.assertThat(payload.kubernetesNamespace()).isEqualTo(OptionalNull.of("ns"));
            softly.assertThat(payload.fluxResourcePath()).isEqualTo(OptionalNull.of("flux"));
            softly.assertThat(payload.clusterAgentId()).isEqualTo(OptionalNull.of(5L));
        });
    }

    @Test
    void clearKubernetesNamespace_shouldSendJsonNull_whenCalled() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload = send(EnvironmentUpdate::clearKubernetesNamespace);

        assertSoftly(softly -> {
            softly.assertThat(payload.kubernetesNamespace()).isEqualTo(OptionalNull.ofNull());
            softly.assertThat(json(payload)).contains("\"kubernetes_namespace\":null");
            softly.assertThat(json(payload)).doesNotContain("flux_resource_path", "cluster_agent_id", "tier");
        });
    }

    @Test
    void clearFluxResourcePath_shouldSendJsonNull_whenCalled() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload = send(EnvironmentUpdate::clearFluxResourcePath);

        assertSoftly(softly -> {
            softly.assertThat(payload.fluxResourcePath()).isEqualTo(OptionalNull.ofNull());
            softly.assertThat(json(payload)).contains("\"flux_resource_path\":null");
            softly.assertThat(json(payload)).doesNotContain("kubernetes_namespace");
        });
    }

    @Test
    void clearClusterAgent_shouldSendJsonNull_whenCalled() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload = send(EnvironmentUpdate::clearClusterAgent);

        assertSoftly(softly -> {
            softly.assertThat(payload.clusterAgentId()).isEqualTo(OptionalNull.ofNull());
            softly.assertThat(json(payload)).contains("\"cluster_agent_id\":null");
        });
    }

    @Test
    void wire_shouldContainValues_whenFieldsAreSet() throws IOException {
        final String json = json(send(u -> u.kubernetesNamespace("ns").clusterAgentId(5L)));

        assertThat(json).contains("\"kubernetes_namespace\":\"ns\"").contains("\"cluster_agent_id\":5");
    }

    @Test
    void clearKubernetesNamespace_shouldMarkUpdate_soSendIsNotEmpty() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload = send(EnvironmentUpdate::clearKubernetesNamespace);

        assertThat(payload).isNotNull();
    }

    @Test
    void kubernetesNamespace_shouldWin_whenCalledAfterClear() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload =
                send(u -> u.clearKubernetesNamespace().kubernetesNamespace("ns"));

        assertThat(payload.kubernetesNamespace()).isEqualTo(OptionalNull.of("ns"));
    }

    @Test
    void clearKubernetesNamespace_shouldWin_whenCalledAfterSet() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload =
                send(u -> u.kubernetesNamespace("ns").clearKubernetesNamespace());

        assertThat(payload.kubernetesNamespace()).isEqualTo(OptionalNull.ofNull());
    }

    @Test
    void fluxResourcePath_shouldWinOverClear_whenCalledLast() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload afterClear =
                send(u -> u.clearFluxResourcePath().fluxResourcePath("p"));

        assertThat(afterClear.fluxResourcePath()).isEqualTo(OptionalNull.of("p"));
    }

    @Test
    void clearFluxResourcePath_shouldWin_whenCalledAfterSet() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload =
                send(u -> u.fluxResourcePath("p").clearFluxResourcePath());

        assertThat(payload.fluxResourcePath()).isEqualTo(OptionalNull.ofNull());
    }

    @Test
    void clusterAgentId_shouldWin_whenCalledAfterClear() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload =
                send(u -> u.clearClusterAgent().clusterAgentId(9L));

        assertThat(payload.clusterAgentId()).isEqualTo(OptionalNull.of(9L));
    }

    @Test
    void clearClusterAgent_shouldWin_whenCalledAfterSet() throws IOException {
        final GitLabProjectEnvironmentsUpdatePayload payload =
                send(u -> u.clusterAgentId(9L).clearClusterAgent());

        assertThat(payload.clusterAgentId()).isEqualTo(OptionalNull.ofNull());
    }

    @Test
    void wire_shouldOmitClearableKeys_whenOnlyDescriptionIsSet() throws IOException {
        final String json = json(send(u -> u.description("d")));

        assertThat(json)
                .contains("\"description\":\"d\"")
                .doesNotContain("kubernetes_namespace", "flux_resource_path", "cluster_agent_id");
    }

    @Test
    void methods_shouldThrowNullPointerException_whenArgumentIsNull() {
        final EnvironmentUpdate update = sut.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> update.externalUrl(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.tier(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.kubernetesNamespace(null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.fluxResourcePath(null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    private GitLabProjectEnvironmentsUpdatePayload send(final UnaryOperator<EnvironmentUpdate> change)
            throws IOException {
        when(gitLab.environments().update(eq(ID), eq(ENV_ID), any(GitLabProjectEnvironmentsUpdatePayload.class)))
                .thenReturn(environment(ENV_ID, "production", "production"));
        change.apply(sut.update()).send();
        final ArgumentCaptor<GitLabProjectEnvironmentsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectEnvironmentsUpdatePayload.class);
        verify(gitLab.environments()).update(eq(ID), eq(ENV_ID), payload.capture());
        return payload.getValue();
    }

    private static String json(final Object payload) {
        return new String(GitLabJsonCodecs.defaults().encode(payload), StandardCharsets.UTF_8);
    }
}
