package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.client.GitLabAuthenticationProviders;
import io.github.piotr_janczukowicz.gitlab.client.GitLabCaller;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClients;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class GitLabSdkTest {

    @Test
    void of_shouldWrapClient_whenGivenGitLab() {
        final GitLab gitlab = mock(GitLab.class);

        final GitLabSdk sut = GitLabSdk.of(gitlab);

        assertThat(sut.github()).isSameAs(gitlab);
        verifyNoInteractions(gitlab);
    }

    @Test
    void of_shouldThrowNullPointerException_whenGitLabIsNull() {
        final Throwable thrown = catchThrowable(() -> GitLabSdk.of((GitLab) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("gitlab");
    }

    @Test
    void of_shouldThrowNullPointerException_whenCallerIsNull() {
        final Throwable thrown = catchThrowable(() -> GitLabSdk.of((GitLabCaller) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("caller");
    }

    @Test
    void of_shouldThrowNullPointerException_whenClientIsNull() {
        final Throwable thrown = catchThrowable(() -> GitLabSdk.of((GitLabClient) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("client");
    }

    @Test
    void of_shouldBuildFromCaller() {
        try (GitLabClient client =
                GitLabClients.create(GitLabAuthenticationProviders.personalToken("token-for-a-test"), "my-app/1.0")) {

            final GitLabSdk sut = GitLabSdk.of(GitLabCaller.of(client));

            assertThat(sut.github()).isNotNull();
        }
    }

    @Test
    void of_shouldBuildFromClient() {
        try (GitLabClient client =
                GitLabClients.create(GitLabAuthenticationProviders.personalToken("token-for-a-test"), "my-app/1.0")) {

            final GitLabSdk sut = GitLabSdk.of(client);

            assertThat(sut.github()).isNotNull();
        }
    }
}
