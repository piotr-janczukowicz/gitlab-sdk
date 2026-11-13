package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.API;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.json;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.nextLink;
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
class IssuesIT {

    private static final String ISSUES = API + "/projects/my-group%2Fmy-project/issues";

    @RegisterExtension
    static final WireMockExtension WIRE = WireGitLab.server();

    private final GitLabClient client = WireGitLab.client(WIRE);
    private final Project project = GitLabSdk.of(client).project("my-group/my-project");

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void close_shouldSendPutWithStateEventClose_whenIssueIsOpen() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(ISSUES + "/7"))
                .willReturn(json("{\"iid\":7,\"title\":\"Broken\",\"state\":\"opened\"}")));
        WIRE.stubFor(put(urlPathEqualTo(ISSUES + "/7"))
                .willReturn(json("{\"iid\":7,\"title\":\"Broken\",\"state\":\"closed\"}")));
        final Issue issue = project.issue(7L).orElseThrow();

        final Issue closed = issue.close();

        assertSoftly(softly -> {
            softly.assertThat(closed.state()).isEqualTo(IssueState.CLOSED);
            softly.assertThat(issue.state()).isEqualTo(IssueState.OPENED);
        });
        WIRE.verify(
                exactly(1),
                putRequestedFor(urlPathEqualTo(ISSUES + "/7"))
                        .withRequestBody(matchingJsonPath("$.state_event", equalTo("close"))));
    }

    @Test
    void issue_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(ISSUES + "/404")).willReturn(notFound()));

        final Optional<Issue> issue = project.issue(404L);

        assertThat(issue).isEmpty();
    }

    @Test
    void issues_shouldFollowTheLinkHeaderLazily_whenTheListHasTwoPages() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(ISSUES))
                .withQueryParam("page", equalTo("2"))
                .willReturn(json("[{\"iid\":3,\"title\":\"Three\",\"state\":\"opened\"}]")));
        WIRE.stubFor(get(urlPathEqualTo(ISSUES))
                .atPriority(10)
                .willReturn(json("[{\"iid\":1,\"title\":\"One\",\"state\":\"opened\"},"
                                + "{\"iid\":2,\"title\":\"Two\",\"state\":\"opened\"}]")
                        .withHeader("Link", nextLink(WIRE, ISSUES + "?page=2&per_page=2"))));

        final List<Long> firstTwo = project.issues().limit(2).map(Issue::iid).toList();
        final int requestsForFirstTwo =
                WIRE.findAll(getRequestedFor(urlPathEqualTo(ISSUES))).size();
        final List<Long> all = project.issues().map(Issue::iid).toList();

        assertSoftly(softly -> {
            softly.assertThat(firstTwo).containsExactly(1L, 2L);
            softly.assertThat(requestsForFirstTwo).isOne();
            softly.assertThat(all).containsExactly(1L, 2L, 3L);
        });
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(ISSUES)).withQueryParam("page", equalTo("2")));
    }
}
