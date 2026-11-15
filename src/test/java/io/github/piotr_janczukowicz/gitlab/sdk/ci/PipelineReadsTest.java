package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.pipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariableBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiBridge;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiBridgeBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabProjectPipelinesApi;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabTestReportSummaryEntity;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabTestReportSummaryEntityBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class PipelineReadsTest {

    private static final String ID = "my-group/my-project";
    private static final long PIPELINE_ID = 5L;
    private static final String SECRET = "s3cret-value";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Pipeline sut = new Pipeline(project, pipeline(PIPELINE_ID, "success"));

    @Test
    void variables_shouldReturnTheVariablesOfThisPipeline_whenGitLabListsThem() throws IOException {
        when(gitLab.pipelines().listVariables(ID, PIPELINE_ID))
                .thenReturn(List.of(GitLabCiVariableBuilder.builder()
                        .key("DEPLOY_ENV")
                        .value(SECRET)
                        .build()));

        final List<GitLabCiVariable> result = sut.variables().toList();

        assertThat(result).extracting(GitLabCiVariable::key).containsExactly("DEPLOY_ENV");
    }

    @Test
    void variables_shouldNotPrintTheValue_whenARecordIsConvertedToText() throws IOException {
        when(gitLab.pipelines().listVariables(ID, PIPELINE_ID))
                .thenReturn(List.of(GitLabCiVariableBuilder.builder()
                        .key("TOKEN")
                        .value(SECRET)
                        .build()));

        final String text = sut.variables().toList().get(0).toString();

        assertSoftly(softly -> {
            softly.assertThat(text).doesNotContain(SECRET);
            softly.assertThat(text).contains("TOKEN");
        });
    }

    @Test
    void variables_shouldPassTheCoreFailure_whenTheRequestFails() throws IOException {
        final GitLabProjectPipelinesApi api = gitLab.pipelines();
        doThrow(new IOException("boom")).when(api).listVariables(ID, PIPELINE_ID);

        final Throwable thrown = catchThrowable(sut::variables);

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void triggerJobs_shouldBeLazy_whenOnlyOneJobIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.pipelines().listTriggerJobs(ID, PIPELINE_ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return bridge(name);
                }));

        final Stream<GitLabCiBridge> stream = sut.triggerJobs();

        assertSoftly(softly -> {
            softly.assertThat(consumed).hasValue(0);
            softly.assertThat(stream.limit(1).toList())
                    .extracting(GitLabCiBridge::name)
                    .containsExactly("a");
            softly.assertThat(consumed).hasValue(1);
        });
    }

    @Test
    void testReportSummary_shouldReturnTheSummary_whenGitLabHasOne() throws IOException {
        final GitLabTestReportSummaryEntity summary = GitLabTestReportSummaryEntityBuilder.builder()
                .testSuites(List.of())
                .build();
        when(gitLab.pipelines().getTestReportSummary(ID, PIPELINE_ID)).thenReturn(Optional.of(summary));

        final Optional<GitLabTestReportSummaryEntity> result = sut.testReportSummary();

        assertThat(result).containsSame(summary);
    }

    @Test
    void testReportSummary_shouldReturnEmpty_whenCoreReturnsEmpty() throws IOException {
        when(gitLab.pipelines().getTestReportSummary(ID, PIPELINE_ID)).thenReturn(Optional.empty());

        final Optional<GitLabTestReportSummaryEntity> result = sut.testReportSummary();

        assertThat(result).isEmpty();
    }

    private static GitLabCiBridge bridge(final String name) {
        return GitLabCiBridgeBuilder.builder().name(name).build();
    }
}
