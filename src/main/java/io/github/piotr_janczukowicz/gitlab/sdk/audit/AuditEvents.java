package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The audit events of one project or group, read only. {@code Project.auditEvents()} and {@code Group.auditEvents()}
 * are the usual way in. This is a Premium feature: GitLab is expected to answer 403 or 404 when the tier lacks it, and
 * a client configured with a lower tier refuses the call with the core's {@code GitLabTierException} before a request.
 * Each method names the project request; the group request is the same with {@code /groups/{id}} in place of
 * {@code /projects/{id}}.
 *
 * <p>Who sees what, per the GitLab REST docs: on a project, a Maintainer (or above) lists the events of all users and a
 * Developer only the events of their own actions; on a group, an Owner lists the events of all users and a Developer or
 * Maintainer only their own. Loading one group event needs the Owner role (or an administrator); a token without it is
 * expected to get a 403, which passes through as the core's exception.
 *
 * <p>Lists walk by keyset pagination with {@code order_by=id} and {@code sort=desc}, the only keyset order the GitLab
 * REST docs list for audit events, so events come newest first. Offset paging of audit events is deprecated by GitLab
 * and never sent.
 */
public final class AuditEvents {

    static final String ORDER_BY = "id";
    static final String SORT = "desc";

    private final AuditEventGateway gateway;

    private AuditEvents(final AuditEventGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the audit events of a project (Premium); no request is made.
     *
     * @param project the project
     * @return the audit events of the project
     * @throws NullPointerException if {@code project} is {@code null}
     */
    public static AuditEvents forProject(final Project project) {
        requireNonNull(project, "project");
        return new AuditEvents(new ProjectAuditEventGateway(Internals.gitLab(project), project.id()));
    }

    /**
     * Returns the audit events of a group (Premium); no request is made.
     *
     * @param group the group
     * @return the audit events of the group
     * @throws NullPointerException if {@code group} is {@code null}
     */
    public static AuditEvents forGroup(final Group group) {
        requireNonNull(group, "group");
        return new AuditEvents(new GroupAuditEventGateway(Internals.gitLab(group), group.id()));
    }

    /**
     * Lists all audit events, newest first ({@code GET /projects/{id}/audit_events} with {@code pagination=keyset},
     * {@code order_by=id} and {@code sort=desc}). The first page is requested at once; further pages are fetched as the
     * stream reaches them, so {@code limit(n)} stops further requests. The stream can be used once; transport failures
     * during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the audit events
     * @throws IOException if the list cannot be loaded
     */
    public Stream<AuditEvent> all() throws IOException {
        return wrapped(gateway.events(null, null));
    }

    /**
     * Lists the audit events created between two instants, newest first ({@code GET /projects/{id}/audit_events} with
     * {@code created_after} and {@code created_before}, walked by keyset as {@link #all()} describes). Per the GitLab
     * REST docs both bounds are inclusive. The instants are sent as {@link Instant#toString()} gives them.
     *
     * @param after the earliest creation time
     * @param before the latest creation time; equal to {@code after} is allowed
     * @return the audit events in that range
     * @throws NullPointerException if an argument is {@code null}; no request is made
     * @throws IllegalArgumentException if {@code after} is later than {@code before}; no request is made
     * @throws IOException if the list cannot be loaded
     */
    public Stream<AuditEvent> createdBetween(final Instant after, final Instant before) throws IOException {
        requireNonNull(after, "after");
        requireNonNull(before, "before");
        if (after.isAfter(before)) {
            throw new IllegalArgumentException("after must not be later than before");
        }
        return wrapped(gateway.events(after, before));
    }

    /**
     * Loads one audit event ({@code GET /projects/{id}/audit_events/{audit_event_id}}). On a group this needs the Owner
     * role, see the class description.
     *
     * @param id the id of the audit event
     * @return the event, or empty when GitLab answers HTTP 404; empty also when audit events are not available
     * @throws IOException if the transport fails
     */
    public Optional<AuditEvent> find(final long id) throws IOException {
        return gateway.event(id).map(loaded -> new AuditEvent(this, loaded));
    }

    private Stream<AuditEvent> wrapped(final Stream<GitLabAuditEvent> listed) {
        return listed.map(event -> new AuditEvent(this, event));
    }
}
