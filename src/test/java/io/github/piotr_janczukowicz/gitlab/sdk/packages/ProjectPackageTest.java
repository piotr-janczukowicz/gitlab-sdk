package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.CREATED;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.file;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.fullPackage;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.groupPkg;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.loadedProject;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabPackage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabPackageBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectPackageTest {

    private static final String ID = "42";
    private static final long PACKAGE_ID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final Project project = sdk.project(ID);

    private ProjectPackage owned(final GitLabPackage response) {
        return new ProjectPackage(project, response);
    }

    private ProjectPackage grouped(final GitLabPackage response) {
        return new ProjectPackage(sdk, response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabPackage response = fullPackage(PACKAGE_ID);

        final ProjectPackage sut = owned(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(PACKAGE_ID);
            softly.assertThat(sut.name()).isEqualTo("my-lib");
            softly.assertThat(sut.version()).contains("1.2.3");
            softly.assertThat(sut.type()).isEqualTo(PackageType.NPM);
            softly.assertThat(sut.typeName()).isEqualTo("npm");
            softly.assertThat(sut.status()).isEqualTo("default");
            softly.assertThat(sut.createdAt()).isEqualTo(CREATED);
            softly.assertThat(sut.lastDownloadedAt()).contains(Instant.parse("2026-02-03T04:05:06Z"));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenOptionalValuesAbsent() {
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        assertSoftly(softly -> {
            softly.assertThat(sut.version()).isEmpty();
            softly.assertThat(sut.lastDownloadedAt()).isEmpty();
        });
    }

    @Test
    void type_shouldThrowIllegalArgumentException_whenUnknown() {
        final ProjectPackage sut = owned(GitLabPackageBuilder.builder(pkg(PACKAGE_ID))
                .packageType("alien")
                .build());

        final Throwable thrown = catchThrowable(sut::type);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.typeName()).isEqualTo("alien");
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new ProjectPackage((Project) null, pkg(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new ProjectPackage(project, null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new ProjectPackage((GitLabSdk) null, pkg(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new ProjectPackage(sdk, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void project_shouldReturnOwnerWithoutRequest_whenBuiltByProject() throws IOException {
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        final Project result = sut.project();

        assertThat(result).isSameAs(project);
        verifyNoInteractions(gitLab);
    }

    @Test
    void project_shouldLoadThroughProjectId_whenBuiltBySdk() throws IOException {
        when(gitLab.projects().project().get("99")).thenReturn(Optional.of(loadedProject(99L)));
        final ProjectPackage sut = grouped(groupPkg(PACKAGE_ID, 99L));

        final Project result = sut.project();

        assertThat(result.id()).isEqualTo("99");
        verify(gitLab.projects().project()).get("99");
    }

    @Test
    void project_shouldThrowIllegalStateException_whenProjectIdAbsent() {
        final ProjectPackage sut = grouped(pkg(PACKAGE_ID));

        final Throwable thrown = catchThrowable(sut::project);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("project id");
    }

    @Test
    void project_shouldThrowIllegalStateException_whenProjectIsGone() throws IOException {
        when(gitLab.projects().project().get("99")).thenReturn(Optional.empty());
        final ProjectPackage sut = grouped(groupPkg(PACKAGE_ID, 99L));

        final Throwable thrown = catchThrowable(sut::project);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("99");
    }

    @Test
    void refresh_shouldLoadThroughOwner_whenBuiltByProject() throws IOException {
        final GitLabPackage fresh = fullPackage(PACKAGE_ID);
        when(gitLab.packages().get(ID, PACKAGE_ID)).thenReturn(Optional.of(fresh));
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        final ProjectPackage result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(fresh);
            softly.assertThat(sut.response()).isNotSameAs(fresh);
        });
        assertThat(result.project()).isSameAs(project);
    }

    @Test
    void refresh_shouldKeepProjectId_whenBuiltBySdkAndAnswerHasNone() throws IOException {
        when(gitLab.packages().get("99", PACKAGE_ID)).thenReturn(Optional.of(pkg(PACKAGE_ID)));
        when(gitLab.packages().listPackageFiles("99", PACKAGE_ID).stream()).thenReturn(Stream.of(file(1L)));
        final ProjectPackage sut = grouped(groupPkg(PACKAGE_ID, 99L));

        final List<PackageFile> files = sut.refresh().files().toList();

        assertThat(files).extracting(PackageFile::id).containsExactly(1L);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.packages().get(ID, PACKAGE_ID)).thenReturn(Optional.empty());
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenProjectIdAbsent() {
        final ProjectPackage sut = grouped(pkg(PACKAGE_ID));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void files_shouldWrapEachFile_whenListed() throws IOException {
        when(gitLab.packages().listPackageFiles(ID, PACKAGE_ID).stream()).thenReturn(Stream.of(file(1L), file(2L)));
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        final List<PackageFile> result = sut.files().toList();

        assertThat(result).extracting(PackageFile::id).containsExactly(1L, 2L);
    }

    @Test
    void files_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.packages().listPackageFiles(ID, PACKAGE_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return file(id);
                }));
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        final Stream<PackageFile> stream = sut.files();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void delete_shouldCallCore_whenBuiltByProject() throws IOException {
        final ProjectPackage sut = owned(pkg(PACKAGE_ID));

        sut.delete();

        verify(gitLab.packages()).delete(ID, PACKAGE_ID);
    }

    @Test
    void delete_shouldUseProjectId_whenBuiltBySdk() throws IOException {
        final ProjectPackage sut = grouped(groupPkg(PACKAGE_ID, 99L));

        sut.delete();

        verify(gitLab.packages()).delete("99", PACKAGE_ID);
    }

    @Test
    void delete_shouldThrowIllegalStateException_whenProjectIdAbsent() throws IOException {
        final ProjectPackage sut = grouped(pkg(PACKAGE_ID));

        final Throwable thrown = catchThrowable(sut::delete);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.packages(), never()).delete(any(), anyLong());
    }
}
