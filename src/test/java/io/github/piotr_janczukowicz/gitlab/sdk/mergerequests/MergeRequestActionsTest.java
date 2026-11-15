package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePipelinePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsRebasePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsRebasePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadataBuilder;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MergeRequestActionsTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabMergeRequest response = mergeRequest(IID, "Fix", "opened");
    private final MergeRequest sut = new MergeRequest(project, response);

    @Test
    void cancelAutoMerge_shouldReturnTheMergeRequestAfterTheChange_whenCalled() throws IOException {
        final GitLabMergeRequest cancelled = mergeRequest(IID, "Fix", "opened");
        when(gitLab.mergerequests().project().cancelAutoMerge(ID, IID)).thenReturn(cancelled);

        final MergeRequest result = sut.cancelAutoMerge();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(cancelled);
            softly.assertThat(result.project()).isSameAs(project);
        });
    }

    @Test
    void cancelAutoMerge_shouldPassCoreException_whenGitLabRefuses() throws IOException {
        final GitLabApiException refused = mock(GitLabApiException.class);
        when(gitLab.mergerequests().project().cancelAutoMerge(ID, IID)).thenThrow(refused);

        final Throwable thrown = catchThrowable(sut::cancelAutoMerge);

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void rebase_shouldSendAnEmptyPayload_whenCalledWithoutArguments() throws IOException {
        sut.rebase();

        final ArgumentCaptor<GitLabProjectMergeRequestsRebasePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsRebasePayload.class);
        verify(gitLab.mergerequests().project()).rebase(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().skipCi()).isNull();
    }

    @Test
    void rebase_shouldSendOptions_whenGivenOptions() throws IOException {
        sut.rebase(GitLabProjectMergeRequestsRebasePayloadBuilder.builder()
                .skipCi(true)
                .build());

        final ArgumentCaptor<GitLabProjectMergeRequestsRebasePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsRebasePayload.class);
        verify(gitLab.mergerequests().project()).rebase(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().skipCi()).isTrue();
    }

    @Test
    void rebase_shouldThrowNullPointerException_whenOptionsAreNull() {
        final Throwable thrown = catchThrowable(() -> sut.rebase(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("options");
    }

    @Test
    void rebase_shouldReturnBeforeTheRebaseFinishes_whenGitLabStartsItAsynchronously() throws IOException {
        final GitLabMergeRequest rebasing = GitLabMergeRequestBuilder.builder(response)
                .rebaseInProgress(true)
                .build();
        when(gitLab.mergerequests().project().get(eq(ID), eq(IID), any())).thenReturn(Optional.of(rebasing));

        sut.rebase();

        verify(gitLab.mergerequests().project()).rebase(eq(ID), eq(IID), any());
        verify(gitLab.mergerequests().project(), never()).get(any(), eq(IID), any());
        assertSoftly(softly -> {
            softly.assertThat(sut.isRebaseInProgress()).isFalse();
            softly.assertThatCode(
                            () -> assertThat(sut.refresh().isRebaseInProgress()).isTrue())
                    .doesNotThrowAnyException();
        });
    }

    @Test
    void rebase_shouldPassCoreException_whenGitLabRefuses() throws IOException {
        final GitLabApiException refused = mock(GitLabApiException.class);
        final var api = gitLab.mergerequests().project();
        doThrow(refused).when(api).rebase(eq(ID), eq(IID), any());

        final Throwable thrown = catchThrowable(sut::rebase);

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void isRebaseInProgress_shouldReadTheResponse_whenGitLabReturnsTheFlag() {
        final MergeRequest rebasing = new MergeRequest(
                project,
                GitLabMergeRequestBuilder.builder(response)
                        .rebaseInProgress(true)
                        .build());
        final MergeRequest idle = new MergeRequest(
                project,
                GitLabMergeRequestBuilder.builder(response)
                        .rebaseInProgress(false)
                        .build());

        assertSoftly(softly -> {
            softly.assertThat(rebasing.isRebaseInProgress()).isTrue();
            softly.assertThat(idle.isRebaseInProgress()).isFalse();
            softly.assertThat(sut.isRebaseInProgress()).isFalse();
        });
    }

    @Test
    void createPipeline_shouldLoadTheCreatedPipeline_whenGitLabAnswersWithItsId() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .createPipeline(eq(ID), eq(IID), any(GitLabProjectMergeRequestsCreatePipelinePayload.class)))
                .thenReturn(GitLabCiPipelineBuilder.builder().id(5L).build());
        when(gitLab.pipelines().get(ID, 5L))
                .thenReturn(Optional.of(
                        GitLabCiPipelineWithMetadataBuilder.builder().id(5L).build()));

        final Pipeline result = sut.createPipeline();

        final ArgumentCaptor<GitLabProjectMergeRequestsCreatePipelinePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMergeRequestsCreatePipelinePayload.class);
        verify(gitLab.mergerequests().project()).createPipeline(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(5L);
            softly.assertThat(result.project()).isSameAs(project);
            softly.assertThat(payload.getValue().asyncValue()).isNull();
        });
    }

    @Test
    void createPipeline_shouldLoadItThroughTheProjectItRanIn_whenGitLabNamesAFork() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .createPipeline(eq(ID), eq(IID), any(GitLabProjectMergeRequestsCreatePipelinePayload.class)))
                .thenReturn(
                        GitLabCiPipelineBuilder.builder().id(5L).projectId(99L).build());
        when(gitLab.pipelines().get("99", 5L))
                .thenReturn(Optional.of(GitLabCiPipelineWithMetadataBuilder.builder()
                        .id(5L)
                        .projectId(99L)
                        .build()));

        final Pipeline result = sut.createPipeline();

        assertThat(result.project().id()).isEqualTo("99");
    }

    @Test
    void createPipeline_shouldThrowNullPointerException_whenGitLabAnswersWithoutAnId() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .createPipeline(eq(ID), eq(IID), any(GitLabProjectMergeRequestsCreatePipelinePayload.class)))
                .thenReturn(GitLabCiPipelineBuilder.builder().build());

        final Throwable thrown = catchThrowable(sut::createPipeline);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
    }

    @Test
    void createPipeline_shouldThrowIllegalStateException_whenThePipelineIsGoneAfterCreation() throws IOException {
        when(gitLab.mergerequests()
                        .project()
                        .createPipeline(eq(ID), eq(IID), any(GitLabProjectMergeRequestsCreatePipelinePayload.class)))
                .thenReturn(GitLabCiPipelineBuilder.builder().id(5L).build());
        when(gitLab.pipelines().get(ID, 5L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::createPipeline);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Pipeline 5 is gone");
    }
}
