package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedGroup;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedProject;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    @Test
    void fetch_shouldReturnLoadedGroup_whenGitLabHasIt() throws IOException {
        final GitLabGroupDetail response = loadedGroup(7L, "my-group");
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(response));

        final Optional<Group> loaded = sut.fetch();

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(loaded.orElseThrow().id()).isEqualTo(ID);
            softly.assertThat(loaded.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void fetch_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.empty());

        assertThat(sut.fetch()).isEmpty();
    }

    @Test
    void response_shouldThrowIllegalStateException_whenGroupIsAHandle() {
        final Throwable thrown = catchThrowable(sut::response);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID + " is gone");
    }

    @Test
    void archive_shouldReturnReloadedGroup_whenCalled() throws IOException {
        final GitLabGroupDetail after = loadedGroup(7L, "my-group");
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(after));

        final Group archived = sut.archive();

        assertThat(archived.response()).isSameAs(after);
        verify(gitLab.groups()).archive(ID);
    }

    @Test
    void unarchive_shouldReturnReloadedGroup_whenCalled() throws IOException {
        final GitLabGroupDetail after = loadedGroup(7L, "my-group");
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(after));

        final Group unarchived = sut.unarchive();

        assertThat(unarchived.response()).isSameAs(after);
        verify(gitLab.groups()).unarchive(ID);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.groups()).delete(ID);
    }

    @Test
    void projects_shouldMapGroupProjectsLazily() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listProjects(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return listedProject(id);
                }));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.of(loadedProject(1L, "one")));

        final Stream<Project> projects = sut.projects();

        assertThat(consumed).hasValue(0);
        verify(gitLab.projects().project(), never()).get("1");
        final List<Project> first = projects.limit(1).toList();
        assertThat(first).singleElement().satisfies(p -> assertThat(p.id()).isEqualTo("1"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.projects().project(), never()).get("2");
    }

    @Test
    void projects_shouldSkipProject_whenGoneBeforeLoading() throws IOException {
        when(gitLab.groups().listProjects(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(listedProject(1L), listedProject(2L)));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.empty());
        when(gitLab.projects().project().get("2")).thenReturn(Optional.of(loadedProject(2L, "two")));

        final List<Project> projects = sut.projects().toList();

        assertThat(projects).singleElement().satisfies(p -> assertThat(p.id()).isEqualTo("2"));
    }

    @Test
    void subgroups_shouldReturnGroups() throws IOException {
        final GitLabGroupDetail child = loadedGroup(8L, "child");
        when(gitLab.groups().listDescendantGroups(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(listedGroup(8L), listedGroup(9L)));
        when(gitLab.groups().get("8")).thenReturn(Optional.of(child));
        when(gitLab.groups().get("9")).thenReturn(Optional.empty());

        final List<Group> groups = sut.subgroups().toList();

        assertThat(groups)
                .singleElement()
                .satisfies(g -> assertSoftly(softly -> {
                    softly.assertThat(g.id()).isEqualTo("8");
                    softly.assertThat(g.response()).isSameAs(child);
                }));
    }

    @Test
    void internals_shouldReachTheClient_whenGivenAGroup() {
        assertThat(Internals.gitLab(sut)).isSameAs(gitLab);
    }
}
