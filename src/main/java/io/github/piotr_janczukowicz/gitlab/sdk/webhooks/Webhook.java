package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectHook;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * A webhook of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. The
 * secret token and the signing token are write-only in GitLab and are never part of the response. URL variables, custom
 * headers and delivery logs are not wrapped; use the core client for them.
 */
public final class Webhook {

    private final Project project;
    private final GitLabProjectHook response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Webhook(final Project project, final GitLabProjectHook response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the webhook belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
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
     * Starts a change of the webhook. Nothing is sent until {@link WebhookUpdate#send()}.
     *
     * @return the update builder
     */
    public WebhookUpdate update() {
        return new WebhookUpdate(this);
    }

    /**
     * Asks GitLab to send a test event ({@code POST /projects/{id}/hooks/{hook_id}/test/{trigger}}).
     *
     * @param event the event to test
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the transport fails
     */
    public void test(final WebhookEvent event) throws IOException {
        gitLab().projectwebhooks()
                .createTest(project.id(), id(), requireNonNull(event, "event").projectTrigger());
    }

    /**
     * Deletes the webhook ({@code DELETE /projects/{id}/hooks/{hook_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().projectwebhooks().delete(project.id(), id());
    }

    /**
     * Loads the webhook again ({@code GET /projects/{id}/hooks/{hook_id}}). This object is not changed.
     *
     * @return the webhook as GitLab holds it now
     * @throws IllegalStateException if the webhook is gone
     * @throws IOException if the transport fails
     */
    public Webhook refresh() throws IOException {
        return project.webhook(id()).orElseThrow(() -> new IllegalStateException("Webhook " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabProjectHook response() {
        return response;
    }

    Webhook apply(final GitLabProjectProjectWebhooksUpdatePayload payload) throws IOException {
        return new Webhook(project, gitLab().projectwebhooks().update(project.id(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
