package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Me;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.User;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class ProjectsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void fetch_shouldReturnTheSandbox_whenTokenHasAccess() throws IOException {
        final Project handle = ENV.sandbox();

        final Optional<Project> loaded = handle.fetch();
        final Project refreshed = loaded.orElseThrow().refresh();

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(refreshed.response().pathWithNamespace()).isEqualTo(ENV.project());
            softly.assertThat(refreshed.response().defaultBranch()).isNotBlank();
        });
    }

    @Test
    void fetch_shouldReturnEmpty_whenProjectDoesNotExist() throws IOException {
        final Optional<Project> missing = ENV.sdk().project(ENV.name("missing")).fetch();

        assertThat(missing).isEmpty();
    }

    @Test
    void group_shouldLoadNamespaceAndListProjects_whenSandboxLivesInAGroup() throws IOException {
        final String namespace = ENV.namespace().orElseThrow();
        final Optional<Group> group = ENV.sdk().group(namespace).fetch();
        assumeTrue(group.isPresent(), namespace + " is a user namespace, not a group");
        final Group loaded = group.orElseThrow();

        final boolean listed = loaded.projects()
                .limit(50)
                .anyMatch(project -> ENV.project().equals(project.response().pathWithNamespace()));
        final long subgroups = loaded.subgroups().limit(5).count();
        final String fullPath = loaded.refresh().response().fullPath();

        assertSoftly(softly -> {
            softly.assertThat(fullPath).isEqualTo(namespace);
            softly.assertThat(listed).isTrue();
            softly.assertThat(subgroups).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void me_shouldReturnTheSignedInUser_andUserLookupFindsThem() throws IOException {
        final Me me = ENV.sdk().me();

        final User current = me.profile();
        final Optional<User> byId = ENV.sdk().user(current.id());
        final Optional<User> missing = ENV.sdk().user(Long.MAX_VALUE);
        final long refreshedId = current.refresh().id();

        assertSoftly(softly -> {
            softly.assertThat(current.username()).isNotBlank();
            softly.assertThat(byId).map(User::username).contains(current.username());
            softly.assertThat(refreshedId).isEqualTo(current.id());
            softly.assertThat(missing).isEmpty();
        });
    }
}
