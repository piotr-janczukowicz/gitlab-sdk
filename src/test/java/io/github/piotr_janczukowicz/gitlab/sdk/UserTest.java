package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUserProfile;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class UserTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    @Test
    void user_shouldReturnLoadedUser_whenGitLabHasIt() throws IOException {
        final GitLabUserProfile response = profile(5L, "tanuki");
        when(gitLab.users().get(5L)).thenReturn(Optional.of(response));

        final Optional<User> user = sdk.user(5L);

        assertSoftly(softly -> {
            softly.assertThat(user).isPresent();
            softly.assertThat(user.orElseThrow().id()).isEqualTo(5L);
            softly.assertThat(user.orElseThrow().username()).isEqualTo("tanuki");
            softly.assertThat(user.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void user_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.users().get(5L)).thenReturn(Optional.empty());

        assertThat(sdk.user(5L)).isEmpty();
    }

    @Test
    void refresh_shouldReturnNewUser_whenGitLabHasIt() throws IOException {
        final GitLabUserProfile before = profile(5L, "tanuki");
        final GitLabUserProfile after = profile(5L, "renamed");
        when(gitLab.users().get(5L)).thenReturn(Optional.of(before), Optional.of(after));
        final User loaded = sdk.user(5L).orElseThrow();

        final User refreshed = loaded.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(loaded);
            softly.assertThat(refreshed.username()).isEqualTo("renamed");
            softly.assertThat(loaded.username()).isEqualTo("tanuki");
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.users().get(5L)).thenReturn(Optional.of(profile(5L, "tanuki")), Optional.empty());
        final User loaded = sdk.user(5L).orElseThrow();

        final Throwable thrown = catchThrowable(loaded::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }
}
