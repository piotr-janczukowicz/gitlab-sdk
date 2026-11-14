package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerType;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerWithUuidRunnerType;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetailsRunnerType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class RunnerTypeTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(RunnerType.from("instance_type")).isEqualTo(RunnerType.INSTANCE_TYPE);
        assertThat(RunnerType.from("group_type")).isEqualTo(RunnerType.GROUP_TYPE);
        assertThat(RunnerType.from("project_type")).isEqualTo(RunnerType.PROJECT_TYPE);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> RunnerType.from("unrecognized"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unrecognized");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> RunnerType.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void value_shouldMatchCoreWireValue_whenEveryTypeIsMapped() {
        for (final RunnerType type : RunnerType.values()) {
            assertThat(GitLabCiRunnerDetailsRunnerType.valueOf(type.name()).wireValue())
                    .isEqualTo(type.value());
            assertThat(GitLabCiRunnerWithUuidRunnerType.valueOf(type.name()).wireValue())
                    .isEqualTo(type.value());
            assertThat(GitLabCiRunnerType.valueOf(type.name()).wireValue()).isEqualTo(type.value());
        }
    }

    @Test
    void values_shouldMatchCoreEnumsWithoutUnrecognized_whenCompared() {
        final String[] names =
                Arrays.stream(RunnerType.values()).map(Enum::name).toArray(String[]::new);

        assertThat(Arrays.stream(GitLabCiRunnerDetailsRunnerType.values())
                        .map(Enum::name)
                        .filter(name -> !"UNRECOGNIZED".equals(name)))
                .containsExactlyInAnyOrder(names);
        assertThat(Arrays.stream(GitLabCiRunnerWithUuidRunnerType.values())
                        .map(Enum::name)
                        .filter(name -> !"UNRECOGNIZED".equals(name)))
                .containsExactlyInAnyOrder(names);
    }
}
