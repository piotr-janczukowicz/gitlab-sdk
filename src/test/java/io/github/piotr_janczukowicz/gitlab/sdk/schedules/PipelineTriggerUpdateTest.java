package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.trigger;
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
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabProjectPipelineTriggersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTriggerBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PipelineTriggerUpdateTest {

    private static final String ID = "42";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final PipelineTrigger trigger =
            new PipelineTrigger(GitLabSdk.of(gitLab).project(ID), trigger(4));

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final PipelineTriggerUpdate sut = trigger.update();

        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.pipelinetriggers());
    }

    @Test
    void send_shouldSendDescription_whenSet() throws IOException {
        when(gitLab.pipelinetriggers().update(eq(ID), eq(4L), any(GitLabProjectPipelineTriggersUpdatePayload.class)))
                .thenReturn(GitLabTriggerBuilder.builder(trigger(4))
                        .description("new")
                        .build());

        final PipelineTrigger result = trigger.update().description("new").send();

        final ArgumentCaptor<GitLabProjectPipelineTriggersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineTriggersUpdatePayload.class);
        verify(gitLab.pipelinetriggers()).update(eq(ID), eq(4L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("new");
            softly.assertThat(result.description()).contains("new");
        });
    }

    @Test
    void description_shouldThrowBeforeCoreCall_whenBlankOrNull() {
        final PipelineTriggerUpdate sut = trigger.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.description(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new PipelineTriggerUpdate(null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.pipelinetriggers());
    }
}
