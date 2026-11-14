package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesListProjectStatus;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class PipelineStatusTest {

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final PipelineStatus each : PipelineStatus.values()) {
            assertThat(PipelineStatus.from(each.value())).isEqualTo(each);
        }
        assertThat(PipelineStatus.from("waiting_for_resource").value()).isEqualTo("waiting_for_resource");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectPipelinesListProjectStatus each : GitLabProjectPipelinesListProjectStatus.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }

        for (final String wireValue : wireValues) {
            assertThat(PipelineStatus.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(1 * PipelineStatus.values().length);
    }

    @Test
    void value_shouldBeKnownToEveryCoreEnum_whenEveryConstantIsMapped() {
        for (final PipelineStatus each : PipelineStatus.values()) {
            final String value = each.value();
            assertThat(GitLabProjectPipelinesListProjectStatus.fromWire(value).wireValue())
                    .isEqualTo(value);
        }
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> PipelineStatus.from("unknown-value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-value");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> PipelineStatus.from(null)).isInstanceOf(NullPointerException.class);
    }
}
