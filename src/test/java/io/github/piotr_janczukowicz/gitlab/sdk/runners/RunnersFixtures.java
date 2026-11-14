package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiResetTokenResult;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiResetTokenResultBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerManager;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerManagerBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerWithUuid;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerWithUuidBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerWithUuidRunnerType;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiJobBasicWithProject;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiJobBasicWithProjectBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetails;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetailsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetailsRunnerType;
import java.time.Instant;

/** Builds the core records for the tests of the runner objects. */
final class RunnersFixtures {

    static final String TOKEN = "gl-runner-s3cr3t";

    private RunnersFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabCiRunnerWithUuid listed(final long id) {
        return GitLabCiRunnerWithUuidBuilder.builder()
                .id(id)
                .description("shared")
                .runnerType(GitLabCiRunnerWithUuidRunnerType.PROJECT_TYPE)
                .build();
    }

    static GitLabCiRunnerDetails runner(final long id) {
        return GitLabCiRunnerDetailsBuilder.builder()
                .id(id)
                .status("online")
                .runnerType(GitLabCiRunnerDetailsRunnerType.PROJECT_TYPE)
                .build();
    }

    static GitLabCiRunnerDetails fullRunner(final long id) {
        return GitLabCiRunnerDetailsBuilder.builder(runner(id))
                .description("build box")
                .name("gitlab-runner")
                .active(true)
                .paused(false)
                .online(true)
                .tagList("docker, linux ,")
                .version("18.0.0")
                .contactedAt(Instant.parse("2026-01-02T03:04:05Z"))
                .build();
    }

    static GitLabCiResetTokenResult resetResult() {
        return GitLabCiResetTokenResultBuilder.builder()
                .token(TOKEN)
                .tokenExpiresAt(Instant.parse("2027-01-01T00:00:00Z"))
                .build();
    }

    static GitLabCiRunnerManager manager(final long id) {
        return GitLabCiRunnerManagerBuilder.builder().id(id).systemId("s_" + id).build();
    }

    static GitLabCiJobBasicWithProject job(final long id) {
        return GitLabCiJobBasicWithProjectBuilder.builder().id(id).name("build").build();
    }
}
