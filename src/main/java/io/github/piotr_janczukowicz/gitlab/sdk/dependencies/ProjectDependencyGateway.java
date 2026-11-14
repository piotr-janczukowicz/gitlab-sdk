package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependency;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabProjectDependenciesListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabProjectDependenciesListPackageManager;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

/** The dependency call for a project. */
final class ProjectDependencyGateway {

    private final GitLab gitLab;
    private final String id;

    ProjectDependencyGateway(final GitLab gitLab, final String id) {
        this.gitLab = gitLab;
        this.id = id;
    }

    Stream<GitLabDependency> list() throws IOException {
        return gitLab.dependencies().list(id).stream();
    }

    Stream<GitLabDependency> list(final List<GitLabProjectDependenciesListPackageManager> packageManagers)
            throws IOException {
        return gitLab
                .dependencies()
                .list(
                        id,
                        GitLabProjectDependenciesListFilterBuilder.builder()
                                .packageManager(packageManagers)
                                .build())
                .stream();
    }
}
