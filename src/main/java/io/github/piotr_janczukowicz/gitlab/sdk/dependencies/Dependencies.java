package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabProjectDependenciesListPackageManager;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The dependencies of one project, as the dependency scanning of GitLab found them. {@code Project.dependencies()} is
 * the usual way in. This is an Ultimate feature: GitLab is expected to answer 403 or 404 when the tier lacks it or the
 * feature is off, and a client configured with a lower tier refuses the call with the core's
 * {@code GitLabTierException} before a request. The area is read only, and GitLab has no call that loads one
 * dependency.
 */
public final class Dependencies {

    private final Project project;
    private final ProjectDependencyGateway gateway;

    private Dependencies(final Project project, final ProjectDependencyGateway gateway) {
        this.project = project;
        this.gateway = gateway;
    }

    /**
     * Returns the dependencies of a project; no request is made.
     *
     * @param project the project
     * @return the dependencies of the project
     * @throws NullPointerException if {@code project} is {@code null}
     */
    public static Dependencies forProject(final Project project) {
        requireNonNull(project, "project");
        return new Dependencies(project, new ProjectDependencyGateway(Internals.gitLab(project), project.id()));
    }

    /**
     * Lists all dependencies of the project ({@code GET /projects/{id}/dependencies}), with no filter. The first page
     * is requested at once; further pages are fetched as the stream reaches them. The stream can be used once;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the dependencies
     * @throws IOException if the list cannot be loaded
     */
    public Stream<Dependency> all() throws IOException {
        return gateway.list().map(listed -> new Dependency(this, listed));
    }

    /**
     * Lists the dependencies of the project that belong to the given package managers ({@code GET
     * /projects/{id}/dependencies} with {@code package_manager}). Paging and the stream are as for {@link #all()}.
     *
     * @param first the first package manager
     * @param more further package managers
     * @return the dependencies of those package managers
     * @throws NullPointerException if an argument or an element of {@code more} is {@code null}; no request is made
     * @throws IOException if the list cannot be loaded
     */
    public Stream<Dependency> all(
            final GitLabProjectDependenciesListPackageManager first,
            final GitLabProjectDependenciesListPackageManager... more)
            throws IOException {
        final List<GitLabProjectDependenciesListPackageManager> managers = new ArrayList<>();
        managers.add(requireNonNull(first, "first"));
        for (final GitLabProjectDependenciesListPackageManager each : requireNonNull(more, "more")) {
            managers.add(requireNonNull(each, "more"));
        }
        return gateway.list(managers).map(listed -> new Dependency(this, listed));
    }

    Project project() {
        return project;
    }
}
