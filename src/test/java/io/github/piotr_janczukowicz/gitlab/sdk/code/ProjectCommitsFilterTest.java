package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.listedCommit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectCommitsFilterTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void commits_shouldPassFilterToCoreAndLoadEachItem_whenFilterIsGiven() throws IOException {
        final GitLabProjectCommitsListFilter filter = GitLabProjectCommitsListFilterBuilder.builder()
                .refName("dev")
                .since(Instant.parse("2026-01-01T00:00:00Z"))
                .until(Instant.parse("2026-02-01T00:00:00Z"))
                .path("README.md")
                .all(false)
                .withStats(true)
                .build();
        when(gitLab.commits().list(ID, filter).stream()).thenReturn(Stream.of(listedCommit("aaa111")));
        when(gitLab.commits().get(ID, "aaa111")).thenReturn(Optional.of(commit("aaa111", "One")));

        final List<Commit> result = sut.commits(filter).toList();

        assertThat(result).extracting(Commit::sha).containsExactly("aaa111");
    }

    @Test
    void commits_shouldSkipItem_whenGoneBeforeLoadedWithFilter() throws IOException {
        final GitLabProjectCommitsListFilter filter =
                GitLabProjectCommitsListFilterBuilder.builder().path("a").build();
        when(gitLab.commits().list(ID, filter).stream()).thenReturn(Stream.of(listedCommit("aaa111")));
        when(gitLab.commits().get(ID, "aaa111")).thenReturn(Optional.empty());

        final List<Commit> result = sut.commits(filter).toList();

        assertThat(result).isEmpty();
    }

    @Test
    void commits_shouldThrowNullPointerException_whenFilterIsNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.commits(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.commits(), never()).list(ID, GitLabProjectCommitsListFilter.empty());
    }
}
