package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The status of a {@link Job}: exactly the thirteen statuses the core documents for it. A status the core does not know
 * is rejected, not mapped to a fallback.
 */
public enum JobStatus {
    /** The job is created; GitLab calls this status {@code created}. */
    CREATED,
    /** The job is waiting for resource; GitLab calls this status {@code waiting_for_resource}. */
    WAITING_FOR_RESOURCE,
    /** The job is preparing; GitLab calls this status {@code preparing}. */
    PREPARING,
    /** The job is waiting for callback; GitLab calls this status {@code waiting_for_callback}. */
    WAITING_FOR_CALLBACK,
    /** The job is pending; GitLab calls this status {@code pending}. */
    PENDING,
    /** The job is running; GitLab calls this status {@code running}. */
    RUNNING,
    /** The job is success; GitLab calls this status {@code success}. */
    SUCCESS,
    /** The job is failed; GitLab calls this status {@code failed}. */
    FAILED,
    /** The job is canceling; GitLab calls this status {@code canceling}. */
    CANCELING,
    /** The job is canceled; GitLab calls this status {@code canceled}. */
    CANCELED,
    /** The job is skipped; GitLab calls this status {@code skipped}. */
    SKIPPED,
    /** The job is manual; GitLab calls this status {@code manual}. */
    MANUAL,
    /** The job is scheduled; GitLab calls this status {@code scheduled}. */
    SCHEDULED;

    /**
     * Maps the status GitLab returned.
     *
     * @param value the wire value, for example {@code waiting_for_resource}
     * @return the status
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is none of the documented statuses
     */
    public static JobStatus from(final String value) {
        return switch (requireNonNull(value, "value")) {
            case "created" -> CREATED;
            case "waiting_for_resource" -> WAITING_FOR_RESOURCE;
            case "preparing" -> PREPARING;
            case "waiting_for_callback" -> WAITING_FOR_CALLBACK;
            case "pending" -> PENDING;
            case "running" -> RUNNING;
            case "success" -> SUCCESS;
            case "failed" -> FAILED;
            case "canceling" -> CANCELING;
            case "canceled" -> CANCELED;
            case "skipped" -> SKIPPED;
            case "manual" -> MANUAL;
            case "scheduled" -> SCHEDULED;
            default -> throw new IllegalArgumentException("Unknown job status: " + value);
        };
    }
}
