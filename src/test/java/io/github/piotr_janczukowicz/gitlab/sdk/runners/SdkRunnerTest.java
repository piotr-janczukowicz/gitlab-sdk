package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.fullRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SdkRunnerTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    @Test
    void runner_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.runners().instance().get(9L)).thenReturn(Optional.empty());

        assertThat(sdk.runner(9L)).isEmpty();
    }

    @Test
    void runner_shouldReturnRunner_whenPresent() throws IOException {
        when(gitLab.runners().instance().get(9L)).thenReturn(Optional.of(fullRunner(9)));

        assertThat(sdk.runner(9L))
                .get()
                .satisfies(runner -> assertThat(runner.id()).isEqualTo(9L));
    }
}
