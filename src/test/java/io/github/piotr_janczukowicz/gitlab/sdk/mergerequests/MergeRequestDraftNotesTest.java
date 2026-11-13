package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.draftNote;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabDraftNote;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesBulkPublishPayload;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesBulkPublishPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesBulkPublishReviewerState;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestDraftNotesTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest sut =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID, "Fix", "opened"));

    @Test
    void draftNotes_shouldMapEachItemToItsMergeRequest_whenListed() throws IOException {
        when(gitLab.draftnotes().list(ID, IID)).thenReturn(List.of(draftNote(1L, "a"), draftNote(2L, "b")));

        final List<DraftNote> result = sut.draftNotes().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(DraftNote::id).containsExactly(1L, 2L);
            softly.assertThat(result).extracting(DraftNote::mergeRequest).containsOnly(sut);
        });
    }

    @Test
    void draftNote_shouldReturnDraftNote_whenGitLabHasIt() throws IOException {
        when(gitLab.draftnotes().get(ID, IID, 5L)).thenReturn(Optional.of(draftNote(5L, "a")));

        final Optional<DraftNote> result = sut.draftNote(5L);

        assertThat(result).get().extracting(DraftNote::id).isEqualTo(5L);
    }

    @Test
    void draftNote_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.draftnotes().get(ID, IID, 5L)).thenReturn(Optional.empty());

        final Optional<DraftNote> result = sut.draftNote(5L);

        assertThat(result).isEmpty();
    }

    @Test
    void createDraftNote_shouldSendNote_whenNoteIsGiven() throws IOException {
        final GitLabDraftNote created = draftNote(9L, "Looks odd");
        when(gitLab.draftnotes().create(eq(ID), eq(IID), any(GitLabProjectDraftNotesCreatePayload.class)))
                .thenReturn(created);

        final DraftNote result = sut.createDraftNote("Looks odd");

        final ArgumentCaptor<GitLabProjectDraftNotesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDraftNotesCreatePayload.class);
        verify(gitLab.draftnotes()).create(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().note()).isEqualTo("Looks odd");
            softly.assertThat(payload.getValue().position()).isNull();
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.mergeRequest()).isSameAs(sut);
        });
    }

    @Test
    void createDraftNote_shouldThrowIllegalArgumentException_whenNoteIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createDraftNote(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.draftnotes());
    }

    @Test
    void createDraftNote_shouldThrowNullPointerException_whenNoteIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createDraftNote((String) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.draftnotes());
    }

    @Test
    void createDraftNote_shouldSendPayloadAsGiven_whenPayloadIsGiven() throws IOException {
        final GitLabProjectDraftNotesCreatePayload given = GitLabProjectDraftNotesCreatePayloadBuilder.builder()
                .note("In a thread")
                .inReplyToDiscussionId("abc")
                .resolveDiscussion(true)
                .build();
        final GitLabDraftNote created = draftNote(9L, "In a thread");
        when(gitLab.draftnotes().create(ID, IID, given)).thenReturn(created);

        final DraftNote result = sut.createDraftNote(given);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void createDraftNote_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.createDraftNote((GitLabProjectDraftNotesCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.draftnotes());
    }

    @Test
    void publishDraftNotes_shouldSendEmptyPayload_whenNoPayloadIsGiven() throws IOException {
        sut.publishDraftNotes();

        final ArgumentCaptor<GitLabProjectDraftNotesBulkPublishPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDraftNotesBulkPublishPayload.class);
        verify(gitLab.draftnotes()).bulkPublish(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().reviewerState()).isNull();
            softly.assertThat(payload.getValue().note()).isNull();
            softly.assertThat(payload.getValue().internal()).isNull();
        });
    }

    @Test
    void publishDraftNotes_shouldSendReviewerState_whenPayloadGiven() throws IOException {
        final GitLabProjectDraftNotesBulkPublishPayload given =
                GitLabProjectDraftNotesBulkPublishPayloadBuilder.builder()
                        .reviewerState(GitLabProjectDraftNotesBulkPublishReviewerState.REQUESTED_CHANGES)
                        .note("Please fix")
                        .build();

        sut.publishDraftNotes(given);

        verify(gitLab.draftnotes()).bulkPublish(ID, IID, given);
    }

    @Test
    void publishDraftNotes_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown =
                catchThrowable(() -> sut.publishDraftNotes((GitLabProjectDraftNotesBulkPublishPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.draftnotes());
    }
}
