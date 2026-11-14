package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.fullPackage;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.pkg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabProjectPackagesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabProjectPackagesListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabProjectPackagesListPackageType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectPackagesTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void packages_shouldWrapEachItemWithoutLoading_whenListed() throws IOException {
        when(gitLab.packages().list(ID).stream()).thenReturn(Stream.of(pkg(1), pkg(2)));

        final List<ProjectPackage> result = project.packages().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(ProjectPackage::id).containsExactly(1L, 2L);
            softly.assertThat(result.get(0).response()).isEqualTo(pkg(1));
        });
    }

    @Test
    void packages_shouldUseOwningProject_whenListedByProject() throws IOException {
        when(gitLab.packages().list(ID).stream()).thenReturn(Stream.of(pkg(1)));

        final ProjectPackage result = project.packages().findFirst().orElseThrow();

        assertThat(result.project()).isSameAs(project);
    }

    @Test
    void packages_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.packages().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return pkg(id);
                }));

        final Stream<ProjectPackage> stream = project.packages();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void packages_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectPackagesListFilter filter = GitLabProjectPackagesListFilterBuilder.builder()
                .packageType(GitLabProjectPackagesListPackageType.NPM)
                .build();
        when(gitLab.packages().list(ID, filter).stream()).thenReturn(Stream.of(pkg(1)));

        final List<ProjectPackage> result = project.packages(filter).toList();

        assertThat(result).extracting(ProjectPackage::id).containsExactly(1L);
    }

    @Test
    void packages_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> project.packages(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void projectPackage_shouldReturnPackage_whenFound() throws IOException {
        when(gitLab.packages().get(ID, 7L)).thenReturn(Optional.of(fullPackage(7)));

        final Optional<ProjectPackage> result = project.projectPackage(7L);

        assertThat(result)
                .hasValueSatisfying(found -> assertThat(found.version()).contains("1.2.3"));
    }

    @Test
    void projectPackage_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.packages().get(ID, 7L)).thenReturn(Optional.empty());

        final Optional<ProjectPackage> result = project.projectPackage(7L);

        assertThat(result).isEmpty();
    }
}
