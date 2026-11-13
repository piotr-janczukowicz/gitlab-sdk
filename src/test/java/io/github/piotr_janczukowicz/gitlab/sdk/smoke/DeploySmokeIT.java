package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.CreatedDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployKey;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployTokenScope;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Deploy keys and deploy tokens of the sandbox. The secret of a token is checked but never printed. Enabling a key of
 * another project is not run live.
 */
@Tag("smoke")
class DeploySmokeIT {

    /**
     * A public key made only for these tests; its private half was discarded. GitLab accepts a key once, so a failed
     * cleanup blocks the next run until the key is deleted by hand.
     */
    private static final String TEST_ONLY_PUBLIC_KEY =
            "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIC6LAWROmVNP/elnEPGoN4fZ7DRbCB7g+cctdEfekjCw gitlab-sdk-smoke-test-only";

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void deployKey_shouldRoundTrip_whenAddedUpdatedListedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String title = "smoke-" + ENV.runId();
        final DeployKey created = project.addDeployKey(title, TEST_ONLY_PUBLIC_KEY);
        SmokeCleanup.run(
                () -> {
                    final DeployKey updated = created.update()
                            .title(ENV.name("key"))
                            .canPush(true)
                            .send();
                    final Optional<DeployKey> reloaded = project.deployKey(created.id());
                    final boolean listed = project.deployKeys().limit(100).anyMatch(key -> key.id() == created.id());
                    updated.delete();
                    final Optional<DeployKey> afterDelete = project.deployKey(created.id());

                    assertSoftly(softly -> {
                        softly.assertThat(created.title()).isEqualTo(title);
                        softly.assertThat(updated.title()).isEqualTo(ENV.name("key"));
                        softly.assertThat(updated.canPush()).isTrue();
                        softly.assertThat(reloaded).map(DeployKey::id).contains(created.id());
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<DeployKey> key = project.deployKey(created.id());
                    if (key.isPresent()) {
                        key.get().delete();
                    }
                });
    }

    @Test
    void deployToken_shouldRoundTrip_whenCreatedListedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final CreatedDeployToken created =
                project.createDeployToken(ENV.name("token"), DeployTokenScope.READ_REPOSITORY);
        final DeployToken token = created.deployToken();
        SmokeCleanup.run(
                () -> {
                    final Optional<DeployToken> reloaded = project.deployToken(token.id());
                    final boolean listed = project.deployTokens().limit(100).anyMatch(t -> t.id() == token.id());
                    final String secret = created.token();
                    final String printed = created.toString();
                    token.delete();
                    final Optional<DeployToken> afterDelete = project.deployToken(token.id());

                    assertSoftly(softly -> {
                        softly.assertThat(token.name()).isEqualTo(ENV.name("token"));
                        softly.assertThat(token.scopes()).containsExactly(DeployTokenScope.READ_REPOSITORY);
                        softly.assertThat(secret).isNotBlank();
                        softly.assertThat(printed).doesNotContain(secret);
                        softly.assertThat(reloaded).map(DeployToken::id).contains(token.id());
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<DeployToken> remaining = project.deployToken(token.id());
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }

    @Test
    void deployKey_shouldReturnEmpty_whenKeyDoesNotExist() throws IOException {
        final Optional<DeployKey> missing = ENV.sandbox().deployKey(Long.MAX_VALUE);

        assertThat(missing).isEmpty();
    }
}
