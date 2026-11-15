package io.github.piotr_janczukowicz.gitlab.sdk.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiffBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabCompare;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabCompareBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabContributor;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabContributorBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabTreeObject;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabTreeObjectBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRange;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRangeBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRangeCommitBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryTypesTest {

    @Test
    void treeEntry_shouldExposeValues_whenResponseIsComplete() {
        final GitLabCommit last = GitLabCommitBuilder.builder().id("c1").build();
        final GitLabTreeObject response = GitLabTreeObjectBuilder.builder()
                .id("sha")
                .name("src")
                .path("a/src")
                .mode("040000")
                .type("tree")
                .lastCommit(last)
                .build();

        final TreeEntry sut = new TreeEntry(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).contains("sha");
            softly.assertThat(sut.name()).isEqualTo("src");
            softly.assertThat(sut.path()).isEqualTo("a/src");
            softly.assertThat(sut.mode()).contains("040000");
            softly.assertThat(sut.isDirectory()).isTrue();
            softly.assertThat(sut.isFile()).isFalse();
            softly.assertThat(sut.lastCommit()).contains(last);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void treeEntry_shouldBeFileAndEmptyOptionals_whenTypeIsBlobAndFieldsAreMissing() {
        final TreeEntry sut =
                new TreeEntry(GitLabTreeObjectBuilder.builder().type("blob").build());

        assertSoftly(softly -> {
            softly.assertThat(sut.isFile()).isTrue();
            softly.assertThat(sut.isDirectory()).isFalse();
            softly.assertThat(sut.id()).isEmpty();
            softly.assertThat(sut.mode()).isEmpty();
            softly.assertThat(sut.lastCommit()).isEmpty();
        });
    }

    @Test
    void treeEntry_shouldBeNeitherFileNorDirectory_whenTypeIsSubmodule() {
        final TreeEntry sut =
                new TreeEntry(GitLabTreeObjectBuilder.builder().type("commit").build());

        assertSoftly(softly -> {
            softly.assertThat(sut.isFile()).isFalse();
            softly.assertThat(sut.isDirectory()).isFalse();
        });
    }

    @Test
    void treeEntry_shouldThrow_whenPathOrNameIsMissing() {
        final TreeEntry sut = new TreeEntry(GitLabTreeObjectBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThatThrownBy(sut::path).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(sut::name).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void comparison_shouldExposeValues_whenResponseIsComplete() {
        final GitLabCommit commit = GitLabCommitBuilder.builder().id("c1").build();
        final GitLabDiff diff = GitLabDiffBuilder.builder().newPath("a.txt").build();
        final GitLabCompare response = GitLabCompareBuilder.builder()
                .commit(commit)
                .commits(List.of(commit))
                .diffs(List.of(diff))
                .compareTimeout(true)
                .compareSameRef(true)
                .webUrl("https://gitlab.example.com/compare")
                .build();

        final Comparison sut = new Comparison(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.commit()).contains(commit);
            softly.assertThat(sut.commits()).containsExactly(commit);
            softly.assertThat(sut.diffs()).containsExactly(diff);
            softly.assertThat(sut.isTimedOut()).isTrue();
            softly.assertThat(sut.isSameRef()).isTrue();
            softly.assertThat(sut.webUrl()).contains("https://gitlab.example.com/compare");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void comparison_shouldReturnEmptyValues_whenResponseHasNoFields() {
        final Comparison sut = new Comparison(GitLabCompareBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.commit()).isEmpty();
            softly.assertThat(sut.commits()).isEmpty();
            softly.assertThat(sut.diffs()).isEmpty();
            softly.assertThat(sut.isTimedOut()).isFalse();
            softly.assertThat(sut.isSameRef()).isFalse();
            softly.assertThat(sut.webUrl()).isEmpty();
        });
    }

    @Test
    void contributor_shouldExposeValues_whenResponseIsComplete() {
        final GitLabContributor response = GitLabContributorBuilder.builder()
                .name("Ada")
                .email("ada@example.com")
                .commits(3)
                .additions(10)
                .deletions(2)
                .build();

        final Contributor sut = new Contributor(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("Ada");
            softly.assertThat(sut.email()).isEqualTo("ada@example.com");
            softly.assertThat(sut.commits()).isEqualTo(3);
            softly.assertThat(sut.additions()).isEqualTo(10);
            softly.assertThat(sut.deletions()).isEqualTo(2);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void contributor_shouldThrow_whenFieldIsMissing() {
        final Contributor sut =
                new Contributor(GitLabContributorBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThatThrownBy(sut::name).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(sut::email).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(sut::commits).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(sut::additions).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(sut::deletions).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void blameRange_shouldExposeValues_whenResponseIsComplete() {
        final GitLabBlameRange response = GitLabBlameRangeBuilder.builder()
                .commit(GitLabBlameRangeCommitBuilder.builder().id("c1").build())
                .lines(List.of("a", "b"))
                .build();

        final BlameRange sut = new BlameRange(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.commit().id()).isEqualTo("c1");
            softly.assertThat(sut.lines()).containsExactly("a", "b");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void blameRange_shouldHaveNoLinesAndThrowOnCommit_whenResponseHasNoFields() {
        final BlameRange sut = new BlameRange(GitLabBlameRangeBuilder.builder().build());

        assertThat(sut.lines()).isEmpty();
        assertThatThrownBy(sut::commit).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenRepositoryRequestsIsInstantiated()
            throws ReflectiveOperationException {
        final Constructor<RepositoryRequests> constructor = RepositoryRequests.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mergeBaseRefs_shouldReturnRefs_whenTwoOrMoreAreGiven() {
        final List<String> result = RepositoryRequests.mergeBaseRefs("a", "b", "c");

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void mergeBaseRefs_shouldRefuse_whenFewerThanTwoOrBlankOrNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> RepositoryRequests.mergeBaseRefs("a"))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> RepositoryRequests.mergeBaseRefs("a", " "))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> RepositoryRequests.mergeBaseRefs((String[]) null))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void lineCount_shouldCountLinesLikeBlame_whenContentVaries() {
        assertSoftly(softly -> {
            softly.assertThat(RepositoryRequests.lineCount(new byte[0])).isZero();
            softly.assertThat(RepositoryRequests.lineCount("a".getBytes())).isEqualTo(1);
            softly.assertThat(RepositoryRequests.lineCount("a\n".getBytes())).isEqualTo(1);
            softly.assertThat(RepositoryRequests.lineCount("a\nb".getBytes())).isEqualTo(2);
            softly.assertThat(RepositoryRequests.lineCount("a\r\nb\r\n".getBytes()))
                    .isEqualTo(2);
            softly.assertThat(RepositoryRequests.lineCount("\n\n".getBytes())).isEqualTo(2);
        });
    }
}
