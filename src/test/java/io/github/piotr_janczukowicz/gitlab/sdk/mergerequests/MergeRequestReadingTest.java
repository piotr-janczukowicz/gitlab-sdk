package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiffBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssueBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssueBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiffBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiffFullBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListDiffsFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@UnitTest
class MergeRequestReadingTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final MergeRequest sut = new MergeRequest(project, mergeRequest(IID, "Fix", "opened"));

    @Test
    void diffs_shouldReturnTheCoreRecordsUnwrapped_whenTheMergeRequestHasChanges() throws IOException {
        final GitLabDiff first = GitLabDiffBuilder.builder().newPath("a.txt").build();
        final GitLabDiff second = GitLabDiffBuilder.builder().newPath("b.txt").build();
        when(gitLab.mergerequests().project().listDiffs(ID, IID).stream()).thenReturn(Stream.of(first, second));

        final List<GitLabDiff> diffs = sut.diffs().toList();

        assertThat(diffs).containsExactly(first, second);
    }

    @Test
    void diffs_shouldPassTheFilter_whenFilterIsGiven() throws IOException {
        final GitLabProjectMergeRequestsListDiffsFilter filter = GitLabProjectMergeRequestsListDiffsFilter.empty();
        final GitLabDiff diff = GitLabDiffBuilder.builder().newPath("a.txt").build();
        when(gitLab.mergerequests().project().listDiffs(ID, IID, filter).stream())
                .thenReturn(Stream.of(diff));

        final List<GitLabDiff> diffs = sut.diffs(filter).toList();

        assertThat(diffs).containsExactly(diff);
    }

    @Test
    void diffs_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.diffs(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void diffs_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.mergerequests().project().listDiffs(ID, IID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(path -> {
                    consumed.incrementAndGet();
                    return GitLabDiffBuilder.builder().newPath(path).build();
                }));

        final Stream<GitLabDiff> diffs = sut.diffs();

        assertThat(consumed).hasValue(0);
        assertThat(diffs.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void versions_shouldWrapEachVersion_whenTheMergeRequestHasVersions() throws IOException {
        when(gitLab.mergerequests().project().listVersions(ID, IID).stream())
                .thenReturn(Stream.of(
                        GitLabMergeRequestDiffBuilder.builder().id(1L).build(),
                        GitLabMergeRequestDiffBuilder.builder().id(2L).build()));

        final List<DiffVersion> versions = sut.versions().toList();

        assertThat(versions).extracting(DiffVersion::id).containsExactly(1L, 2L);
    }

    @Test
    void version_shouldWrapTheFullVersion_whenGitLabHasIt() throws IOException {
        when(gitLab.mergerequests().project().getVersion(ID, IID, 3L))
                .thenReturn(Optional.of(
                        GitLabMergeRequestDiffFullBuilder.builder().id(3L).build()));

        final Optional<MergeRequestDiff> found = sut.version(3L);

        assertThat(found).hasValueSatisfying(version -> assertThat(version.id()).isEqualTo(3L));
    }

    @Test
    void version_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequests().project().getVersion(ID, IID, 3L)).thenReturn(Optional.empty());

        final Optional<MergeRequestDiff> found = sut.version(3L);

        assertThat(found).isEmpty();
    }

    @Test
    void participants_shouldReturnTheCoreRecordsUnwrapped_whenUsersTakePart() throws IOException {
        final GitLabUserBasic user = GitLabUserBasicBuilder.builder().id(1L).build();
        when(gitLab.mergerequests().project().listParticipants(ID, IID)).thenReturn(List.of(user));

        final List<GitLabUserBasic> participants = sut.participants().toList();

        assertThat(participants).containsExactly(user);
    }

    @Test
    void closesIssues_shouldLoadEachIssue_andSkipOneThatIsGone() throws IOException {
        when(gitLab.mergerequests().project().listClosesIssues(ID, IID).stream())
                .thenReturn(Stream.of(basicIssue(1L, null), basicIssue(2L, null), basicIssue(3L, 99L)));
        when(gitLab.issues().project().get(ID, 1L))
                .thenReturn(Optional.of(GitLabIssueBuilder.builder().iid(1L).build()));
        when(gitLab.issues().project().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.issues().project().get("99", 3L))
                .thenReturn(Optional.of(GitLabIssueBuilder.builder().iid(3L).build()));

        final List<Issue> result = sut.closesIssues().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Issue::iid).containsExactly(1L, 3L);
            softly.assertThat(result.get(0).project()).isSameAs(project);
            softly.assertThat(result.get(1).project().id()).isEqualTo("99");
        });
    }

    @Test
    void closesIssues_shouldKeepTheProject_whenTheSummaryNamesTheProjectById() throws IOException {
        final Project numeric = GitLabSdk.of(gitLab).project("42");
        final MergeRequest inNumeric = new MergeRequest(numeric, mergeRequest(IID, "Fix", "opened"));
        when(gitLab.mergerequests().project().listClosesIssues("42", IID).stream())
                .thenReturn(Stream.of(basicIssue(5L, 42L)));
        when(gitLab.issues().project().get("42", 5L))
                .thenReturn(Optional.of(GitLabIssueBuilder.builder().iid(5L).build()));

        final List<Issue> result = inNumeric.closesIssues().toList();

        assertThat(result)
                .singleElement()
                .satisfies(found -> assertThat(found.project()).isSameAs(numeric));
    }

    @Test
    void closesIssues_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.mergerequests().project().listClosesIssues(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L).map(iid -> {
                    consumed.incrementAndGet();
                    return basicIssue(iid, null);
                }));
        when(gitLab.issues().project().get(ID, 1L))
                .thenReturn(Optional.of(GitLabIssueBuilder.builder().iid(1L).build()));

        final Stream<Issue> issues = sut.closesIssues();

        assertThat(consumed).hasValue(0);
        assertThat(issues.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    private static GitLabIssueBasic basicIssue(final long iid, final @Nullable Long projectId) {
        return GitLabIssueBasicBuilder.builder().iid(iid).projectId(projectId).build();
    }
}
