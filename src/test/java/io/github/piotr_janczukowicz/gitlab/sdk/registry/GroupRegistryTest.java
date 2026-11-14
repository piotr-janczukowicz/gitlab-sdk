package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.groupRepository;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupRegistryTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    @Test
    void registryRepositories_shouldWrapEachItem_whenListed() throws IOException {
        when(gitLab.groups().listRegistryRepositories(ID).stream())
                .thenReturn(Stream.of(groupRepository(1, 11), groupRepository(2, 12)));

        final List<RegistryRepository> result = group.registryRepositories().toList();

        assertThat(result).extracting(RegistryRepository::id).containsExactly(1L, 2L);
    }

    @Test
    void project_shouldReturnHandleWithoutRequest_whenListedByGroup() throws IOException {
        when(gitLab.groups().listRegistryRepositories(ID).stream()).thenReturn(Stream.of(groupRepository(1, 11)));
        final RegistryRepository item = group.registryRepositories().findFirst().orElseThrow();

        final Project result = item.project();

        assertThat(result.id()).isEqualTo("11");
        verify(gitLab, never()).projects();
    }

    @Test
    void project_shouldThrowIllegalStateException_whenListedWithoutProjectId() throws IOException {
        when(gitLab.groups().listRegistryRepositories(ID).stream()).thenReturn(Stream.of(repository(1)));
        final RegistryRepository item = group.registryRepositories().findFirst().orElseThrow();

        final Throwable thrown = catchThrowable(item::project);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registryRepositories_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listRegistryRepositories(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return groupRepository(id, 11);
                }));

        final Stream<RegistryRepository> stream = group.registryRepositories();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }
}
