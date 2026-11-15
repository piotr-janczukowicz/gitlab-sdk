package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabProjectIssueLinksCreateLinkType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class IssueLinkTypeTest {

    @Test
    void value_shouldRoundTripThroughFrom_whenEveryConstant() {
        for (final IssueLinkType each : IssueLinkType.values()) {
            assertThat(IssueLinkType.from(each.value())).isEqualTo(each);
        }
        assertThat(IssueLinkType.IS_BLOCKED_BY.value()).isEqualTo("is_blocked_by");
    }

    @Test
    void from_shouldMapEveryCoreValue_whenLoopingCoreEnum() {
        for (final GitLabProjectIssueLinksCreateLinkType each : GitLabProjectIssueLinksCreateLinkType.values()) {
            assertThat(IssueLinkType.from(each.wireValue()).value()).isEqualTo(each.wireValue());
        }
        assertThat(GitLabProjectIssueLinksCreateLinkType.values()).hasSize(IssueLinkType.values().length);
    }

    @Test
    void value_shouldBeKnownToTheCoreEnum_whenEveryConstantIsMapped() {
        for (final IssueLinkType each : IssueLinkType.values()) {
            assertThat(GitLabProjectIssueLinksCreateLinkType.fromWire(each.value())
                            .wireValue())
                    .isEqualTo(each.value());
        }
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> IssueLinkType.from("unknown-value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-value");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> IssueLinkType.from(null)).isInstanceOf(NullPointerException.class);
    }
}
