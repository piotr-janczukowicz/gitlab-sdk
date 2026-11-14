package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEventBuilder;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPage;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageWalk;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Builds the core records for the tests of the audit-event objects. */
final class AuditFixtures {

    static final Instant CREATED_AT = Instant.parse("2020-05-26T22:55:04.230Z");

    private AuditFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabAuditEvent event(final long id) {
        return GitLabAuditEventBuilder.builder()
                .id(id)
                .authorId(1L)
                .entityId(7L)
                .entityType("Project")
                .eventName("project_feature_access_level_updated")
                .details(Map.of(
                        "change", "prevent merge request approval from committers",
                        "from", "",
                        "to", "true"))
                .createdAt(CREATED_AT)
                .build();
    }

    static GitLabAuditEvent bareEvent(final long id) {
        return GitLabAuditEventBuilder.builder().id(id).build();
    }

    static GitLabPage<GitLabAuditEvent> page(
            final GitLabPageWalk<GitLabAuditEvent> walk, final GitLabAuditEvent... items) {
        return new GitLabPage<>(List.of(items), walk, 1, null);
    }
}
