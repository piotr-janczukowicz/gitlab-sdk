package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.util.ArrayList;
import java.util.List;

/** Validates the arguments that several object packages take in the same shape. */
public final class Arguments {

    private Arguments() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks a list of names that may be empty, and returns it as an immutable list.
     *
     * @param values the values
     * @param name the argument name used in the exception message
     * @return the values as an immutable list
     * @throws IllegalArgumentException if a value is blank
     * @throws NullPointerException if the list or a value is {@code null}
     */
    public static List<String> requireNotBlankElements(final List<String> values, final String name) {
        final List<String> checked =
                new ArrayList<>(requireNonNull(values, name).size());
        for (final String value : values) {
            checked.add(requireNotBlank(value, name));
        }
        return List.copyOf(checked);
    }
}
