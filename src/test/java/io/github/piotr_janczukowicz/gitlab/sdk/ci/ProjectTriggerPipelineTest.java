package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.createdPipeline;
import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.pipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadata;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsPipelineRefTriggerPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectTriggerPipelineTest {

    private static final String ID = "my-group/my-project";
    private static final String TOKEN = "glptt-secret-trigger-token";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void triggerPipeline_shouldSendTokenAndVariablesForTheRefAndLoadPipeline_whenGiven() throws IOException {
        when(gitLab.projects().project().pipelineRefTrigger(anyString(), anyString(), any()))
                .thenReturn(createdPipeline(9L));
        final GitLabCiPipelineWithMetadata loaded = pipeline(9L, "created");
        when(gitLab.pipelines().get(ID, 9L)).thenReturn(Optional.of(loaded));

        final Pipeline triggered = sut.triggerPipeline(TOKEN, "release/1.x", Map.of("DEPLOY", "yes"));

        final ArgumentCaptor<GitLabProjectProjectsPipelineRefTriggerPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsPipelineRefTriggerPayload.class);
        verify(gitLab.projects().project()).pipelineRefTrigger(eq(ID), eq("release/1.x"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().token()).isEqualTo(TOKEN);
            softly.assertThat(payload.getValue().variables()).containsExactly(Map.entry("DEPLOY", "yes"));
            softly.assertThat(triggered.response()).isSameAs(loaded);
        });
    }

    @Test
    void triggerPipeline_shouldSendNoVariables_whenOverloadWithoutThem() throws IOException {
        when(gitLab.projects().project().pipelineRefTrigger(anyString(), anyString(), any()))
                .thenReturn(createdPipeline(9L));
        when(gitLab.pipelines().get(ID, 9L)).thenReturn(Optional.of(pipeline(9L, "created")));

        sut.triggerPipeline(TOKEN, "main");

        final ArgumentCaptor<GitLabProjectProjectsPipelineRefTriggerPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsPipelineRefTriggerPayload.class);
        verify(gitLab.projects().project()).pipelineRefTrigger(eq(ID), eq("main"), payload.capture());
        assertThat(payload.getValue().variables()).isNull();
    }

    @Test
    void triggerPipeline_shouldThrowIllegalArgumentExceptionWithoutRequest_whenTokenIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.triggerPipeline(" ", "main", Map.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.projects().project(), never()).pipelineRefTrigger(anyString(), anyString(), any());
    }

    @Test
    void triggerPipeline_shouldThrowIllegalArgumentExceptionWithoutRequest_whenRefIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.triggerPipeline(TOKEN, " ", Map.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining(TOKEN);
        verify(gitLab.projects().project(), never()).pipelineRefTrigger(anyString(), anyString(), any());
    }

    @Test
    void triggerPipeline_shouldThrowNullPointerExceptionWithoutRequest_whenVariablesAreNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.triggerPipeline(TOKEN, "main", null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.projects().project(), never()).pipelineRefTrigger(anyString(), anyString(), any());
    }

    @Test
    void triggerPipeline_shouldNotRevealTheToken_whenThePipelineIsGoneAfterTheTrigger() throws IOException {
        when(gitLab.projects().project().pipelineRefTrigger(anyString(), anyString(), any()))
                .thenReturn(createdPipeline(9L));
        when(gitLab.pipelines().get(ID, 9L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.triggerPipeline(TOKEN, "main", Map.of()));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pipeline 9 is gone")
                .hasMessageNotContaining(TOKEN);
    }
}
