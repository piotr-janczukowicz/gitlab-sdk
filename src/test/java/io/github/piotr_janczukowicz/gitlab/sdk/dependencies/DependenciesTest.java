package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import static io.github.piotr_janczukowicz.gitlab.sdk.dependencies.DependenciesFixtures.dependency;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabProjectDependenciesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabProjectDependenciesListPackageManager;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class DependenciesTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Dependencies sut = project.dependencies();

    @Test
    void dependencies_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).project(ID).dependencies();

        verifyNoInteractions(untouched);
    }

    @Test
    void forProject_shouldThrowNullPointerException_whenProjectIsNull() {
        final Throwable thrown = catchThrowable(() -> Dependencies.forProject(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void all_shouldWrapEachListedDependency_whenListed() throws IOException {
        when(gitLab.dependencies().list(ID).stream()).thenReturn(Stream.of(dependency("rails"), dependency("rack")));

        final List<Dependency> result = sut.all().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Dependency::name).containsExactly("rails", "rack");
            softly.assertThat(result.getFirst().project()).isSameAs(project);
        });
        verify(gitLab.dependencies().list(ID), times(1)).stream();
    }

    @Test
    void all_shouldBeLazy_whenNotConsumed() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.dependencies().list(ID).stream())
                .thenReturn(Stream.of("rails", "rack").map(name -> {
                    consumed.incrementAndGet();
                    return dependency(name);
                }));

        final Stream<Dependency> result = sut.all();

        assertThat(result).isNotNull();
        assertThat(consumed).hasValue(0);
    }

    @Test
    void all_shouldSendNoFilter_whenNoneGiven() throws IOException {
        when(gitLab.dependencies().list(ID).stream()).thenReturn(Stream.of(dependency("rails")));

        sut.all().toList();

        verify(gitLab.dependencies().list(ID), times(1)).stream();
        verify(gitLab.dependencies(), never()).list(anyString(), any(GitLabProjectDependenciesListFilter.class));
        verify(gitLab.dependencies(), never()).list(anyString(), any(GitLabPageRequest.class));
    }

    @Test
    void all_shouldPassPackageManagers_whenGiven() throws IOException {
        when(gitLab.dependencies().list(eq(ID), any(GitLabProjectDependenciesListFilter.class)).stream())
                .thenReturn(Stream.of(dependency("rails")));

        final List<Dependency> result = sut.all(
                        GitLabProjectDependenciesListPackageManager.BUNDLER,
                        GitLabProjectDependenciesListPackageManager.NPM,
                        GitLabProjectDependenciesListPackageManager.MAVEN)
                .toList();

        final ArgumentCaptor<GitLabProjectDependenciesListFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectDependenciesListFilter.class);
        verify(gitLab.dependencies(), times(1)).list(eq(ID), filter.capture());
        assertSoftly(softly -> {
            softly.assertThat(filter.getValue().packageManager())
                    .containsExactly(
                            GitLabProjectDependenciesListPackageManager.BUNDLER,
                            GitLabProjectDependenciesListPackageManager.NPM,
                            GitLabProjectDependenciesListPackageManager.MAVEN);
            softly.assertThat(result).extracting(Dependency::name).containsExactly("rails");
        });
        verify(gitLab.dependencies(), never()).list(ID);
    }

    @Test
    void all_shouldPassOnePackageManager_whenOnlyFirstGiven() throws IOException {
        when(gitLab.dependencies().list(eq(ID), any(GitLabProjectDependenciesListFilter.class)).stream())
                .thenReturn(Stream.empty());

        sut.all(GitLabProjectDependenciesListPackageManager.GO).toList();

        final ArgumentCaptor<GitLabProjectDependenciesListFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectDependenciesListFilter.class);
        verify(gitLab.dependencies(), times(1)).list(eq(ID), filter.capture());
        assertThat(filter.getValue().packageManager()).containsExactly(GitLabProjectDependenciesListPackageManager.GO);
    }

    @Test
    void all_shouldRejectNullPackageManager_withoutCoreCall() {
        final GitLab untouched = mock(GitLab.class);
        final Dependencies guarded = GitLabSdk.of(untouched).project(ID).dependencies();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> guarded.all(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> guarded.all(
                            GitLabProjectDependenciesListPackageManager.NPM,
                            (GitLabProjectDependenciesListPackageManager) null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> guarded.all(
                            GitLabProjectDependenciesListPackageManager.NPM,
                            (GitLabProjectDependenciesListPackageManager[]) null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(untouched);
    }

    @Test
    void all_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier =
                new GitLabTierException("getApiV4ProjectsIdDependencies", GitLabTier.ULTIMATE, GitLabTier.FREE);
        when(gitLab.dependencies().list(ID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(sut::all);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void all_shouldPassTierException_whenFilteredAndCoreRefusesTier() throws IOException {
        final GitLabTierException tier =
                new GitLabTierException("getApiV4ProjectsIdDependencies", GitLabTier.ULTIMATE, GitLabTier.FREE);
        when(gitLab.dependencies().list(eq(ID), any(GitLabProjectDependenciesListFilter.class)))
                .thenThrow(tier);

        final Throwable thrown = catchThrowable(() -> sut.all(GitLabProjectDependenciesListPackageManager.NPM));

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void all_shouldPassForbiddenException_whenTierMissing() throws IOException {
        final GitLabForbiddenException forbidden = mock(GitLabForbiddenException.class);
        when(gitLab.dependencies().list(ID)).thenThrow(forbidden);

        final Throwable thrown = catchThrowable(sut::all);

        assertThat(thrown).isSameAs(forbidden);
    }
}
