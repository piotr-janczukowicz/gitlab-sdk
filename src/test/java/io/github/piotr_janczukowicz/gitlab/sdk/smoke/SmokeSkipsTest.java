package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

@UnitTest
class SmokeSkipsTest {

    @Test
    void probe_shouldReturnResult_whenCallSucceeds() throws IOException {
        final String result = SmokeSkips.probe("packages", () -> "listed");

        assertThat(result).isEqualTo("listed");
    }

    @Test
    void probe_shouldSkip_whenGitLabAnswersForbidden() {
        final GitLabForbiddenException forbidden = mock(GitLabForbiddenException.class);
        when(forbidden.statusCode()).thenReturn(403);
        final SmokeSkips.Probe<String> probe = () -> {
            throw forbidden;
        };

        final Throwable thrown = catchThrowable(() -> SmokeSkips.probe("runner jobs", probe));

        assertThat(thrown)
                .isInstanceOf(TestAbortedException.class)
                .hasMessageContaining("runner jobs")
                .hasMessageContaining("HTTP 403")
                .hasCause(forbidden);
    }

    @Test
    void probe_shouldSkip_whenGitLabAnswersNotFound() {
        final GitLabNotFoundException notFound = mock(GitLabNotFoundException.class);
        when(notFound.statusCode()).thenReturn(404);
        final SmokeSkips.Probe<String> probe = () -> {
            throw notFound;
        };

        final Throwable thrown = catchThrowable(() -> SmokeSkips.probe("container registry", probe));

        assertThat(thrown)
                .isInstanceOf(TestAbortedException.class)
                .hasMessageContaining("container registry")
                .hasMessageContaining("HTTP 404");
    }

    @Test
    void probe_shouldPassFailureThrough_whenGitLabAnswersOtherError() {
        final GitLabApiException serverError = mock(GitLabApiException.class);
        final SmokeSkips.Probe<String> probe = () -> {
            throw serverError;
        };

        final Throwable thrown = catchThrowable(() -> SmokeSkips.probe("packages", probe));

        assertThat(thrown).isSameAs(serverError);
    }

    @Test
    void probe_shouldPassFailureThrough_whenTransportFails() {
        final IOException failure = new IOException("connection reset");
        final SmokeSkips.Probe<String> probe = () -> {
            throw failure;
        };

        final Throwable thrown = catchThrowable(() -> SmokeSkips.probe("packages", probe));

        assertThat(thrown).isSameAs(failure);
    }
}
