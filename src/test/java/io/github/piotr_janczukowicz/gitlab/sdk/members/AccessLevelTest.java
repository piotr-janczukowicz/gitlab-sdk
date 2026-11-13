package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class AccessLevelTest {

    @Test
    void from_shouldThrowIllegalArgumentException_whenUnknownValue() {
        final Throwable thrown = catchThrowable(() -> AccessLevel.from(35));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("35");
    }

    @Test
    void from_shouldMapEveryValue_whenValueIsOfALevel() {
        for (final AccessLevel level : AccessLevel.values()) {
            assertThat(AccessLevel.from(level.value())).isSameAs(level);
        }
    }

    @Test
    void from_shouldReturnMinimalAccess_whenValueIsFive() {
        assertThat(AccessLevel.from(5)).isSameAs(AccessLevel.MINIMAL_ACCESS);
    }

    @Test
    void value_shouldReturnGitLabNumbers_whenAskedForEachLevel() {
        assertSoftly(softly -> {
            softly.assertThat(AccessLevel.values())
                    .extracting(AccessLevel::value)
                    .containsExactly(5, 10, 15, 20, 25, 30, 40, 50);
            softly.assertThat(AccessLevel.valueOf("SECURITY_MANAGER").value()).isEqualTo(25);
        });
    }
}
