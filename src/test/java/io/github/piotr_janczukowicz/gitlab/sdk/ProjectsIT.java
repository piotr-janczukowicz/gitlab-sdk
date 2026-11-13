package io.github.piotr_janczukowicz.gitlab.sdk;

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
import io.github.piotr_janczukowicz.gitlab.sdk.wire.WireGitLab;
import io.github.piotr_janczukowicz.testsupport.IntegrationTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@IntegrationTest
class ProjectsIT {

    @RegisterExtension
    static final WireMockExtension WIRE = WireGitLab.server();

    private final GitLabClient client = WireGitLab.client(WIRE);
    private final GitLabSdk sdk = GitLabSdk.of(client);

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void fetch_shouldSendTheSlashOfAPathIdAsPercent2F_whenProjectIsAddressedByPath() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(API + "/projects/my-group%2Fmy-project"))
                .willReturn(json("{\"id\":42,\"path\":\"my-project\"}")));

        final Optional<Project> loaded = sdk.project("my-group/my-project").fetch();

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(loaded.orElseThrow().id()).isEqualTo("my-group/my-project");
            softly.assertThat(loaded.orElseThrow().response().id()).isEqualTo(42L);
        });
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(API + "/projects/my-group%2Fmy-project")));
    }

    @Test
    void fetch_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(API + "/projects/my-group%2Fmissing")).willReturn(notFound()));

        final Optional<Project> loaded = sdk.project("my-group/missing").fetch();

        assertThat(loaded).isEmpty();
    }

    @Test
    void user_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(API + "/users/999")).willReturn(notFound()));

        final Optional<User> user = sdk.user(999L);

        assertThat(user).isEmpty();
    }

    @Test
    void profile_shouldLoadTheSignedInAccount_whenCalled() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(API + "/user")).willReturn(json("{\"id\":5,\"username\":\"tanuki\"}")));
        WIRE.stubFor(get(urlPathEqualTo(API + "/users/5")).willReturn(json("{\"id\":5,\"username\":\"tanuki\"}")));

        final User me = sdk.me().profile();

        assertThat(me.username()).isEqualTo("tanuki");
    }

    @Test
    void subgroups_shouldListDirectChildrenAndLoadEach_whenGroupHasSubgroups() throws IOException {
        WIRE.stubFor(get(urlPathEqualTo(API + "/groups/parent%2Fgroup/subgroups"))
                .willReturn(json("[{\"id\":8},{\"id\":9}]")));
        WIRE.stubFor(get(urlPathEqualTo(API + "/groups/8")).willReturn(json("{\"id\":8,\"path\":\"child\"}")));
        WIRE.stubFor(get(urlPathEqualTo(API + "/groups/9")).willReturn(notFound()));

        final List<Group> groups = sdk.group("parent/group").subgroups().toList();

        assertThat(groups).extracting(Group::id).containsExactly("8");
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(API + "/groups/8")));
        WIRE.verify(exactly(1), getRequestedFor(urlPathEqualTo(API + "/groups/9")));
    }
}
