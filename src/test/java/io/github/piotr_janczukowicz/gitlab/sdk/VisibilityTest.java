package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListVisibility;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsListVisibility;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class VisibilityTest {

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final Visibility each : Visibility.values()) {
            assertThat(Visibility.from(each.value())).isEqualTo(each);
        }
        assertThat(Visibility.from("private").value()).isEqualTo("private");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectProjectsListVisibility each : GitLabProjectProjectsListVisibility.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }
        for (final GitLabGroupGroupsListVisibility each : GitLabGroupGroupsListVisibility.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }
        for (final GitLabProjectProjectForksCreateVisibility each :
                GitLabProjectProjectForksCreateVisibility.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }

        for (final String wireValue : wireValues) {
            assertThat(Visibility.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(3 * Visibility.values().length);
    }

    @Test
    void value_shouldBeKnownToEveryCoreEnum_whenEveryConstantIsMapped() {
        for (final Visibility each : Visibility.values()) {
            final String value = each.value();
            assertThat(GitLabProjectProjectsListVisibility.fromWire(value).wireValue())
                    .isEqualTo(value);
            assertThat(GitLabGroupGroupsListVisibility.fromWire(value).wireValue())
                    .isEqualTo(value);
            assertThat(GitLabProjectProjectForksCreateVisibility.fromWire(value).wireValue())
                    .isEqualTo(value);
        }
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> Visibility.from("unknown-value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-value");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> Visibility.from(null)).isInstanceOf(NullPointerException.class);
    }
}
