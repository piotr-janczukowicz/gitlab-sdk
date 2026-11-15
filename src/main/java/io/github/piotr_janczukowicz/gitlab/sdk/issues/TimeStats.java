package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStats;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The time tracking of an {@link Issue}, as GitLab returned it. The object is immutable, and {@link #response()} gives
 * the complete response.
 */
public final class TimeStats {

    private final GitLabIssuableTimeStats response;

    /**
     * Wraps a response. Applications normally get this object from the issue; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param response the response record the object is built from
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public TimeStats(final GitLabIssuableTimeStats response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the time estimate.
     *
     * @return the estimate, zero when none is set
     */
    public Duration estimate() {
        return seconds(response.timeEstimate());
    }

    /**
     * Returns the time spent so far.
     *
     * @return the total time spent, zero when none is booked
     */
    public Duration totalSpent() {
        return seconds(response.totalTimeSpent());
    }

    /**
     * Returns the estimate as GitLab writes it for people, such as {@code 1h 30m}.
     *
     * @return the text, or empty when GitLab does not return one
     */
    public Optional<String> humanEstimate() {
        return Optional.ofNullable(response.humanTimeEstimate());
    }

    /**
     * Returns the time spent as GitLab writes it for people, such as {@code 30m}.
     *
     * @return the text, or empty when GitLab does not return one
     */
    public Optional<String> humanTotalSpent() {
        return Optional.ofNullable(response.humanTotalTimeSpent());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabIssuableTimeStats response() {
        return response;
    }

    private static Duration seconds(final @Nullable Integer value) {
        return value == null ? Duration.ZERO : Duration.ofSeconds(value);
    }
}
