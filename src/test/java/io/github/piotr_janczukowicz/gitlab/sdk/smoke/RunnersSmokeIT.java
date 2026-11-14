package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only, and skipped when the sandbox project has no runner. This is the first live check of the core's patched
 * runner types: {@code tagList()} must come back as a JSON array and {@code type()} must map. Registering, updating,
 * enabling, disabling and deleting a runner and resetting a token are not run live, because they would change the
 * runners of the instance or invalidate a token.
 */
@Tag("smoke")
class RunnersSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void runner_shouldBeListedAndLoadedWithTypedFields_whenSandboxHasRunners() throws IOException {
        final Project project = ENV.sandbox();
        final List<Runner> runners = project.runners().limit(5).toList();
        assumeFalse(runners.isEmpty(), "the sandbox project has no runners");
        final Runner first = runners.get(0);

        final Optional<Runner> loaded = ENV.sdk().runner(first.id());
        final Runner refreshed = first.refresh();

        assertSoftly(softly -> {
            softly.assertThat(loaded).map(Runner::id).contains(first.id());
            softly.assertThat(refreshed.id()).isEqualTo(first.id());
            softly.assertThat(first.tagList()).isNotNull().doesNotContainNull();
            softly.assertThat(first.type()).isNotNull();
            softly.assertThat(first.typeName()).isNotNull();
            softly.assertThat(first.status()).isNotBlank();
            softly.assertThat(loaded.orElseThrow().tagList()).isEqualTo(first.tagList());
        });
    }

    @Test
    void managersAndJobs_shouldBeRead_whenSandboxHasRunners() throws IOException {
        final List<Runner> runners = ENV.sandbox().runners().limit(5).toList();
        assumeFalse(runners.isEmpty(), "the sandbox project has no runners");
        final Runner first = runners.get(0);

        final var managers = first.managers();
        final var jobs = first.jobs().limit(5).toList();

        assertSoftly(softly -> {
            softly.assertThat(managers).isNotNull();
            softly.assertThat(jobs).isNotNull();
        });
    }
}
