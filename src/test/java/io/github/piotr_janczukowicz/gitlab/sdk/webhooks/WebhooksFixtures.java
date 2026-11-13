package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupHook;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupHookBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectHook;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectHookBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksUpdatePayload;
import java.util.EnumSet;
import java.util.Set;

/** Builds the core records for the tests of the webhook objects. */
final class WebhooksFixtures {

    private WebhooksFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabProjectHook hook(final long id, final String url) {
        return GitLabProjectHookBuilder.builder().id(id).url(url).build();
    }

    static GitLabProjectHook hookNamed(final long id, final String url, final String name) {
        return GitLabProjectHookBuilder.builder().id(id).url(url).name(name).build();
    }

    static GitLabProjectHook hookWithOnly(final WebhookEvent event) {
        final GitLabProjectHookBuilder builder =
                GitLabProjectHookBuilder.builder().id(7L).url("https://h.example");
        switch (event) {
            case PUSH -> builder.pushEvents(true);
            case TAG_PUSH -> builder.tagPushEvents(true);
            case ISSUES -> builder.issuesEvents(true);
            case NOTE -> builder.noteEvents(true);
            case MERGE_REQUESTS -> builder.mergeRequestsEvents(true);
            case JOB -> builder.jobEvents(true);
            case PIPELINE -> builder.pipelineEvents(true);
            case WIKI_PAGE -> builder.wikiPageEvents(true);
            case RELEASES -> builder.releasesEvents(true);
        }
        return builder.build();
    }

    static GitLabGroupHook groupHook(final long id, final String url) {
        return GitLabGroupHookBuilder.builder().id(id).url(url).build();
    }

    static GitLabGroupHook groupHookNamed(final long id, final String url, final String name) {
        return GitLabGroupHookBuilder.builder().id(id).url(url).name(name).build();
    }

    static GitLabGroupHook groupHookWithOnly(final WebhookEvent event) {
        final GitLabGroupHookBuilder builder =
                GitLabGroupHookBuilder.builder().id(7L).url("https://h.example");
        switch (event) {
            case PUSH -> builder.pushEvents(true);
            case TAG_PUSH -> builder.tagPushEvents(true);
            case ISSUES -> builder.issuesEvents(true);
            case NOTE -> builder.noteEvents(true);
            case MERGE_REQUESTS -> builder.mergeRequestsEvents(true);
            case JOB -> builder.jobEvents(true);
            case PIPELINE -> builder.pipelineEvents(true);
            case WIKI_PAGE -> builder.wikiPageEvents(true);
            case RELEASES -> builder.releasesEvents(true);
        }
        return builder.build();
    }

    /** The events a project payload turns on. */
    static Set<WebhookEvent> sent(final GitLabProjectProjectWebhooksUpdatePayload payload) {
        final Set<WebhookEvent> events = EnumSet.noneOf(WebhookEvent.class);
        add(events, WebhookEvent.PUSH, payload.pushEvents());
        add(events, WebhookEvent.TAG_PUSH, payload.tagPushEvents());
        add(events, WebhookEvent.ISSUES, payload.issuesEvents());
        add(events, WebhookEvent.NOTE, payload.noteEvents());
        add(events, WebhookEvent.MERGE_REQUESTS, payload.mergeRequestsEvents());
        add(events, WebhookEvent.JOB, payload.jobEvents());
        add(events, WebhookEvent.PIPELINE, payload.pipelineEvents());
        add(events, WebhookEvent.WIKI_PAGE, payload.wikiPageEvents());
        add(events, WebhookEvent.RELEASES, payload.releasesEvents());
        return events;
    }

    /** The events a group payload turns on. */
    static Set<WebhookEvent> sent(final GitLabGroupGroupWebhooksUpdatePayload payload) {
        final Set<WebhookEvent> events = EnumSet.noneOf(WebhookEvent.class);
        add(events, WebhookEvent.PUSH, payload.pushEvents());
        add(events, WebhookEvent.TAG_PUSH, payload.tagPushEvents());
        add(events, WebhookEvent.ISSUES, payload.issuesEvents());
        add(events, WebhookEvent.NOTE, payload.noteEvents());
        add(events, WebhookEvent.MERGE_REQUESTS, payload.mergeRequestsEvents());
        add(events, WebhookEvent.JOB, payload.jobEvents());
        add(events, WebhookEvent.PIPELINE, payload.pipelineEvents());
        add(events, WebhookEvent.WIKI_PAGE, payload.wikiPageEvents());
        add(events, WebhookEvent.RELEASES, payload.releasesEvents());
        return events;
    }

    private static void add(final Set<WebhookEvent> events, final WebhookEvent event, final Boolean flag) {
        if (Boolean.TRUE.equals(flag)) {
            events.add(event);
        }
    }
}
