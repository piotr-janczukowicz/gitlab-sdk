package io.github.piotr_janczukowicz.gitlab.sdk.pushrules;

import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupPushRule;
import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupPushRuleBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectPushRule;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectPushRuleBuilder;
import java.time.Instant;

/** Builds the core records for the tests of the push rule objects. */
final class PushRulesFixtures {

    static final Instant CREATED = Instant.parse("2026-01-02T03:04:05Z");

    private PushRulesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabProjectPushRule rule(final long id) {
        return GitLabProjectPushRuleBuilder.builder().id(id).build();
    }

    static GitLabProjectPushRule fullRule(final long id) {
        return GitLabProjectPushRuleBuilder.builder()
                .id(id)
                .projectId(42L)
                .createdAt(CREATED)
                .commitMessageRegex("^JIRA-\\d+")
                .commitMessageNegativeRegex("WIP")
                .branchNameRegex("^feature/")
                .authorEmailRegex("@example\\.test$")
                .fileNameRegex("\\.exe$")
                .maxFileSize(5L)
                .denyDeleteTag(true)
                .memberCheck(true)
                .preventSecrets(true)
                .commitCommitterCheck(true)
                .commitCommitterNameCheck(true)
                .rejectUnsignedCommits(true)
                .rejectNonDcoCommits(true)
                .build();
    }

    static GitLabGroupPushRule groupRule(final long id) {
        return GitLabGroupPushRuleBuilder.builder().id(id).build();
    }

    static GitLabGroupPushRule fullGroupRule(final long id) {
        return GitLabGroupPushRuleBuilder.builder()
                .id(id)
                .createdAt(CREATED)
                .commitMessageRegex("^JIRA-\\d+")
                .commitMessageNegativeRegex("WIP")
                .branchNameRegex("^feature/")
                .authorEmailRegex("@example\\.test$")
                .fileNameRegex("\\.exe$")
                .maxFileSize(5L)
                .denyDeleteTag(true)
                .memberCheck(true)
                .preventSecrets(true)
                .commitCommitterCheck(true)
                .commitCommitterNameCheck(true)
                .rejectUnsignedCommits(true)
                .rejectNonDcoCommits(true)
                .build();
    }
}
