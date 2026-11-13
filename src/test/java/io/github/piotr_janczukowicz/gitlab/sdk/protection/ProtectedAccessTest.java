package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class ProtectedAccessTest {

    @ParameterizedTest
    @CsvSource({"NO_ACCESS,0", "DEVELOPER,30", "MAINTAINER,40", "ADMINISTRATOR,60"})
    void value_shouldReturnGitLabNumber_whenLevelGiven(final ProtectedAccess level, final int value) {
        assertThat(level.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @CsvSource({"NO_ACCESS,0", "DEVELOPER,30", "MAINTAINER,40", "ADMINISTRATOR,60"})
    void from_shouldReturnLevel_whenNumberIsKnown(final ProtectedAccess level, final int value) {
        assertThat(ProtectedAccess.from(value)).isEqualTo(level);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenNumberIsUnknown() {
        final Throwable thrown = catchThrowable(() -> ProtectedAccess.from(20));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("20");
    }
}
