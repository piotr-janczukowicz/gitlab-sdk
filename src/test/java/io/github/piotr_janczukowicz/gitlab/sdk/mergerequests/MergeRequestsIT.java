package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

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
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab;
import io.github.piotr_janczukowicz.testsupport.IntegrationTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@IntegrationTest
class MergeRequestsIT {

    private static final String MERGE_REQUESTS = API + "/projects/my-group%2Fmy-project/merge_requests";

    @RegisterExtension
    static final WireMockExtension WIRE = WireGitLab.server();

    private final GitLabClient client = WireGitLab.client(WIRE);
    private final Project project = GitLabSdk.of(client).project("my-group/my-project");

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void mergeRequests_shouldLoadEachListedMergeRequestOnce_andSkipOneThatIsGone() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS)).willReturn(json("[{\"iid\":1},{\"iid\":2},{\"iid\":3}]")));
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS + "/1"))
                .willReturn(json("{\"iid\":1,\"title\":\"One\",\"state\":\"opened\"}")));
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS + "/2")).willReturn(notFound()));
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS + "/3"))
                .willReturn(json("{\"iid\":3,\"title\":\"Three\",\"state\":\"merged\"}")));

        final List<MergeRequest> loaded = project.mergeRequests().toList();

        assertThat(loaded).extracting(MergeRequest::title).containsExactly("One", "Three");
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(MERGE_REQUESTS + "/1")));
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(MERGE_REQUESTS + "/2")));
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(MERGE_REQUESTS + "/3")));
    }

    @Test
    void mergeRequests_shouldLoadOnlyWhatIsConsumed_whenTheStreamIsLimited() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS)).willReturn(json("[{\"iid\":1},{\"iid\":2}]")));
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS + "/1"))
                .willReturn(json("{\"iid\":1,\"title\":\"One\",\"state\":\"opened\"}")));

        final List<MergeRequest> first = project.mergeRequests().limit(1).toList();

        assertThat(first).hasSize(1);
        WIRE.verify(exactly(0), getRequestedFor(urlPathEqualTo(MERGE_REQUESTS + "/2")));
    }

    @Test
    void pipelines_shouldLoadAForkPipelineFromTheFork_whenTheMergeRequestComesFromAFork() throws IOException {
        WIRE.stubFor(
                get(urlPathEqualTo(MERGE_REQUESTS + "/7"))
                        .willReturn(
                                json(
                                        "{\"iid\":7,\"title\":\"Fix\",\"state\":\"opened\",\"project_id\":42,\"target_project_id\":42}")));
        WIRE.stubFor(get(urlPathEqualTo(MERGE_REQUESTS + "/7/pipelines"))
                .willReturn(json("[{\"id\":4,\"project_id\":99},{\"id\":5,\"project_id\":42}]")));
        WIRE.stubFor(get(urlPathEqualTo(API + "/projects/99/pipelines/4"))
                .willReturn(json("{\"id\":4,\"project_id\":99,\"status\":\"success\"}")));
        WIRE.stubFor(get(urlPathEqualTo(API + "/projects/my-group%2Fmy-project/pipelines/5"))
                .willReturn(json("{\"id\":5,\"project_id\":42,\"status\":\"running\"}")));
        final MergeRequest mergeRequest = project.mergeRequest(7L).orElseThrow();

        final List<Pipeline> pipelines = mergeRequest.pipelines().toList();

        assertSoftly(softly -> {
            softly.assertThat(pipelines).extracting(Pipeline::id).containsExactly(4L, 5L);
            softly.assertThat(pipelines.get(0).project().id()).isEqualTo("99");
            softly.assertThat(pipelines.get(1).project()).isSameAs(project);
        });
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(API + "/projects/99/pipelines/4")));
    }
}
