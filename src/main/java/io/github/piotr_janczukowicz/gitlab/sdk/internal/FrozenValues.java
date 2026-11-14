package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Copies the free-form JSON maps of core records (front matter, audit details) into immutable values. */
public final class FrozenValues {

    private FrozenValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Copies a map deeply: nested maps and lists become immutable copies, other values (including {@code null}) are
     * kept as they are, and the order of the keys is kept.
     *
     * @param source the map to copy
     * @return an immutable copy
     */
    public static Map<String, Object> frozenMap(final Map<String, Object> source) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, frozen(value)));
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object frozen(final @Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            return frozenMap((Map<String, Object>) map);
        }
        if (value instanceof List<?> list) {
            final List<@Nullable Object> copy = new ArrayList<>(list.size());
            list.forEach(element -> copy.add(frozen(element)));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
