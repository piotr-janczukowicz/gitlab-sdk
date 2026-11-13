package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link GroupWebhook} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The object has no {@code toString()}, so a token set here is never printed.
 * Premium.
 */
public final class GroupWebhookUpdate {

    private final GroupWebhook webhook;
    private final GitLabGroupGroupWebhooksUpdatePayloadBuilder payload =
            GitLabGroupGroupWebhooksUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param webhook the webhook to change
     * @throws NullPointerException if {@code webhook} is {@code null}
     */
    public GroupWebhookUpdate(final GroupWebhook webhook) {
        this.webhook = requireNonNull(webhook, "webhook");
    }

    /**
     * Sets the URL the events are sent to.
     *
     * @param url the URL
     * @return this update
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     */
    public GroupWebhookUpdate url(final String url) {
        payload.url(requireNotBlank(url, "url"));
        return changed();
    }

    /**
     * Sets the name.
     *
     * @param name the name
     * @return this update
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public GroupWebhookUpdate name(final String name) {
        payload.name(requireNotBlank(name, "name"));
        return changed();
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GroupWebhookUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets the secret token GitLab sends with each event. The token is write-only: GitLab never returns it.
     *
     * @param token the secret token
     * @return this update
     * @throws NullPointerException if {@code token} is {@code null}
     */
    public GroupWebhookUpdate token(final String token) {
        payload.token(requireNonNull(token, "token"));
        return changed();
    }

    /**
     * Sets whether GitLab verifies the SSL certificate of the URL.
     *
     * @param enabled {@code true} to verify
     * @return this update
     */
    public GroupWebhookUpdate enableSslVerification(final boolean enabled) {
        payload.enableSslVerification(enabled);
        return changed();
    }

    /**
     * Switches the webhook on for the named events. Only those flags are sent, set to {@code true}; the flags of other
     * events are left as they are, so this cannot switch an event off.
     *
     * @param events the events to switch on
     * @return this update
     * @throws IllegalArgumentException if {@code events} is {@code null} or empty
     * @throws NullPointerException if an event is {@code null}
     */
    public GroupWebhookUpdate events(final WebhookEvent... events) {
        for (final WebhookEvent event : WebhookEvent.requireEvents(events)) {
            event.turnOn(payload);
        }
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/hooks/{hook_id}}). The webhook this update was made from is not
     * changed. Premium.
     *
     * @return the webhook as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupWebhook send() throws IOException {
        state.requireChange();
        return webhook.apply(payload.build());
    }

    private GroupWebhookUpdate changed() {
        state.mark();
        return this;
    }
}
