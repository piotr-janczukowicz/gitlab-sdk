package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditEvent;
import io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditEvents;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only: nothing in the smoke tests writes an audit event on purpose. Each test lists the first five audit events,
 * lists the events of the day before the newest one's creation time (the bounds truncated to whole seconds), expecting
 * that event first, and last finds the newest one by its id. A test is skipped when its first list call answers 403 or
 * 404 (audit events are a Premium feature; see {@link SmokeSkips}) and when the list is empty. The group test needs the
 * optional {@code SMOKE_GITLAB_GROUP}; its lookup by id needs the Owner role on that group, and for a token without it
 * GitLab is expected to answer 403, which skips the lookup after the list checks have run. NOT run live in the session
 * that wrote it.
 */
@Tag("smoke")
class AuditEventsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void projectEvents_shouldListFindAndFilter_whenSandboxHasEvents() throws IOException {
        readBack(ENV.sandbox().auditEvents(), "the audit events of the sandbox");
    }

    @Test
    void groupEvents_shouldListFindAndFilter_whenSandboxGroupIsSet() throws IOException {
        ENV.requireGroupConfigured();
        readBack(ENV.sandboxGroup().auditEvents(), "the audit events of the sandbox group");
    }

    private static void readBack(final AuditEvents events, final String feature) throws IOException {
        final List<AuditEvent> listed =
                SmokeSkips.probe(feature, () -> events.all().limit(5).toList());
        assumeFalse(listed.isEmpty(), feature + " are empty");
        final AuditEvent newest = listed.get(0);
        final Optional<Instant> createdAt = newest.createdAt();
        final Optional<List<AuditEvent>> window = createdAt.isPresent()
                ? Optional.of(events.createdBetween(
                                createdAt.get().truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofDays(1)),
                                createdAt.get().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1))
                        .limit(1)
                        .toList())
                : Optional.empty();

        assertSoftly(softly -> {
            softly.assertThat(listed).extracting(AuditEvent::id).isSortedAccordingTo((a, b) -> Long.compare(b, a));
            softly.assertThat(newest.details()).isNotNull();
            softly.assertThat(newest.auditEvents()).isSameAs(events);
            softly.assertThat(newest.response()).isNotNull();
            window.ifPresent(
                    list -> softly.assertThat(list).extracting(AuditEvent::id).containsExactly(newest.id()));
        });
        final Optional<AuditEvent> found =
                SmokeSkips.probe(feature + ": one event by id", () -> events.find(newest.id()));
        assertThat(found).map(AuditEvent::id).contains(newest.id());
    }
}
