package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.Visibility;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Creating, updating and deleting a throwaway project next to the sandbox, and the read-only project relations.
 * Transfer, fork, sharing and deleting the sandbox itself are not run live, because they would change or lose the
 * sandbox. Group creation is not run either: it needs the right to create groups.
 */
@Tag("smoke")
class ManagementSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void project_shouldRoundTrip_whenCreatedInTheNamespaceOfTheSandboxUpdatedAndDeleted() throws IOException {
        final long namespaceId =
                ENV.sandbox().fetch().orElseThrow().response().namespace().id();
        final String name = ENV.name("project");
        final Project created = ENV.sdk()
                .createProject(name)
                .namespaceId(namespaceId)
                .description("created by a smoke test")
                .visibility(Visibility.PRIVATE)
                .send();
        SmokeCleanup.run(
                () -> {
                    final Project updated = created.update()
                            .description("changed by a smoke test")
                            .send();
                    final Optional<Project> reloaded =
                            ENV.sdk().project(created.id()).fetch();
                    final long forks = created.forks().limit(5).count();
                    final long shared = created.sharedGroups().limit(5).count();

                    assertSoftly(softly -> {
                        softly.assertThat(created.response().path()).isEqualTo(name);
                        softly.assertThat(created.response().description()).isEqualTo("created by a smoke test");
                        softly.assertThat(updated.response().description()).isEqualTo("changed by a smoke test");
                        softly.assertThat(reloaded).map(Project::id).contains(created.id());
                        softly.assertThat(forks).isZero();
                        softly.assertThat(shared).isZero();
                    });
                },
                created::delete);
    }

    @Test
    void relations_shouldBeReadable_whenListingForksAndSharedGroupsOfTheSandbox() throws IOException {
        final Project sandbox = ENV.sandbox();

        final long forks = sandbox.forks().limit(5).count();
        final long shared = sandbox.sharedGroups().limit(5).count();

        assertSoftly(softly -> {
            softly.assertThat(forks).isGreaterThanOrEqualTo(0);
            softly.assertThat(shared).isGreaterThanOrEqualTo(0);
        });
    }
}
