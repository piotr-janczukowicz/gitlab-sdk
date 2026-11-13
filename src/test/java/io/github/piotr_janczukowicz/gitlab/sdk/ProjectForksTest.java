package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedProject;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectforks.GitLabProjectProjectForksCreatePayload;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectForksTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void fork_shouldSendNamespacePathAndReloadViaGet_whenCalled() throws IOException {
        when(gitLab.projectforks().create(eq(ID), any())).thenReturn(listedProject(77L));
        when(gitLab.projects().project().get("77")).thenReturn(Optional.of(loadedProject(77L, "demo")));

        final Project fork = sut.fork("me/sandbox");

        final ArgumentCaptor<GitLabProjectProjectForksCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectForksCreatePayload.class);
        verify(gitLab.projectforks()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().namespacePath()).isEqualTo("me/sandbox");
            softly.assertThat(fork.id()).isEqualTo("77");
            softly.assertThat(fork.response().path()).isEqualTo("demo");
        });
    }

    @Test
    void fork_shouldThrowIllegalStateException_whenGoneAfterFork() throws IOException {
        when(gitLab.projectforks().create(eq(ID), any())).thenReturn(listedProject(77L));
        when(gitLab.projects().project().get("77")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.fork("me/sandbox"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("77");
    }

    @Test
    void fork_shouldThrowIllegalArgumentException_whenNamespacePathIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.fork(""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fork_shouldThrowNullPointerException_whenNamespacePathIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.fork(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void forks_shouldSkipForkGone_whenListedButNotLoadable() throws IOException {
        when(gitLab.projectforks().list(ID).stream()).thenReturn(Stream.of(listedProject(1L), listedProject(2L)));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.empty());
        when(gitLab.projects().project().get("2")).thenReturn(Optional.of(loadedProject(2L, "two")));

        final List<Project> forks = sut.forks().toList();

        assertThat(forks).singleElement().satisfies(p -> assertThat(p.id()).isEqualTo("2"));
    }

    @Test
    void forks_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectforks().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return listedProject(id);
                }));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.of(loadedProject(1L, "one")));

        final Stream<Project> forks = sut.forks();

        assertThat(consumed).hasValue(0);
        assertThat(forks.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
        verify(gitLab.projects().project(), never()).get("2");
    }

    @Test
    void removeForkRelationship_shouldCallDelete_whenCalled() throws IOException {
        sut.removeForkRelationship();

        verify(gitLab.projectforks()).delete(ID);
    }
}
