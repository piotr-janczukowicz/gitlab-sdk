package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.timeStats;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStats;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStatsBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class TimeStatsTest {

    @Test
    void accessors_shouldReturnResponseValues_whenGitLabReturnsThem() {
        final GitLabIssuableTimeStats response = timeStats(3600, 1800);

        final TimeStats sut = new TimeStats(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.estimate()).isEqualTo(Duration.ofHours(1));
            softly.assertThat(sut.totalSpent()).isEqualTo(Duration.ofMinutes(30));
            softly.assertThat(sut.humanEstimate()).contains("1h");
            softly.assertThat(sut.humanTotalSpent()).contains("30m");
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldReturnZeroAndEmpty_whenGitLabReturnsNothing() {
        final TimeStats sut =
                new TimeStats(GitLabIssuableTimeStatsBuilder.builder().build());

        assertSoftly(softly -> {
            softly.assertThat(sut.estimate()).isEqualTo(Duration.ZERO);
            softly.assertThat(sut.totalSpent()).isEqualTo(Duration.ZERO);
            softly.assertThat(sut.humanEstimate()).isEmpty();
            softly.assertThat(sut.humanTotalSpent()).isEmpty();
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenResponseIsNull() {
        final Throwable thrown = catchThrowable(() -> new TimeStats(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("response");
    }
}
