package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiffBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiffFull;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiffFullBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class MergeRequestDiffTest {

    @Test
    void accessors_shouldReturnResponseValues_whenTheVersionIsLoaded() {
        final Instant created = Instant.parse("2026-01-02T03:04:05Z");
        final GitLabCommit commit = GitLabCommitBuilder.builder().id("abc").build();
        final GitLabDiff diff = GitLabDiffBuilder.builder().newPath("a.txt").build();
        final GitLabMergeRequestDiffFull response = GitLabMergeRequestDiffFullBuilder.builder()
                .id(4L)
                .headCommitSha("head")
                .baseCommitSha("base")
                .startCommitSha("start")
                .createdAt(created)
                .state("collected")
                .commits(List.of(commit))
                .diffs(List.of(diff))
                .build();

        final MergeRequestDiff sut = new MergeRequestDiff(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(4L);
            softly.assertThat(sut.headCommitSha()).contains("head");
            softly.assertThat(sut.baseCommitSha()).contains("base");
            softly.assertThat(sut.startCommitSha()).contains("start");
            softly.assertThat(sut.createdAt()).contains(created);
            softly.assertThat(sut.state()).contains("collected");
            softly.assertThat(sut.commits()).containsExactly(commit);
            softly.assertThat(sut.diffs()).containsExactly(diff);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabDoesNotSayAnything() {
        final MergeRequestDiff sut =
                new MergeRequestDiff(GitLabMergeRequestDiffFullBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.headCommitSha()).isEmpty();
            softly.assertThat(sut.baseCommitSha()).isEmpty();
            softly.assertThat(sut.startCommitSha()).isEmpty();
            softly.assertThat(sut.createdAt()).isEmpty();
            softly.assertThat(sut.state()).isEmpty();
            softly.assertThat(sut.commits()).isEmpty();
            softly.assertThat(sut.diffs()).isEmpty();
        });
    }

    @Test
    void lists_shouldReturnUnmodifiableCopies_whenTheResponseListsAreMutable() {
        final List<GitLabDiff> diffs =
                new ArrayList<>(List.of(GitLabDiffBuilder.builder().build()));
        final List<GitLabCommit> commits =
                new ArrayList<>(List.of(GitLabCommitBuilder.builder().build()));
        final MergeRequestDiff sut = new MergeRequestDiff(GitLabMergeRequestDiffFullBuilder.builder()
                .diffs(diffs)
                .commits(commits)
                .build());

        final List<GitLabDiff> diffCopy = sut.diffs();
        final List<GitLabCommit> commitCopy = sut.commits();
        diffs.clear();
        commits.clear();

        assertSoftly(softly -> {
            softly.assertThat(diffCopy).hasSize(1);
            softly.assertThat(commitCopy).hasSize(1);
            softly.assertThatThrownBy(diffCopy::clear).isInstanceOf(UnsupportedOperationException.class);
            softly.assertThatThrownBy(commitCopy::clear).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void id_shouldThrowNullPointerException_whenGitLabReturnsNoId() {
        final MergeRequestDiff sut =
                new MergeRequestDiff(GitLabMergeRequestDiffFullBuilder.builder().build());

        final Throwable thrown = catchThrowable(sut::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new MergeRequestDiff(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }
}
