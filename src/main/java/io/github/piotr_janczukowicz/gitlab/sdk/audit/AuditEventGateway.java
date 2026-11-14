package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * The audit-event calls for one project or group. GitLab has separate endpoints, and separate core filters, for each
 * owner; this interface hides which one is used. A list always walks by keyset ({@code order_by=id},
 * {@code sort=desc}): offset paging of audit events is deprecated by GitLab and never sent.
 */
interface AuditEventGateway {

    Stream<GitLabAuditEvent> events(@Nullable Instant after, @Nullable Instant before) throws IOException;

    Optional<GitLabAuditEvent> event(long id) throws IOException;
}
