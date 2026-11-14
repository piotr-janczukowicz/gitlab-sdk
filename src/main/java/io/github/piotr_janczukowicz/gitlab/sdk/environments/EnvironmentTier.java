package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The tier of an {@link Environment}. The values mirror the tiers the core accepts when it creates or updates an
 * environment, with the same constant names. A tier in a response that is not listed here makes
 * {@link Environment#tier()} throw; {@link Environment#tierName()} gives the raw name.
 */
public enum EnvironmentTier {
    /** A production environment. */
    PRODUCTION("production"),
    /** A staging environment. */
    STAGING("staging"),
    /** A testing environment. */
    TESTING("testing"),
    /** A development environment. */
    DEVELOPMENT("development"),
    /** Any other environment. */
    OTHER("other");

    private final String value;

    EnvironmentTier(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the tier as GitLab spells it, such as {@code production}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the tier GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the tier
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static EnvironmentTier from(final String value) {
        requireNonNull(value, "value");
        for (final EnvironmentTier tier : values()) {
            if (tier.value.equals(value)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown environment tier: " + value);
    }
}
