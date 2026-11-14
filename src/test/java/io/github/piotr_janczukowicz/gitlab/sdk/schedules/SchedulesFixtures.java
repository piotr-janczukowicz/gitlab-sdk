package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariableBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineSchedule;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineScheduleBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineScheduleDetails;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineScheduleDetailsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTrigger;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTriggerBuilder;
import java.time.Instant;
import java.util.List;

/** Builds the core records for the tests of the schedule and trigger objects. */
final class SchedulesFixtures {

    static final String SECRET = "s3cr3t-value";
    static final String TOKEN = "tr1gger-t0ken";

    private SchedulesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabCiPipelineSchedule listed(final long id) {
        return GitLabCiPipelineScheduleBuilder.builder()
                .id(id)
                .description("nightly")
                .ref("main")
                .cron("0 1 * * *")
                .build();
    }

    static GitLabCiPipelineScheduleDetails schedule(final long id) {
        return GitLabCiPipelineScheduleDetailsBuilder.builder()
                .id(id)
                .description("nightly")
                .ref("main")
                .cron("0 1 * * *")
                .build();
    }

    static GitLabCiPipelineScheduleDetails fullSchedule(final long id) {
        return GitLabCiPipelineScheduleDetailsBuilder.builder(schedule(id))
                .cronTimezone("UTC")
                .active(true)
                .nextRunAt(Instant.parse("2026-01-02T01:00:00Z"))
                .owner(owner())
                .lastPipeline(pipelineBasic(55))
                .variables(List.of(variable("A", SECRET), variable("B", "b")))
                .build();
    }

    static GitLabUserBasic owner() {
        return GitLabUserBasicBuilder.builder().id(3L).username("ann").build();
    }

    static GitLabCiPipelineBasic pipelineBasic(final long id) {
        return GitLabCiPipelineBasicBuilder.builder().id(id).status("success").build();
    }

    static GitLabCiVariable variable(final String key, final String value) {
        return GitLabCiVariableBuilder.builder()
                .key(key)
                .value(value)
                .variableType("env_var")
                .build();
    }

    static GitLabTrigger trigger(final long id) {
        return GitLabTriggerBuilder.builder()
                .id(id)
                .token(TOKEN)
                .description("deploy hook")
                .build();
    }
}
