package io.github.piotr_janczukowicz.gitlab.sdk;

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
     * Returns the value GitLab takes in its forms.
     *
     * @return the lower-case value, {@code private}, {@code internal} or {@code public}
     */
    String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
