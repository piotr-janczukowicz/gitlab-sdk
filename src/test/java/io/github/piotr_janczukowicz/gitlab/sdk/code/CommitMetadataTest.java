package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.basicMergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commitNote;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commitStatus;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.ref;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.signature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabBasicRef;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitSignature;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentLineType;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentPayload;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateState;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListStatusesFilter;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListStatusesFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitMetadataTest {

    private static final String ID = "my-group/my-project";
    private static final String SHA = "abcdef0123456789";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Commit sut = new Commit(project, commit(SHA, "Fix it"));

    @Test
    void statuses_shouldWrapEachStatus_whenGitLabListsThem() throws IOException {
        when(gitLab.commits().listStatuses(ID, SHA).stream())
                .thenReturn(Stream.of(commitStatus(1L, "success", "build"), commitStatus(2L, "failed", "test")));

        final List<CommitStatus> result = sut.statuses().toList();

        assertThat(result).extracting(each -> each.name().orElseThrow()).containsExactly("build", "test");
        assertThat(result).allSatisfy(each -> assertThat(each.commit()).isSameAs(sut));
    }

    @Test
    void statuses_shouldReturnEmptyStream_whenCommitHasNoStatus() throws IOException {
        when(gitLab.commits().listStatuses(ID, SHA).stream()).thenReturn(Stream.empty());

        final List<CommitStatus> result = sut.statuses().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void statuses_shouldLoadLazily_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.commits().listStatuses(ID, SHA).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return commitStatus(id, "success", "build");
                }));

        final Stream<CommitStatus> stream = sut.statuses();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void statuses_shouldSendRefNameAndAll_whenAllIsTrue() throws IOException {
        final GitLabProjectCommitsListStatusesFilter filter = GitLabProjectCommitsListStatusesFilterBuilder.builder()
                .ref("dev")
                .name("build")
                .all(true)
                .build();
        when(gitLab.commits().listStatuses(ID, SHA, filter).stream())
                .thenReturn(Stream.of(commitStatus(1L, "success", "build")));

        final List<CommitStatus> result = sut.statuses("dev", "build", true).toList();

        assertThat(result).hasSize(1);
    }

    @Test
    void statuses_shouldLeaveAllOutAndFilterNothing_whenArgumentsAreNullAndAllIsFalse() throws IOException {
        final GitLabProjectCommitsListStatusesFilter filter =
                GitLabProjectCommitsListStatusesFilterBuilder.builder().build();
        when(gitLab.commits().listStatuses(ID, SHA, filter).stream()).thenReturn(Stream.empty());

        final List<CommitStatus> result = sut.statuses(null, null, false).toList();

        assertThat(result).isEmpty();
    }

    @Test
    void statuses_shouldThrowIllegalArgumentException_whenRefOrNameIsBlank() {
        final Throwable ref = catchThrowable(() -> sut.statuses(" ", null, false));
        final Throwable name = catchThrowable(() -> sut.statuses(null, " ", false));

        assertSoftly(softly -> {
            softly.assertThat(ref).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(name).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void addStatus_shouldSendStateOnly_whenStateIsGiven() throws IOException {
        final GitLabProjectCommitsCreatePayload payload = GitLabProjectCommitsCreatePayloadBuilder.builder()
                .state(GitLabProjectCommitsCreateState.RUNNING)
                .build();
        when(gitLab.commits().create(ID, SHA, payload)).thenReturn(commitStatus(9L, "running", "default"));

        final CommitStatus result = sut.addStatus(CommitStatusState.RUNNING);

        assertSoftly(softly -> {
            softly.assertThat(result.id()).contains(9L);
            softly.assertThat(result.status()).contains("running");
            softly.assertThat(result.commit()).isSameAs(sut);
        });
    }

    @Test
    void addStatus_shouldThrowNullPointerException_whenStateIsNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.addStatus((CommitStatusState) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.commits(), never()).create(any(), any(), any());
    }

    @Test
    void addStatus_shouldSendPayloadUnchanged_whenPayloadIsGiven() throws IOException {
        final GitLabProjectCommitsCreatePayload payload = GitLabProjectCommitsCreatePayloadBuilder.builder()
                .state(GitLabProjectCommitsCreateState.SUCCESS)
                .name("coverage")
                .ref("main")
                .coverage(91.5)
                .build();
        when(gitLab.commits().create(ID, SHA, payload)).thenReturn(commitStatus(3L, "success", "coverage"));

        final CommitStatus result = sut.addStatus(payload);

        assertThat(result.name()).contains("coverage");
    }

    @Test
    void addStatus_shouldThrowNullPointerException_whenPayloadIsNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.addStatus((GitLabProjectCommitsCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.commits(), never()).create(any(), any(), any());
    }

    @Test
    void comments_shouldWrapEachComment_whenGitLabListsThem() throws IOException {
        when(gitLab.commits().listComments(ID, SHA).stream())
                .thenReturn(Stream.of(commitNote("One", null, null, null), commitNote("Two", "a.txt", 3, "old")));

        final List<CommitComment> result = sut.comments().toList();

        assertThat(result).extracting(each -> each.body().orElseThrow()).containsExactly("One", "Two");
        assertThat(result).allSatisfy(each -> assertThat(each.commit()).isSameAs(sut));
    }

    @Test
    void comment_shouldSendNewLineType_whenOnlyBodyIsGiven() throws IOException {
        final GitLabProjectCommitsCreateCommentPayload payload =
                GitLabProjectCommitsCreateCommentPayloadBuilder.builder()
                        .note("Nice")
                        .lineType(GitLabProjectCommitsCreateCommentLineType.NEW)
                        .build();
        when(gitLab.commits().createComment(ID, SHA, payload)).thenReturn(commitNote("Nice", null, null, null));

        final CommitComment result = sut.comment("Nice");

        assertThat(result.body()).contains("Nice");
    }

    @Test
    void comment_shouldSendPathLineAndType_whenFileCommentIsGiven() throws IOException {
        final GitLabProjectCommitsCreateCommentPayload payload =
                GitLabProjectCommitsCreateCommentPayloadBuilder.builder()
                        .note("Nice")
                        .path("src/App.java")
                        .line(11)
                        .lineType(GitLabProjectCommitsCreateCommentLineType.OLD)
                        .build();
        when(gitLab.commits().createComment(ID, SHA, payload))
                .thenReturn(commitNote("Nice", "src/App.java", 11, "old"));

        final CommitComment result = sut.comment("Nice", "src/App.java", 11, CommitLineType.OLD);

        assertSoftly(softly -> {
            softly.assertThat(result.path()).contains("src/App.java");
            softly.assertThat(result.line()).contains(11);
            softly.assertThat(result.lineType()).contains("old");
        });
    }

    @Test
    void comment_shouldThrowBeforeAnyRequest_whenArgumentsAreInvalid() throws IOException {
        final Throwable nullBody = catchThrowable(() -> sut.comment(null));
        final Throwable blankBody = catchThrowable(() -> sut.comment(" "));
        final Throwable blankPath = catchThrowable(() -> sut.comment("x", " ", 1, CommitLineType.NEW));
        final Throwable zeroLine = catchThrowable(() -> sut.comment("x", "a", 0, CommitLineType.NEW));
        final Throwable nullType = catchThrowable(() -> sut.comment("x", "a", 1, null));
        final Throwable nullPath = catchThrowable(() -> sut.comment("x", null, 1, CommitLineType.NEW));

        assertSoftly(softly -> {
            softly.assertThat(nullBody).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankBody).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankPath).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(zeroLine).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullType).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullPath).isInstanceOf(NullPointerException.class);
        });
        verify(gitLab.commits(), never()).createComment(any(), any(), any());
    }

    @Test
    void mergeRequests_shouldLoadEachOne_whenGitLabListsBasicRecords() throws IOException {
        when(gitLab.commits().listMergeRequests(ID, SHA).stream())
                .thenReturn(Stream.of(basicMergeRequest(1L), basicMergeRequest(2L)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(1L), any())).thenReturn(Optional.of(mergeRequest(1L)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(2L), any())).thenReturn(Optional.of(mergeRequest(2L)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 2L);
    }

    @Test
    void mergeRequests_shouldSkipOne_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.commits().listMergeRequests(ID, SHA).stream())
                .thenReturn(Stream.of(basicMergeRequest(1L), basicMergeRequest(2L)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(1L), any())).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get(eq(ID), eq(2L), any())).thenReturn(Optional.of(mergeRequest(2L)));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result).extracting(MergeRequest::iid).containsExactly(2L);
    }

    @Test
    void refs_shouldReturnCoreRecords_whenGitLabListsThem() throws IOException {
        final GitLabBasicRef branch = ref("branch", "main");
        final GitLabBasicRef tag = ref("tag", "v1");
        when(gitLab.commits().listRefs(ID, SHA).stream()).thenReturn(Stream.of(branch, tag));

        final List<GitLabBasicRef> result = sut.refs().toList();

        assertThat(result).containsExactly(branch, tag);
    }

    @Test
    void signature_shouldReturnCoreRecord_whenCommitIsSigned() throws IOException {
        final GitLabCommitSignature signed = signature("PGP");
        when(gitLab.commits().getSignature(ID, SHA)).thenReturn(Optional.of(signed));

        final Optional<GitLabCommitSignature> result = sut.signature();

        assertThat(result).containsSame(signed);
    }

    @Test
    void signature_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.commits().getSignature(ID, SHA)).thenReturn(Optional.empty());

        final Optional<GitLabCommitSignature> result = sut.signature();

        assertThat(result).isEmpty();
    }
}
