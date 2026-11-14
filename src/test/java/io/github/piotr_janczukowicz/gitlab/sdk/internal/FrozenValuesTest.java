package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class FrozenValuesTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<FrozenValues> constructor = FrozenValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void frozenMap_shouldCopyDeeplyAndImmutably_whenNested() {
        final Map<String, Object> nested = new HashMap<>();
        nested.put("level", 2);
        final List<Object> list = new ArrayList<>(List.of("a", Map.of("b", 1)));
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("nested", nested);
        source.put("list", list);
        source.put("missing", null);
        source.put("number", 7);

        final Map<String, Object> result = FrozenValues.frozenMap(source);
        nested.put("level", 3);
        list.add("c");
        source.put("late", "x");

        assertSoftly(softly -> {
            softly.assertThat(result)
                    .containsKeys("nested", "list", "missing", "number")
                    .doesNotContainKey("late");
            softly.assertThat(result.keySet()).containsExactly("nested", "list", "missing", "number");
            softly.assertThat(result.get("missing")).isNull();
            softly.assertThat(result.get("number")).isEqualTo(7);
            softly.assertThat(result.get("nested")).isEqualTo(Map.of("level", 2));
            softly.assertThat(result.get("list")).isEqualTo(List.of("a", Map.of("b", 1)));
            softly.assertThat(catchThrowable(() -> result.put("key", "value")))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(() -> castMap(result.get("nested")).put("x", 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(() -> castList(result.get("list")).add("x")))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(
                            () -> castMap(castList(result.get("list")).get(1)).put("x", 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void frozenMap_shouldReturnEmptyImmutableMap_whenSourceIsEmpty() {
        final Map<String, Object> result = FrozenValues.frozenMap(Map.of());

        assertSoftly(softly -> {
            softly.assertThat(result).isEmpty();
            softly.assertThat(catchThrowable(() -> result.put("key", "value")))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(final Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(final Object value) {
        return (List<Object>) value;
    }
}
