package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.Webhook;
import io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhookEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Webhooks of the sandbox, pointed at a path under {@code example.com}: GitLab validates hook URLs and may refuse a
 * host that does not resolve, such as an {@code .invalid} one (not yet verified live). {@code Webhook.test(...)} is not
 * run live, because it makes GitLab send a real HTTP request to the hook URL.
 */
@Tag("smoke")
class WebhooksSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void webhook_shouldRoundTrip_whenCreatedUpdatedListedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String url = "https://example.com/smoke-" + ENV.runId();
        final Webhook created = project.createWebhook(url);
        SmokeCleanup.run(
                () -> {
                    final Webhook updated = created.update()
                            .name(ENV.name("hook"))
                            .events(List.of(WebhookEvent.PIPELINE))
                            .send();
                    final Optional<Webhook> reloaded = project.webhook(created.id());
                    final boolean listed = project.webhooks().limit(100).anyMatch(hook -> hook.id() == created.id());
                    updated.delete();
                    final Optional<Webhook> afterDelete = project.webhook(created.id());

                    assertSoftly(softly -> {
                        softly.assertThat(created.url()).isEqualTo(url);
                        softly.assertThat(updated.name()).contains(ENV.name("hook"));
                        softly.assertThat(updated.isEnabledFor(WebhookEvent.PIPELINE))
                                .isTrue();
                        softly.assertThat(updated.isEnabledFor(WebhookEvent.PUSH))
                                .isFalse();
                        softly.assertThat(reloaded).map(Webhook::url).contains(url);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<Webhook> hook = project.webhook(created.id());
                    if (hook.isPresent()) {
                        hook.get().delete();
                    }
                });
    }

    @Test
    void webhook_shouldReturnEmpty_whenHookDoesNotExist() throws IOException {
        final Optional<Webhook> missing = ENV.sandbox().webhook(Long.MAX_VALUE);

        assertThat(missing).isEmpty();
    }
}
