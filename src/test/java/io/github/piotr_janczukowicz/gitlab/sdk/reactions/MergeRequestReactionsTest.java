package io.github.piotr_janczukowicz.gitlab.sdk.reactions;

import static io.github.piotr_janczukowicz.gitlab.sdk.reactions.ReactionsFixtures.award;
import static io.github.piotr_janczukowicz.gitlab.sdk.reactions.ReactionsFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.reactions.ReactionsFixtures.verifyNoCreateExcept;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmoji;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestReactionsTest {

    private static final String ID = "42";
    private static final long IID = 7L;
    private static final long AWARD_ID = 11L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final MergeRequest sut = new MergeRequest(GitLabSdk.of(gitLab).project(ID), mergeRequest(IID));

    @Test
    void react_shouldSendNameThroughTheRightCoreMethod_whenNameIsGiven() throws IOException {
        final GitLabAwardEmoji created = award(AWARD_ID, "thumbsup");
        when(gitLab.emojireactions()
                        .createRequestsAwardEmoji(
                                eq(ID), eq(IID), any(GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload.class)))
                .thenReturn(created);

        final Reaction result = sut.react("thumbsup");

        final ArgumentCaptor<GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload.class);
        verify(gitLab.emojireactions()).createRequestsAwardEmoji(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("thumbsup");
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.id()).isEqualTo(AWARD_ID);
        });
        verifyNoCreateExcept(gitLab, "createRequestsAwardEmoji");
    }

    @Test
    void react_shouldThrowIllegalArgumentException_whenNameIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.react(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoCreateExcept(gitLab, "");
    }

    @Test
    void react_shouldThrowNullPointerException_whenNameIsNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.react(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoCreateExcept(gitLab, "");
    }

    @Test
    void react_shouldPassCoreExceptionThrough_whenGitLabRefusesTheReaction() throws IOException {
        final IOException failure = new IOException("404 Duplicate award emoji");
        when(gitLab.emojireactions()
                        .createRequestsAwardEmoji(
                                eq(ID), eq(IID), any(GitLabProjectEmojiReactionsCreateRequestsAwardEmojiPayload.class)))
                .thenThrow(failure);

        final Throwable thrown = catchThrowable(() -> sut.react("thumbsup"));

        assertThat(thrown).isSameAs(failure);
    }

    @Test
    void reactions_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.emojireactions().listRequestsAwardEmoji(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return award(id, "star");
                }));

        final Stream<Reaction> reactions = sut.reactions();

        assertThat(consumed).hasValue(0);
        assertThat(reactions.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void delete_shouldCallCoreWithAwardId_whenReactionIsDeleted() throws IOException {
        final Reaction reaction = new Reaction(sut, award(AWARD_ID, "star"));

        reaction.delete();

        verify(gitLab.emojireactions()).deleteRequestsAwardEmoji(ID, IID, AWARD_ID);
    }

    @Test
    void refresh_shouldReturnLoadedReaction_whenGitLabHasIt() throws IOException {
        final GitLabAwardEmoji loaded = award(AWARD_ID, "rocket");
        when(gitLab.emojireactions().getRequestsAwardEmoji(ID, IID, AWARD_ID)).thenReturn(Optional.of(loaded));
        final Reaction reaction = new Reaction(sut, award(AWARD_ID, "star"));

        final Reaction refreshed = reaction.refresh();

        assertThat(refreshed.response()).isSameAs(loaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.emojireactions().getRequestsAwardEmoji(ID, IID, AWARD_ID)).thenReturn(Optional.empty());
        final Reaction reaction = new Reaction(sut, award(AWARD_ID, "star"));

        final Throwable thrown = catchThrowable(reaction::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("11");
    }
}
