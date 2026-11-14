package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Locale;

/** Who can see a project or group. */
public enum Visibility {

    /** Only members. */
    PRIVATE,

    /** Every signed-in user. */
    INTERNAL,

    /** Everyone. */
    PUBLIC;

    /**
     * Returns the name of the visibility as GitLab spells it, such as {@code private}.
     *
     * @return the lower-case name, {@code private}, {@code internal} or {@code public}
     */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Finds the visibility GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the visibility
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static Visibility from(final String value) {
        requireNonNull(value, "value");
        for (final Visibility visibility : values()) {
            if (visibility.value().equals(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown visibility: " + value);
    }
}
