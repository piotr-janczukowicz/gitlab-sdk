package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsListAuditEventsFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabKeysetRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** The audit-event calls for a project (Premium; the core's tier guard applies when a tier is configured). */
final class ProjectAuditEventGateway implements AuditEventGateway {

    private final GitLab gitLab;
    private final String id;

    ProjectAuditEventGateway(final GitLab gitLab, final String id) {
        this.gitLab = gitLab;
        this.id = id;
    }

    @Override
    public Stream<GitLabAuditEvent> events(final @Nullable Instant after, final @Nullable Instant before)
            throws IOException {
        return gitLab
                .projects()
                .project()
                .listAuditEvents(
                        id,
                        GitLabProjectProjectsListAuditEventsFilterBuilder.builder()
                                .createdAfter(after)
                                .createdBefore(before)
                                .build(),
                        GitLabKeysetRequest.of(AuditEvents.ORDER_BY, AuditEvents.SORT))
                .stream();
    }

    @Override
    public Optional<GitLabAuditEvent> event(final long eventId) throws IOException {
        return gitLab.projects().project().getAuditEvent(id, eventId);
    }
}
