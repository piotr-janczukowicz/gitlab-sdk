package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The type of a {@link Runner}: the three types the core documents. A type in a response that is not listed here makes
 * {@link Runner#type()} throw; {@link Runner#typeName()} gives the raw name.
 */
public enum RunnerType {
    /** A runner that every project of the instance can use. */
    INSTANCE_TYPE("instance_type"),
    /** A runner that the projects of a group can use. */
    GROUP_TYPE("group_type"),
    /** A runner that is enabled for projects one by one. */
    PROJECT_TYPE("project_type");

    private final String value;

    RunnerType(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the type as GitLab spells it, such as {@code project_type}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the type GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the type
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static RunnerType from(final String value) {
        requireNonNull(value, "value");
        for (final RunnerType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown runner type: " + value);
    }
}
