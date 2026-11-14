package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependency;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityLicenseEntity;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityVulnerabilityEntity;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import java.util.List;
import java.util.Optional;

/**
 * A dependency of a project, as GitLab returned it. It is a value: the list is the only call, so the object has neither
 * a lookup nor {@code refresh()}. It keeps the data of the moment it was listed, and {@link #response()} gives the
 * complete response. {@link #project()} returns the project it was listed for. This is an Ultimate feature.
 */
public final class Dependency {

    private final Dependencies dependencies;
    private final GitLabDependency response;

    /**
     * Wraps a response. Applications normally get this object from {@link Dependencies}; call the constructor only to
     * wrap a response that was loaded some other way.
     *
     * @param dependencies the dependencies of the project the dependency belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Dependency(final Dependencies dependencies, final GitLabDependency response) {
        this.dependencies = requireNonNull(dependencies, "dependencies");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the name of the dependency.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the version.
     *
     * @return the version, or empty when GitLab sent none
     */
    public Optional<String> version() {
        return Optional.ofNullable(response.version());
    }

    /**
     * Returns the package manager as GitLab spells it, such as {@code bundler}. It is the raw name, while
     * {@code Dependencies.all(first, more...)} filters by the core's enum
     * {@code GitLabProjectDependenciesListPackageManager}: a name the enum does not know is still read here.
     *
     * @return the package manager, or empty when GitLab sent none
     */
    public Optional<String> packageManager() {
        return Optional.ofNullable(response.packageManager());
    }

    /**
     * Returns the path of the file that declares the dependency, such as {@code Gemfile.lock}.
     *
     * @return the path, or empty when GitLab sent none
     */
    public Optional<String> filePath() {
        return Optional.ofNullable(response.dependencyFilePath());
    }

    /**
     * Returns the advisories GitLab matched to this dependency. They are the core's records, not SDK vulnerability
     * objects: the SDK does not offer the vulnerabilities REST API, which GitLab deprecates.
     *
     * @return an immutable list; empty when GitLab sent none
     */
    public List<GitLabDependencyEntityVulnerabilityEntity> vulnerabilities() {
        final List<GitLabDependencyEntityVulnerabilityEntity> value = response.vulnerabilities();
        return value == null ? List.of() : List.copyOf(value);
    }

    /**
     * Returns the licenses GitLab found for this dependency.
     *
     * @return an immutable list of the core's records; empty when GitLab sent none
     */
    public List<GitLabDependencyEntityLicenseEntity> licenses() {
        final List<GitLabDependencyEntityLicenseEntity> value = response.licenses();
        return value == null ? List.of() : List.copyOf(value);
    }

    /**
     * Returns whether GitLab flags the dependency as malware.
     *
     * @return the flag, or empty when GitLab sent none
     */
    public Optional<Boolean> malware() {
        return Optional.ofNullable(response.malware());
    }

    /**
     * Returns the project the dependency was listed for, without a request.
     *
     * @return the project
     */
    public Project project() {
        return dependencies.project();
    }

    /**
     * Returns the complete response GitLab sent for this dependency.
     *
     * @return the response record
     */
    public GitLabDependency response() {
        return response;
    }
}
