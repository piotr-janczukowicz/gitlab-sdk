package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupHook;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A webhook of a {@link Group}, as GitLab returned it. Premium: every group webhook operation needs the Premium tier,
 * and the core's tier guard decides. The object is immutable: it keeps the data of the moment it was loaded, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover. The secret token and
 * the signing token are write-only in GitLab and are never part of the response. URL variables, custom headers and
 * delivery logs are not wrapped; use the core client for them.
 */
public final class GroupWebhook {

    private final Group group;
    private final GitLabGroupHook response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupWebhook(final Group group, final GitLabGroupHook response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the webhook belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the id of the webhook.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the URL GitLab sends the events to.
     *
     * @return the URL
     */
    public String url() {
        return requireNonNull(response.url(), "url");
    }

    /**
     * Returns the name of the webhook.
     *
     * @return the name, or empty when it has none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Tells whether the webhook is switched on for an event.
     *
     * @param event the event
     * @return {@code true} when the flag of that event is set in the response; a missing flag counts as {@code false}
     * @throws NullPointerException if {@code event} is {@code null}
     */
    public boolean isEnabledFor(final WebhookEvent event) {
        return requireNonNull(event, "event").isOn(response);
    }

    /**
     * Starts a change of the webhook. Nothing is sent until {@link GroupWebhookUpdate#send()}.
     *
     * @return the update builder
     */
    public GroupWebhookUpdate update() {
        return new GroupWebhookUpdate(this);
    }

    /**
     * Asks GitLab to send a test event ({@code POST /groups/{id}/hooks/{hook_id}/test/{trigger}}). Premium.
     *
     * @param event the event to test
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the transport fails
     */
    public void test(final WebhookEvent event) throws IOException {
        gitLab().groupwebhooks()
                .createTest(group.id(), id(), requireNonNull(event, "event").groupTrigger());
    }

    /**
     * Deletes the webhook ({@code DELETE /groups/{id}/hooks/{hook_id}}). Premium.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().groupwebhooks().delete(group.id(), id());
    }

    /**
     * Loads the webhook again ({@code GET /groups/{id}/hooks/{hook_id}}). This object is not changed. Premium.
     *
     * @return the webhook as GitLab holds it now
     * @throws IllegalStateException if the webhook is gone
     * @throws IOException if the transport fails
     */
    public GroupWebhook refresh() throws IOException {
        return group.webhook(id()).orElseThrow(() -> new IllegalStateException("Webhook " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabGroupHook response() {
        return response;
    }

    GroupWebhook apply(final GitLabGroupGroupWebhooksUpdatePayload payload) throws IOException {
        return new GroupWebhook(group, gitLab().groupwebhooks().update(group.id(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
