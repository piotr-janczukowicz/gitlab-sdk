package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.fullSchedule;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PipelineScheduleUpdateTest {

    private static final String ID = "42";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final PipelineSchedule schedule =
            new PipelineSchedule(GitLabSdk.of(gitLab).project(ID), schedule(7));

    private GitLabProjectPipelineSchedulesUpdatePayload payload() throws IOException {
        final ArgumentCaptor<GitLabProjectPipelineSchedulesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineSchedulesUpdatePayload.class);
        verify(gitLab.pipelineschedules()).update(eq(ID), eq(7L), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.pipelineschedules().update(eq(ID), eq(7L), any(GitLabProjectPipelineSchedulesUpdatePayload.class)))
                .thenReturn(fullSchedule(7));
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final PipelineScheduleUpdate sut = schedule.update();

        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.pipelineschedules());
    }

    @Test
    void send_shouldSendOnlySetFields_whenOnlyCronSet() throws IOException {
        stubUpdate();

        final PipelineSchedule result = schedule.update().cron("5 5 * * *").send();

        final GitLabProjectPipelineSchedulesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.cron()).isEqualTo("5 5 * * *");
            softly.assertThat(sent.description()).isNull();
            softly.assertThat(sent.ref()).isNull();
            softly.assertThat(sent.cronTimezone()).isNull();
            softly.assertThat(sent.active()).isNull();
            softly.assertThat(sent.inputs()).isNull();
            softly.assertThat(result.isActive()).isTrue();
        });
    }

    @Test
    void send_shouldSendAllFields_whenAllSet() throws IOException {
        stubUpdate();

        schedule.update()
                .description("d")
                .ref("dev")
                .cron("1 1 * * *")
                .cronTimezone("Europe/Warsaw")
                .active(false)
                .send();

        final GitLabProjectPipelineSchedulesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.description()).isEqualTo("d");
            softly.assertThat(sent.ref()).isEqualTo("dev");
            softly.assertThat(sent.cron()).isEqualTo("1 1 * * *");
            softly.assertThat(sent.cronTimezone()).isEqualTo("Europe/Warsaw");
            softly.assertThat(sent.active()).isFalse();
        });
    }

    @Test
    void send_shouldAllowRefreshAfterwards_whenUpdated() throws IOException {
        stubUpdate();
        when(gitLab.pipelineschedules().get(ID, 7L)).thenReturn(Optional.of(fullSchedule(7)));

        final PipelineSchedule updated = schedule.update().active(true).send();

        assertThat(updated.refresh().isActive()).isTrue();
    }

    @Test
    void setters_shouldThrowBeforeCoreCall_whenArgumentIsBlankOrNull() {
        final PipelineScheduleUpdate sut = schedule.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.description(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.ref(""))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.cron(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.cronTimezone(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.ref(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.cron(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.cronTimezone(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new PipelineScheduleUpdate(null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.pipelineschedules());
    }
}
