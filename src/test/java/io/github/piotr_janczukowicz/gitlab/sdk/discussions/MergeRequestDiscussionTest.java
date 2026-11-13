package io.github.piotr_janczukowicz.gitlab.sdk.discussions;

import static io.github.piotr_janczukowicz.gitlab.sdk.discussions.DiscussionsFixtures.discussion;
import static io.github.piotr_janczukowicz.gitlab.sdk.discussions.DiscussionsFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.discussions.DiscussionsFixtures.note;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussion;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussionBuilder;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateRequestsDiscussionsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestNote;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestDiscussionTest {

    private static final String ID = "42";
    private static final long IID = 7L;
    private static final String DISCUSSION_ID = "abc123";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest mergeRequest =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID));
    private final GitLabDiscussion response = discussion(DISCUSSION_ID, note(1L, "a"), note(2L, "b"));
    private final MergeRequestDiscussion sut = new MergeRequestDiscussion(mergeRequest, response);

    @Test
    void accessors_shouldReturnResponseValues_whenDiscussionIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(DISCUSSION_ID);
            softly.assertThat(sut.isIndividualNote()).isFalse();
            softly.assertThat(sut.isResolvable()).isTrue();
            softly.assertThat(sut.isResolved()).isFalse();
            softly.assertThat(sut.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void isResolved_shouldBeFalse_whenFlagIsAbsent() {
        final MergeRequestDiscussion bare = new MergeRequestDiscussion(
                mergeRequest,
                GitLabDiscussionBuilder.builder().id(DISCUSSION_ID).build());

        assertSoftly(softly -> {
            softly.assertThat(bare.isResolved()).isFalse();
            softly.assertThat(bare.isResolvable()).isFalse();
            softly.assertThat(bare.isIndividualNote()).isFalse();
            softly.assertThat(bare.notes()).isEmpty();
        });
    }

    @Test
    void notes_shouldMapEachNoteToItsOwner_whenDiscussionHasNotes() {
        final List<MergeRequestNote> notes = sut.notes();

        assertSoftly(softly -> {
            softly.assertThat(notes).hasSize(2);
            softly.assertThat(notes)
                    .allSatisfy(n -> assertThat(n.mergeRequest()).isSameAs(mergeRequest));
            softly.assertThat(notes.get(1).body()).contains("b");
            softly.assertThat(catchThrowable(() -> notes.add(notes.get(0))))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void reply_shouldCreateNoteInThread_whenBodyGiven() throws IOException {
        final GitLabNote created = note(9L, "re");
        when(gitLab.notes()
                        .project()
                        .createRequestsDiscussionsNotes(
                                eq(ID),
                                eq(IID),
                                eq(DISCUSSION_ID),
                                any(GitLabProjectNotesCreateRequestsDiscussionsNotesPayload.class)))
                .thenReturn(created);

        final MergeRequestNote result = sut.reply("re");

        final ArgumentCaptor<GitLabProjectNotesCreateRequestsDiscussionsNotesPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectNotesCreateRequestsDiscussionsNotesPayload.class);
        verify(gitLab.notes().project())
                .createRequestsDiscussionsNotes(eq(ID), eq(IID), eq(DISCUSSION_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("re");
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.mergeRequest()).isSameAs(mergeRequest);
        });
    }

    @Test
    void reply_shouldThrowIllegalArgumentException_whenBodyIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.reply(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.notes().project(), never())
                .createRequestsDiscussionsNotes(
                        any(), eq(IID), any(), any(GitLabProjectNotesCreateRequestsDiscussionsNotesPayload.class));
    }

    @Test
    void reply_shouldPassCoreException_whenDiscussionIsGone() throws IOException {
        final IOException failure = new IOException("404");
        when(gitLab.notes()
                        .project()
                        .createRequestsDiscussionsNotes(
                                eq(ID),
                                eq(IID),
                                eq(DISCUSSION_ID),
                                any(GitLabProjectNotesCreateRequestsDiscussionsNotesPayload.class)))
                .thenThrow(failure);

        final Throwable thrown = catchThrowable(() -> sut.reply("re"));

        assertThat(thrown).isSameAs(failure);
    }

    @Test
    void resolve_shouldSendResolvedTrue_whenCalled() throws IOException {
        when(gitLab.discussions()
                        .project()
                        .updateRequestsDiscussions(
                                eq(ID),
                                eq(IID),
                                eq(DISCUSSION_ID),
                                any(GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload.class)))
                .thenReturn(response);

        sut.resolve();

        final ArgumentCaptor<GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload.class);
        verify(gitLab.discussions().project())
                .updateRequestsDiscussions(eq(ID), eq(IID), eq(DISCUSSION_ID), payload.capture());
        assertThat(payload.getValue().resolved()).isTrue();
    }

    @Test
    void unresolve_shouldSendResolvedFalse_whenCalled() throws IOException {
        when(gitLab.discussions()
                        .project()
                        .updateRequestsDiscussions(
                                eq(ID),
                                eq(IID),
                                eq(DISCUSSION_ID),
                                any(GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload.class)))
                .thenReturn(response);

        sut.unresolve();

        final ArgumentCaptor<GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload.class);
        verify(gitLab.discussions().project())
                .updateRequestsDiscussions(eq(ID), eq(IID), eq(DISCUSSION_ID), payload.capture());
        assertThat(payload.getValue().resolved()).isFalse();
    }

    @Test
    void resolve_shouldReturnDiscussionFromAnswer_whenCalled() throws IOException {
        final GitLabDiscussion answer = GitLabDiscussionBuilder.builder()
                .id(DISCUSSION_ID)
                .resolvable(true)
                .resolved(true)
                .build();
        when(gitLab.discussions()
                        .project()
                        .updateRequestsDiscussions(
                                eq(ID),
                                eq(IID),
                                eq(DISCUSSION_ID),
                                any(GitLabProjectDiscussionsUpdateRequestsDiscussionsPayload.class)))
                .thenReturn(answer);

        final MergeRequestDiscussion result = sut.resolve();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(answer);
            softly.assertThat(result.isResolved()).isTrue();
            softly.assertThat(sut.isResolved()).isFalse();
        });
        verify(gitLab.discussions().project(), never()).getRequestsDiscussions(any(), eq(IID), any());
    }

    @Test
    void refresh_shouldReturnNewDiscussion_whenGitLabHasIt() throws IOException {
        final GitLabDiscussion reloaded = discussion(DISCUSSION_ID, note(1L, "a"));
        when(gitLab.discussions().project().getRequestsDiscussions(ID, IID, DISCUSSION_ID))
                .thenReturn(Optional.of(reloaded));

        final MergeRequestDiscussion result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.discussions().project().getRequestsDiscussions(ID, IID, DISCUSSION_ID))
                .thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(DISCUSSION_ID);
    }

    @Test
    void resolve_shouldPassCoreException_whenDiscussionIsNotResolvable() throws IOException {
        final IOException failure = new IOException("not resolvable");
        when(gitLab.discussions().project().updateRequestsDiscussions(eq(ID), eq(IID), eq(DISCUSSION_ID), any()))
                .thenThrow(failure);

        final Throwable thrown = catchThrowable(sut::resolve);

        assertThat(thrown).isSameAs(failure);
        verify(gitLab.discussions().project(), never()).getRequestsDiscussions(any(), eq(IID), any());
    }

    @Test
    void unresolve_shouldPassCoreException_whenDiscussionIsNotResolvable() throws IOException {
        final IOException failure = new IOException("not resolvable");
        when(gitLab.discussions().project().updateRequestsDiscussions(eq(ID), eq(IID), eq(DISCUSSION_ID), any()))
                .thenThrow(failure);

        final Throwable thrown = catchThrowable(sut::unresolve);

        assertThat(thrown).isSameAs(failure);
        verify(gitLab.discussions().project(), never()).getRequestsDiscussions(any(), eq(IID), any());
    }

    @Test
    void reply_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.reply(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void id_shouldThrowNullPointerException_whenResponseHasNoId() {
        final MergeRequestDiscussion bare = new MergeRequestDiscussion(
                mergeRequest, GitLabDiscussionBuilder.builder().build());

        final Throwable thrown = catchThrowable(bare::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
