package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Locale;

/** The kind of a link between two issues: GitLab knows exactly these three. */
public enum IssueLinkType {
    /** The issues are related. */
    RELATES_TO,
    /** The issue blocks the other one. */
    BLOCKS,
    /** The issue is blocked by the other one. */
    IS_BLOCKED_BY;

    /**
     * Returns the name of the type as GitLab spells it, such as {@code relates_to}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Maps the type GitLab returned.
     *
     * @param value the wire value, such as {@code relates_to}
     * @return the type
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not a type GitLab knows
     */
    public static IssueLinkType from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "relates_to" -> RELATES_TO;
            case "blocks" -> BLOCKS;
            case "is_blocked_by" -> IS_BLOCKED_BY;
            default -> throw new IllegalArgumentException("Unknown issue link type: " + value);
        };
    }
}
