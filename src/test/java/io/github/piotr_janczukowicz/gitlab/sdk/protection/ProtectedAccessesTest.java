package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.access;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ProtectedAccessesTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<ProtectedAccesses> constructor = ProtectedAccesses.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void values_shouldReturnEmpty_whenListIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(ProtectedAccesses.values(null)).isEmpty();
            softly.assertThat(ProtectedAccesses.levels(null)).isEmpty();
        });
    }

    @Test
    void levels_shouldMapEveryEntry_whenLevelsAreKnown() {
        final List<ProtectedAccess> levels = ProtectedAccesses.levels(List.of(access(30), access(60)));

        assertThat(levels).containsExactly(ProtectedAccess.DEVELOPER, ProtectedAccess.ADMINISTRATOR);
    }
}
