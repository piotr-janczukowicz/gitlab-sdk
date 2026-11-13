package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.branch;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.file;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.listedCommit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranch;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponse;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectCodeTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void file_shouldReturnLoadedFile_whenGitLabHasIt() throws IOException {
        final GitLabProjectRepositoryFilesGetResponse response = file("src/App.java", new byte[] {1});
        when(gitLab.repositoryfiles().get(ID, "src/App.java", "main")).thenReturn(Optional.of(response));

        final Optional<RepositoryFile> found = sut.file("src/App.java", "main");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().ref()).isEqualTo("main");
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void file_shouldReturnEmpty_whenMissing() throws IOException {
        when(gitLab.repositoryfiles().get(ID, "nope.txt", "main")).thenReturn(Optional.empty());

        final Optional<RepositoryFile> found = sut.file("nope.txt", "main");

        assertThat(found).isEmpty();
    }

    @Test
    void file_shouldPassNumericProjectIdUnchanged_whenProjectIsNumeric() throws IOException {
        final GitLab other = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(other.repositoryfiles().get("42", "a.txt", "dev")).thenReturn(Optional.of(file("a.txt", new byte[0])));

        final Optional<RepositoryFile> found = GitLabSdk.of(other).project("42").file("a.txt", "dev");

        assertThat(found).isPresent();
    }

    @Test
    void branches_shouldListBranches_whenGitLabReturnsThem() throws IOException {
        when(gitLab.branches().list(ID).stream()).thenReturn(Stream.of(branch("main", true), branch("dev", false)));

        final List<Branch> result = sut.branches().toList();

        assertThat(result).extracting(Branch::name).containsExactly("main", "dev");
    }

    @Test
    void branches_shouldReturnEmptyStream_whenProjectHasNoBranches() throws IOException {
        when(gitLab.branches().list(ID).stream()).thenReturn(Stream.empty());

        final List<Branch> result = sut.branches().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void branches_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.branches().list(ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return branch(name, false);
                }));

        final Stream<Branch> stream = sut.branches();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void branch_shouldReturnLoadedBranch_whenGitLabHasIt() throws IOException {
        final GitLabBranch response = branch("feature/x", false);
        when(gitLab.branches().get(ID, "feature/x")).thenReturn(Optional.of(response));

        final Optional<Branch> found = sut.branch("feature/x");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void branch_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.branches().get(ID, "gone")).thenReturn(Optional.empty());

        final Optional<Branch> found = sut.branch("gone");

        assertThat(found).isEmpty();
    }

    @Test
    void createBranch_shouldReturnBranch_whenGitLabCreatesIt() throws IOException {
        final GitLabBranch created = branch("dev", false);
        when(gitLab.branches()
                        .create(
                                ID,
                                GitLabProjectBranchesCreatePayloadBuilder.builder()
                                        .branch("dev")
                                        .ref("main")
                                        .build()))
                .thenReturn(created);

        final Branch result = sut.createBranch("dev", "main");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.project()).isSameAs(sut);
        });
    }

    @Test
    void commit_shouldReturnLoadedCommit_whenGitLabHasIt() throws IOException {
        final GitLabCommitDetail response = commit("abc123", "Msg");
        when(gitLab.commits().get(ID, "abc123")).thenReturn(Optional.of(response));

        final Optional<Commit> found = sut.commit("abc123");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void commit_shouldReturnEmpty_whenUnknownSha() throws IOException {
        when(gitLab.commits().get(ID, "unknown")).thenReturn(Optional.empty());

        final Optional<Commit> found = sut.commit("unknown");

        assertThat(found).isEmpty();
    }

    @Test
    void commits_shouldLoadEachItem_whenListReturnsSmallerRecords() throws IOException {
        when(gitLab.commits().list(ID).stream()).thenReturn(Stream.of(listedCommit("aaa111"), listedCommit("bbb222")));
        when(gitLab.commits().get(ID, "aaa111")).thenReturn(Optional.of(commit("aaa111", "One")));
        when(gitLab.commits().get(ID, "bbb222")).thenReturn(Optional.of(commit("bbb222", "Two")));

        final List<Commit> result = sut.commits().toList();

        assertThat(result).extracting(Commit::sha).containsExactly("aaa111", "bbb222");
    }

    @Test
    void commits_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.commits().list(ID).stream()).thenReturn(Stream.of(listedCommit("aaa111"), listedCommit("bbb222")));
        when(gitLab.commits().get(ID, "aaa111")).thenReturn(Optional.empty());
        when(gitLab.commits().get(ID, "bbb222")).thenReturn(Optional.of(commit("bbb222", "Two")));

        final List<Commit> result = sut.commits().toList();

        assertThat(result).extracting(Commit::sha).containsExactly("bbb222");
    }

    @Test
    void commits_shouldLoadLazily_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.commits().list(ID).stream())
                .thenReturn(Stream.of("aaa111", "bbb222", "ccc333").map(sha -> {
                    consumed.incrementAndGet();
                    return listedCommit(sha);
                }));
        when(gitLab.commits().get(ID, "aaa111")).thenReturn(Optional.of(commit("aaa111", "One")));

        final Stream<Commit> stream = sut.commits();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
        verify(gitLab.commits(), never()).get(ID, "bbb222");
    }

    @Test
    void commits_shouldReturnEmptyStream_whenProjectHasNoCommits() throws IOException {
        when(gitLab.commits().list(ID).stream()).thenReturn(Stream.empty());

        final List<Commit> result = sut.commits().toList();

        assertThat(result).isEmpty();
    }
}
