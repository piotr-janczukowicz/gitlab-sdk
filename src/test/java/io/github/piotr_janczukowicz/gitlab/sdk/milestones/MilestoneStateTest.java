package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
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
}
