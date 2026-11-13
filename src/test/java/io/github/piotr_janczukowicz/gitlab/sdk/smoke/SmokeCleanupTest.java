package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class SmokeCleanupTest {

    private final List<String> ran = new ArrayList<>();

    @Test
    void run_shouldKeepTheTestFailure_whenACleanupStepFailsToo() {
        final AssertionError failure = new AssertionError("test failed");
        final IOException cleanupFailure = new IOException("cleanup failed");

        final Throwable thrown = catchThrowable(() -> SmokeCleanup.run(
                () -> {
                    throw failure;
                },
                () -> {
                    throw cleanupFailure;
                },
                () -> ran.add("second")));

        assertSoftly(softly -> {
            softly.assertThat(thrown).isSameAs(failure);
            softly.assertThat(thrown.getSuppressed()).containsExactly(cleanupFailure);
            softly.assertThat(ran).containsExactly("second");
        });
    }

    @Test
    void run_shouldThrowTheFirstCleanupFailure_whenTheBodySucceeds() {
        final IOException first = new IOException("first");
        final IllegalStateException second = new IllegalStateException("second");

        final Throwable thrown = catchThrowable(() -> SmokeCleanup.run(
                () -> ran.add("body"),
                () -> {
                    throw first;
                },
                () -> {
                    throw second;
                }));

        assertSoftly(softly -> {
            softly.assertThat(thrown).isSameAs(first);
            softly.assertThat(thrown.getSuppressed()).containsExactly(second);
            softly.assertThat(ran).containsExactly("body");
        });
    }

    @Test
    void run_shouldRunBodyThenStepsInOrder_whenNothingFails() throws IOException {
        SmokeCleanup.run(() -> ran.add("body"), () -> ran.add("first"), () -> ran.add("second"));

        assertThat(ran).containsExactly("body", "first", "second");
    }

    @Test
    void all_shouldRethrowARuntimeExceptionUnwrapped_whenItIsTheFirstFailure() {
        final IllegalStateException failure = new IllegalStateException("gone");

        final Throwable thrown = catchThrowable(() -> SmokeCleanup.all(() -> {
            throw failure;
        }));

        assertThat(thrown).isSameAs(failure);
    }
}
