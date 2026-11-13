package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import java.io.IOException;

/**
 * Runs cleanup steps independently, so a failing step never skips the ones after it, and never lets a cleanup failure
 * hide the failure of the test itself.
 */
final class SmokeCleanup {

    private SmokeCleanup() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** The body of a test, or one cleanup step. */
    @FunctionalInterface
    interface Step {
        void run() throws IOException;
    }

    /**
     * Runs the body, then every cleanup step. When the body fails, its failure is rethrown with the failures of the
     * steps suppressed, so the test reports why it failed and not why the cleanup failed. When only steps fail, the
     * first failure is rethrown with the others suppressed.
     *
     * @param body the test body
     * @param steps the cleanup steps, in order
     * @throws IOException the failure of the body, or the first failure of a step
     */
    static void run(final Step body, final Step... steps) throws IOException {
        try {
            body.run();
        } catch (final IOException | RuntimeException | Error primary) {
            for (final Step step : steps) {
                try {
                    step.run();
                } catch (final IOException | RuntimeException e) {
                    primary.addSuppressed(e);
                }
            }
            throw primary;
        }
        all(steps);
    }

    /**
     * Attempts every step, then rethrows the first failure, unchanged, with the others suppressed.
     *
     * @param steps the cleanup steps, in order
     * @throws IOException the first failure, when it is an {@code IOException}
     */
    static void all(final Step... steps) throws IOException {
        Exception first = null;
        for (final Step step : steps) {
            try {
                step.run();
            } catch (final IOException | RuntimeException e) {
                if (first == null) {
                    first = e;
                } else {
                    first.addSuppressed(e);
                }
            }
        }
        if (first instanceof IOException io) {
            throw io;
        }
        if (first instanceof RuntimeException runtime) {
            throw runtime;
        }
    }
}
