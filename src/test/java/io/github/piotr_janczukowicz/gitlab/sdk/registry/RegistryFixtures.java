package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryRepository;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryRepositoryBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryTag;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryTagBuilder;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabContainerRegistryTagDetails;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabContainerRegistryTagDetailsBuilder;

/** Builds the core records for the tests of the registry objects. */
final class RegistryFixtures {

    private RegistryFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** A repository as the project list answers it: no project id. */
    static GitLabContainerRegistryRepository repository(final long id) {
        return GitLabContainerRegistryRepositoryBuilder.builder()
                .id(id)
                .name("app")
                .path("my-group/demo/app")
                .location("registry.example.com/my-group/demo/app")
                .build();
    }

    /** A repository as the group list answers it: with the project id. */
    static GitLabContainerRegistryRepository groupRepository(final long id, final long projectId) {
        return GitLabContainerRegistryRepositoryBuilder.builder(repository(id))
                .projectId(projectId)
                .build();
    }

    static GitLabContainerRegistryRepository fullRepository(final long id) {
        return GitLabContainerRegistryRepositoryBuilder.builder(groupRepository(id, 99L))
                .tagsCount(3)
                .build();
    }

    static GitLabContainerRegistryTag listedTag(final String name) {
        return GitLabContainerRegistryTagBuilder.builder()
                .name(name)
                .path("my-group/demo/app:" + name)
                .location("registry.example.com/my-group/demo/app:" + name)
                .build();
    }

    static GitLabContainerRegistryTagDetails details(final String name) {
        return GitLabContainerRegistryTagDetailsBuilder.builder()
                .name(name)
                .path("my-group/demo/app:" + name)
                .location("registry.example.com/my-group/demo/app:" + name)
                .build();
    }

    static GitLabContainerRegistryTagDetails fullDetails(final String name) {
        return GitLabContainerRegistryTagDetailsBuilder.builder(details(name))
                .digest("sha256:abc")
                .totalSize(1024L)
                .build();
    }
}
