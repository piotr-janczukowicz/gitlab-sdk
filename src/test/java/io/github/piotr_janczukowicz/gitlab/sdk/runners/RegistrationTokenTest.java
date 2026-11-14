package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.TOKEN;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.resetResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiResetTokenResult;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiResetTokenResultBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistrationTokenTest {

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabCiResetTokenResult response = resetResult();

        final RegistrationToken sut = new RegistrationToken(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.token()).isEqualTo(TOKEN);
            softly.assertThat(sut.expiresAt()).contains(Instant.parse("2027-01-01T00:00:00Z"));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void expiresAt_shouldBeEmpty_whenAbsent() {
        final RegistrationToken sut = new RegistrationToken(
                GitLabCiResetTokenResultBuilder.builder().token(TOKEN).build());

        assertThat(sut.expiresAt()).isEmpty();
    }

    @Test
    void toString_shouldNotContainToken_whenPrinted() {
        final RegistrationToken sut = new RegistrationToken(resetResult());

        assertThat(sut.toString()).doesNotContain(TOKEN).contains("<redacted>");
    }

    @Test
    void token_shouldThrowNullPointerException_whenAbsent() {
        final RegistrationToken sut =
                new RegistrationToken(GitLabCiResetTokenResultBuilder.builder().build());

        assertThat(catchThrowable(sut::token)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        assertThat(catchThrowable(() -> new RegistrationToken(null))).isInstanceOf(NullPointerException.class);
    }
}
