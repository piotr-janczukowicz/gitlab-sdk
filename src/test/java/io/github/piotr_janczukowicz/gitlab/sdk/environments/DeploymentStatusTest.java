package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsCreateStatus;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsListStatus;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsUpdateStatus;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class DeploymentStatusTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(DeploymentStatus.from("created")).isEqualTo(DeploymentStatus.CREATED);
        assertThat(DeploymentStatus.from("running")).isEqualTo(DeploymentStatus.RUNNING);
        assertThat(DeploymentStatus.from("success")).isEqualTo(DeploymentStatus.SUCCESS);
        assertThat(DeploymentStatus.from("failed")).isEqualTo(DeploymentStatus.FAILED);
        assertThat(DeploymentStatus.from("canceled")).isEqualTo(DeploymentStatus.CANCELED);
        assertThat(DeploymentStatus.from("skipped")).isEqualTo(DeploymentStatus.SKIPPED);
        assertThat(DeploymentStatus.from("blocked")).isEqualTo(DeploymentStatus.BLOCKED);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> DeploymentStatus.from("paused"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paused");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> DeploymentStatus.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isSettable_shouldBeTrueOnlyForFourStatuses_whenEveryStatusIsAsked() {
        assertThat(Arrays.stream(DeploymentStatus.values()).filter(DeploymentStatus::isSettable))
                .containsExactly(
                        DeploymentStatus.RUNNING,
                        DeploymentStatus.SUCCESS,
                        DeploymentStatus.FAILED,
                        DeploymentStatus.CANCELED);
    }

    @Test
    void value_shouldMatchCoreWireValue_whenEveryStatusIsMapped() {
        for (final DeploymentStatus status : DeploymentStatus.values()) {
            assertThat(GitLabProjectDeploymentsListStatus.valueOf(status.name()).wireValue())
                    .isEqualTo(status.value());
            if (status.isSettable()) {
                assertThat(GitLabProjectDeploymentsCreateStatus.valueOf(status.name())
                                .wireValue())
                        .isEqualTo(status.value());
                assertThat(GitLabProjectDeploymentsUpdateStatus.valueOf(status.name())
                                .wireValue())
                        .isEqualTo(status.value());
            }
        }
    }

    @Test
    void isSettable_shouldMatchCoreEnums_whenCreateUpdateAndListStatusesAreChecked() {
        final String[] settable = Arrays.stream(DeploymentStatus.values())
                .filter(DeploymentStatus::isSettable)
                .map(Enum::name)
                .toArray(String[]::new);

        assertThat(Arrays.stream(GitLabProjectDeploymentsCreateStatus.values()).map(Enum::name))
                .containsExactlyInAnyOrder(settable);
        assertThat(Arrays.stream(GitLabProjectDeploymentsUpdateStatus.values()).map(Enum::name))
                .containsExactlyInAnyOrder(settable);
        assertThat(Arrays.stream(GitLabProjectDeploymentsListStatus.values()).map(Enum::name))
                .containsExactlyInAnyOrder(
                        Arrays.stream(DeploymentStatus.values()).map(Enum::name).toArray(String[]::new));
    }
}
