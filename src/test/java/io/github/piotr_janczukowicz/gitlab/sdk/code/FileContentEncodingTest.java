package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.createdFile;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.file;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.updatedFile;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
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
class FileContentEncodingTest {

    private static final String ID = "my-group/my-project";
    private static final String PATH = "docs/unicode.txt";
    private static final String TEXT = "zażółć gęślą jaźń ✓";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private static byte[] bytesOf(final MultipartFormData form) {
        return form.body().mapBytes(bytes -> bytes, () -> new byte[0]);
    }

    private static String latin1(final byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private void assertContentPartIsUtf8(final MultipartFormData form) {
        final String wire = latin1(bytesOf(form));
        final String utf8Text = latin1(TEXT.getBytes(StandardCharsets.UTF_8));
        final String head = "Content-Disposition: form-data; name=\"content\"\r\n\r\n";
        assertSoftly(softly -> {
            // the content part carries exactly the UTF-8 bytes of the text, right after its bare head
            softly.assertThat(wire).contains(head + utf8Text + "\r\n");
            // a text part has no filename and no Content-Type of its own, so RFC 7578 defaults apply (UTF-8 text)
            softly.assertThat(wire).doesNotContain("filename=").doesNotContain("Content-Type:");
            softly.assertThat(wire).doesNotContain("?");
            softly.assertThat(form.contentType()).startsWith("multipart/form-data; boundary=");
        });
    }

    @Test
    void createFile_shouldSendUtf8Bytes_whenContentIsNonAscii() throws IOException {
        when(gitLab.repositoryfiles().create(eq(ID), eq(PATH), any(MultipartFormData.class)))
                .thenReturn(createdFile(PATH, "dev"));
        when(gitLab.repositoryfiles().get(ID, PATH, "dev"))
                .thenReturn(Optional.of(file(PATH, TEXT.getBytes(StandardCharsets.UTF_8))));

        final RepositoryFile result = project.createFile(PATH, "dev", TEXT, "Add it");

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.repositoryfiles()).create(eq(ID), eq(PATH), form.capture());
        assertContentPartIsUtf8(form.getValue());
        assertSoftly(softly -> softly.assertThat(result.text()).isEqualTo(TEXT));
    }

    @Test
    void update_shouldSendUtf8Bytes_whenContentIsNonAscii() throws IOException {
        final RepositoryFile sut = new RepositoryFile(project, "dev", file(PATH, new byte[0]));
        when(gitLab.repositoryfiles().update(eq(ID), eq(PATH), any(MultipartFormData.class)))
                .thenReturn(updatedFile(PATH, "dev"));
        when(gitLab.repositoryfiles().get(ID, PATH, "dev"))
                .thenReturn(Optional.of(file(PATH, TEXT.getBytes(StandardCharsets.UTF_8))));

        final RepositoryFile result = sut.update(TEXT, "Change it");

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.repositoryfiles()).update(eq(ID), eq(PATH), form.capture());
        assertContentPartIsUtf8(form.getValue());
        assertSoftly(softly -> softly.assertThat(result.text()).isEqualTo(TEXT));
    }
}
