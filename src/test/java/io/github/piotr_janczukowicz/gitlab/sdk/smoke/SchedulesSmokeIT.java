package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.PipelineSchedule;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.PipelineTrigger;
import io.github.piotr_janczukowicz.gitlab.sdk.schedules.ScheduleVariable;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A throwaway pipeline schedule with a variable, and a throwaway pipeline trigger. The schedule is switched off right
 * after it is created and its cron expression fires once a year, so it never starts a pipeline. {@code play()} (it
 * starts a pipeline), {@code takeOwnership()} (it would change the owner) and firing a trigger are not run live.
 */
@Tag("smoke")
class SchedulesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void schedule_shouldRoundTrip_whenCreatedUpdatedAndGivenAVariable() throws IOException {
        final Project project = ENV.sandbox();
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        assumeTrue(defaultBranch != null, "the sandbox has no default branch (empty repository)");
        final String description = ENV.name("schedule");
        final String key = "SMOKE_" + ENV.runId().toUpperCase(Locale.ROOT);
        final String secret = "smoke-secret-" + ENV.runId();
        final PipelineSchedule created = project.createPipelineSchedule(description, defaultBranch, "0 3 1 1 *");
        SmokeCleanup.run(
                () -> {
                    final PipelineSchedule updated = created.update()
                            .description(description + "-2")
                            .cronTimezone("UTC")
                            .active(false)
                            .send();
                    final ScheduleVariable variable = updated.addVariable(key, secret);
                    final ScheduleVariable changed =
                            variable.update().value(secret + "-2").send();
                    final Optional<ScheduleVariable> loaded = updated.variable(key);
                    final PipelineSchedule refreshed = updated.refresh();
                    final boolean listed =
                            project.pipelineSchedules().limit(100).anyMatch(each -> each.id() == created.id());
                    final Optional<Long> lastPipeline = refreshed.lastPipeline().map(pipeline -> pipeline.id());
                    final String printed =
                            variable + " " + changed + " " + variable.update().value(secret);
                    changed.delete();
                    final Optional<ScheduleVariable> variableAfterDelete = updated.variable(key);
                    refreshed.delete();
                    final Optional<PipelineSchedule> afterDelete = project.pipelineSchedule(created.id());

                    assertSoftly(softly -> {
                        softly.assertThat(created.description()).isEqualTo(description);
                        softly.assertThat(updated.description()).isEqualTo(description + "-2");
                        softly.assertThat(updated.isActive()).isFalse();
                        softly.assertThat(updated.cronTimezone()).contains("UTC");
                        softly.assertThat(variable.key()).isEqualTo(key);
                        softly.assertThat(changed.value()).contains(secret + "-2");
                        softly.assertThat(loaded).map(ScheduleVariable::key).contains(key);
                        softly.assertThat(refreshed.variables())
                                .extracting(ScheduleVariable::key)
                                .contains(key);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(lastPipeline).isEmpty();
                        softly.assertThat(printed).doesNotContain(secret);
                        softly.assertThat(variableAfterDelete).isEmpty();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<PipelineSchedule> remaining = project.pipelineSchedule(created.id());
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }

    @Test
    void trigger_shouldRoundTrip_whenCreatedUpdatedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String description = ENV.name("trigger");
        final PipelineTrigger created = project.createPipelineTrigger(description);
        SmokeCleanup.run(
                () -> {
                    final PipelineTrigger updated =
                            created.update().description(description + "-2").send();
                    final PipelineTrigger refreshed = updated.refresh();
                    final boolean listed =
                            project.pipelineTriggers().limit(100).anyMatch(each -> each.id() == created.id());
                    final String printed = created + " " + updated + " " + refreshed;
                    refreshed.delete();
                    final Optional<PipelineTrigger> afterDelete = project.pipelineTrigger(created.id());

                    assertSoftly(softly -> {
                        softly.assertThat(created.description()).isEqualTo(description);
                        softly.assertThat(created.token()).isNotBlank();
                        softly.assertThat(updated.description()).isEqualTo(description + "-2");
                        softly.assertThat(refreshed.id()).isEqualTo(created.id());
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(printed).doesNotContain(created.token());
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<PipelineTrigger> remaining = project.pipelineTrigger(created.id());
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }
}
