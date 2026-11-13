package io.github.piotr_janczukowicz.gitlab.sdk.reactions;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmoji;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmojiBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import java.time.Instant;

/** Builds the core records for the tests of the reaction object. */
final class ReactionsFixtures {

    private ReactionsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabIssue issue(final long iid) {
        return GitLabIssueBuilder.builder()
                .iid(iid)
                .title("Broken")
                .state("opened")
                .build();
    }

    static GitLabMergeRequest mergeRequest(final long iid) {
        return GitLabMergeRequestBuilder.builder()
                .iid(iid)
                .title("Fix")
                .state("opened")
                .build();
    }

    static GitLabNote note(final long id) {
        return GitLabNoteBuilder.builder().id(id).body("text").build();
    }

    static GitLabAwardEmoji award(final long id, final String name) {
        return GitLabAwardEmojiBuilder.builder()
                .id(id)
                .name(name)
                .user(GitLabUserBasicBuilder.builder().id(5L).username("anna").build())
                .createdAt(Instant.parse("2026-01-02T03:04:05Z"))
                .build();
    }
}
