package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.packages.PackageFile;
import io.github.piotr_janczukowicz.gitlab.sdk.packages.ProjectPackage;
import io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryRepository;
import io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryTag;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only: publishing a package or pushing an image needs a build or a registry login that a sandbox may not have,
 * and deleting would destroy real data. Each test is skipped when the sandbox has no package or no repository, and when
 * the first list call answers 403 or 404 (the feature is disabled or the token may not use it; see {@link SmokeSkips}).
 * Downloading a package file and deleting files, packages, tags or repositories are not run live.
 */
@Tag("smoke")
class PackagesAndRegistrySmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void packages_shouldListLoadAndReadFiles_whenSandboxHasPackages() throws IOException {
        final Project project = ENV.sandbox();
        final List<ProjectPackage> packages = SmokeSkips.probe(
                "the package registry", () -> project.packages().limit(5).toList());
        assumeFalse(packages.isEmpty(), "the sandbox has no packages");
        final ProjectPackage first = packages.get(0);

        final Optional<ProjectPackage> loaded = project.projectPackage(first.id());
        final ProjectPackage refreshed = first.refresh();
        final List<PackageFile> files = first.files().limit(10).toList();

        assertSoftly(softly -> {
            softly.assertThat(loaded).map(ProjectPackage::id).contains(first.id());
            softly.assertThat(refreshed.id()).isEqualTo(first.id());
            softly.assertThat(first.type()).isNotNull();
            softly.assertThat(first.typeName()).isNotBlank();
            softly.assertThat(first.name()).isNotBlank();
            softly.assertThat(first.status()).isNotBlank();
            softly.assertThat(files).extracting(PackageFile::fileName).doesNotContainNull();
        });
    }

    @Test
    void registry_shouldListLoadAndReadTags_whenSandboxHasRepositories() throws IOException {
        final Project project = ENV.sandbox();
        final List<RegistryRepository> repositories = SmokeSkips.probe(
                "the container registry",
                () -> project.registryRepositories().limit(5).toList());
        assumeFalse(repositories.isEmpty(), "the sandbox has no container registry repository");
        final RegistryRepository first = repositories.get(0);

        final RegistryRepository refreshed = first.refresh();
        final List<RegistryTag> tags = first.tags().limit(10).toList();
        final Optional<RegistryTag> loaded =
                tags.isEmpty() ? Optional.empty() : first.tag(tags.get(0).name());

        assertSoftly(softly -> {
            softly.assertThat(refreshed.id()).isEqualTo(first.id());
            softly.assertThat(first.path()).isNotBlank();
            softly.assertThat(first.location()).isNotBlank();
            softly.assertThat(tags).extracting(RegistryTag::name).doesNotContainNull();
            if (!tags.isEmpty()) {
                softly.assertThat(loaded)
                        .map(RegistryTag::name)
                        .contains(tags.get(0).name());
            }
        });
    }
}
