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
import io.github.piotr_janczukowicz.gitlab.api.draftnotes.GitLabProjectDraftNotesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class DraftNoteUpdateTest {

    private static final String ID = "42";
    private static final long IID = 7L;
    private static final long DRAFT_ID = 5L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest mergeRequest =
            new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID, "Fix", "opened"));
    private final DraftNote draftNote = new DraftNote(mergeRequest, draftNote(DRAFT_ID, "Old"));

    @Test
    void send_shouldSendOnlyNote_whenNoteIsSet() throws IOException {
        final GitLabDraftNote updated = draftNote(DRAFT_ID, "New");
        when(gitLab.draftnotes().update(eq(ID), eq(IID), eq(DRAFT_ID), any(GitLabProjectDraftNotesUpdatePayload.class)))
                .thenReturn(updated);

        final DraftNote result = draftNote.update().note("New").send();

        final ArgumentCaptor<GitLabProjectDraftNotesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDraftNotesUpdatePayload.class);
        verify(gitLab.draftnotes()).update(eq(ID), eq(IID), eq(DRAFT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().note()).isEqualTo("New");
            softly.assertThat(payload.getValue().position()).isNull();
            softly.assertThat(result.response()).isSameAs(updated);
            softly.assertThat(result.mergeRequest()).isSameAs(mergeRequest);
            softly.assertThat(draftNote.response().note()).isEqualTo("Old");
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final Throwable thrown = catchThrowable(() -> draftNote.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.draftnotes());
    }

    @Test
    void note_shouldThrowIllegalArgumentException_whenNoteIsBlank() {
        final Throwable thrown = catchThrowable(() -> draftNote.update().note(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.draftnotes());
    }

    @Test
    void note_shouldThrowNullPointerException_whenNoteIsNull() {
        final Throwable thrown = catchThrowable(() -> draftNote.update().note(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
