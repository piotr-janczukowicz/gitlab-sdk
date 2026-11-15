package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commitStatus;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitStatus;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitStatusBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitStatusTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project("my-group/my-project");
    private final Commit commit = new Commit(project, commit("abcdef0123", "Fix it"));

    @Test
    void accessors_shouldReturnResponseValues_whenStatusIsLoaded() {
        final GitLabCommitStatus response = commitStatus(5L, "success", "build");

        final CommitStatus sut = new CommitStatus(commit, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).contains(5L);
            softly.assertThat(sut.status()).contains("success");
            softly.assertThat(sut.name()).contains("build");
            softly.assertThat(sut.ref()).contains("main");
            softly.assertThat(sut.description()).contains("described");
            softly.assertThat(sut.targetUrl()).contains("https://ci.example/1");
            softly.assertThat(sut.commit()).isSameAs(commit);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabLeavesFieldsOut() {
        final CommitStatus sut =
                new CommitStatus(commit, GitLabCommitStatusBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEmpty();
            softly.assertThat(sut.status()).isEmpty();
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.ref()).isEmpty();
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.targetUrl()).isEmpty();
        });
    }
}
