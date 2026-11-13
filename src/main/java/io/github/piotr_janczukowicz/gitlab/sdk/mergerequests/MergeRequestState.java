package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/** The state of a {@link MergeRequest}: GitLab knows exactly these four. */
public enum MergeRequestState {
    /** The merge request is open; GitLab calls this state {@code opened}. */
    OPENED,
    /** The merge request is closed without being merged. */
    CLOSED,
    /** The merge request is locked, which GitLab does while it is being merged. */
    LOCKED,
    /** The merge request is merged. */
    MERGED;

    /**
     * Maps the state GitLab returned.
     *
     * @param value the wire value: {@code opened}, {@code closed}, {@code locked} or {@code merged}
     * @return the state
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is none of the four states
     */
    public static MergeRequestState from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "opened" -> OPENED;
            case "closed" -> CLOSED;
            case "locked" -> LOCKED;
            case "merged" -> MERGED;
            default -> throw new IllegalArgumentException("Unknown merge request state: " + value);
        };
    }
}
