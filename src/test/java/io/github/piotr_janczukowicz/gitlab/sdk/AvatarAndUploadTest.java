package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.markdownuploads.GitLabProjectUploadBuilder;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

@UnitTest
class AvatarAndUploadTest {

    private static final String PROJECT_ID = "my-group/demo";
    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(PROJECT_ID);
    private final Group group = GitLabSdk.of(gitLab).group(GROUP_ID);

    @TempDir
    Path directory;

    @Test
    void avatar_shouldReturnTheByteCount_whenProjectAvatarIsWrittenToAStream() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(gitLab.projects().project().getAvatar(PROJECT_ID, out)).thenReturn(1234L);

        final long written = project.avatar(out);

        assertThat(written).isEqualTo(1234L);
    }

    @Test
    void avatar_shouldReturnTheTargetPath_whenProjectAvatarIsWrittenToAFile() throws IOException {
        final Path target = directory.resolve("avatar.png");
        when(gitLab.projects().project().getAvatar(PROJECT_ID, target)).thenReturn(target);

        final Path written = project.avatar(target);

        assertThat(written).isEqualTo(target);
    }

    @Test
    void avatar_shouldReturnTheByteCount_whenGroupAvatarIsWrittenToAStream() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(gitLab.groups().getAvatar(GROUP_ID, out)).thenReturn(77L);

        final long written = group.avatar(out);

        assertThat(written).isEqualTo(77L);
    }

    @Test
    void avatar_shouldReturnTheTargetPath_whenGroupAvatarIsWrittenToAFile() throws IOException {
        final Path target = directory.resolve("group.png");
        when(gitLab.groups().getAvatar(GROUP_ID, target)).thenReturn(target);

        final Path written = group.avatar(target);

        assertThat(written).isEqualTo(target);
    }

    @Test
    void avatar_shouldThrowNullPointerException_whenTargetIsNull() {
        final Throwable projectStream = catchThrowable(() -> project.avatar((java.io.OutputStream) null));
        final Throwable projectPath = catchThrowable(() -> project.avatar((Path) null));
        final Throwable groupStream = catchThrowable(() -> group.avatar((java.io.OutputStream) null));
        final Throwable groupPath = catchThrowable(() -> group.avatar((Path) null));

        assertSoftly(softly -> {
            softly.assertThat(projectStream).isInstanceOf(NullPointerException.class);
            softly.assertThat(projectPath).isInstanceOf(NullPointerException.class);
            softly.assertThat(groupStream).isInstanceOf(NullPointerException.class);
            softly.assertThat(groupPath).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void upload_shouldSendTheFileAsMultipartAndReturnTheMarkdown_whenFileExists() throws IOException {
        final Path file = directory.resolve("diagram.txt");
        Files.writeString(file, "hello upload", StandardCharsets.UTF_8);
        when(gitLab.markdownuploads()
                        .create(
                                org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                                org.mockito.ArgumentMatchers.any(MultipartFormData.class)))
                .thenReturn(GitLabProjectUploadBuilder.builder()
                        .markdown("![diagram](/uploads/abc/diagram.txt)")
                        .build());

        final String markdown = project.upload(file);

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.markdownuploads()).create(org.mockito.ArgumentMatchers.eq(PROJECT_ID), form.capture());
        final String body = form.getValue().body().mapBytes(b -> new String(b, StandardCharsets.UTF_8), () -> "");
        assertSoftly(softly -> {
            softly.assertThat(markdown).isEqualTo("![diagram](/uploads/abc/diagram.txt)");
            softly.assertThat(form.getValue().contentType()).startsWith("multipart/form-data");
            softly.assertThat(body).contains("name=\"file\"").contains("filename=\"diagram.txt\"");
            softly.assertThat(body).contains("hello upload");
        });
    }

    @Test
    void upload_shouldThrowIoExceptionBeforeTheRequest_whenFileDoesNotExist() {
        final Path missing = directory.resolve("missing.txt");

        final Throwable thrown = catchThrowable(() -> project.upload(missing));

        assertThat(thrown).isInstanceOf(IOException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void upload_shouldThrowNullPointerException_whenFileIsNull() {
        final Throwable thrown = catchThrowable(() -> project.upload(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }
}
