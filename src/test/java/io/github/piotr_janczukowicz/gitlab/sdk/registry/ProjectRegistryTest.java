package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabProjectContainerRegistryListFilter;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabProjectContainerRegistryListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectRegistryTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void registryRepositories_shouldWrapEachItem_whenListed() throws IOException {
        when(gitLab.containerregistry().list(ID).stream()).thenReturn(Stream.of(repository(1), repository(2)));

        final List<RegistryRepository> result = project.registryRepositories().toList();

        assertThat(result).extracting(RegistryRepository::id).containsExactly(1L, 2L);
    }

    @Test
    void registryRepositories_shouldUseOwningProject_whenListedByProject() throws IOException {
        when(gitLab.containerregistry().list(ID).stream()).thenReturn(Stream.of(repository(1)));

        final RegistryRepository result =
                project.registryRepositories().findFirst().orElseThrow();

        assertThat(result.project()).isSameAs(project);
    }

    @Test
    void registryRepositories_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.containerregistry().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return repository(id);
                }));

        final Stream<RegistryRepository> stream = project.registryRepositories();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void registryRepositories_shouldAskForTags_whenWithTagsIsTrue() throws IOException {
        final GitLabProjectContainerRegistryListFilter expected =
                GitLabProjectContainerRegistryListFilterBuilder.builder()
                        .tags(true)
                        .build();
        when(gitLab.containerregistry().list(ID, expected).stream()).thenReturn(Stream.of(repository(1)));

        final List<RegistryRepository> result =
                project.registryRepositories(true).toList();

        assertThat(result).extracting(RegistryRepository::id).containsExactly(1L);
    }

    @Test
    void registryRepositories_shouldNotAskForTags_whenWithTagsIsFalse() throws IOException {
        final GitLabProjectContainerRegistryListFilter expected =
                GitLabProjectContainerRegistryListFilterBuilder.builder()
                        .tags(false)
                        .build();
        when(gitLab.containerregistry().list(ID, expected).stream()).thenReturn(Stream.of(repository(1)));

        final List<RegistryRepository> result =
                project.registryRepositories(false).toList();

        assertThat(result).hasSize(1);
    }
}
