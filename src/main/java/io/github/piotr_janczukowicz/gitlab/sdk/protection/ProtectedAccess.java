package io.github.piotr_janczukowicz.gitlab.sdk.protection;

/**
 * Who may push to, merge into or create a protected branch or tag: the access levels the core client documents for the
 * create requests. GitLab may also report other numbers (for example 10 or 20); {@link #from(int)} throws for them, and
 * the raw numbers stay available from the {@code ...AccessValues()} methods of the protected branch and tag objects.
 */
public enum ProtectedAccess {
    /** Nobody, 0. */
    NO_ACCESS(0),
    /** Developers and above, 30. */
    DEVELOPER(30),
    /** Maintainers and above, 40. */
    MAINTAINER(40),
    /** Administrators, 60. */
    ADMINISTRATOR(60);

    private final int value;

    ProtectedAccess(final int value) {
        this.value = value;
    }

    /**
     * Returns the number GitLab uses for the level.
     *
     * @return the access level number
     */
    public int value() {
        return value;
    }

    /**
     * Returns the level with a number.
     *
     * @param value the access level number
     * @return the level
     * @throws IllegalArgumentException if no level has the number
     */
    public static ProtectedAccess from(final int value) {
        for (final ProtectedAccess level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown protected access level: " + value);
    }
}
