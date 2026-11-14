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
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabProjectPipelineTriggersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectTriggersTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void pipelineTriggers_shouldWrapEachItem_whenListed() throws IOException {
        when(gitLab.pipelinetriggers().list(ID).stream()).thenReturn(Stream.of(trigger(1), trigger(2)));

        final List<PipelineTrigger> result = project.pipelineTriggers().toList();

        assertThat(result).extracting(PipelineTrigger::id).containsExactly(1L, 2L);
    }

    @Test
    void pipelineTriggers_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.pipelinetriggers().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return trigger(id);
                }));

        final Stream<PipelineTrigger> stream = project.pipelineTriggers();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void pipelineTrigger_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.pipelinetriggers().get(ID, 9L)).thenReturn(Optional.empty());

        assertThat(project.pipelineTrigger(9L)).isEmpty();
    }

    @Test
    void pipelineTrigger_shouldReturnTrigger_whenPresent() throws IOException {
        when(gitLab.pipelinetriggers().get(ID, 9L)).thenReturn(Optional.of(trigger(9)));

        assertThat(project.pipelineTrigger(9L)).get().satisfies(t -> {
            assertThat(t.id()).isEqualTo(9L);
            assertThat(t.project()).isSameAs(project);
        });
    }

    @Test
    void createPipelineTrigger_shouldSendDescriptionOnly_whenNoExpiry() throws IOException {
        when(gitLab.pipelinetriggers().create(eq(ID), any(GitLabProjectPipelineTriggersCreatePayload.class)))
                .thenReturn(trigger(10));

        final PipelineTrigger result = project.createPipelineTrigger("deploy hook");

        final ArgumentCaptor<GitLabProjectPipelineTriggersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineTriggersCreatePayload.class);
        verify(gitLab.pipelinetriggers()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().description()).isEqualTo("deploy hook");
            softly.assertThat(payload.getValue().expiresAt()).isNull();
            softly.assertThat(result.id()).isEqualTo(10L);
        });
    }

    @Test
    void createPipelineTrigger_shouldSendExpiresAt_whenGiven() throws IOException {
        final Instant expiry = Instant.parse("2027-01-01T00:00:00Z");
        when(gitLab.pipelinetriggers().create(eq(ID), any(GitLabProjectPipelineTriggersCreatePayload.class)))
                .thenReturn(trigger(10));

        project.createPipelineTrigger("deploy hook", expiry);

        final ArgumentCaptor<GitLabProjectPipelineTriggersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPipelineTriggersCreatePayload.class);
        verify(gitLab.pipelinetriggers()).create(eq(ID), payload.capture());
        assertThat(payload.getValue().expiresAt()).isEqualTo(expiry);
    }

    @Test
    void createPipelineTrigger_shouldThrowBeforeCoreCall_whenArgumentIsBlankOrNull() {
        final Throwable blank = catchThrowable(() -> project.createPipelineTrigger(" "));
        final Throwable nullDescription = catchThrowable(() -> project.createPipelineTrigger(null));
        final Throwable nullExpiry = catchThrowable(() -> project.createPipelineTrigger("d", null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullDescription).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullExpiry).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
