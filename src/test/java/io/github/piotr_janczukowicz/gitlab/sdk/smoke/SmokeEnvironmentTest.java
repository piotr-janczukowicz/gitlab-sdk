package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class SmokeEnvironmentTest {

    @Test
    void configured_shouldBeFalse_whenTokenIsMissing() {
        final SmokeEnvironment sut = SmokeEnvironment.of(Map.of(SmokeEnvironment.PROJECT_VARIABLE, "me/gitlab-smoke"));

        assertThat(sut.configured()).isFalse();
    }

    @Test
    void configured_shouldBeTrue_whenTokenAndProjectAreSet() {
        final SmokeEnvironment sut = SmokeEnvironment.of(Map.of(
                SmokeEnvironment.TOKEN_VARIABLE, " token ", SmokeEnvironment.PROJECT_VARIABLE, "me/gitlab-smoke"));

        assertSoftly(softly -> {
            softly.assertThat(sut.configured()).isTrue();
            softly.assertThat(sut.namespace()).contains("me");
            softly.assertThat(sut.name("x")).isEqualTo("smoke-" + sut.runId() + "-x");
        });
    }

    @Test
    void requireSandbox_shouldThrowIllegalStateException_whenPathDoesNotContainSmoke() {
        final Throwable thrown = catchThrowable(() -> SmokeEnvironment.requireSandbox("me/production"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("smoke");
    }

    @Test
    void requireSandbox_shouldThrowIllegalStateException_whenProjectIsANumericId() {
        final Throwable thrown = catchThrowable(() -> SmokeEnvironment.requireSandbox("12345"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireSandbox_shouldAccept_whenPathContainsSmoke() {
        final Throwable thrown = catchThrowable(() -> SmokeEnvironment.requireSandbox("Me/GitLab-Smoke"));

        assertThat(thrown).isNull();
    }
}
