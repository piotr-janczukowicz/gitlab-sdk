package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreateTestTrigger;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupHook;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectHook;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksCreateTestTrigger;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksUpdatePayloadBuilder;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * An event a webhook can be switched on for, limited to the events that GitLab lets both a project webhook and a group
 * webhook choose in a request and trigger as a test. Other events need the core's payload records.
 */
public enum WebhookEvent {
    /** Push events. */
    PUSH("push_events"),
    /** Tag push events. */
    TAG_PUSH("tag_push_events"),
    /** Issue events. */
    ISSUES("issues_events"),
    /** Comment events. */
    NOTE("note_events"),
    /** Merge request events. */
    MERGE_REQUESTS("merge_requests_events"),
    /** Job events. */
    JOB("job_events"),
    /** Pipeline events. */
    PIPELINE("pipeline_events"),
    /** Wiki page events. */
    WIKI_PAGE("wiki_page_events"),
    /** Release events. */
    RELEASES("releases_events");

    private final String trigger;

    WebhookEvent(final String trigger) {
        this.trigger = trigger;
    }

    GitLabProjectProjectWebhooksCreateTestTrigger projectTrigger() {
        return GitLabProjectProjectWebhooksCreateTestTrigger.fromWire(trigger);
    }

    GitLabGroupGroupWebhooksCreateTestTrigger groupTrigger() {
        return GitLabGroupGroupWebhooksCreateTestTrigger.fromWire(trigger);
    }

    boolean isOn(final GitLabProjectHook hook) {
        return isTrue(
                switch (this) {
                    case PUSH -> hook.pushEvents();
                    case TAG_PUSH -> hook.tagPushEvents();
                    case ISSUES -> hook.issuesEvents();
                    case NOTE -> hook.noteEvents();
                    case MERGE_REQUESTS -> hook.mergeRequestsEvents();
                    case JOB -> hook.jobEvents();
                    case PIPELINE -> hook.pipelineEvents();
                    case WIKI_PAGE -> hook.wikiPageEvents();
                    case RELEASES -> hook.releasesEvents();
                });
    }

    boolean isOn(final GitLabGroupHook hook) {
        return isTrue(
                switch (this) {
                    case PUSH -> hook.pushEvents();
                    case TAG_PUSH -> hook.tagPushEvents();
                    case ISSUES -> hook.issuesEvents();
                    case NOTE -> hook.noteEvents();
                    case MERGE_REQUESTS -> hook.mergeRequestsEvents();
                    case JOB -> hook.jobEvents();
                    case PIPELINE -> hook.pipelineEvents();
                    case WIKI_PAGE -> hook.wikiPageEvents();
                    case RELEASES -> hook.releasesEvents();
                });
    }

    void set(final GitLabProjectProjectWebhooksUpdatePayloadBuilder payload, final boolean on) {
        switch (this) {
            case PUSH -> payload.pushEvents(on);
            case TAG_PUSH -> payload.tagPushEvents(on);
            case ISSUES -> payload.issuesEvents(on);
            case NOTE -> payload.noteEvents(on);
            case MERGE_REQUESTS -> payload.mergeRequestsEvents(on);
            case JOB -> payload.jobEvents(on);
            case PIPELINE -> payload.pipelineEvents(on);
            case WIKI_PAGE -> payload.wikiPageEvents(on);
            case RELEASES -> payload.releasesEvents(on);
        }
    }

    void set(final GitLabGroupGroupWebhooksUpdatePayloadBuilder payload, final boolean on) {
        switch (this) {
            case PUSH -> payload.pushEvents(on);
            case TAG_PUSH -> payload.tagPushEvents(on);
            case ISSUES -> payload.issuesEvents(on);
            case NOTE -> payload.noteEvents(on);
            case MERGE_REQUESTS -> payload.mergeRequestsEvents(on);
            case JOB -> payload.jobEvents(on);
            case PIPELINE -> payload.pipelineEvents(on);
            case WIKI_PAGE -> payload.wikiPageEvents(on);
            case RELEASES -> payload.releasesEvents(on);
        }
    }

    static WebhookEvent[] requireEvents(final WebhookEvent... events) {
        requireNonNull(events, "events");
        if (events.length == 0) {
            throw new IllegalArgumentException("At least one event is required");
        }
        for (final WebhookEvent event : events) {
            requireNonNull(event, "event");
        }
        return events;
    }

    static Set<WebhookEvent> requireEventSet(final Collection<WebhookEvent> events) {
        final Set<WebhookEvent> checked = EnumSet.noneOf(WebhookEvent.class);
        for (final WebhookEvent event : requireNonNull(events, "events")) {
            checked.add(requireNonNull(event, "event"));
        }
        return checked;
    }

    private static boolean isTrue(final @Nullable Boolean flag) {
        return Boolean.TRUE.equals(flag);
    }
}
