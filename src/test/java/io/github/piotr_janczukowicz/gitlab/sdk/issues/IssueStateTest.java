package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabGroupIssuesListState;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesListState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class IssueStateTest {

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final IssueState each : IssueState.values()) {
            assertThat(IssueState.from(each.value())).isEqualTo(each);
        }
        assertThat(IssueState.from("opened").value()).isEqualTo("opened");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectIssuesListState each : GitLabProjectIssuesListState.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }
        for (final GitLabGroupIssuesListState each : GitLabGroupIssuesListState.values()) {
            if (!"all".equals(each.wireValue())) {
                wireValues.add(each.wireValue());
            }
        }

        for (final String wireValue : wireValues) {
            assertThat(IssueState.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(2 * IssueState.values().length);
    }

    @Test
    void value_shouldBeKnownToEveryCoreEnum_whenEveryConstantIsMapped() {
        for (final IssueState each : IssueState.values()) {
            final String value = each.value();
            assertThat(GitLabProjectIssuesListState.fromWire(value).wireValue()).isEqualTo(value);
            assertThat(GitLabGroupIssuesListState.fromWire(value).wireValue()).isEqualTo(value);
        }
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> IssueState.from("unknown-value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-value");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> IssueState.from(null)).isInstanceOf(NullPointerException.class);
    }
}
