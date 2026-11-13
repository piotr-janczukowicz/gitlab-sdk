package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.API;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.json;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.notFound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab;
import io.github.piotr_janczukowicz.testsupport.IntegrationTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@IntegrationTest
class PipelinesIT {

    private static final String PIPELINES = API + "/projects/my-group%2Fmy-project/pipelines";

    @RegisterExtension
    static final WireMockExtension WIRE = WireGitLab.server();

    private final GitLabClient client = WireGitLab.client(WIRE);
    private final Project project = GitLabSdk.of(client).project("my-group/my-project");

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void pipelines_shouldLoadEachListedPipelineOnce_andSkipOneThatIsGone() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(PIPELINES))
                .willReturn(json("[{\"id\":1,\"status\":\"success\"},{\"id\":2,\"status\":\"failed\"}]")));
        WIRE.stubFor(get(urlPathEqualTo(PIPELINES + "/1"))
                .willReturn(json("{\"id\":1,\"status\":\"success\",\"ref\":\"main\"}")));
        WIRE.stubFor(get(urlPathEqualTo(PIPELINES + "/2")).willReturn(notFound()));

        final List<Pipeline> loaded = project.pipelines().toList();

        assertSoftly(softly -> {
            softly.assertThat(loaded).extracting(Pipeline::id).containsExactly(1L);
            softly.assertThat(loaded.get(0).ref()).contains("main");
            softly.assertThat(loaded.get(0).status()).isEqualTo(PipelineStatus.SUCCESS);
        });
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(PIPELINES + "/1")));
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(PIPELINES + "/2")));
    }

    @Test
    void statusName_shouldReturnTheRawValue_whenGitLabSendsAStatusTheSdkDoesNotKnow() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(PIPELINES + "/3")).willReturn(json("{\"id\":3,\"status\":\"brand_new\"}")));

        final Pipeline pipeline = project.pipeline(3L).orElseThrow();

        assertThat(pipeline.statusName()).isEqualTo("brand_new");
    }

    @Test
    void job_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(API + "/projects/my-group%2Fmy-project/jobs/404"))
                .willReturn(notFound()));

        final Optional<Job> job = project.job(404L);

        assertThat(job).isEmpty();
    }
}
