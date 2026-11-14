package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.listed;
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
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectSchedulesTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void pipelineSchedules_shouldLoadEachItem_whenListReturnsSmallerRecord() throws IOException {
        when(gitLab.pipelineschedules().list(ID).stream()).thenReturn(Stream.of(listed(1), listed(2)));
        when(gitLab.pipelineschedules().get(ID, 1L)).thenReturn(Optional.of(schedule(1)));
        when(gitLab.pipelineschedules().get(ID, 2L)).thenReturn(Optional.of(schedule(2)));

        final List<PipelineSchedule> result = project.pipelineSchedules().toList();

        assertThat(result).extracting(PipelineSchedule::id).containsExactly(1L, 2L);
    }

    @Test
    void pipelineSchedules_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.pipelineschedules().list(ID).stream()).thenReturn(Stream.of(listed(1), listed(2), listed(3)));
        when(gitLab.pipelineschedules().get(ID, 1L)).thenReturn(Optional.of(schedule(1)));
        when(gitLab.pipelineschedules().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.pipelineschedules().get(ID, 3L)).thenReturn(Optional.of(schedule(3)));

        final List<PipelineSchedule> result = project.pipelineSchedules().toList();

        assertThat(result).extracting(PipelineSchedule::id).containsExactly(1L, 3L);
    }

    @Test
    void pipelineSchedules_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.pipelineschedules().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return listed(id);
                }));
        when(gitLab.pipelineschedules().get(eq(ID), any(Long.class))).thenReturn(Optional.of(schedule(1)));

        final Stream<PipelineSchedule> stream = project.pipelineSchedules();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void pipelineSchedule_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.pipelineschedules().get(ID, 9L)).thenReturn(Optional.empty());

        assertThat(project.pipelineSchedule(9L)).isEmpty();
    }

    @Test
    void pipelineSchedule_shouldReturnSchedule_whenPresent() throws IOException {
        when(gitLab.pipelineschedules().get(ID, 9L)).thenReturn(Optional.of(schedule(9)));

        assertThat(project.pipelineSchedule(9L)).get().satisfies(s -> {
            assertThat(s.id()).isEqualTo(9L);
            assertThat(s.project()).isSameAs(project);
        });
    }

    @Test
    void createPipelineSchedule_shouldSendDescriptionRefAndCron_whenGiven() throws IOException {
        when(gitLab.pipelineschedules().create(eq(ID), any(GitLabProjectPipelineSchedulesCreatePayload.class)))
                .thenReturn(schedule(10));

        final PipelineSchedule result = project.createPipelineSchedule("nightly", "main", "0 1 * * *");

        final ArgumentCaptor<GitLabProjectPipelineSchedulesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineSchedulesCreatePayload.class);
        verify(gitLab.pipelineschedules()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("nightly");
            softly.assertThat(payload.getValue().ref()).isEqualTo("main");
            softly.assertThat(payload.getValue().cron()).isEqualTo("0 1 * * *");
            softly.assertThat(payload.getValue().cronTimezone()).isNull();
            softly.assertThat(payload.getValue().active()).isNull();
            softly.assertThat(result.id()).isEqualTo(10L);
        });
    }

    @Test
    void createPipelineSchedule_shouldThrowBeforeCoreCall_whenAnArgumentIsBlankOrNull() {
        final Throwable description = catchThrowable(() -> project.createPipelineSchedule(" ", "main", "0 1 * * *"));
        final Throwable ref = catchThrowable(() -> project.createPipelineSchedule("n", "", "0 1 * * *"));
        final Throwable cron = catchThrowable(() -> project.createPipelineSchedule("n", "main", " "));
        final Throwable nullRef = catchThrowable(() -> project.createPipelineSchedule("n", null, "0 1 * * *"));

        assertSoftly(softly -> {
            softly.assertThat(description).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(ref).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(cron).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullRef).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void createPipelineSchedule_shouldSendPayload_whenCorePayloadGiven() throws IOException {
        final GitLabProjectPipelineSchedulesCreatePayload payload =
                GitLabProjectPipelineSchedulesCreatePayloadBuilder.builder()
                        .description("d")
                        .ref("main")
                        .cron("* * * * *")
                        .cronTimezone("UTC")
                        .build();
        when(gitLab.pipelineschedules().create(ID, payload)).thenReturn(schedule(11));

        final PipelineSchedule result = project.createPipelineSchedule(payload);

        assertThat(result.id()).isEqualTo(11L);
    }

    @Test
    void createPipelineSchedule_shouldThrowNullPointerException_whenCorePayloadIsNull() {
        final GitLabProjectPipelineSchedulesCreatePayload payload = null;

        final Throwable thrown = catchThrowable(() -> project.createPipelineSchedule(payload));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }
}
