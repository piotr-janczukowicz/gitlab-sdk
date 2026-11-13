package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Collects changes to a {@link Webhook} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The object has no {@code toString()}, so a token set here is never printed.
 */
public final class WebhookUpdate {

    private final Webhook webhook;
    private final GitLabProjectProjectWebhooksUpdatePayloadBuilder payload =
            GitLabProjectProjectWebhooksUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param webhook the webhook to change
     * @throws NullPointerException if {@code webhook} is {@code null}
     */
    public WebhookUpdate(final Webhook webhook) {
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
    public WebhookUpdate url(final String url) {
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
    public WebhookUpdate name(final String name) {
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
    public WebhookUpdate description(final String description) {
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
    public WebhookUpdate token(final String token) {
        payload.token(requireNonNull(token, "token"));
        return changed();
    }

    /**
     * Sets whether GitLab verifies the SSL certificate of the URL.
     *
     * @param enabled {@code true} to verify
     * @return this update
     */
    public WebhookUpdate enableSslVerification(final boolean enabled) {
        payload.enableSslVerification(enabled);
        return changed();
    }

    /**
     * Replaces the events the webhook is subscribed to. Every flag of {@link WebhookEvent} is sent: {@code true} for
     * the listed events and {@code false} for the others. An empty collection is allowed, as in {@code github-sdk}, and
     * switches all of them off; the request still carries the nine flags, so it is not empty. Events outside
     * {@link WebhookEvent} are left as they are. The event calls of one update apply in order.
     *
     * @param events the events the webhook is subscribed to afterwards
     * @return this update
     * @throws NullPointerException if {@code events} or an event is {@code null}
     */
    public WebhookUpdate events(final Collection<WebhookEvent> events) {
        final Set<WebhookEvent> subscribed = WebhookEvent.requireEventSet(events);
        for (final WebhookEvent event : WebhookEvent.values()) {
            event.set(payload, subscribed.contains(event));
        }
        return changed();
    }

    /**
     * Adds events to the subscription. Only the flags of the named events are sent, set to {@code true}; the others are
     * left as they are. The event calls of one update apply in order.
     *
     * @param events the events to add
     * @return this update
     * @throws NullPointerException if {@code events} or an event is {@code null}
     * @throws IllegalArgumentException if no event is given
     */
    public WebhookUpdate addEvents(final WebhookEvent... events) {
        for (final WebhookEvent event : WebhookEvent.requireEvents(events)) {
            event.set(payload, true);
        }
        return changed();
    }

    /**
     * Removes events from the subscription. Only the flags of the named events are sent, set to {@code false}; the
     * others are left as they are. The event calls of one update apply in order.
     *
     * @param events the events to remove
     * @return this update
     * @throws NullPointerException if {@code events} or an event is {@code null}
     * @throws IllegalArgumentException if no event is given
     */
    public WebhookUpdate removeEvents(final WebhookEvent... events) {
        for (final WebhookEvent event : WebhookEvent.requireEvents(events)) {
            event.set(payload, false);
        }
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/hooks/{hook_id}}). The webhook this update was made from is not
     * changed.
     *
     * @return the webhook as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Webhook send() throws IOException {
        state.requireChange();
        return webhook.apply(payload.build());
    }

    private WebhookUpdate changed() {
        state.mark();
        return this;
    }
}
