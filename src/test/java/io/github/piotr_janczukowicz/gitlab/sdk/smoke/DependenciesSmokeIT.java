package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabProjectDependenciesListPackageManager;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.dependencies.Dependency;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only: the dependency list is read only, and GitLab fills it from a dependency scan. The test is skipped when
 * the first list call answers 403 or 404 (the tier lacks the list, which is an Ultimate feature, or the feature is off;
 * see {@link SmokeSkips}) and when the list is empty, because the sandbox needs a dependency scan. It reads the
 * accessors of the first dependency and lists again with the package manager of that dependency as the filter. NOT run
 * live in the session that wrote it.
 */
@Tag("smoke")
class DependenciesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void dependencies_shouldListAndFilter_whenSandboxHasDependencyScan() throws IOException {
        final Project project = ENV.sandbox();
        final List<Dependency> listed = SmokeSkips.probe(
                "the dependency list",
                () -> project.dependencies().all().limit(5).toList());
        assumeFalse(listed.isEmpty(), "the sandbox has no dependency scan result");
        final Dependency first = listed.get(0);
        final Optional<GitLabProjectDependenciesListPackageManager> manager = first.packageManager()
                .flatMap(name -> Arrays.stream(GitLabProjectDependenciesListPackageManager.values())
                        .filter(each -> each.wireValue().equals(name))
                        .findFirst());
        final Optional<List<Dependency>> filtered = manager.isPresent()
                ? Optional.of(project.dependencies().all(manager.get()).limit(5).toList())
                : Optional.empty();

        assertSoftly(softly -> {
            softly.assertThat(first.name()).isNotBlank();
            softly.assertThat(first.vulnerabilities()).doesNotContainNull();
            softly.assertThat(first.licenses()).doesNotContainNull();
            softly.assertThat(first.project()).isSameAs(project);
            softly.assertThat(first.response()).isNotNull();
            filtered.ifPresent(list -> softly.assertThat(list)
                    .extracting(each -> each.packageManager().orElse(""))
                    .containsOnly(manager.get().wireValue()));
        });
    }
}
