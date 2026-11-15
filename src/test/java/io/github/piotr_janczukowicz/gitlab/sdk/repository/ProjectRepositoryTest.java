package io.github.piotr_janczukowicz.gitlab.sdk.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabChangelogBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabCompare;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabCompareBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabContributorBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesGetChangelogFilter;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesGetChangelogFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesGetCompareFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesListTreeFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabTreeObject;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabTreeObjectBuilder;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectRepositoryTest {

    private static final String ID = "my-group/my-project";
    private static final String SHA = "abc123def456";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    private static GitLabTreeObject entry(final String path, final String type) {
        return GitLabTreeObjectBuilder.builder()
                .path(path)
                .name(path.substring(path.lastIndexOf('/') + 1))
                .type(type)
                .build();
    }

    @Test
    void tree_shouldSendNoParameters_whenRootOfDefaultBranchIsListed() throws IOException {
        when(gitLab
                        .repositories()
                        .listTree(
                                ID,
                                GitLabProjectRepositoriesListTreeFilterBuilder.builder()
                                        .recursive(false)
                                        .build())
                        .stream())
                .thenReturn(Stream.of(entry("src", "tree"), entry("README.md", "blob")));

        final List<TreeEntry> result = sut.tree().toList();

        assertThat(result).extracting(TreeEntry::path).containsExactly("src", "README.md");
    }

    @Test
    void tree_shouldSendPathRefAndRecursive_whenGiven() throws IOException {
        when(gitLab
                        .repositories()
                        .listTree(
                                ID,
                                GitLabProjectRepositoriesListTreeFilterBuilder.builder()
                                        .path("src")
                                        .ref("dev")
                                        .recursive(true)
                                        .build())
                        .stream())
                .thenReturn(Stream.of(entry("src/App.java", "blob")));

        final List<TreeEntry> result = sut.tree("src", "dev", true).toList();

        assertThat(result).extracting(TreeEntry::name).containsExactly("App.java");
    }

    @Test
    void tree_shouldReturnEmptyStream_whenDirectoryIsEmpty() throws IOException {
        when(gitLab
                        .repositories()
                        .listTree(
                                ID,
                                GitLabProjectRepositoriesListTreeFilterBuilder.builder()
                                        .recursive(false)
                                        .build())
                        .stream())
                .thenReturn(Stream.empty());

        final List<TreeEntry> result = sut.tree().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void tree_shouldRefuseBlankPathAndRef_whenBlank() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.tree(" ", null, false)).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.tree(null, " ", false)).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void compare_shouldCompareWithMergeBase_whenNotStraight() throws IOException {
        final GitLabCompare response =
                GitLabCompareBuilder.builder().compareSameRef(false).build();
        when(gitLab.repositories()
                        .getCompare(
                                ID,
                                "main",
                                "feature",
                                GitLabProjectRepositoriesGetCompareFilterBuilder.builder()
                                        .straight(false)
                                        .build()))
                .thenReturn(response);

        final Comparison result = sut.compare("main", "feature", false);

        assertThat(result.response()).isSameAs(response);
    }

    @Test
    void compare_shouldSendStraight_whenStraight() throws IOException {
        final GitLabCompare response = GitLabCompareBuilder.builder().build();
        when(gitLab.repositories()
                        .getCompare(
                                ID,
                                "main",
                                "feature",
                                GitLabProjectRepositoriesGetCompareFilterBuilder.builder()
                                        .straight(true)
                                        .build()))
                .thenReturn(response);

        final Comparison result = sut.compare("main", "feature", true);

        assertThat(result.response()).isSameAs(response);
    }

    @Test
    void compare_shouldRefuseBlankRef_whenBlank() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.compare(" ", "b", false)).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.compare("a", " ", false)).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void contributors_shouldListContributors_whenGitLabReturnsThem() throws IOException {
        when(gitLab.repositories().listContributors(ID).stream())
                .thenReturn(Stream.of(GitLabContributorBuilder.builder()
                        .name("Ada")
                        .email("ada@example.com")
                        .commits(3)
                        .additions(10)
                        .deletions(2)
                        .build()));

        final List<Contributor> result = sut.contributors().toList();

        assertThat(result).extracting(Contributor::name).containsExactly("Ada");
    }

    @Test
    void mergeBase_shouldLoadCommit_whenGitLabReturnsMergeBase() throws IOException {
        final GitLabCommit listed = GitLabCommitBuilder.builder().id(SHA).build();
        final GitLabCommitDetail loaded =
                GitLabCommitDetailBuilder.builder().id(SHA).build();
        when(gitLab.repositories().getMergeBase(ID, List.of("main", "feature"))).thenReturn(listed);
        when(gitLab.commits().get(ID, SHA)).thenReturn(Optional.of(loaded));

        final Optional<Commit> result = sut.mergeBase("main", "feature");

        assertThat(result).map(Commit::response).containsSame(loaded);
    }

    @Test
    void mergeBase_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.repositories().getMergeBase(ID, List.of("a", "b"))).thenThrow(mock(GitLabNotFoundException.class));

        final Optional<Commit> result = sut.mergeBase("a", "b");

        assertThat(result).isEmpty();
    }

    @Test
    void mergeBase_shouldReturnEmpty_whenCommitIsGoneAfterLookup() throws IOException {
        when(gitLab.repositories().getMergeBase(ID, List.of("a", "b")))
                .thenReturn(GitLabCommitBuilder.builder().id(SHA).build());
        when(gitLab.commits().get(ID, SHA)).thenReturn(Optional.empty());

        final Optional<Commit> result = sut.mergeBase("a", "b");

        assertThat(result).isEmpty();
    }

    @Test
    void mergeBase_shouldPassOtherExceptionsThrough_whenGitLabRefuses() throws IOException {
        final GitLabForbiddenException refused = mock(GitLabForbiddenException.class);
        when(gitLab.repositories().getMergeBase(ID, List.of("a", "b"))).thenThrow(refused);

        final Throwable thrown = catchThrowable(() -> sut.mergeBase("a", "b"));

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void mergeBase_shouldRefuseBadRefs_whenFewerThanTwoBlankOrNull() throws IOException {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.mergeBase("main")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.mergeBase("main", " ")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.mergeBase((String[]) null)).isInstanceOf(NullPointerException.class);
        });
        verify(gitLab.repositories(), never()).getMergeBase(ID, List.of("main"));
    }

    @Test
    void deleteMergedBranches_shouldRequestDeletion_whenCalled() throws IOException {
        sut.deleteMergedBranches();

        verify(gitLab.branches()).deleteMergedBranches(ID);
    }

    @Test
    void changelog_shouldReturnNotes_whenVersionGiven() throws IOException {
        when(gitLab.repositories().getChangelog(ID, "1.2.0"))
                .thenReturn(GitLabChangelogBuilder.builder().notes("## 1.2.0").build());

        final String notes = sut.changelog("1.2.0");

        assertThat(notes).isEqualTo("## 1.2.0");
    }

    @Test
    void changelog_shouldPassFilter_whenFilterGiven() throws IOException {
        final GitLabProjectRepositoriesGetChangelogFilter filter =
                GitLabProjectRepositoriesGetChangelogFilterBuilder.builder()
                        .fromValue("v1.1.0")
                        .build();
        when(gitLab.repositories().getChangelog(ID, "1.2.0", filter))
                .thenReturn(GitLabChangelogBuilder.builder().notes("x").build());

        final String notes = sut.changelog("1.2.0", filter);

        assertThat(notes).isEqualTo("x");
    }

    @Test
    void changelog_shouldThrow_whenGitLabReturnsNoNotes() throws IOException {
        when(gitLab.repositories().getChangelog(ID, "1.2.0"))
                .thenReturn(GitLabChangelogBuilder.builder().build());

        assertThatThrownBy(() -> sut.changelog("1.2.0")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void changelog_shouldRefuseVersion_whenBlankOrNull() throws IOException {
        final GitLabProjectRepositoriesGetChangelogFilter filter =
                GitLabProjectRepositoriesGetChangelogFilterBuilder.builder().build();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.changelog(" ")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.changelog(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.changelog(" ", filter)).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.changelog(null, filter)).isInstanceOf(NullPointerException.class);
        });
        verify(gitLab.repositories(), never()).getChangelog(ID, " ");
    }

    @Test
    void changelog_shouldRefuseNullFilter_whenFilterIsNull() {
        assertThatThrownBy(() -> sut.changelog("1.2.0", null)).isInstanceOf(NullPointerException.class);
    }
}
