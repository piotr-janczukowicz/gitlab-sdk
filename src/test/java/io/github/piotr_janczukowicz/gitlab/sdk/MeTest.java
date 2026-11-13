package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.currentUser;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
class MeTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Me sut = GitLabSdk.of(gitLab).me();

    @Test
    void profile_shouldReturnCurrentUser_whenGitLabHasTheAccount() throws IOException {
        final GitLabUserProfile response = profile(5L, "tanuki");
        when(gitLab.users().getUser()).thenReturn(currentUser(5L));
        when(gitLab.users().get(5L)).thenReturn(Optional.of(response));

        final User user = sut.profile();

        assertThat(user.response()).isSameAs(response);
    }

    @Test
    void profile_shouldThrowIllegalStateException_whenProfileIsGone() throws IOException {
        when(gitLab.users().getUser()).thenReturn(currentUser(5L));
        when(gitLab.users().get(5L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::profile);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("is gone");
    }

    @Test
    void constructor_shouldBePublicAndWrapTheSdk_whenCalled() throws ReflectiveOperationException, IOException {
        final GitLabSdk sdk = GitLabSdk.of(gitLab);
        when(gitLab.users().getUser()).thenReturn(currentUser(5L));
        when(gitLab.users().get(5L)).thenReturn(Optional.of(profile(5L, "tanuki")));

        final Me created = Me.class.getConstructor(GitLabSdk.class).newInstance(sdk);

        assertThat(created.profile().username()).isEqualTo("tanuki");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> new Me(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
