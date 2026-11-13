package io.github.piotr_janczukowicz.gitlab.sdk.wire;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClientConfiguration;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClients;
import io.github.piotr_janczukowicz.http.client.authentication.AuthenticationProviders;
import java.net.URI;

/**
 * Starts WireMock for the {@code *IT} integration tests and builds a real {@link GitLabClient} against it, so the SDK
 * is driven through the core and its transport, and its requests are checked on the wire.
 */
public final class WireGitLab {

    /** The prefix the core puts before every resource path. */
    public static final String API = "/api/v4";

    private WireGitLab() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * A WireMock server on a free port, started before and stopped after the test class.
     *
     * @return the extension to register
     */
    public static WireMockExtension server() {
        return WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();
    }

    /**
     * A client whose base URI is the root of {@code server}; close it when the test is done.
     *
     * @param server the running server
     * @return the client
     */
    public static GitLabClient client(final WireMockExtension server) {
        final GitLabClientConfiguration configuration = GitLabClientConfiguration.builder()
                .baseUri(URI.create(server.baseUrl()))
                .build();
        return GitLabClients.create(AuthenticationProviders.bearer("token"), "gitlab-sdk-it/1.0", configuration);
    }

    /**
     * A {@code 200} answer with a JSON body.
     *
     * @param body the JSON text
     * @return the answer
     */
    public static ResponseDefinitionBuilder json(final String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    /**
     * A {@code 201} answer with a JSON body.
     *
     * @param body the JSON text
     * @return the answer
     */
    public static ResponseDefinitionBuilder created(final String body) {
        return json(body).withStatus(201);
    }

    /**
     * A {@code 404} answer as GitLab sends it.
     *
     * @return the answer
     */
    public static ResponseDefinitionBuilder notFound() {
        return aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"404 Not Found\"}");
    }

    /**
     * A {@code Link} header value that names the next page on {@code server}.
     *
     * @param server the running server
     * @param pathAndQuery the path, with {@link #API}, and the query of the next page
     * @return the header value
     */
    public static String nextLink(final WireMockExtension server, final String pathAndQuery) {
        return "<" + server.baseUrl() + pathAndQuery + ">; rel=\"next\"";
    }
}
