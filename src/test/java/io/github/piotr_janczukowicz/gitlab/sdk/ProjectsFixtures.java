package io.github.piotr_janczukowicz.gitlab.sdk;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroup;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProject;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProjectBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserPublic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserPublicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalogBuilder;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUserProfile;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUserProfileBuilder;

/** Builds the core's records for the tests of the project, group and user objects. */
final class ProjectsFixtures {

    private ProjectsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabProjectWithAccessAndCatalog loadedProject(final long id, final String path) {
        return GitLabProjectWithAccessAndCatalogBuilder.builder()
                .id(id)
                .path(path)
                .build();
    }

    static GitLabProject listedProject(final long id) {
        return GitLabProjectBuilder.builder().id(id).build();
    }

    static GitLabGroupDetail loadedGroup(final long id, final String path) {
        return GitLabGroupDetailBuilder.builder().id(id).path(path).build();
    }

    static GitLabGroup listedGroup(final long id) {
        return GitLabGroupBuilder.builder().id(id).build();
    }

    static GitLabUserProfile profile(final long id, final String username) {
        return GitLabUserProfileBuilder.builder().id(id).username(username).build();
    }

    static GitLabUserPublic currentUser(final long id) {
        return GitLabUserPublicBuilder.builder().id(id).build();
    }
}
