package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static java.util.Objects.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import java.io.IOException;
import org.opentest4j.TestAbortedException;

/**
 * Turns an unavailable feature into a skipped test: a disabled feature or a 403 skips, never fails. Use it only for the
 * first call that tells whether an area is available to the sandbox and the token (an availability probe), never for
 * the calls under test, so a wrong request still fails.
 */
final class SmokeSkips {

    private SmokeSkips() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** An availability probe: the first call of an area. */
    @FunctionalInterface
    interface Probe<T> {
        T call() throws IOException;
    }

    /**
     * Runs an availability probe. GitLab's 403 Forbidden (the token or its role may not use the area) and 404 Not Found
     * (the feature is disabled for the sandbox) abort the calling test as an assumption failure.
     *
     * @param feature what the probe checks, for the skip message
     * @param probe the call
     * @param <T> the result type
     * @return the result of the call
     * @throws IOException if the transport fails
     * @throws TestAbortedException if GitLab answers 403 or 404
     */
    static <T> T probe(final String feature, final Probe<T> probe) throws IOException {
        requireNonNull(feature, "feature");
        requireNonNull(probe, "probe");
        try {
            return probe.call();
        } catch (final GitLabForbiddenException | GitLabNotFoundException e) {
            throw skip(feature, e);
        }
    }

    private static TestAbortedException skip(final String feature, final GitLabApiException cause) {
        return new TestAbortedException(
                feature + " is not available to the token or is disabled in the sandbox (HTTP " + cause.statusCode()
                        + "), skipped",
                cause);
    }
}
