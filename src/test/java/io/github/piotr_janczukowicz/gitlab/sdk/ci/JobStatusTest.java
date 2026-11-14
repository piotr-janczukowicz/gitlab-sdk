package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.jobs.GitLabProjectJobsListJobsScope;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class JobStatusTest {

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final JobStatus each : JobStatus.values()) {
            assertThat(JobStatus.from(each.value())).isEqualTo(each);
        }
        assertThat(JobStatus.from("waiting_for_resource").value()).isEqualTo("waiting_for_resource");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectJobsListJobsScope each : GitLabProjectJobsListJobsScope.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }

        for (final String wireValue : wireValues) {
            assertThat(JobStatus.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(1 * JobStatus.values().length);
    }

    @Test
    void value_shouldBeKnownToEveryCoreEnum_whenEveryConstantIsMapped() {
        for (final JobStatus each : JobStatus.values()) {
            final String value = each.value();
            assertThat(GitLabProjectJobsListJobsScope.fromWire(value).wireValue())
                    .isEqualTo(value);
        }
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> JobStatus.from("unknown-value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-value");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> JobStatus.from(null)).isInstanceOf(NullPointerException.class);
    }
}
