package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.groupPkg;
import static io.github.piotr_janczukowicz.gitlab.sdk.packages.PackagesFixtures.pkg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListPackagesFilter;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListPackagesFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListPackagesPackageType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupPackagesTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    @Test
    void packages_shouldWrapEachItem_whenListed() throws IOException {
        when(gitLab.groups().listPackages(ID).stream()).thenReturn(Stream.of(groupPkg(1, 11), groupPkg(2, 12)));

        final List<ProjectPackage> result = group.packages().toList();

        assertThat(result).extracting(ProjectPackage::id).containsExactly(1L, 2L);
    }

    @Test
    void project_shouldReturnHandleWithoutRequest_whenListedByGroup() throws IOException {
        when(gitLab.groups().listPackages(ID).stream()).thenReturn(Stream.of(groupPkg(1, 11)));
        final ProjectPackage item = group.packages().findFirst().orElseThrow();

        final Project result = item.project();

        assertThat(result.id()).isEqualTo("11");
        verify(gitLab, never()).projects();
    }

    @Test
    void project_shouldThrowIllegalStateException_whenListedWithoutProjectId() throws IOException {
        when(gitLab.groups().listPackages(ID).stream()).thenReturn(Stream.of(pkg(1)));
        final ProjectPackage item = group.packages().findFirst().orElseThrow();

        final Throwable thrown = catchThrowable(item::project);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void packages_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listPackages(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return groupPkg(id, 11);
                }));

        final Stream<ProjectPackage> stream = group.packages();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void packages_shouldPassFilter_whenGiven() throws IOException {
        final GitLabGroupGroupsListPackagesFilter filter = GitLabGroupGroupsListPackagesFilterBuilder.builder()
                .packageType(GitLabGroupGroupsListPackagesPackageType.NPM)
                .build();
        when(gitLab.groups().listPackages(ID, filter).stream()).thenReturn(Stream.of(groupPkg(1, 11)));

        final List<ProjectPackage> result = group.packages(filter).toList();

        assertThat(result).extracting(ProjectPackage::id).containsExactly(1L);
    }

    @Test
    void packages_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> group.packages(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }
}
