package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiff;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiffBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class DiffVersionTest {

    @Test
    void accessors_shouldReturnResponseValues_whenTheVersionIsLoaded() {
        final Instant created = Instant.parse("2026-01-02T03:04:05Z");
        final GitLabMergeRequestDiff response = GitLabMergeRequestDiffBuilder.builder()
                .id(4L)
                .headCommitSha("head")
                .baseCommitSha("base")
                .startCommitSha("start")
                .createdAt(created)
                .state("collected")
                .build();

        final DiffVersion sut = new DiffVersion(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(4L);
            softly.assertThat(sut.headCommitSha()).contains("head");
            softly.assertThat(sut.baseCommitSha()).contains("base");
            softly.assertThat(sut.startCommitSha()).contains("start");
            softly.assertThat(sut.createdAt()).contains(created);
            softly.assertThat(sut.state()).contains("collected");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabDoesNotSayAnything() {
        final DiffVersion sut =
                new DiffVersion(GitLabMergeRequestDiffBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.headCommitSha()).isEmpty();
            softly.assertThat(sut.baseCommitSha()).isEmpty();
            softly.assertThat(sut.startCommitSha()).isEmpty();
            softly.assertThat(sut.createdAt()).isEmpty();
            softly.assertThat(sut.state()).isEmpty();
        });
    }

    @Test
    void id_shouldThrowNullPointerException_whenGitLabReturnsNoId() {
        final DiffVersion sut =
                new DiffVersion(GitLabMergeRequestDiffBuilder.builder().build());

        final Throwable thrown = catchThrowable(sut::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new DiffVersion(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }
}
