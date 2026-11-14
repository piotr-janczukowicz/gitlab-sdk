package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.file;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.fullFile;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.groupPkg;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.pkg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabPackageFile;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@UnitTest
class PackageFileTest {

    private static final String ID = "42";
    private static final long PACKAGE_ID = 7L;
    private static final long FILE_ID = 3L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final ProjectPackage owner = new ProjectPackage(sdk.project(ID), pkg(PACKAGE_ID));

    private PackageFile wrap(final GitLabPackageFile response) {
        return new PackageFile(owner, response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabPackageFile response = fullFile(FILE_ID);

        final PackageFile sut = wrap(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(FILE_ID);
            softly.assertThat(sut.fileName()).isEqualTo("lib-1.2.3.tgz");
            softly.assertThat(sut.size()).contains(2048L);
            softly.assertThat(sut.sha256()).contains("abc123");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenOptionalValuesAbsent() {
        final PackageFile sut = wrap(file(FILE_ID));

        assertSoftly(softly -> {
            softly.assertThat(sut.size()).isEmpty();
            softly.assertThat(sut.sha256()).isEmpty();
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new PackageFile(null, file(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new PackageFile(owner, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void download_shouldWriteToPathAndReturnIt_whenTargetGiven() throws IOException {
        final Path target = Path.of("out", "lib.tgz");
        when(gitLab.packages().getPackageFilesDownload(ID, PACKAGE_ID, FILE_ID, target))
                .thenReturn(target);

        final Path result = wrap(file(FILE_ID)).download(target);

        assertThat(result).isSameAs(target);
    }

    @Test
    void download_shouldReturnByteCount_whenStreamGiven() throws IOException {
        final OutputStream out = new ByteArrayOutputStream();
        when(gitLab.packages().getPackageFilesDownload(ID, PACKAGE_ID, FILE_ID, out))
                .thenReturn(4096L);

        final long written = wrap(file(FILE_ID)).download(out);

        assertThat(written).isEqualTo(4096L);
    }

    @Test
    void download_shouldUseProjectId_whenPackageBuiltBySdk() throws IOException {
        final PackageFile sut = new PackageFile(new ProjectPackage(sdk, groupPkg(PACKAGE_ID, 99L)), file(FILE_ID));
        final Path target = Path.of("lib.tgz");
        when(gitLab.packages().getPackageFilesDownload("99", PACKAGE_ID, FILE_ID, target))
                .thenReturn(target);

        final Path result = sut.download(target);

        assertThat(result).isSameAs(target);
    }

    @Test
    void download_shouldPassCoreException_whenTransportFails() throws IOException {
        final Path target = Path.of("lib.tgz");
        when(gitLab.packages().getPackageFilesDownload(ID, PACKAGE_ID, FILE_ID, target))
                .thenThrow(new IOException("boom"));

        final Throwable thrown = catchThrowable(() -> wrap(file(FILE_ID)).download(target));

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void download_shouldThrowNullPointerException_whenTargetIsNull() throws IOException {
        final PackageFile sut = wrap(file(FILE_ID));

        final Throwable thrown = catchThrowable(() -> sut.download((Path) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.packages(), never()).getPackageFilesDownload(any(), anyLong(), anyLong(), any(Path.class));
    }

    @Test
    void download_shouldThrowNullPointerException_whenStreamIsNull() throws IOException {
        final PackageFile sut = wrap(file(FILE_ID));

        final Throwable thrown = catchThrowable(() -> sut.download((OutputStream) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.packages(), never())
                .getPackageFilesDownload(any(), anyLong(), anyLong(), any(OutputStream.class));
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        wrap(file(FILE_ID)).delete();

        verify(gitLab.packages()).deletePackageFile(ID, PACKAGE_ID, FILE_ID);
    }
}
