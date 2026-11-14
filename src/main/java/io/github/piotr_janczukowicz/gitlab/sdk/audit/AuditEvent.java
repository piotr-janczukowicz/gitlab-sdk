package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import static io.github.piotr_janczukowicz.gitlab.sdk.internal.FrozenValues.frozenMap;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * An audit event of a project or group, as GitLab returned it (Premium). Audit events do not change, so the object has
 * no {@code refresh()}; {@link #response()} gives the complete response and {@link #auditEvents()} the audit events it
 * was read from. The {@link #details()} can hold anything the event recorded (names, e-mail addresses, IP addresses,
 * old and new values), so no {@code toString()} of the SDK prints them.
 */
public final class AuditEvent {

    private final AuditEvents auditEvents;
    private final GitLabAuditEvent response;

    /**
     * Wraps a response. Applications normally get this object from {@link AuditEvents}; call the constructor only to
     * wrap a response that was loaded some other way.
     *
     * @param auditEvents the audit events of the project or group the event belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public AuditEvent(final AuditEvents auditEvents, final GitLabAuditEvent response) {
        this.auditEvents = requireNonNull(auditEvents, "auditEvents");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id of the event.
     *
     * @return the id
     * @throws NullPointerException if GitLab sent no id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the id of the user who caused the event.
     *
     * @return the author id, or empty when GitLab sent none
     */
    public Optional<Long> authorId() {
        return Optional.ofNullable(response.authorId());
    }

    /**
     * Returns the id of the project or group the event is recorded on.
     *
     * @return the entity id, or empty when GitLab sent none
     */
    public Optional<Long> entityId() {
        return Optional.ofNullable(response.entityId());
    }

    /**
     * Returns the kind of the entity as GitLab spells it, such as {@code Project} or {@code Group}.
     *
     * @return the entity type, or empty when GitLab sent none
     */
    public Optional<String> entityType() {
        return Optional.ofNullable(response.entityType());
    }

    /**
     * Returns the name of the event as GitLab spells it.
     *
     * @return the event name, or empty when GitLab sent none
     */
    public Optional<String> eventName() {
        return Optional.ofNullable(response.eventName());
    }

    /**
     * Returns the details GitLab recorded, such as {@code change}, {@code from}, {@code to}, {@code author_name} or
     * {@code target_details}. Their keys and value types differ between events (a {@code target_id} can be a number or
     * a text), so they are passed on as GitLab sent them.
     *
     * @return an immutable copy, nested maps and lists included; empty when GitLab sent none
     */
    public Map<String, Object> details() {
        final Map<String, Object> details = response.details();
        return details == null ? Map.of() : frozenMap(details);
    }

    /**
     * Returns when the event was recorded.
     *
     * @return the creation time, or empty when GitLab sent none
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(response.createdAt());
    }

    /**
     * Returns the audit events this event was read from, without a request.
     *
     * @return the audit events of the project or group
     */
    public AuditEvents auditEvents() {
        return auditEvents;
    }

    /**
     * Returns the complete response GitLab sent for this event.
     *
     * @return the response record
     */
    public GitLabAuditEvent response() {
        return response;
    }
}
