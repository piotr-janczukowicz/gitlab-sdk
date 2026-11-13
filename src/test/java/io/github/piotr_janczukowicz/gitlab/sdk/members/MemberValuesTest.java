package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@UnitTest
class MemberValuesTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<MemberValues> constructor = MemberValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void startOfDayUtc_shouldReturnMidnightUtc_whenGivenDate() {
        assertThat(MemberValues.startOfDayUtc(LocalDate.of(2027, 3, 1)))
                .isEqualTo(Instant.parse("2027-03-01T00:00:00Z"));
    }

    @Test
    void accessLevelValue_shouldThrowNullPointerException_whenAbsent() {
        final Throwable thrown = catchThrowable(() -> MemberValues.accessLevelValue(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
