package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.file;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRange;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRangeBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.BlameRange;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryFileBlameTest {

    private static final String ID = "my-group/my-project";
    private static final String PATH = "src/App.java";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private RepositoryFile fileOf(final String text) {
        return new RepositoryFile(project, "main", file(PATH, text.getBytes(StandardCharsets.UTF_8)));
    }

    private static GitLabBlameRange range(final String... lines) {
        return GitLabBlameRangeBuilder.builder().lines(List.of(lines)).build();
    }

    @Test
    void blame_shouldBlameWholeFile_whenFileHasLines() throws IOException {
        when(gitLab.repositoryfiles().listBlame(ID, PATH, "main", 1, 3)).thenReturn(List.of(range("a", "b", "c")));

        final List<BlameRange> result = fileOf("a\nb\nc\n").blame().toList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lines()).containsExactly("a", "b", "c");
    }

    @Test
    void blame_shouldReturnEmptyWithoutRequest_whenFileIsEmpty() throws IOException {
        final List<BlameRange> result = fileOf("").blame().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).isEmpty();
            verifyNoInteractions(gitLab.repositoryfiles());
        });
    }

    @Test
    void blame_shouldSendGivenRange_whenRangeIsGiven() throws IOException {
        when(gitLab.repositoryfiles().listBlame(ID, PATH, "main", 2, 4)).thenReturn(List.of(range("b")));

        final List<BlameRange> result = fileOf("a\nb\n").blame(2, 4).toList();

        assertThat(result).hasSize(1);
        verify(gitLab.repositoryfiles()).listBlame(ID, PATH, "main", 2, 4);
    }

    @Test
    void blame_shouldRefuseRange_whenStartIsBelowOneOrEndBeforeStart() {
        final RepositoryFile sut = fileOf("a\n");

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.blame(0, 1)).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.blame(3, 2)).isInstanceOf(IllegalArgumentException.class);
        });
    }
}
