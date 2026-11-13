package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static java.util.Objects.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.client.GitLabAuthenticationProviders;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClientConfiguration;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClients;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;

/** Reads the smoke variables and builds the SDK of a live GitLab instance, once per run. */
final class SmokeEnvironment {

    static final String TOKEN_VARIABLE = "SMOKE_GITLAB_TOKEN";
    static final String PROJECT_VARIABLE = "SMOKE_GITLAB_PROJECT";
    static final String URL_VARIABLE = "SMOKE_GITLAB_URL";
    static final String USER_ID_VARIABLE = "SMOKE_GITLAB_USER_ID";

    private static final String DEFAULT_URL = "https://gitlab.com";
    private static final String USER_AGENT = "gitlab-sdk-smoke/1.0";
    private static final String RUN_ID = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
    private static final SmokeEnvironment FROM_ENV = of(System.getenv());

    private final String token;
    private final String project;
    private final String url;
    private final String userId;
    private GitLabClient client;
    private GitLabSdk sdk;

    private SmokeEnvironment(final String token, final String project, final String url, final String userId) {
        this.token = token;
        this.project = project;
        this.url = url;
        this.userId = userId;
    }

    static SmokeEnvironment fromEnv() {
        return FROM_ENV;
    }

    static SmokeEnvironment of(final Map<String, String> env) {
        requireNonNull(env, "env");
        final String project = env.getOrDefault(PROJECT_VARIABLE, "").strip();
        final String url = env.getOrDefault(URL_VARIABLE, DEFAULT_URL).strip();
        final String userId = env.getOrDefault(USER_ID_VARIABLE, "").strip();
        return new SmokeEnvironment(env.getOrDefault(TOKEN_VARIABLE, "").strip(), project, url, userId);
    }

    boolean configured() {
        return !token.isBlank() && !project.isBlank();
    }

    String project() {
        return project;
    }

    /**
     * The namespace part of the sandbox path ({@code group} of {@code group/name}), which is a group or a user.
     *
     * @return the path before the last slash, or empty when the sandbox is not a path
     */
    Optional<String> namespace() {
        final int slash = project.lastIndexOf('/');
        return slash > 0 ? Optional.of(project.substring(0, slash)) : Optional.empty();
    }

    /**
     * The id of a second user who may be added to and removed from the sandbox, from the optional
     * {@value #USER_ID_VARIABLE}.
     *
     * @return the user id, or empty when the variable is not set
     * @throws IllegalStateException if the value is not a positive number
     */
    Optional<Long> secondUserId() {
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        try {
            final long id = Long.parseLong(userId);
            if (id > 0) {
                return Optional.of(id);
            }
        } catch (final NumberFormatException ignored) {
            // reported below with the variable name
        }
        throw new IllegalStateException(USER_ID_VARIABLE + " must be the numeric id of a user, but was: " + userId);
    }

    String runId() {
        return RUN_ID;
    }

    String name(final String suffix) {
        return "smoke-" + RUN_ID + "-" + suffix;
    }

    /**
     * Skips the calling test class when no token or sandbox is set; refuses a sandbox that does not look like one, and
     * a {@value #URL_VARIABLE} that is not an absolute {@code http} or {@code https} URI.
     */
    void requireConfigured() {
        Assumptions.assumeTrue(configured(), TOKEN_VARIABLE + " and " + PROJECT_VARIABLE + " are not set");
        requireSandbox(project);
        baseUri();
    }

    /**
     * The root of the instance, read from {@value #URL_VARIABLE} only when it is needed, so a malformed value fails the
     * smoke run with a clear message instead of failing the class initialization of every test that reads the
     * variables.
     *
     * @return the base URI
     * @throws IllegalStateException if the value is not an absolute {@code http} or {@code https} URI
     */
    URI baseUri() {
        final URI parsed;
        try {
            parsed = new URI(url);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(URL_VARIABLE + " is not a valid URI: " + url, e);
        }
        final String scheme = parsed.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme) || parsed.getHost() == null) {
            throw new IllegalStateException(URL_VARIABLE + " must be an absolute http or https URI, but was: " + url);
        }
        return parsed;
    }

    static void requireSandbox(final String project) {
        if (!project.contains("/") || !project.toLowerCase(Locale.ROOT).contains("smoke")) {
            throw new IllegalStateException(PROJECT_VARIABLE
                    + " must be the path of a sandbox project (group/name) whose path contains \"smoke\", but was: "
                    + project);
        }
    }

    synchronized GitLabSdk sdk() {
        if (sdk == null) {
            client = GitLabClients.create(
                    GitLabAuthenticationProviders.personalToken(token),
                    USER_AGENT,
                    GitLabClientConfiguration.builder().baseUri(baseUri()).build());
            sdk = GitLabSdk.of(client);
        }
        return sdk;
    }

    /**
     * The sandbox as an SDK handle. The path goes to the core as it is; the core encodes it.
     *
     * @return the handle
     */
    Project sandbox() {
        return sdk().project(project);
    }

    @Override
    public String toString() {
        return "SmokeEnvironment[" + url + " " + project + ", run " + RUN_ID + "]";
    }
}
