package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.file;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetRawFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RawFileTest {

    private static final String ID = "my-group/my-project";
    private static final String PATH = "docs/a b.bin";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void rawFile_shouldStreamToTheStreamAtTheRef_whenGivenAStream() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(gitLab.repositoryfiles()
                        .getRaw(eq(ID), eq(PATH), any(GitLabProjectRepositoryFilesGetRawFilter.class), eq(out)))
                .thenReturn(12L);

        final long written = project.rawFile(PATH, "release/1.x", out);

        final ArgumentCaptor<GitLabProjectRepositoryFilesGetRawFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectRepositoryFilesGetRawFilter.class);
        verify(gitLab.repositoryfiles()).getRaw(eq(ID), eq(PATH), filter.capture(), eq(out));
        assertSoftly(softly -> {
            softly.assertThat(written).isEqualTo(12L);
            softly.assertThat(filter.getValue().ref()).isEqualTo("release/1.x");
        });
    }

    @Test
    void rawFile_shouldReturnTheTarget_whenGivenAPath() throws IOException {
        final Path target = Path.of("out", "a.bin");
        when(gitLab.repositoryfiles()
                        .getRaw(eq(ID), eq(PATH), any(GitLabProjectRepositoryFilesGetRawFilter.class), eq(target)))
                .thenReturn(target);

        final Path written = project.rawFile(PATH, "main", target);

        final ArgumentCaptor<GitLabProjectRepositoryFilesGetRawFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectRepositoryFilesGetRawFilter.class);
        verify(gitLab.repositoryfiles()).getRaw(eq(ID), eq(PATH), filter.capture(), eq(target));
        assertSoftly(softly -> {
            softly.assertThat(written).isSameAs(target);
            softly.assertThat(filter.getValue().ref()).isEqualTo("main");
        });
    }

    @Test
    void rawFile_shouldRefuseBeforeAnyRequest_whenArgumentsAreBad() {
        final Throwable blankPath = catchThrowable(() -> project.rawFile(" ", "main", new ByteArrayOutputStream()));
        final Throwable blankRef = catchThrowable(() -> project.rawFile(PATH, "", new ByteArrayOutputStream()));
        final Throwable nullOut = catchThrowable(() -> project.rawFile(PATH, "main", (java.io.OutputStream) null));
        final Throwable nullPath = catchThrowable(() -> project.rawFile(PATH, "main", (Path) null));

        assertSoftly(softly -> {
            softly.assertThat(blankPath).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankRef).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullOut).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullPath).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void raw_shouldStreamThisFileAtItsRef_whenCalledOnAFile() throws IOException {
        final RepositoryFile sut = new RepositoryFile(project, "dev", file("src/App.java", new byte[] {1}));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(gitLab.repositoryfiles()
                        .getRaw(
                                eq(ID),
                                eq("src/App.java"),
                                any(GitLabProjectRepositoryFilesGetRawFilter.class),
                                eq(out)))
                .thenReturn(1L);

        final long written = sut.raw(out);

        final ArgumentCaptor<GitLabProjectRepositoryFilesGetRawFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectRepositoryFilesGetRawFilter.class);
        verify(gitLab.repositoryfiles()).getRaw(eq(ID), eq("src/App.java"), filter.capture(), eq(out));
        assertSoftly(softly -> {
            softly.assertThat(written).isEqualTo(1L);
            softly.assertThat(filter.getValue().ref()).isEqualTo("dev");
        });
    }

    @Test
    void raw_shouldWriteThePath_whenGivenATarget() throws IOException {
        final RepositoryFile sut = new RepositoryFile(project, "dev", file("src/App.java", new byte[] {1}));
        final Path target = Path.of("out", "App.java");
        when(gitLab.repositoryfiles()
                        .getRaw(
                                eq(ID),
                                eq("src/App.java"),
                                any(GitLabProjectRepositoryFilesGetRawFilter.class),
                                eq(target)))
                .thenReturn(target);

        final Path written = sut.raw(target);

        assertThat(written).isSameAs(target);
    }
}
