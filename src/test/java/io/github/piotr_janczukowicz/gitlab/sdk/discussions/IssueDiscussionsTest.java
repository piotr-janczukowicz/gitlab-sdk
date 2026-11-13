package io.github.piotr_janczukowicz.gitlab.sdk.discussions;

import static io.github.piotr_janczukowicz.gitlab.sdk.discussions.DiscussionsFixtures.discussion;
import static io.github.piotr_janczukowicz.gitlab.sdk.discussions.DiscussionsFixtures.issue;
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
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussion;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateIssuesDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueDiscussionsTest {

    private static final String ID = "42";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Issue sut = new Issue(GitLabSdk.of(gitLab).project(ID), issue(IID));

    @Test
    void startDiscussion_shouldSendBody_whenBodyGiven() throws IOException {
        final GitLabDiscussion created = discussion("d1", note(1L, "hi"));
        when(gitLab.discussions()
                        .project()
                        .createIssuesDiscussions(
                                eq(ID), eq(IID), any(GitLabProjectDiscussionsCreateIssuesDiscussionsPayload.class)))
                .thenReturn(created);

        final IssueDiscussion result = sut.startDiscussion("hi");

        final ArgumentCaptor<GitLabProjectDiscussionsCreateIssuesDiscussionsPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDiscussionsCreateIssuesDiscussionsPayload.class);
        verify(gitLab.discussions().project()).createIssuesDiscussions(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("hi");
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.issue()).isSameAs(sut);
        });
    }

    @Test
    void startDiscussion_shouldThrowIllegalArgumentException_whenBodyIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.startDiscussion(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.discussions().project(), never())
                .createIssuesDiscussions(
                        any(), eq(IID), any(GitLabProjectDiscussionsCreateIssuesDiscussionsPayload.class));
    }

    @Test
    void discussions_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.discussions().project().listIssuesDiscussions(ID, IID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(id -> {
                    consumed.incrementAndGet();
                    return discussion(id);
                }));

        final Stream<IssueDiscussion> discussions = sut.discussions();

        assertThat(consumed).hasValue(0);
        assertThat(discussions.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void discussions_shouldMapEachItemToItsIssue_whenListed() throws IOException {
        when(gitLab.discussions().project().listIssuesDiscussions(ID, IID).stream())
                .thenReturn(Stream.of(discussion("a"), discussion("b")));

        final List<IssueDiscussion> discussions = sut.discussions().toList();

        assertThat(discussions).hasSize(2).allSatisfy(d -> assertThat(d.issue()).isSameAs(sut));
    }

    @Test
    void discussion_shouldReturnDiscussion_whenGitLabHasIt() throws IOException {
        final GitLabDiscussion loaded = discussion("d1");
        when(gitLab.discussions().project().getIssuesDiscussions(ID, IID, "d1")).thenReturn(Optional.of(loaded));

        final Optional<IssueDiscussion> found = sut.discussion("d1");

        assertThat(found).hasValueSatisfying(d -> assertThat(d.response()).isSameAs(loaded));
    }

    @Test
    void discussion_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.discussions().project().getIssuesDiscussions(ID, IID, "d1")).thenReturn(Optional.empty());

        final Optional<IssueDiscussion> found = sut.discussion("d1");

        assertThat(found).isEmpty();
    }

    @Test
    void discussion_shouldThrowIllegalArgumentException_whenIdIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.discussion(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.discussions().project(), never()).getIssuesDiscussions(any(), eq(IID), any());
    }
}
