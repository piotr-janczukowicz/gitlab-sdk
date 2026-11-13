package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.createdFile;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.file;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.fileWithContent;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.updatedFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponseBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RepositoryFileTest {

    private static final String ID = "my-group/my-project";
    private static final String PATH = "src/main/App.java";
    private static final String REF = "main";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private RepositoryFile fileOf(final byte[] content) {
        return new RepositoryFile(project, REF, file(PATH, content));
    }

    private static String bodyOf(final MultipartFormData form) {
        return form.body().mapBytes(bytes -> new String(bytes, StandardCharsets.UTF_8), () -> "");
    }

    @Test
    void accessors_shouldReturnResponseValues_whenFileIsLoaded() {
        final GitLabProjectRepositoryFilesGetResponse response = file(PATH, new byte[] {1});

        final RepositoryFile sut = new RepositoryFile(project, REF, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.path()).isEqualTo(PATH);
            softly.assertThat(sut.name()).contains("App.java");
            softly.assertThat(sut.ref()).isEqualTo(REF);
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void bytes_shouldReturnExactBytes_whenContentIsBinary() {
        final byte[] binary = {0, (byte) 0xff, (byte) 0xfe, 10, 13, (byte) 0x80};

        final byte[] result = fileOf(binary).bytes();

        assertThat(result).containsExactly(binary);
    }

    @Test
    void bytes_shouldReturnEmptyArray_whenFileIsEmpty() {
        final byte[] result = fileOf(new byte[0]).bytes();

        assertThat(result).isEmpty();
    }

    @Test
    void bytes_shouldReturnCopy_whenCalledTwice() {
        final RepositoryFile sut = fileOf(new byte[] {1, 2});

        final byte[] first = sut.bytes();
        first[0] = 9;

        assertThat(sut.bytes()).containsExactly(1, 2);
    }

    @Test
    void bytes_shouldDecodeBase64_whenContentHasLineBreaks() {
        final RepositoryFile sut = new RepositoryFile(project, REF, fileWithContent(PATH, "base64", "aGVs\nbG8="));

        final byte[] result = sut.bytes();

        assertThat(result).containsExactly("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void bytes_shouldReturnUtf8Bytes_whenEncodingIsText() {
        final RepositoryFile sut = new RepositoryFile(project, REF, fileWithContent(PATH, "text", "hé"));

        final byte[] result = sut.bytes();

        assertThat(result).containsExactly("hé".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void bytes_shouldDecodeBase64_whenEncodingIsMissing() {
        final RepositoryFile sut = new RepositoryFile(
                project,
                REF,
                GitLabProjectRepositoryFilesGetResponseBuilder.builder()
                        .filePath(PATH)
                        .content("aGk=")
                        .build());

        final byte[] result = sut.bytes();

        assertThat(result).containsExactly("hi".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void bytes_shouldThrowIllegalStateException_whenContentIsMissing() {
        final RepositoryFile sut = new RepositoryFile(
                project,
                REF,
                GitLabProjectRepositoryFilesGetResponseBuilder.builder()
                        .filePath(PATH)
                        .build());

        final Throwable thrown = catchThrowable(sut::bytes);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(PATH);
    }

    @Test
    void bytes_shouldThrowIllegalStateException_whenContentIsNotBase64() {
        final RepositoryFile sut = new RepositoryFile(project, REF, fileWithContent(PATH, "base64", "@@@"));

        final Throwable thrown = catchThrowable(sut::bytes);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("base64");
    }

    @Test
    void name_shouldReturnEmpty_whenGitLabReturnsNoName() {
        final RepositoryFile sut = new RepositoryFile(
                project,
                REF,
                GitLabProjectRepositoryFilesGetResponseBuilder.builder()
                        .filePath(PATH)
                        .build());

        final Optional<String> result = sut.name();

        assertThat(result).isEmpty();
    }

    @Test
    void delete_shouldThrowIllegalArgumentException_whenMessageIsBlank() {
        final RepositoryFile sut = fileOf(new byte[0]);

        final Throwable thrown = catchThrowable(() -> sut.delete(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bytes_shouldThrowIllegalStateException_whenEncodingIsUnknown() {
        final RepositoryFile sut = new RepositoryFile(project, REF, fileWithContent(PATH, "rot13", "abc"));

        final Throwable thrown = catchThrowable(sut::bytes);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("rot13");
    }

    @Test
    void text_shouldReturnString_whenContentIsUtf8() {
        final RepositoryFile sut = fileOf("café €".getBytes(StandardCharsets.UTF_8));

        final String result = sut.text();

        assertThat(result).isEqualTo("café €");
    }

    @Test
    void text_shouldReturnEmptyString_whenFileIsEmpty() {
        final String result = fileOf(new byte[0]).text();

        assertThat(result).isEmpty();
    }

    @Test
    void text_shouldThrowIllegalStateException_whenNotUtf8() {
        final RepositoryFile sut = fileOf(new byte[] {(byte) 0xc3, (byte) 0x28});

        final Throwable thrown = catchThrowable(sut::text);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(PATH);
    }

    @Test
    void update_shouldReturnNewFile_whenGitLabAcceptsChange() throws IOException {
        final RepositoryFile sut = fileOf("old".getBytes(StandardCharsets.UTF_8));
        final GitLabProjectRepositoryFilesGetResponse reloaded = file(PATH, "new".getBytes(StandardCharsets.UTF_8));
        when(gitLab.repositoryfiles().update(eq(ID), eq(PATH), any(MultipartFormData.class)))
                .thenReturn(updatedFile(PATH, REF));
        when(gitLab.repositoryfiles().get(ID, PATH, REF)).thenReturn(Optional.of(reloaded));

        final RepositoryFile result = sut.update("new", "Change it");

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.repositoryfiles()).update(eq(ID), eq(PATH), form.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.text()).isEqualTo("new");
            softly.assertThat(result.ref()).isEqualTo(REF);
            softly.assertThat(sut.text()).isEqualTo("old");
            softly.assertThat(bodyOf(form.getValue()))
                    .contains("name=\"branch\"")
                    .contains(REF)
                    .contains("name=\"content\"")
                    .contains("name=\"commit_message\"")
                    .contains("Change it");
        });
    }

    @Test
    void update_shouldThrowIllegalStateException_whenFileIsGoneAfterUpdate() throws IOException {
        final RepositoryFile sut = fileOf(new byte[0]);
        when(gitLab.repositoryfiles().update(eq(ID), eq(PATH), any(MultipartFormData.class)))
                .thenReturn(updatedFile(PATH, REF));
        when(gitLab.repositoryfiles().get(ID, PATH, REF)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.update("x", "m"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(PATH);
    }

    @Test
    void update_shouldThrowNullPointerException_whenContentIsNull() {
        final RepositoryFile sut = fileOf(new byte[0]);

        final Throwable thrown = catchThrowable(() -> sut.update(null, "m"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void delete_shouldCallCoreOnReadBranch_whenCalled() throws IOException {
        final RepositoryFile sut = fileOf(new byte[0]);

        sut.delete("Remove it");

        verify(gitLab.repositoryfiles()).delete(ID, PATH, REF, "Remove it");
    }

    @Test
    void createFile_shouldPassPathUnchangedToCore_whenPathHasSlashes() throws IOException {
        final GitLabProjectRepositoryFilesGetResponse reloaded = file(PATH, new byte[] {1});
        when(gitLab.repositoryfiles().create(eq(ID), eq(PATH), any(MultipartFormData.class)))
                .thenReturn(createdFile(PATH, "dev"));
        when(gitLab.repositoryfiles().get(ID, PATH, "dev")).thenReturn(Optional.of(reloaded));

        final RepositoryFile result = project.createFile(PATH, "dev", "text", "Add it");

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.repositoryfiles()).create(eq(ID), eq(PATH), form.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.ref()).isEqualTo("dev");
            softly.assertThat(bodyOf(form.getValue()))
                    .contains("name=\"branch\"")
                    .contains("dev")
                    .contains("name=\"content\"")
                    .contains("text")
                    .contains("Add it");
        });
    }

    @Test
    void createFile_shouldThrowIllegalStateException_whenFileIsGoneAfterCreate() throws IOException {
        when(gitLab.repositoryfiles().create(eq(ID), eq(PATH), any(MultipartFormData.class)))
                .thenReturn(createdFile(PATH, "dev"));
        when(gitLab.repositoryfiles().get(ID, PATH, "dev")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> project.createFile(PATH, "dev", "", "Add it"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(PATH);
    }
}
