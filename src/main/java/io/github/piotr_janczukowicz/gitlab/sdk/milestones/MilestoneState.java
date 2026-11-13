package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The state of a {@link Milestone} or {@link GroupMilestone}: GitLab knows exactly these two. A state in a response
 * that is not listed here makes {@code state()} throw; {@code stateName()} gives the raw name.
 */
public enum MilestoneState {
    /** The milestone is open. */
    ACTIVE,
    /** The milestone is closed. */
    CLOSED;

    /**
     * Maps the state GitLab returned.
     *
     * @param value the wire value, {@code active} or {@code closed}
     * @return the state
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is neither {@code active} nor {@code closed}
     */
    public static MilestoneState from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "active" -> ACTIVE;
            case "closed" -> CLOSED;
            default -> throw new IllegalArgumentException("Unknown milestone state: " + value);
        };
    }
}
