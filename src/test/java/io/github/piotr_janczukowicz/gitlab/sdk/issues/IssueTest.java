package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issue;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.note;
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
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesNotesPayload;
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
class IssueTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabIssue response = issue(IID, "Broken", "opened");
    private final Issue sut = new Issue(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenIssueIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.iid()).isEqualTo(IID);
            softly.assertThat(sut.title()).isEqualTo("Broken");
            softly.assertThat(sut.state()).isEqualTo(IssueState.OPENED);
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.labels()).isEmpty();
        });
    }

    @Test
    void state_shouldReturnClosed_whenGitLabSaysClosed() {
        final Issue closed = new Issue(project, issue(IID, "Broken", "closed"));

        assertThat(closed.state()).isEqualTo(IssueState.CLOSED);
    }

    @Test
    void state_shouldThrowIllegalArgumentException_whenUnknownState() {
        final Issue unknown = new Issue(project, issue(IID, "Broken", "locked"));

        final Throwable thrown = catchThrowable(unknown::state);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("locked");
    }

    @Test
    void close_shouldSendStateEventClose_andReturnNewIssue() throws IOException {
        final GitLabIssue closed = issue(IID, "Broken", "closed");
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(closed);

        final Issue result = sut.close();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().stateEvent()).isEqualTo(GitLabProjectIssuesUpdateStateEvent.CLOSE);
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(closed);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void reopen_shouldSendStateEventReopen_whenCalled() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Broken", "opened"));

        sut.reopen();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().stateEvent()).isEqualTo(GitLabProjectIssuesUpdateStateEvent.REOPEN);
    }

    @Test
    void comment_shouldCreateNoteOnIid_whenBodyIsGiven() throws IOException {
        when(gitLab.notes()
                        .project()
                        .createIssuesNotes(eq(ID), eq(IID), any(GitLabProjectNotesCreateIssuesNotesPayload.class)))
                .thenReturn(note(5L, "Thanks"));

        final Note created = sut.comment("Thanks");

        final ArgumentCaptor<GitLabProjectNotesCreateIssuesNotesPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectNotesCreateIssuesNotesPayload.class);
        verify(gitLab.notes().project()).createIssuesNotes(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Thanks");
            softly.assertThat(created.id()).isEqualTo(5L);
            softly.assertThat(created.issue()).isSameAs(sut);
        });
    }

    @Test
    void comment_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.comment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notes_shouldMapEachNoteToItsIssue() throws IOException {
        when(gitLab.notes().project().listIssuesNotes(ID, IID).stream())
                .thenReturn(Stream.of(note(1L, "a"), note(2L, "b")));

        final List<Note> notes = sut.notes().toList();

        assertThat(notes).hasSize(2).allSatisfy(n -> assertThat(n.issue()).isSameAs(sut));
    }

    @Test
    void notes_shouldBeLazy_andStopAtLimit() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.notes().project().listIssuesNotes(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return note(id, "n");
                }));

        final Stream<Note> notes = sut.notes();

        assertThat(consumed).hasValue(0);
        assertThat(notes.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void notes_shouldReturnEmptyStream_whenIssueHasNoNotes() throws IOException {
        when(gitLab.notes().project().listIssuesNotes(ID, IID).stream()).thenReturn(Stream.empty());

        final List<Note> notes = sut.notes().toList();

        assertThat(notes).isEmpty();
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.issues().project()).delete(ID, IID);
    }

    @Test
    void refresh_shouldReturnNewIssue_whenGitLabHasIt() throws IOException {
        final GitLabIssue reloaded = issue(IID, "Renamed", "opened");
        when(gitLab.issues().project().get(ID, IID)).thenReturn(Optional.of(reloaded));

        final Issue refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.issues().project().get(ID, IID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Issue 7 is gone");
    }
}
