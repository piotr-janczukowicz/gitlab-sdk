package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class ArgumentsTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Arguments> constructor = Arguments.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("listsWithoutBlankValues")
    void requireNotBlankElements_shouldReturnTheValues_whenTheListMayBeEmpty(final List<String> values) {
        final List<String> result = Arguments.requireNotBlankElements(values, "labels");

        assertThat(result).containsExactlyElementsOf(values);
    }

    private static Stream<List<String>> listsWithoutBlankValues() {
        return Stream.of(List.of(), List.of("a", "b"));
    }

    @Test
    void requireNotBlankElements_shouldThrowIllegalArgumentException_whenAValueIsBlank() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireNotBlankElements(List.of("a", " "), "labels"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("labels");
    }

    @Test
    void requireNotBlankElements_shouldThrowNullPointerException_whenTheListIsNull() {
        final Throwable thrown = catchThrowable(() -> Arguments.requireNotBlankElements(null, "labels"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("labels");
    }
}
