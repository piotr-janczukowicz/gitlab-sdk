package io.github.piotr_janczukowicz.gitlab.sdk.members;

/**
 * A GitLab access level, the role of a member in a project or group, also used when sharing a project or group with a
 * group. The levels are the ones the core client documents. GitLab may add levels later; {@link #from(int)} then throws
 * and the member's raw number stays available from {@link Member#accessLevelValue()}.
 */
public enum AccessLevel {
    /** Guest, 10. */
    GUEST(10),
    /** Planner, 15. */
    PLANNER(15),
    /** Reporter, 20. */
    REPORTER(20),
    /** Security manager, 25. */
    SECURITY_MANAGER(25),
    /** Developer, 30. */
    DEVELOPER(30),
    /** Maintainer, 40. */
    MAINTAINER(40),
    /** Owner, 50. */
    OWNER(50);

    private final int value;

    AccessLevel(final int value) {
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
    public static AccessLevel from(final int value) {
        for (final AccessLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown access level: " + value);
    }
}
