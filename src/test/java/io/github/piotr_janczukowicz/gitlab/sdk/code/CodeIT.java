package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.API;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.created;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.json;
import static io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab.notFound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab;
import io.github.piotr_janczukowicz.testsupport.IntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@IntegrationTest
class CodeIT {

    private static final String PROJECT = API + "/projects/my-group%2Fmy-project";
    private static final String FILE = PROJECT + "/repository/files/docs%2Fnotes.txt";
    private static final String CONTENT = "Zażółć gęślą jaźń – €";

    @RegisterExtension
    static final WireMockExtension WIRE = WireGitLab.server();

    private final GitLabClient client = WireGitLab.client(WIRE);
    private final Project project = GitLabSdk.of(client).project("my-group/my-project");

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void createFile_shouldSendAMultipartFormWithUtf8Content_andLoadTheFileAtTheBranch() throws IOException {
        WIRE.stubFor(post(urlPathEqualTo(FILE))
                .willReturn(created("{\"file_path\":\"docs/notes.txt\",\"branch\":\"main\"}")));
        final String encoded = Base64.getEncoder().encodeToString(CONTENT.getBytes(StandardCharsets.UTF_8));
        WIRE.stubFor(get(urlPathEqualTo(FILE))
                .withQueryParam("ref", equalTo("main"))
                .willReturn(json("{\"file_name\":\"notes.txt\",\"file_path\":\"docs/notes.txt\",\"ref\":\"main\","
                        + "\"encoding\":\"base64\",\"content\":\"" + encoded + "\"}")));

        final RepositoryFile file = project.createFile("docs/notes.txt", "main", CONTENT, "Add notes");

        final List<LoggedRequest> posts = WIRE.findAll(postRequestedFor(urlPathEqualTo(FILE)));
        assertThat(posts).hasSize(1);
        final LoggedRequest sent = posts.get(0);
        final String body = new String(sent.getBody(), StandardCharsets.UTF_8);
        assertSoftly(softly -> {
            softly.assertThat(sent.getHeader("Content-Type")).startsWith("multipart/form-data");
            softly.assertThat(body).contains("name=\"branch\"", "main");
            softly.assertThat(body).contains("name=\"commit_message\"", "Add notes");
            softly.assertThat(body).contains("name=\"content\"", CONTENT);
            softly.assertThat(file.text()).isEqualTo(CONTENT);
            softly.assertThat(file.ref()).isEqualTo("main");
        });
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(FILE)).withQueryParam("ref", equalTo("main")));
    }

    @Test
    void file_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        WIRE.stubFor(
                get(urlPathEqualTo(PROJECT + "/repository/files/missing.txt")).willReturn(notFound()));

        final Optional<RepositoryFile> file = project.file("missing.txt", "main");

        assertThat(file).isEmpty();
    }

    @Test
    void commits_shouldLoadEachListedCommitOnce_andSkipOneThatIsGone() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(PROJECT + "/repository/commits"))
                .willReturn(json("[{\"id\":\"aaa111\"},{\"id\":\"bbb222\"}]")));
        WIRE.stubFor(get(urlPathEqualTo(PROJECT + "/repository/commits/aaa111"))
                .willReturn(json("{\"id\":\"aaa111\",\"title\":\"First\",\"message\":\"First\\n\"}")));
        WIRE.stubFor(get(urlPathEqualTo(PROJECT + "/repository/commits/bbb222")).willReturn(notFound()));

        final List<Commit> loaded = project.commits().toList();

        assertThat(loaded).extracting(Commit::sha).containsExactly("aaa111");
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(PROJECT + "/repository/commits/aaa111")));
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(PROJECT + "/repository/commits/bbb222")));
    }
}
