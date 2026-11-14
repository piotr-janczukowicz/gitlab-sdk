package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.fullSchedule;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.owner;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.schedule;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.variable;
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
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadataBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineScheduleDetails;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreateVariablePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.VariableType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PipelineScheduleTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private PipelineSchedule sut(final GitLabCiPipelineScheduleDetails response) {
        return new PipelineSchedule(project, response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabCiPipelineScheduleDetails response = fullSchedule(7);

        final PipelineSchedule sut = sut(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(7L);
            softly.assertThat(sut.description()).isEqualTo("nightly");
            softly.assertThat(sut.ref()).isEqualTo("main");
            softly.assertThat(sut.cron()).isEqualTo("0 1 * * *");
            softly.assertThat(sut.cronTimezone()).contains("UTC");
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.nextRunAt()).contains(Instant.parse("2026-01-02T01:00:00Z"));
            softly.assertThat(sut.owner()).contains(owner());
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenAbsent() {
        final PipelineSchedule sut = sut(schedule(7));

        assertSoftly(softly -> {
            softly.assertThat(sut.cronTimezone()).isEmpty();
            softly.assertThat(sut.isActive()).isFalse();
            softly.assertThat(sut.nextRunAt()).isEmpty();
            softly.assertThat(sut.owner()).isEmpty();
            softly.assertThat(sut.variables()).isEmpty();
        });
    }

    @Test
    void lastPipeline_shouldLoadThroughProject_whenPresent() throws IOException {
        when(gitLab.pipelines().get(ID, 55L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(55L)
                        .status("success")
                        .ref("main")
                        .build()));

        final var result = sut(fullSchedule(7)).lastPipeline();

        assertThat(result).get().satisfies(p -> assertThat(p.id()).isEqualTo(55L));
    }

    @Test
    void lastPipeline_shouldBeEmpty_whenAbsent() throws IOException {
        final var result = sut(schedule(7)).lastPipeline();

        assertThat(result).isEmpty();
        verifyNoInteractions(gitLab.pipelines());
    }

    @Test
    void variables_shouldBeImmutableAndMapped_whenRecordHasVariables() {
        final PipelineSchedule sut = sut(fullSchedule(7));

        final var result = sut.variables();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(ScheduleVariable::key).containsExactly("A", "B");
            softly.assertThat(result.get(0).value()).contains(SECRET);
            softly.assertThat(result.get(0).schedule()).isSameAs(sut);
            softly.assertThat(catchThrowable(() -> result.add(result.get(0))))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void update_shouldSendOnlySetFields_whenDescriptionSet() throws IOException {
        when(gitLab.pipelineschedules().update(eq(ID), eq(7L), any())).thenReturn(schedule(7));

        final var result = sut(schedule(7)).update().description("x").send();

        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void takeOwnership_shouldReturnScheduleFromAnswer_whenCalled() throws IOException {
        when(gitLab.pipelineschedules().takeOwnership(ID, 7L)).thenReturn(fullSchedule(7));

        final PipelineSchedule result = sut(schedule(7)).takeOwnership();

        assertSoftly(softly -> {
            softly.assertThat(result.owner()).contains(owner());
            softly.assertThat(result.project()).isSameAs(project);
        });
    }

    @Test
    void play_shouldCallCore_whenCalled() throws IOException {
        sut(schedule(7)).play();

        verify(gitLab.pipelineschedules()).play(ID, 7L);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut(schedule(7)).delete();

        verify(gitLab.pipelineschedules()).delete(ID, 7L);
    }

    @Test
    void refresh_shouldReloadSchedule_whenPresent() throws IOException {
        when(gitLab.pipelineschedules().get(ID, 7L)).thenReturn(Optional.of(fullSchedule(7)));

        final PipelineSchedule result = sut(schedule(7)).refresh();

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.pipelineschedules().get(ID, 7L)).thenReturn(Optional.empty());
        final PipelineSchedule sut = sut(schedule(7));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void addVariable_shouldSendKeyValueAndType_whenTypeGiven() throws IOException {
        when(gitLab.pipelineschedules().createVariable(eq(ID), eq(7L), any())).thenReturn(variable("K", SECRET));

        final ScheduleVariable result = sut(schedule(7)).addVariable("K", SECRET, VariableType.FILE);

        final ArgumentCaptor<GitLabProjectPipelineSchedulesCreateVariablePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineSchedulesCreateVariablePayload.class);
        verify(gitLab.pipelineschedules()).createVariable(eq(ID), eq(7L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().key()).isEqualTo("K");
            softly.assertThat(payload.getValue().value()).isEqualTo(SECRET);
            softly.assertThat(payload.getValue().variableType())
                    .isEqualTo(GitLabProjectPipelineSchedulesCreateVariableType.FILE);
            softly.assertThat(result.key()).isEqualTo("K");
        });
    }

    @Test
    void addVariable_shouldSendNoType_whenTypeOmitted() throws IOException {
        when(gitLab.pipelineschedules().createVariable(eq(ID), eq(7L), any())).thenReturn(variable("K", ""));

        sut(schedule(7)).addVariable("K", "");

        final ArgumentCaptor<GitLabProjectPipelineSchedulesCreateVariablePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineSchedulesCreateVariablePayload.class);
        verify(gitLab.pipelineschedules()).createVariable(eq(ID), eq(7L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().value()).isEmpty();
            softly.assertThat(payload.getValue().variableType()).isNull();
        });
    }

    @Test
    void addVariable_shouldThrowBeforeCoreCall_whenArgumentIsBlankOrNull() {
        final PipelineSchedule sut = sut(schedule(7));

        final Throwable blank = catchThrowable(() -> sut.addVariable(" ", "v"));
        final Throwable nullKey = catchThrowable(() -> sut.addVariable(null, "v"));
        final Throwable nullValue = catchThrowable(() -> sut.addVariable("K", null));
        final Throwable nullType = catchThrowable(() -> sut.addVariable("K", "v", null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullKey).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullValue).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullType).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.pipelineschedules());
    }

    @Test
    void variable_shouldReturnVariable_whenPresent() throws IOException {
        when(gitLab.pipelineschedules().getVariable(ID, 7L, "K")).thenReturn(Optional.of(variable("K", "v")));

        assertThat(sut(schedule(7)).variable("K"))
                .get()
                .satisfies(v -> assertThat(v.key()).isEqualTo("K"));
    }

    @Test
    void variable_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.pipelineschedules().getVariable(ID, 7L, "K")).thenReturn(Optional.empty());

        assertThat(sut(schedule(7)).variable("K")).isEmpty();
    }

    @Test
    void variable_shouldThrowBeforeCoreCall_whenKeyIsBlank() {
        final PipelineSchedule sut = sut(schedule(7));

        final Throwable thrown = catchThrowable(() -> sut.variable(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.pipelineschedules());
    }

    @Test
    void toString_shouldNotContainVariableValue_whenScheduleHasVariables() {
        final PipelineSchedule sut = sut(fullSchedule(7));

        assertSoftly(softly -> {
            softly.assertThat(sut.toString()).doesNotContain(SECRET);
            softly.assertThat(sut.variables().get(0).toString()).doesNotContain(SECRET);
            softly.assertThat(sut.response().toString()).doesNotContain(SECRET);
        });
    }
}
