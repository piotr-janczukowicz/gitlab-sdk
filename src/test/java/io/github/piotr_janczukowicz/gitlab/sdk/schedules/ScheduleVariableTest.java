package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.SECRET;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariableBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreateVariableType;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdateVariablePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.VariableType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ScheduleVariableTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final PipelineSchedule schedule =
            new PipelineSchedule(GitLabSdk.of(gitLab).project(ID), schedule(7));

    private ScheduleVariable sut(final GitLabCiVariable response) {
        return new ScheduleVariable(schedule, response);
    }

    @Test
    void accessors_shouldReadResponse_whenSet() {
        final GitLabCiVariable response = GitLabCiVariableBuilder.builder()
                .key("K")
                .value(SECRET)
                .variableType("file")
                .build();

        final ScheduleVariable sut = sut(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.key()).isEqualTo("K");
            softly.assertThat(sut.value()).contains(SECRET);
            softly.assertThat(sut.type()).isEqualTo(VariableType.FILE);
            softly.assertThat(sut.typeName()).isEqualTo("file");
            softly.assertThat(sut.schedule()).isSameAs(schedule);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void value_shouldBeEmpty_whenAbsent() {
        assertThat(sut(variable("K", null)).value()).isEmpty();
    }

    @Test
    void type_shouldThrowIllegalArgumentException_whenUnknown() {
        final ScheduleVariable sut = sut(
                GitLabCiVariableBuilder.builder().key("K").variableType("weird").build());

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(sut::type)).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.typeName()).isEqualTo("weird");
        });
    }

    @Test
    void variableType_shouldExistInCoreEnums_whenLooped() {
        for (final VariableType type : VariableType.values()) {
            assertSoftly(softly -> {
                softly.assertThat(GitLabProjectPipelineSchedulesCreateVariableType.valueOf(type.name())
                                .wireValue())
                        .isEqualTo(type.value());
                softly.assertThat(GitLabProjectPipelineSchedulesUpdateVariableType.valueOf(type.name())
                                .wireValue())
                        .isEqualTo(type.value());
            });
        }
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() {
        final ScheduleVariableUpdate sut = sut(variable("K", "v")).update();

        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.pipelineschedules());
    }

    @Test
    void update_shouldSendOnlyValueAndKeyAsPathSegment_whenValueSet() throws IOException {
        when(gitLab.pipelineschedules().updateVariable(eq(ID), eq(7L), eq("K"), any()))
                .thenReturn(variable("K", "n"));

        final ScheduleVariable result =
                sut(variable("K", "v")).update().value("n").send();

        final ArgumentCaptor<GitLabProjectPipelineSchedulesUpdateVariablePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineSchedulesUpdateVariablePayload.class);
        verify(gitLab.pipelineschedules()).updateVariable(eq(ID), eq(7L), eq("K"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().value()).isEqualTo("n");
            softly.assertThat(payload.getValue().variableType()).isNull();
            softly.assertThat(result.value()).contains("n");
        });
    }

    @Test
    void update_shouldSendTypeAndEmptyValue_whenBothSet() throws IOException {
        when(gitLab.pipelineschedules().updateVariable(eq(ID), eq(7L), eq("K"), any()))
                .thenReturn(variable("K", ""));

        sut(variable("K", "v")).update().value("").type(VariableType.FILE).send();

        final ArgumentCaptor<GitLabProjectPipelineSchedulesUpdateVariablePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineSchedulesUpdateVariablePayload.class);
        verify(gitLab.pipelineschedules()).updateVariable(eq(ID), eq(7L), eq("K"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().value()).isEmpty();
            softly.assertThat(payload.getValue().variableType())
                    .isEqualTo(GitLabProjectPipelineSchedulesUpdateVariableType.FILE);
        });
    }

    @Test
    void update_shouldThrowNullPointerException_whenArgumentIsNull() {
        final ScheduleVariableUpdate sut = sut(variable("K", "v")).update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.value(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.type(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new ScheduleVariableUpdate(null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void delete_shouldCallCoreWithKey_whenCalled() throws IOException {
        sut(variable("K", "v")).delete();

        verify(gitLab.pipelineschedules()).deleteVariable(ID, 7L, "K");
    }

    @Test
    void refresh_shouldReloadByKey_whenPresent() throws IOException {
        when(gitLab.pipelineschedules().getVariable(ID, 7L, "K")).thenReturn(Optional.of(variable("K", "new")));

        final ScheduleVariable result = sut(variable("K", "v")).refresh();

        assertThat(result.value()).contains("new");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.pipelineschedules().getVariable(ID, 7L, "K")).thenReturn(Optional.empty());
        final ScheduleVariable sut = sut(variable("K", "v"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("K");
    }

    @Test
    void toString_shouldNotContainValue_whenPrinted() {
        final ScheduleVariable sut = sut(variable("K", SECRET));

        assertSoftly(softly -> {
            softly.assertThat(sut.toString()).doesNotContain(SECRET);
            softly.assertThat(sut.response().toString()).doesNotContain(SECRET);
        });
    }
}
