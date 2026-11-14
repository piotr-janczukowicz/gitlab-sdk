package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The status of a {@link Deployment}: the seven statuses the core documents for filtering deployments. A status in a
 * response that is not listed here makes {@link Deployment#status()} throw; {@link Deployment#statusName()} gives the
 * raw name. GitLab accepts only {@link #RUNNING}, {@link #SUCCESS}, {@link #FAILED} and {@link #CANCELED} when a
 * deployment is created or its status is updated; see {@link #isSettable()}.
 */
public enum DeploymentStatus {
    /** The deployment is created. */
    CREATED("created"),
    /** The deployment is running. */
    RUNNING("running"),
    /** The deployment succeeded. */
    SUCCESS("success"),
    /** The deployment failed. */
    FAILED("failed"),
    /** The deployment was canceled. */
    CANCELED("canceled"),
    /** The deployment was skipped. */
    SKIPPED("skipped"),
    /** The deployment is blocked, for example by a manual approval. */
    BLOCKED("blocked");

    private final String value;

    DeploymentStatus(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the status as GitLab spells it, such as {@code success}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Tells whether GitLab accepts the status when a deployment is created or its status is updated: only
     * {@link #RUNNING}, {@link #SUCCESS}, {@link #FAILED} and {@link #CANCELED} are.
     *
     * @return {@code true} for the four statuses that can be set
     */
    public boolean isSettable() {
        return this == RUNNING || this == SUCCESS || this == FAILED || this == CANCELED;
    }

    /**
     * Finds the status GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the status
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static DeploymentStatus from(final String value) {
        requireNonNull(value, "value");
        for (final DeploymentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown deployment status: " + value);
    }
}
