package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.Deployment;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.DeploymentStatus;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.Environment;
import io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentTier;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A throwaway environment {@code smoke-<runId>-...} with two deployments of the newest commit of the default branch.
 * GitLab may refuse to delete the latest deployment of an environment, so the older one is deleted and the newer one
 * goes with the environment. A stopped environment may stay {@code stopping} for a while, so the test waits up to
 * {@value #STOP_WAIT_SECONDS} seconds for {@code stopped} before it deletes. The deployment test is skipped when the
 * sandbox has no default branch or no commit, and both tests are skipped when the first environment call answers 403 or
 * 404 (see {@link SmokeSkips}). Approving and rejecting a deployment (Premium) are not run live.
 */
@Tag("smoke")
class EnvironmentsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final int STOP_WAIT_SECONDS = 60;
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final String STOPPED = "stopped";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void environment_shouldRoundTrip_whenCreatedUpdatedClearedStoppedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String name = ENV.name("env");
        SmokeSkips.probe(
                "the environments of the sandbox",
                () -> project.environments().limit(1).toList());
        final Environment created = project.createEnvironment(name);
        SmokeCleanup.run(
                () -> {
                    final Environment updated = created.update()
                            .tier(EnvironmentTier.STAGING)
                            .description("smoke")
                            .kubernetesNamespace("smoke-ns")
                            .send();
                    final Environment cleared =
                            updated.update().clearKubernetesNamespace().send();
                    final Environment reloaded = cleared.refresh();
                    final boolean listed =
                            project.environments().limit(100).anyMatch(each -> each.id() == created.id());
                    final Environment stopped = reloaded.stop();
                    final Environment stoppedLoaded = awaitStopped(stopped);
                    final boolean isStopped = STOPPED.equals(stoppedLoaded.state());
                    final Optional<Environment> afterDelete =
                            isStopped ? deleteAndReload(project, stoppedLoaded) : Optional.empty();

                    assertSoftly(softly -> {
                        softly.assertThat(created.name()).isEqualTo(name);
                        softly.assertThat(updated.tier()).contains(EnvironmentTier.STAGING);
                        softly.assertThat(updated.description()).contains("smoke");
                        softly.assertThat(updated.kubernetesNamespace()).contains("smoke-ns");
                        softly.assertThat(cleared.kubernetesNamespace()).isEmpty();
                        softly.assertThat(reloaded.kubernetesNamespace()).isEmpty();
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(stoppedLoaded.state())
                                .as(
                                        "the environment did not reach %s within %d s, so delete() was not run; the"
                                                + " cleanup tries again",
                                        STOPPED, STOP_WAIT_SECONDS)
                                .isEqualTo(STOPPED);
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> removeEnvironment(project, created.id()));
    }

    @Test
    void deployment_shouldRoundTrip_whenCreatedUpdatedListedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        assumeTrue(defaultBranch != null, "the sandbox has no default branch (empty repository)");
        SmokeSkips.probe(
                "the environments of the sandbox",
                () -> project.environments().limit(1).toList());
        final Optional<Commit> newest = project.commits().findFirst();
        assumeTrue(newest.isPresent(), "the sandbox has no commit");
        final String sha = newest.get().sha();
        final String name = ENV.name("deploy-env");
        final Environment environment = project.createEnvironment(name);
        SmokeCleanup.run(
                () -> {
                    final Deployment first =
                            project.createDeployment(name, sha, defaultBranch, DeploymentStatus.RUNNING);
                    final Deployment succeeded = first.updateStatus(DeploymentStatus.SUCCESS);
                    final Deployment second =
                            project.createDeployment(name, sha, defaultBranch, DeploymentStatus.SUCCESS);
                    final List<Deployment> listed =
                            environment.deployments().limit(10).toList();
                    final Optional<Deployment> loaded = project.deployment(first.id());
                    first.delete();
                    final Optional<Deployment> afterDelete = project.deployment(first.id());

                    assertSoftly(softly -> {
                        softly.assertThat(first.status()).isEqualTo(DeploymentStatus.RUNNING);
                        softly.assertThat(succeeded.status()).isEqualTo(DeploymentStatus.SUCCESS);
                        softly.assertThat(second.sha()).isEqualTo(sha);
                        softly.assertThat(listed).extracting(Deployment::id).contains(first.id(), second.id());
                        softly.assertThat(loaded).map(Deployment::id).contains(first.id());
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> removeEnvironment(project, environment.id()));
    }

    private static void removeEnvironment(final Project project, final long id) throws IOException {
        final Optional<Environment> remaining = project.environment(id);
        if (remaining.isPresent()) {
            final Environment current = remaining.get();
            final Environment stopped = STOPPED.equals(current.state()) ? current : awaitStopped(current.stop());
            stopped.delete();
        }
    }

    private static Optional<Environment> deleteAndReload(final Project project, final Environment environment)
            throws IOException {
        environment.delete();
        return project.environment(environment.id());
    }

    private static Environment awaitStopped(final Environment stopping) throws IOException {
        final Instant deadline = Instant.now().plusSeconds(STOP_WAIT_SECONDS);
        Environment current = stopping.refresh();
        while (!STOPPED.equals(current.state()) && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(POLL_INTERVAL);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return current;
            }
            current = current.refresh();
        }
        return current;
    }
}
