package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineScheduleDetails;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTrigger;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.VariableType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class SchedulesJsonTest {

    private static final String USER = """
            {"id": 1, "username": "john_smith", "name": "John Smith", "state": "active", "locked": false,
             "avatar_url": "http://localhost:3000/uploads/user/avatar/1/cd8.jpeg",
             "web_url": "http://localhost:3000/john_smith"}""";

    private static final String SCHEDULE = """
            {"id": 13, "description": "Nightly build", "ref": "refs/heads/main", "cron": "0 1 * * *",
             "cron_timezone": "UTC", "next_run_at": "2026-11-14T01:00:00.000Z", "active": true,
             "created_at": "2026-11-13T10:00:00.000Z", "updated_at": "2026-11-13T10:00:00.000Z", "owner": %s,
             "inputs": [{"name": "deploy_strategy", "value": "blue-green"}, {"name": "dry_run", "value": true},
                        {"name": "replicas", "value": 3}, {"name": "regions", "value": ["eu", "us"]}],
             "last_pipeline": {"id": 332, "iid": 12, "project_id": 3, "sha": "0e788619", "ref": "main",
                               "status": "pending", "source": "schedule",
                               "created_at": "2026-11-13T10:00:00.000Z", "updated_at": "2026-11-13T10:00:00.000Z",
                               "web_url": "http://localhost/p/-/pipelines/332"},
             "variables": [{"variable_type": "env_var", "key": "TARGET", "value": "staging", "raw": false},
                           {"variable_type": "file", "key": "CONFIG", "value": "a=b", "raw": true}]}""".formatted(USER);

    private static final String TRIGGER = """
            {"id": 10, "description": "my trigger", "created_at": "2026-11-13T10:00:00.000Z",
             "expires_at": "2027-01-01T00:00:00.000Z", "last_used": null, "token": "glptt-6d056f63",
             "updated_at": "2026-11-13T10:00:00.000Z", "owner": %s}""".formatted(USER);

    private final Project project =
            GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)).project("root/demo");

    @Test
    void accessors_shouldReadRealShapedJson_whenScheduleHasVariablesAndInputs() {
        final GitLabCiPipelineScheduleDetails decoded = decode(SCHEDULE, GitLabCiPipelineScheduleDetails.class);

        final PipelineSchedule sut = new PipelineSchedule(project, decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(13L);
            softly.assertThat(sut.description()).isEqualTo("Nightly build");
            softly.assertThat(sut.ref()).isEqualTo("refs/heads/main");
            softly.assertThat(sut.cron()).isEqualTo("0 1 * * *");
            softly.assertThat(sut.cronTimezone()).contains("UTC");
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.nextRunAt()).contains(Instant.parse("2026-11-14T01:00:00Z"));
            softly.assertThat(sut.owner())
                    .hasValueSatisfying(
                            owner -> softly.assertThat(owner.username()).isEqualTo("john_smith"));
            softly.assertThat(sut.variables())
                    .extracting(ScheduleVariable::key, ScheduleVariable::type)
                    .containsExactly(tuple("TARGET", VariableType.ENV_VAR), tuple("CONFIG", VariableType.FILE));
            softly.assertThat(sut.variables().getFirst().value()).contains("staging");
            softly.assertThat(sut.response().lastPipeline().id()).isEqualTo(332L);
            softly.assertThat(sut.response().inputs()).hasSize(4);
            softly.assertThat((Object) sut.response().inputs().getLast().value())
                    .isEqualTo(List.of("eu", "us"));
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenTriggerWasNeverUsed() {
        final GitLabTrigger decoded = decode(TRIGGER, GitLabTrigger.class);

        final PipelineTrigger sut = new PipelineTrigger(project, decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(10L);
            softly.assertThat(sut.description()).isEqualTo("my trigger");
            softly.assertThat(sut.token()).isEqualTo("glptt-6d056f63");
            softly.assertThat(sut.lastUsed()).isEmpty();
            softly.assertThat(sut.expiresAt()).contains(Instant.parse("2027-01-01T00:00:00Z"));
            softly.assertThat(sut.owner()).isPresent();
        });
    }

    private static <T> T decode(final String json, final Class<T> type) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), type);
    }
}
