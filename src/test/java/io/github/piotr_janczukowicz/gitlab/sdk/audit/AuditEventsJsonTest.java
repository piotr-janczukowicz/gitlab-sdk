package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsListAuditEventsFilter;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPage;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageWalk;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class AuditEventsJsonTest {

    /**
     * The project list of the REST docs ({@code GET /projects/:id/audit_events},
     * https://docs.gitlab.com/api/audit_events/), verbatim: {@code details} holds {@code change}, {@code from} and
     * {@code to}, and {@code target_id} is a number. The docs example has no {@code event_name}.
     */
    private static final String PROJECT_LIST = """
            [
              {
                "id": 5,
                "author_id": 1,
                "entity_id": 7,
                "entity_type": "Project",
                "details": {
                    "change": "prevent merge request approval from committers",
                    "from": "",
                    "to": "true",
                    "author_name": "Administrator",
                    "author_email": "admin@example.com",
                    "target_id": 7,
                    "target_type": "Project",
                    "target_details": "twitter/typeahead-js",
                    "ip_address": "127.0.0.1",
                    "entity_path": "twitter/typeahead-js"
                },
                "created_at": "2020-05-26T22:55:04.230Z"
              },
              {
                  "id": 4,
                  "author_id": 1,
                  "entity_id": 7,
                  "entity_type": "Project",
                  "details": {
                      "change": "prevent merge request approval from authors",
                      "from": "false",
                      "to": "true",
                      "author_name": "Administrator",
                      "author_email": "admin@example.com",
                      "target_id": 7,
                      "target_type": "Project",
                      "target_details": "twitter/typeahead-js",
                      "ip_address": "127.0.0.1",
                      "entity_path": "twitter/typeahead-js"
                  },
                  "created_at": "2020-05-26T22:55:04.218Z"
              }
            ]""";

    /**
     * The group event of the REST docs ({@code GET /groups/:id/audit_events/:audit_event_id}), verbatim: here
     * {@code target_id} is a string.
     */
    private static final String GROUP_EVENT = """
            {
              "id": 2,
              "author_id": 1,
              "entity_id": 60,
              "entity_type": "Group",
              "details": {
                "custom_message": "Group marked for deletion",
                "author_name": "Administrator",
                "author_email": "admin@example.com",
                "target_id": "flightjs",
                "target_type": "Group",
                "target_details": "flightjs",
                "ip_address": "127.0.0.1",
                "entity_path": "flightjs"
              },
              "created_at": "2019-08-28T19:36:44.162Z"
            }""";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private static <T> T decode(final String json, final Class<T> type) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), type);
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenDocsExample() throws IOException {
        @SuppressWarnings("unchecked")
        final GitLabPageWalk<GitLabAuditEvent> walk = mock(GitLabPageWalk.class);
        final List<GitLabAuditEvent> listed = List.of(decode(PROJECT_LIST, GitLabAuditEvent[].class));
        when(gitLab.projects()
                        .project()
                        .listAuditEvents(
                                eq("twitter/typeahead-js"),
                                any(GitLabProjectProjectsListAuditEventsFilter.class),
                                any()))
                .thenReturn(new GitLabPage<>(listed, walk, 1, null));
        when(gitLab.groups().getAuditEvent("flightjs", 2L))
                .thenReturn(Optional.of(decode(GROUP_EVENT, GitLabAuditEvent.class)));
        final GitLabSdk sdk = GitLabSdk.of(gitLab);

        final List<AuditEvent> events =
                sdk.project("twitter/typeahead-js").auditEvents().all().toList();
        final Optional<AuditEvent> group = sdk.group("flightjs").auditEvents().find(2L);

        assertSoftly(softly -> {
            softly.assertThat(events).extracting(AuditEvent::id).containsExactly(5L, 4L);
            final AuditEvent first = events.getFirst();
            softly.assertThat(first.authorId()).contains(1L);
            softly.assertThat(first.entityId()).contains(7L);
            softly.assertThat(first.entityType()).contains("Project");
            softly.assertThat(first.eventName()).isEmpty();
            softly.assertThat(first.createdAt()).contains(Instant.parse("2020-05-26T22:55:04.230Z"));
            softly.assertThat(first.details())
                    .containsEntry("change", "prevent merge request approval from committers")
                    .containsEntry("from", "")
                    .containsEntry("to", "true")
                    .containsEntry("author_name", "Administrator")
                    .containsEntry("target_details", "twitter/typeahead-js")
                    .containsEntry("target_id", 7)
                    .hasSize(10);
            softly.assertThat(events.get(1).details()).containsEntry("from", "false");
            softly.assertThat(group).map(AuditEvent::id).contains(2L);
            softly.assertThat(group).map(AuditEvent::entityType).contains(Optional.of("Group"));
            softly.assertThat(group.orElseThrow().details())
                    .containsEntry("custom_message", "Group marked for deletion")
                    .containsEntry("target_id", "flightjs")
                    .containsEntry("target_details", "flightjs");
            softly.assertThat(group.orElseThrow().createdAt()).contains(Instant.parse("2019-08-28T19:36:44.162Z"));
        });
    }
}
