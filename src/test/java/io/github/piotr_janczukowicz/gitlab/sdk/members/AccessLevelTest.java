package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
    void value_shouldReturnGitLabNumbers_whenAskedForEachLevel() {
        assertThat(AccessLevel.values()).extracting(AccessLevel::value).containsExactly(10, 15, 20, 25, 30, 40, 50);
        assertThat(AccessLevel.valueOf("SECURITY_MANAGER").value()).isEqualTo(25);
    }
}
