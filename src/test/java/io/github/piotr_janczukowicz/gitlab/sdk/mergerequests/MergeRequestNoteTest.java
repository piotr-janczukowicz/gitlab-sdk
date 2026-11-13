package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesUpdateRequestsNotesPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestNoteTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;
    private static final long NOTE_ID = 5L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest mergeRequest =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID, "Fix", "opened"));
    private final GitLabNote response = note(NOTE_ID, "Old");
    private final MergeRequestNote sut = new MergeRequestNote(mergeRequest, response);

    @Test
    void accessors_shouldReturnResponseValues_whenNoteIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(NOTE_ID);
            softly.assertThat(sut.body()).contains("Old");
            softly.assertThat(sut.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void update_shouldSendBodyOnIidAndNoteId_andReturnNewNote() throws IOException {
        final GitLabNote changed = note(NOTE_ID, "New");
        when(gitLab.notes()
                        .project()
                        .updateRequestsNotes(
                                eq(ID), eq(IID), eq(NOTE_ID), any(GitLabProjectNotesUpdateRequestsNotesPayload.class)))
                .thenReturn(changed);

        final MergeRequestNote result = sut.update("New");

        final ArgumentCaptor<GitLabProjectNotesUpdateRequestsNotesPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectNotesUpdateRequestsNotesPayload.class);
        verify(gitLab.notes().project()).updateRequestsNotes(eq(ID), eq(IID), eq(NOTE_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("New");
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(changed);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void update_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.update(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.notes().project()).deleteRequestsNotes(ID, IID, NOTE_ID);
    }

    @Test
    void refresh_shouldReturnNewNote_whenGitLabHasIt() throws IOException {
        final GitLabNote reloaded = note(NOTE_ID, "Edited elsewhere");
        when(gitLab.notes().project().getRequestsNotes(ID, IID, NOTE_ID)).thenReturn(Optional.of(reloaded));

        final MergeRequestNote refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.notes().project().getRequestsNotes(ID, IID, NOTE_ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Note 5 is gone");
    }
}
