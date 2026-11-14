package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesListState;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class MilestoneStateTest {

    @Test
    void from_shouldMapWireValues_whenActiveOrClosed() {
        assertThat(MilestoneState.from("active")).isEqualTo(MilestoneState.ACTIVE);
        assertThat(MilestoneState.from("closed")).isEqualTo(MilestoneState.CLOSED);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> MilestoneState.from("opened"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opened");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> MilestoneState.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final MilestoneState each : MilestoneState.values()) {
            assertThat(MilestoneState.from(each.value())).isEqualTo(each);
        }
        assertThat(MilestoneState.from("active").value()).isEqualTo("active");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectProjectMilestonesListState each : GitLabProjectProjectMilestonesListState.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }
        for (final GitLabGroupGroupMilestonesListState each : GitLabGroupGroupMilestonesListState.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }

        for (final String wireValue : wireValues) {
            assertThat(MilestoneState.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(2 * MilestoneState.values().length);
    }

    @Test
    void value_shouldBeKnownToEveryCoreEnum_whenEveryConstantIsMapped() {
        for (final MilestoneState each : MilestoneState.values()) {
            final String value = each.value();
            assertThat(GitLabProjectProjectMilestonesListState.fromWire(value).wireValue())
                    .isEqualTo(value);
            assertThat(GitLabGroupGroupMilestonesListState.fromWire(value).wireValue())
                    .isEqualTo(value);
        }
    }
}
