package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import static io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditFixtures.CREATED_AT;
import static io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditFixtures.bareEvent;
import static io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditFixtures.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEventBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class AuditEventTest {

    private static final String SECRET = "prevent merge request approval from committers";

    private final AuditEvents events =
            GitLabSdk.of(mock(GitLab.class)).project("my-group/my-project").auditEvents();

    @Test
    void accessors_shouldReturnResponseValues_whenPresent() {
        final GitLabAuditEvent response = event(5L);

        final AuditEvent sut = new AuditEvent(events, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(5L);
            softly.assertThat(sut.authorId()).contains(1L);
            softly.assertThat(sut.entityId()).contains(7L);
            softly.assertThat(sut.entityType()).contains("Project");
            softly.assertThat(sut.eventName()).contains("project_feature_access_level_updated");
            softly.assertThat(sut.details())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("change", SECRET, "from", "", "to", "true"));
            softly.assertThat(sut.createdAt()).contains(CREATED_AT);
            softly.assertThat(sut.auditEvents()).isSameAs(events);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenAbsent() {
        final AuditEvent sut = new AuditEvent(events, bareEvent(5L));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(5L);
            softly.assertThat(sut.authorId()).isEmpty();
            softly.assertThat(sut.entityId()).isEmpty();
            softly.assertThat(sut.entityType()).isEmpty();
            softly.assertThat(sut.eventName()).isEmpty();
            softly.assertThat(sut.createdAt()).isEmpty();
        });
    }

    @Test
    void details_shouldBeImmutableAndEmpty_whenAbsent() {
        final AuditEvent sut = new AuditEvent(events, bareEvent(5L));

        final Map<String, Object> details = sut.details();

        assertSoftly(softly -> {
            softly.assertThat(details).isEmpty();
            softly.assertThat(catchThrowable(() -> details.put("key", "value")))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void details_shouldBeDeepImmutableCopy_whenNested() {
        final Map<String, Object> nested = new HashMap<>();
        nested.put("level", 2);
        final List<Object> list = new ArrayList<>(List.of("a", Map.of("b", 1)));
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("nested", nested);
        source.put("list", list);
        source.put("missing", null);
        source.put("target_id", 7);
        final AuditEvent sut = new AuditEvent(
                events, GitLabAuditEventBuilder.builder().id(5L).details(source).build());

        final Map<String, Object> details = sut.details();
        nested.put("level", 3);
        list.add("c");

        assertSoftly(softly -> {
            softly.assertThat(details).containsKeys("nested", "list", "missing", "target_id");
            softly.assertThat(details.get("missing")).isNull();
            softly.assertThat(details.get("target_id")).isEqualTo(7);
            softly.assertThat(details.get("nested")).isEqualTo(Map.of("level", 2));
            softly.assertThat(details.get("list")).isEqualTo(List.of("a", Map.of("b", 1)));
            softly.assertThat(catchThrowable(() -> details.put("key", "value")))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(
                            catchThrowable(() -> castMap(details.get("nested")).put("x", 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(() -> castList(details.get("list")).add("x")))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(
                            () -> castMap(castList(details.get("list")).get(1)).put("x", 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void id_shouldThrowNullPointerException_whenGitLabSentNone() {
        final AuditEvent sut =
                new AuditEvent(events, GitLabAuditEventBuilder.builder().build());

        final Throwable thrown = catchThrowable(sut::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new AuditEvent(null, event(5L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new AuditEvent(events, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void toString_shouldNotContainDetails_whenObjectPrinted() {
        final AuditEvent sut = new AuditEvent(events, event(5L));

        final String printed = sut + " " + events;

        assertSoftly(softly -> {
            softly.assertThat(printed).doesNotContain(SECRET);
            softly.assertThat(printed).doesNotContain("change");
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(final Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(final Object value) {
        return (List<Object>) value;
    }
}
