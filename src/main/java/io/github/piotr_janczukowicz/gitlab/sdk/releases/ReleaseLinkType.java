package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The type of a release link. The values are the ones the core accepts when a link is created or changed, with the same
 * constant names. A type in a response that is not listed here makes {@link ReleaseLink#type()} throw;
 * {@link ReleaseLink#typeName()} gives the raw name.
 */
public enum ReleaseLinkType {
    /** Any other link; GitLab's default. */
    OTHER("other"),
    /** A runbook. */
    RUNBOOK("runbook"),
    /** A package. */
    PACKAGE("package"),
    /** An image. */
    IMAGE("image");

    private final String value;

    ReleaseLinkType(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the type as GitLab spells it, such as {@code runbook}.
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
    public static ReleaseLinkType from(final String value) {
        requireNonNull(value, "value");
        for (final ReleaseLinkType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown release link type: " + value);
    }
}
