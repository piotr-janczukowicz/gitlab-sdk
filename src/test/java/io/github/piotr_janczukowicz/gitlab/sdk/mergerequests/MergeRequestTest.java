package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.approvals;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.note;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsGetFilter;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadataBuilder;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabMergeRequest response = mergeRequest(IID, "Fix", "opened");
    private final MergeRequest sut = new MergeRequest(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenMergeRequestIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.iid()).isEqualTo(IID);
            softly.assertThat(sut.title()).isEqualTo("Fix");
            softly.assertThat(sut.state()).isEqualTo(MergeRequestState.OPENED);
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.targetBranch()).isEmpty();
            softly.assertThat(sut.sourceBranch()).isEmpty();
            softly.assertThat(sut.labels()).isEmpty();
        });
    }

    @Test
    void labels_shouldReturnAnUnmodifiableCopy_whenTheResponseListIsMutable() {
        final List<String> names = new ArrayList<>(List.of("bug"));
        final MergeRequest labelled = new MergeRequest(
                project,
                GitLabMergeRequestBuilder.builder(response).labels(names).build());

        final List<String> labels = labelled.labels();
        names.add("later");

        assertSoftly(softly -> {
            softly.assertThat(labels).containsExactly("bug");
            softly.assertThatThrownBy(() -> labels.add("x")).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void state_shouldMapAllKnownStates_whenGitLabReturnsThem() {
        final List<MergeRequestState> states = Stream.of("opened", "closed", "locked", "merged")
                .map(value -> new MergeRequest(project, mergeRequest(IID, "Fix", value)).state())
                .toList();

        assertThat(states)
                .containsExactly(
                        MergeRequestState.OPENED,
                        MergeRequestState.CLOSED,
                        MergeRequestState.LOCKED,
                        MergeRequestState.MERGED);
    }

    @Test
    void state_shouldThrowIllegalArgumentException_whenUnknownState() {
        final MergeRequest unknown = new MergeRequest(project, mergeRequest(IID, "Fix", "draft"));

        final Throwable thrown = catchThrowable(unknown::state);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("draft");
    }

    @Test
    void stateName_shouldReturnRawValue_whenStateIsUnknown() {
        final MergeRequest sut = new MergeRequest(project, mergeRequest(IID, "Fix", "draft"));

        assertThat(sut.stateName()).isEqualTo("draft");
    }

    @Test
    void close_shouldSendStateEventClose_andReturnNewMergeRequest() throws IOException {
        final GitLabMergeRequest closed = mergeRequest(IID, "Fix", "closed");
        when(gitLab.mergerequests()
                        .project()
                        .update(eq(ID), eq(IID), any(GitLabProjectMergeRequestsUpdatePayload.class)))
                .thenReturn(closed);

        final MergeRequest result = sut.close();

        final ArgumentCaptor<GitLabProjectMergeRequestsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsUpdatePayload.class);
        verify(gitLab.mergerequests().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().stateEvent())
                    .isEqualTo(GitLabProjectMergeRequestsUpdateStateEvent.CLOSE);
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(closed);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void reopen_shouldSendStateEventReopen_whenCalled() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .update(eq(ID), eq(IID), any(GitLabProjectMergeRequestsUpdatePayload.class)))
                .thenReturn(mergeRequest(IID, "Fix", "opened"));

        sut.reopen();

        final ArgumentCaptor<GitLabProjectMergeRequestsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsUpdatePayload.class);
        verify(gitLab.mergerequests().project()).update(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().stateEvent()).isEqualTo(GitLabProjectMergeRequestsUpdateStateEvent.REOPEN);
    }

    @Test
    void merge_shouldReturnMergedRequest_whenCalled() throws IOException {
        final GitLabMergeRequest merged = mergeRequest(IID, "Fix", "merged");
        when(gitLab.mergerequests().project().merge(eq(ID), eq(IID), any(GitLabProjectMergeRequestsMergePayload.class)))
                .thenReturn(merged);

        final MergeRequest result = sut.merge();

        assertSoftly(softly -> {
            softly.assertThat(result.state()).isEqualTo(MergeRequestState.MERGED);
            softly.assertThat(result.response()).isSameAs(merged);
            softly.assertThat(sut.state()).isEqualTo(MergeRequestState.OPENED);
        });
    }

    @Test
    void merge_shouldSendPayload_whenGivenOptions() throws IOException {
        final GitLabMergeRequest merged = mergeRequest(IID, "Fix", "merged");
        final GitLabProjectMergeRequestsMergePayload options = GitLabProjectMergeRequestsMergePayloadBuilder.builder()
                .squash(true)
                .squashCommitMessage("Squashed")
                .build();
        when(gitLab.mergerequests().project().merge(ID, IID, options)).thenReturn(merged);

        final MergeRequest result = sut.merge(options);

        final ArgumentCaptor<GitLabProjectMergeRequestsMergePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsMergePayload.class);
        verify(gitLab.mergerequests().project()).merge(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().squash()).isTrue();
            softly.assertThat(payload.getValue().squashCommitMessage()).isEqualTo("Squashed");
            softly.assertThat(result.response()).isSameAs(merged);
        });
    }

    @Test
    void merge_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.merge(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void merge_shouldSendEmptyPayload_whenCalledWithoutArguments() throws IOException {
        when(gitLab.mergerequests().project().merge(eq(ID), eq(IID), any(GitLabProjectMergeRequestsMergePayload.class)))
                .thenReturn(mergeRequest(IID, "Fix", "merged"));

        sut.merge();

        verify(gitLab.mergerequests().project())
                .merge(
                        ID,
                        IID,
                        GitLabProjectMergeRequestsMergePayloadBuilder.builder().build());
    }

    @Test
    void merge_shouldPassCoreException_whenNotMergeable() throws IOException {
        final GitLabApiException refused = mock(GitLabApiException.class);
        when(gitLab.mergerequests().project().merge(eq(ID), eq(IID), any(GitLabProjectMergeRequestsMergePayload.class)))
                .thenThrow(refused);

        final Throwable thrown = catchThrowable(sut::merge);

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void approve_shouldReturnApprovals_whenCalled() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .approve(eq(ID), eq(IID), any(GitLabProjectMergeRequestsApprovePayload.class)))
                .thenReturn(approvals(true, 0));

        final Approvals result = sut.approve();

        assertSoftly(softly -> {
            softly.assertThat(result.approved()).contains(true);
            softly.assertThat(result.mergeRequest()).isSameAs(sut);
        });
    }

    @Test
    void approve_shouldSendShaAndPublishReview_whenGivenOptions() throws IOException {
        final GitLabProjectMergeRequestsApprovePayload options =
                GitLabProjectMergeRequestsApprovePayloadBuilder.builder()
                        .sha("abc123")
                        .publishReview(true)
                        .build();
        when(gitLab.mergerequests().project().approve(ID, IID, options)).thenReturn(approvals(true, 0));

        final Approvals result = sut.approve(options);

        final ArgumentCaptor<GitLabProjectMergeRequestsApprovePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsApprovePayload.class);
        verify(gitLab.mergerequests().project()).approve(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().sha()).isEqualTo("abc123");
            softly.assertThat(payload.getValue().publishReview()).isTrue();
            softly.assertThat(result.mergeRequest()).isSameAs(sut);
        });
    }

    @Test
    void approve_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.approve(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void approve_shouldSendEmptyPayload_whenCalledWithoutArguments() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .approve(eq(ID), eq(IID), any(GitLabProjectMergeRequestsApprovePayload.class)))
                .thenReturn(approvals(true, 0));

        sut.approve();

        verify(gitLab.mergerequests().project())
                .approve(
                        ID,
                        IID,
                        GitLabProjectMergeRequestsApprovePayloadBuilder.builder()
                                .build());
    }

    @Test
    void unapprove_shouldReturnApprovals_whenCalled() throws IOException {
        when(gitLab.mergerequests().project().unapprove(ID, IID)).thenReturn(approvals(false, 1));

        final Approvals result = sut.unapprove();

        assertSoftly(softly -> {
            softly.assertThat(result.approved()).contains(false);
            softly.assertThat(result.approvalsLeft()).contains(1);
        });
    }

    @Test
    void approvals_shouldReturnApprovals_whenGitLabHasThem() throws IOException {
        when(gitLab.mergerequestapprovals().getRequestsApprovals(ID, IID)).thenReturn(Optional.of(approvals(false, 2)));

        final Optional<Approvals> result = sut.approvals();

        assertThat(result).hasValueSatisfying(a -> assertThat(a.approvalsLeft()).contains(2));
    }

    @Test
    void approvals_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequestapprovals().getRequestsApprovals(ID, IID)).thenReturn(Optional.empty());

        final Optional<Approvals> result = sut.approvals();

        assertThat(result).isEmpty();
    }

    @Test
    void comment_shouldCreateNoteOnIid_whenBodyIsGiven() throws IOException {
        when(gitLab.notes()
                        .project()
                        .createRequestsNotes(eq(ID), eq(IID), any(GitLabProjectNotesCreateRequestsNotesPayload.class)))
                .thenReturn(note(5L, "Thanks"));

        final MergeRequestNote created = sut.comment("Thanks");

        final ArgumentCaptor<GitLabProjectNotesCreateRequestsNotesPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectNotesCreateRequestsNotesPayload.class);
        verify(gitLab.notes().project()).createRequestsNotes(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Thanks");
            softly.assertThat(created.id()).isEqualTo(5L);
            softly.assertThat(created.mergeRequest()).isSameAs(sut);
        });
    }

    @Test
    void comment_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.comment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notes_shouldMapEachNoteToItsMergeRequest_whenListed() throws IOException {
        when(gitLab.notes().project().listRequestsNotes(ID, IID).stream())
                .thenReturn(Stream.of(note(1L, "a"), note(2L, "b")));

        final List<MergeRequestNote> notes = sut.notes().toList();

        assertThat(notes)
                .hasSize(2)
                .allSatisfy(n -> assertThat(n.mergeRequest()).isSameAs(sut));
    }

    @Test
    void notes_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.notes().project().listRequestsNotes(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return note(id, "n");
                }));

        final Stream<MergeRequestNote> notes = sut.notes();

        assertThat(consumed).hasValue(0);
        assertThat(notes.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void notes_shouldReturnEmptyStream_whenMergeRequestHasNoNotes() throws IOException {
        when(gitLab.notes().project().listRequestsNotes(ID, IID).stream()).thenReturn(Stream.empty());

        final List<MergeRequestNote> notes = sut.notes().toList();

        assertThat(notes).isEmpty();
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.mergerequests().project()).delete(ID, IID);
    }

    @Test
    void refresh_shouldReturnNewMergeRequest_whenGitLabHasIt() throws IOException {
        final GitLabMergeRequest reloaded = mergeRequest(IID, "Renamed", "opened");
        when(gitLab.mergerequests().project().get(eq(ID), eq(IID), any())).thenReturn(Optional.of(reloaded));

        final MergeRequest refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldAskForTheRebaseState_whenCalled() throws IOException {
        when(gitLab.mergerequests().project().get(eq(ID), eq(IID), any()))
                .thenReturn(Optional.of(mergeRequest(IID, "Renamed", "opened")));
        final ArgumentCaptor<GitLabProjectMergeRequestsGetFilter> filter =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsGetFilter.class);

        sut.refresh();

        verify(gitLab.mergerequests().project()).get(eq(ID), eq(IID), filter.capture());
        assertThat(filter.getValue().includeRebaseInProgress()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.mergerequests().project().get(eq(ID), eq(IID), any())).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Merge request 7 is gone");
    }

    @Test
    void pipelines_shouldLoadEachPipeline_whenGitLabListsBasicRecords() throws IOException {
        when(gitLab.mergerequests().project().listPipelines(ID, IID))
                .thenReturn(List.of(
                        GitLabCiPipelineBasicBuilder.builder().id(1L).build(),
                        GitLabCiPipelineBasicBuilder.builder().id(2L).build(),
                        GitLabCiPipelineBasicBuilder.builder().id(3L).build()));
        when(gitLab.pipelines().get(ID, 1L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(1L)
                        .status("success")
                        .build()));
        when(gitLab.pipelines().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.pipelines().get(ID, 3L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(3L)
                        .status("failed")
                        .build()));

        final List<Pipeline> result = sut.pipelines().toList();

        assertThat(result).extracting(Pipeline::id).containsExactly(1L, 3L);
    }

    @Test
    void pipelines_shouldLoadPipelineThroughItsOwnProject_whenItRanInAFork() throws IOException {
        when(gitLab.mergerequests().project().listPipelines(ID, IID))
                .thenReturn(List.of(GitLabCiPipelineBasicBuilder.builder()
                        .id(4L)
                        .projectId(99L)
                        .build()));
        when(gitLab.pipelines().get("99", 4L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(4L)
                        .projectId(99L)
                        .status("success")
                        .build()));

        final List<Pipeline> result = sut.pipelines().toList();

        assertThat(result)
                .singleElement()
                .satisfies(pipeline -> assertSoftly(softly -> {
                    softly.assertThat(pipeline.id()).isEqualTo(4L);
                    softly.assertThat(pipeline.project().id()).isEqualTo("99");
                }));
        verify(gitLab.pipelines(), never()).get(ID, 4L);
    }

    @Test
    void pipelines_shouldKeepTheProjectOfTheMergeRequest_whenPipelineRanInTheTargetProject() throws IOException {
        final MergeRequest inProject42 = new MergeRequest(
                project,
                GitLabMergeRequestBuilder.builder(response).projectId(42L).build());
        when(gitLab.mergerequests().project().listPipelines(ID, IID))
                .thenReturn(List.of(GitLabCiPipelineBasicBuilder.builder()
                        .id(1L)
                        .projectId(42L)
                        .build()));
        when(gitLab.pipelines().get(ID, 1L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(1L)
                        .status("success")
                        .build()));

        final List<Pipeline> result = inProject42.pipelines().toList();

        assertThat(result)
                .singleElement()
                .satisfies(pipeline -> assertThat(pipeline.project()).isSameAs(project));
        verify(gitLab.pipelines(), never()).get("42", 1L);
    }

    @Test
    void pipelines_shouldKeepTheProjectOfTheMergeRequest_whenPipelineRanInTheTargetProjectById() throws IOException {
        final MergeRequest targeting42 = new MergeRequest(
                project,
                GitLabMergeRequestBuilder.builder(response).targetProjectId(42L).build());
        when(gitLab.mergerequests().project().listPipelines(ID, IID))
                .thenReturn(List.of(GitLabCiPipelineBasicBuilder.builder()
                        .id(1L)
                        .projectId(42L)
                        .build()));
        when(gitLab.pipelines().get(ID, 1L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(1L)
                        .status("success")
                        .build()));

        final List<Pipeline> result = targeting42.pipelines().toList();

        assertThat(result)
                .singleElement()
                .satisfies(pipeline -> assertThat(pipeline.project()).isSameAs(project));
    }

    @Test
    void pipelines_shouldKeepTheProjectOfTheMergeRequest_whenItIsAddressedByTheSameNumericId() throws IOException {
        final Project numeric = GitLabSdk.of(gitLab).project("42");
        final MergeRequest inNumeric = new MergeRequest(numeric, response);
        when(gitLab.mergerequests().project().listPipelines("42", IID))
                .thenReturn(List.of(GitLabCiPipelineBasicBuilder.builder()
                        .id(1L)
                        .projectId(42L)
                        .build()));
        when(gitLab.pipelines().get("42", 1L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(1L)
                        .status("success")
                        .build()));

        final List<Pipeline> result = inNumeric.pipelines().toList();

        assertThat(result)
                .singleElement()
                .satisfies(pipeline -> assertThat(pipeline.project()).isSameAs(numeric));
    }

    @Test
    void pipelines_shouldLoadLazily_whenStreamIsLimited() throws IOException {
        when(gitLab.mergerequests().project().listPipelines(ID, IID))
                .thenReturn(List.of(
                        GitLabCiPipelineBasicBuilder.builder().id(1L).build(),
                        GitLabCiPipelineBasicBuilder.builder().id(2L).build()));
        when(gitLab.pipelines().get(ID, 1L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(1L)
                        .status("success")
                        .build()));

        final List<Pipeline> result = sut.pipelines().limit(1).toList();

        assertThat(result).extracting(Pipeline::id).containsExactly(1L);
        verify(gitLab.pipelines(), never()).get(ID, 2L);
    }

    @Test
    void pipelines_shouldReturnEmptyStream_whenMergeRequestHasNoPipelines() throws IOException {
        when(gitLab.mergerequests().project().listPipelines(ID, IID)).thenReturn(List.of());

        final List<Pipeline> result = sut.pipelines().toList();

        assertThat(result).isEmpty();
    }

    @Test
    void commits_shouldLoadEachCommitAndSkipGoneOnes_whenListReturnsSmallerRecords() throws IOException {
        when(gitLab.mergerequests().project().listCommits(ID, IID).stream())
                .thenReturn(Stream.of(
                        GitLabCommitBuilder.builder().id("aaa111").build(),
                        GitLabCommitBuilder.builder().id("bbb222").build()));
        when(gitLab.commits().get(ID, "aaa111")).thenReturn(Optional.empty());
        when(gitLab.commits().get(ID, "bbb222"))
                .thenReturn(Optional.of(
                        GitLabCommitDetailBuilder.builder().id("bbb222").build()));

        final List<Commit> result = sut.commits().toList();

        assertThat(result).extracting(Commit::sha).containsExactly("bbb222");
    }

    @Test
    void commits_shouldReturnEmptyStream_whenMergeRequestHasNoCommits() throws IOException {
        when(gitLab.mergerequests().project().listCommits(ID, IID).stream()).thenReturn(Stream.empty());

        final List<Commit> result = sut.commits().toList();

        assertThat(result).isEmpty();
    }
}
