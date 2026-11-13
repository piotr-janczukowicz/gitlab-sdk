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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsMergePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
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
        when(gitLab.mergerequests().project().get(ID, IID)).thenReturn(Optional.of(reloaded));

        final MergeRequest refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.mergerequests().project().get(ID, IID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Merge request 7 is gone");
    }
}
