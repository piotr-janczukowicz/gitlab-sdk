package io.github.piotr_janczukowicz.gitlab.sdk.reactions;

import static io.github.piotr_janczukowicz.gitlab.sdk.reactions.ReactionsFixtures.award;
import static io.github.piotr_janczukowicz.gitlab.sdk.reactions.ReactionsFixtures.issue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmoji;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmojiBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class ReactionTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Issue issue = new Issue(GitLabSdk.of(gitLab).project("42"), issue(7L));

    @Test
    void accessors_shouldReturnResponseValues_whenReactionIsLoaded() {
        final GitLabAwardEmoji response = award(11L, "thumbsup");
        final Reaction sut = new Reaction(issue, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(11L);
            softly.assertThat(sut.name()).contains("thumbsup");
            softly.assertThat(sut.user())
                    .hasValueSatisfying(u -> assertThat(u.username()).isEqualTo("anna"));
            softly.assertThat(sut.createdAt()).contains(Instant.parse("2026-01-02T03:04:05Z"));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void user_shouldBeEmpty_whenAbsent() {
        final Reaction sut =
                new Reaction(issue, GitLabAwardEmojiBuilder.builder().id(1L).build());

        assertSoftly(softly -> {
            softly.assertThat(sut.user()).isEmpty();
            softly.assertThat(sut.createdAt()).isEmpty();
            softly.assertThat(sut.name()).isEmpty();
        });
    }

    @Test
    void id_shouldThrowNullPointerException_whenResponseHasNoId() {
        final Reaction sut =
                new Reaction(issue, GitLabAwardEmojiBuilder.builder().build());

        final Throwable thrown = catchThrowable(sut::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new Reaction(issue, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
