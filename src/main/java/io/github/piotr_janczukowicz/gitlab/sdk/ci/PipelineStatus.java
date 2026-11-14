package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Locale;

/**
 * The status of a {@link Pipeline}: exactly the thirteen statuses the core documents for it. A status the core does not
 * know is rejected, not mapped to a fallback.
 */
public enum PipelineStatus {
    /** The pipeline is created; GitLab calls this status {@code created}. */
    CREATED,
    /** The pipeline is waiting for resource; GitLab calls this status {@code waiting_for_resource}. */
    WAITING_FOR_RESOURCE,
    /** The pipeline is preparing; GitLab calls this status {@code preparing}. */
    PREPARING,
    /** The pipeline is waiting for callback; GitLab calls this status {@code waiting_for_callback}. */
    WAITING_FOR_CALLBACK,
    /** The pipeline is pending; GitLab calls this status {@code pending}. */
    PENDING,
    /** The pipeline is running; GitLab calls this status {@code running}. */
    RUNNING,
    /** The pipeline is success; GitLab calls this status {@code success}. */
    SUCCESS,
    /** The pipeline is failed; GitLab calls this status {@code failed}. */
    FAILED,
    /** The pipeline is canceling; GitLab calls this status {@code canceling}. */
    CANCELING,
    /** The pipeline is canceled; GitLab calls this status {@code canceled}. */
    CANCELED,
    /** The pipeline is skipped; GitLab calls this status {@code skipped}. */
    SKIPPED,
    /** The pipeline is manual; GitLab calls this status {@code manual}. */
    MANUAL,
    /** The pipeline is scheduled; GitLab calls this status {@code scheduled}. */
    SCHEDULED;

    /**
     * Returns the name of the status as GitLab spells it, such as {@code waiting_for_resource}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Maps the status GitLab returned.
     *
     * @param value the wire value, for example {@code waiting_for_resource}
     * @return the status
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is none of the documented statuses
     */
    public static PipelineStatus from(final String value) {
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
            default -> throw new IllegalArgumentException("Unknown pipeline status: " + value);
        };
    }
}
