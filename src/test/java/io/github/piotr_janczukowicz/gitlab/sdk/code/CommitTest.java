package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.diff;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.listedCommit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCherryPickPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsRevertPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitTest {

    private static final String ID = "my-group/my-project";
    private static final String SHA = "abcdef0123456789";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabCommitDetail response = commit(SHA, "Fix it");
    private final Commit sut = new Commit(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenCommitIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.sha()).isEqualTo(SHA);
            softly.assertThat(sut.message()).contains("Fix it");
            softly.assertThat(sut.title()).contains("Fix it");
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void diff_shouldListChangedFiles_whenGitLabReturnsThem() throws IOException {
        final GitLabDiff first = diff("a.txt");
        final GitLabDiff second = diff("b.txt");
        when(gitLab.commits().listDiff(ID, SHA).stream()).thenReturn(Stream.of(first, second));

        final List<GitLabDiff> result = sut.diff().toList();

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void diff_shouldReturnEmptyStream_whenCommitChangesNothing() throws IOException {
        when(gitLab.commits().listDiff(ID, SHA).stream()).thenReturn(Stream.empty());

        final List<GitLabDiff> result = sut.diff().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void cherryPick_shouldReturnNewCommit_whenGitLabAnswers() throws IOException {
        final GitLabCommitDetail loaded = commit("1111111111", "Fix it");
        when(gitLab.commits()
                        .cherryPick(
                                ID,
                                SHA,
                                GitLabProjectCommitsCherryPickPayloadBuilder.builder()
                                        .branch("release")
                                        .build()))
                .thenReturn(listedCommit("1111111111"));
        when(gitLab.commits().get(ID, "1111111111")).thenReturn(Optional.of(loaded));

        final Commit result = sut.cherryPick("release");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(loaded);
            softly.assertThat(result.sha()).isEqualTo("1111111111");
            softly.assertThat(sut.sha()).isEqualTo(SHA);
        });
    }

    @Test
    void cherryPick_shouldThrowIllegalStateException_whenNewCommitIsGone() throws IOException {
        when(gitLab.commits()
                        .cherryPick(
                                ID,
                                SHA,
                                GitLabProjectCommitsCherryPickPayloadBuilder.builder()
                                        .branch("release")
                                        .build()))
                .thenReturn(listedCommit("1111111111"));
        when(gitLab.commits().get(ID, "1111111111")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.cherryPick("release"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Commit 1111111111 is gone");
    }

    @Test
    void cherryPick_shouldThrowIllegalArgumentException_whenBranchIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.cherryPick(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revert_shouldReturnNewCommit_whenGitLabAnswers() throws IOException {
        final GitLabCommitDetail loaded = commit("2222222222", "Revert it");
        when(gitLab.commits()
                        .revert(
                                ID,
                                SHA,
                                GitLabProjectCommitsRevertPayloadBuilder.builder()
                                        .branch("main")
                                        .build()))
                .thenReturn(listedCommit("2222222222"));
        when(gitLab.commits().get(ID, "2222222222")).thenReturn(Optional.of(loaded));

        final Commit result = sut.revert("main");

        assertThat(result.response()).isSameAs(loaded);
        verify(gitLab.commits()).get(ID, "2222222222");
    }
}
