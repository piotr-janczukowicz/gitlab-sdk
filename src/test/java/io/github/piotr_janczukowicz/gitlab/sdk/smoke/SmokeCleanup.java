package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import java.io.IOException;

/** Runs cleanup steps independently, so a failing step never skips the ones after it. */
final class SmokeCleanup {

    private SmokeCleanup() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** One cleanup step. */
    @FunctionalInterface
    interface Step {
        void run() throws IOException;
    }

    /**
     * Attempts every step, then rethrows the first failure with the others suppressed.
     *
     * @param steps the cleanup steps, in order
     * @throws IOException the first failure
     */
    static void all(final Step... steps) throws IOException {
        IOException first = null;
        for (final Step step : steps) {
            try {
                step.run();
            } catch (final IOException | RuntimeException e) {
                final IOException wrapped = e instanceof IOException io ? io : new IOException(e);
                if (first == null) {
                    first = wrapped;
                } else {
                    first.addSuppressed(wrapped);
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
