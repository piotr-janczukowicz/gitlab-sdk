package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreateVariableType;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdateVariableType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class VariableTypeTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(VariableType.from("env_var")).isEqualTo(VariableType.ENV_VAR);
        assertThat(VariableType.from("file")).isEqualTo(VariableType.FILE);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenUnknown() {
        assertThatThrownBy(() -> VariableType.from("other"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("other");
    }

    @Test
    void from_shouldThrowNullPointerException_whenNull() {
        assertThatThrownBy(() -> VariableType.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void value_shouldReturnWireName_whenCalledOnType() {
        assertThat(VariableType.ENV_VAR.value()).isEqualTo("env_var");
        assertThat(VariableType.FILE.value()).isEqualTo("file");
    }

    @Test
    void values_shouldExistInEveryCoreVariableTypeEnum_whenCoreEnumsAreChecked() {
        for (final VariableType type : VariableType.values()) {
            assertThat(GitLabProjectProjectLevelVariablesUpdateVariableType.valueOf(type.name())
                            .wireValue())
                    .isEqualTo(type.value());
            assertThat(GitLabGroupGroupLevelVariablesUpdateVariableType.valueOf(type.name())
                            .wireValue())
                    .isEqualTo(type.value());
            assertThat(GitLabProjectPipelineSchedulesCreateVariableType.valueOf(type.name())
                            .wireValue())
                    .isEqualTo(type.value());
            assertThat(GitLabProjectPipelineSchedulesUpdateVariableType.valueOf(type.name())
                            .wireValue())
                    .isEqualTo(type.value());
        }
    }

    @Test
    void values_shouldCoverEveryCoreVariableType_whenCoreEnumsAreChecked() {
        final String[] names =
                Arrays.stream(VariableType.values()).map(Enum::name).toArray(String[]::new);

        assertThat(Stream.of(GitLabProjectProjectLevelVariablesUpdateVariableType.values())
                        .map(Enum::name))
                .containsExactlyInAnyOrder(names);
        assertThat(Stream.of(GitLabGroupGroupLevelVariablesUpdateVariableType.values())
                        .map(Enum::name))
                .containsExactlyInAnyOrder(names);
        assertThat(Stream.of(GitLabProjectPipelineSchedulesCreateVariableType.values())
                        .map(Enum::name))
                .containsExactlyInAnyOrder(names);
        assertThat(Stream.of(GitLabProjectPipelineSchedulesUpdateVariableType.values())
                        .map(Enum::name))
                .containsExactlyInAnyOrder(names);
    }
}
