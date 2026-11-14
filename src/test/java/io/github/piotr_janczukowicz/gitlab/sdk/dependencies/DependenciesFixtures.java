package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependency;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyBuilder;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityLicenseEntity;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityLicenseEntityBuilder;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityVulnerabilityEntity;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityVulnerabilityEntityBuilder;
import java.util.List;

/** Builds the core records for the tests of the dependency objects. */
final class DependenciesFixtures {

    private DependenciesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabDependencyEntityVulnerabilityEntity advisory() {
        return GitLabDependencyEntityVulnerabilityEntityBuilder.builder()
                .severity("high")
                .id(34L)
                .name("Vulnerability 1")
                .url("https://example.com/vulnerabilities/34")
                .build();
    }

    static GitLabDependencyEntityLicenseEntity license() {
        return GitLabDependencyEntityLicenseEntityBuilder.builder()
                .spdxIdentifier("MIT")
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")
                .build();
    }

    static GitLabDependency dependency(final String name) {
        return GitLabDependencyBuilder.builder()
                .name(name)
                .version("6.1.6")
                .packageManager("bundler")
                .dependencyFilePath("Gemfile.lock")
                .vulnerabilities(List.of(advisory()))
                .licenses(List.of(license()))
                .malware(false)
                .build();
    }

    static GitLabDependency bareDependency(final String name) {
        return GitLabDependencyBuilder.builder().name(name).build();
    }
}
