package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/** The state of an {@link Issue}: GitLab knows exactly these two. */
public enum IssueState {
    /** The issue is open; GitLab calls this state {@code opened}. */
    OPENED,
    /** The issue is closed. */
    CLOSED;

    /**
     * Maps the state GitLab returned.
     *
     * @param value the wire value, {@code opened} or {@code closed}
     * @return the state
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is neither {@code opened} nor {@code closed}
     */
    public static IssueState from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "opened" -> OPENED;
            case "closed" -> CLOSED;
            default -> throw new IllegalArgumentException("Unknown issue state: " + value);
        };
    }
}
