package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commitNote;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitNote;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitCommentTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project("my-group/my-project");
    private final Commit commit = new Commit(project, commit("abcdef0123", "Fix it"));

    @Test
    void accessors_shouldReturnResponseValues_whenCommentIsLoaded() {
        final GitLabCommitNote response = commitNote("Nice", "src/App.java", 11, "new");

        final CommitComment sut = new CommitComment(commit, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.body()).contains("Nice");
            softly.assertThat(sut.path()).contains("src/App.java");
            softly.assertThat(sut.line()).contains(11);
            softly.assertThat(sut.lineType()).contains("new");
            softly.assertThat(sut.commit()).isSameAs(commit);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabLeavesFieldsOut() {
        final CommitComment sut =
                new CommitComment(commit, GitLabCommitNoteBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.body()).isEmpty();
            softly.assertThat(sut.path()).isEmpty();
            softly.assertThat(sut.line()).isEmpty();
            softly.assertThat(sut.lineType()).isEmpty();
        });
    }
}
