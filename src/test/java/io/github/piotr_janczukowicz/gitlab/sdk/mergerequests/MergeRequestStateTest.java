package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabGroupMergeRequestsListState;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class MergeRequestStateTest {

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final MergeRequestState each : MergeRequestState.values()) {
            assertThat(MergeRequestState.from(each.value())).isEqualTo(each);
        }
        assertThat(MergeRequestState.from("merged").value()).isEqualTo("merged");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectMergeRequestsListState each : GitLabProjectMergeRequestsListState.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }
        for (final GitLabGroupMergeRequestsListState each : GitLabGroupMergeRequestsListState.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }

        for (final String wireValue : wireValues) {
            assertThat(MergeRequestState.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(2 * MergeRequestState.values().length);
    }

    @Test
    void value_shouldBeKnownToEveryCoreEnum_whenEveryConstantIsMapped() {
        for (final MergeRequestState each : MergeRequestState.values()) {
            final String value = each.value();
            assertThat(GitLabProjectMergeRequestsListState.fromWire(value).wireValue())
                    .isEqualTo(value);
            assertThat(GitLabGroupMergeRequestsListState.fromWire(value).wireValue())
                    .isEqualTo(value);
        }
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> MergeRequestState.from("unknown-value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-value");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> MergeRequestState.from(null)).isInstanceOf(NullPointerException.class);
    }
}
