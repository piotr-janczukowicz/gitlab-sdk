package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void fetch_shouldReturnLoadedProject_whenGitLabHasIt() throws IOException {
        final GitLabProjectWithAccessAndCatalog response = loadedProject(42L, "my-project");
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(response));

        final Optional<Project> loaded = sut.fetch();

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(loaded.orElseThrow().id()).isEqualTo(ID);
            softly.assertThat(loaded.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void fetch_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.empty());

        final Optional<Project> loaded = sut.fetch();

        assertThat(loaded).isEmpty();
    }

    @Test
    void project_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).project(ID);

        verifyNoInteractions(untouched);
    }

    @Test
    void response_shouldThrowIllegalStateException_whenProjectIsAHandle() {
        final Throwable thrown = catchThrowable(sut::response);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID);
    }

    @Test
    void project_shouldThrowIllegalArgumentException_whenIdIsBlank() {
        final GitLabSdk sdk = GitLabSdk.of(gitLab);

        final Throwable thrown = catchThrowable(() -> sdk.project(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refresh_shouldReturnNewLoadedProject_whenGitLabHasIt() throws IOException {
        final GitLabProjectWithAccessAndCatalog response = loadedProject(42L, "my-project");
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(response));

        final Project refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID + " is gone");
    }

    @Test
    void archive_shouldReturnNewProject_andLeaveOldUnchanged() throws IOException {
        final GitLabProjectWithAccessAndCatalog before = loadedProject(42L, "my-project");
        final GitLabProjectWithAccessAndCatalog after = loadedProject(42L, "my-project-archived");
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(before), Optional.of(after));
        final Project loaded = sut.refresh();

        final Project archived = loaded.archive();

        assertSoftly(softly -> {
            softly.assertThat(archived).isNotSameAs(loaded);
            softly.assertThat(archived.response()).isSameAs(after);
            softly.assertThat(loaded.response()).isSameAs(before);
        });
        verify(gitLab.projects().project()).archive(ID);
    }

    @Test
    void unarchive_shouldReturnReloadedProject_whenCalled() throws IOException {
        final GitLabProjectWithAccessAndCatalog after = loadedProject(42L, "my-project");
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(after));

        final Project unarchived = sut.unarchive();

        assertThat(unarchived.response()).isSameAs(after);
        verify(gitLab.projects().project()).unarchive(ID);
    }

    @Test
    void archive_shouldThrowIllegalStateException_whenProjectIsGoneAfterwards() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::archive);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.projects().project()).delete(ID);
    }

    @Test
    void internals_shouldReachTheClient_whenGivenAProject() {
        assertThat(Internals.gitLab(sut)).isSameAs(gitLab);
    }
}
