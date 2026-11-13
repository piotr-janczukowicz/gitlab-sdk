package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.draftNote;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabDraftNote;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabDraftNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesApi;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class DraftNoteTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;
    private static final long DRAFT_ID = 5L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest mergeRequest =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID, "Fix", "opened"));
    private final GitLabDraftNote response = draftNote(DRAFT_ID, "Old");
    private final DraftNote sut = new DraftNote(mergeRequest, response);

    @Test
    void accessors_shouldReturnResponseValues_whenDraftNoteIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(DRAFT_ID);
            softly.assertThat(sut.note()).contains("Old");
            softly.assertThat(sut.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void note_shouldBeEmpty_whenNoteIsAbsent() {
        final DraftNote withoutNote = new DraftNote(
                mergeRequest, GitLabDraftNoteBuilder.builder().id(DRAFT_ID).build());

        assertThat(withoutNote.note()).isEmpty();
    }

    @Test
    void isResolvingDiscussion_shouldBeFalse_whenFlagIsAbsent() {
        assertThat(sut.isResolvingDiscussion()).isFalse();
    }

    @Test
    void isResolvingDiscussion_shouldBeTrue_whenFlagIsSet() {
        final DraftNote resolving = new DraftNote(
                mergeRequest,
                GitLabDraftNoteBuilder.builder()
                        .id(DRAFT_ID)
                        .resolveDiscussion(true)
                        .build());

        assertThat(resolving.isResolvingDiscussion()).isTrue();
    }

    @Test
    void refresh_shouldReturnNewDraftNote_whenGitLabHasIt() throws IOException {
        final GitLabDraftNote reloaded = draftNote(DRAFT_ID, "Edited elsewhere");
        when(gitLab.draftnotes().get(ID, IID, DRAFT_ID)).thenReturn(Optional.of(reloaded));

        final DraftNote refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.draftnotes().get(ID, IID, DRAFT_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void publish_shouldCallCoreWithDraftNoteId_whenCalled() throws IOException {
        sut.publish();

        verify(gitLab.draftnotes()).publish(ID, IID, DRAFT_ID);
    }

    @Test
    void publish_shouldPassCoreExceptionThrough_whenCoreFails() throws IOException {
        final IOException failure = new IOException("boom");
        final GitLabProjectDraftNotesApi api = gitLab.draftnotes();
        doThrow(failure).when(api).publish(ID, IID, DRAFT_ID);

        final Throwable thrown = catchThrowable(sut::publish);

        assertThat(thrown).isSameAs(failure);
    }

    @Test
    void delete_shouldCallCoreWithDraftNoteId_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.draftnotes()).delete(ID, IID, DRAFT_ID);
    }
}
