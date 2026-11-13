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
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussion;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPayload;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPosition;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPositionBuilder;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabProjectDiscussionsCreateRequestsDiscussionsPositionType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestDiscussionsTest {

    private static final String ID = "42";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest sut = new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID));

    @Test
    void startDiscussion_shouldSendBody_whenBodyGiven() throws IOException {
        final GitLabDiscussion created = discussion("d1", note(1L, "hi"));
        when(gitLab.discussions()
                        .project()
                        .createRequestsDiscussions(
                                eq(ID), eq(IID), any(GitLabProjectDiscussionsCreateRequestsDiscussionsPayload.class)))
                .thenReturn(created);

        final MergeRequestDiscussion result = sut.startDiscussion("hi");

        final ArgumentCaptor<GitLabProjectDiscussionsCreateRequestsDiscussionsPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDiscussionsCreateRequestsDiscussionsPayload.class);
        verify(gitLab.discussions().project()).createRequestsDiscussions(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("hi");
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.mergeRequest()).isSameAs(sut);
        });
    }

    @Test
    void startDiscussion_shouldThrowIllegalArgumentException_whenBodyIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.startDiscussion(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.discussions().project(), never())
                .createRequestsDiscussions(
                        any(), eq(IID), any(GitLabProjectDiscussionsCreateRequestsDiscussionsPayload.class));
    }

    @Test
    void discussions_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.discussions().project().listRequestsDiscussions(ID, IID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(id -> {
                    consumed.incrementAndGet();
                    return discussion(id);
                }));

        final Stream<MergeRequestDiscussion> discussions = sut.discussions();

        assertThat(consumed).hasValue(0);
        assertThat(discussions.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void discussions_shouldMapEachItemToItsMergeRequest_whenListed() throws IOException {
        when(gitLab.discussions().project().listRequestsDiscussions(ID, IID).stream())
                .thenReturn(Stream.of(discussion("a"), discussion("b")));

        final List<MergeRequestDiscussion> discussions = sut.discussions().toList();

        assertThat(discussions)
                .hasSize(2)
                .allSatisfy(d -> assertThat(d.mergeRequest()).isSameAs(sut));
    }

    @Test
    void discussion_shouldReturnDiscussion_whenGitLabHasIt() throws IOException {
        final GitLabDiscussion loaded = discussion("d1");
        when(gitLab.discussions().project().getRequestsDiscussions(ID, IID, "d1"))
                .thenReturn(Optional.of(loaded));

        final Optional<MergeRequestDiscussion> found = sut.discussion("d1");

        assertThat(found).hasValueSatisfying(d -> assertThat(d.response()).isSameAs(loaded));
    }

    @Test
    void discussion_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.discussions().project().getRequestsDiscussions(ID, IID, "d1"))
                .thenReturn(Optional.empty());

        final Optional<MergeRequestDiscussion> found = sut.discussion("d1");

        assertThat(found).isEmpty();
    }

    @Test
    void discussion_shouldThrowIllegalArgumentException_whenIdIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.discussion(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.discussions().project(), never()).getRequestsDiscussions(any(), eq(IID), any());
    }

    @Test
    void startDiscussion_shouldSendPosition_whenPayloadGiven() throws IOException {
        final GitLabProjectDiscussionsCreateRequestsDiscussionsPosition position =
                GitLabProjectDiscussionsCreateRequestsDiscussionsPositionBuilder.builder()
                        .positionType(GitLabProjectDiscussionsCreateRequestsDiscussionsPositionType.TEXT)
                        .newPath("a.txt")
                        .newLine(3)
                        .build();
        final GitLabProjectDiscussionsCreateRequestsDiscussionsPayload given =
                GitLabProjectDiscussionsCreateRequestsDiscussionsPayloadBuilder.builder()
                        .body("here")
                        .position(position)
                        .build();
        final GitLabDiscussion created = discussion("d1", note(1L, "here"));
        when(gitLab.discussions().project().createRequestsDiscussions(ID, IID, given))
                .thenReturn(created);

        final MergeRequestDiscussion result = sut.startDiscussion(given);

        assertThat(result.response()).isSameAs(created);
        verify(gitLab.discussions().project()).createRequestsDiscussions(ID, IID, given);
    }

    @Test
    void startDiscussion_shouldThrowNullPointerException_whenPayloadIsNull() throws IOException {
        final Throwable thrown = catchThrowable(
                () -> sut.startDiscussion((GitLabProjectDiscussionsCreateRequestsDiscussionsPayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.discussions().project(), never()).createRequestsDiscussions(any(), eq(IID), any());
    }

    @Test
    void startDiscussion_shouldThrowNullPointerException_whenBodyIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.startDiscussion((String) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void discussion_shouldThrowNullPointerException_whenIdIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.discussion(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
