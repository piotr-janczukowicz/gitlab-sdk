package io.github.piotr_janczukowicz.gitlab.sdk.reactions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmoji;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmojiBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import java.io.IOException;
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

    /**
     * Verifies that the mocked client made none of the four create calls except the allowed one.
     *
     * @param gitLab the mocked client
     * @param allowed the name of the create method that may have been called, or an empty string for none
     * @throws IOException never; declared by the mocked calls
     */
    static void verifyNoCreateExcept(final GitLab gitLab, final String allowed) throws IOException {
        if (!"createIssuesAwardEmoji".equals(allowed)) {
            verify(gitLab.emojireactions(), never()).createIssuesAwardEmoji(any(), anyLong(), any());
        }
        if (!"createIssuesNotesAwardEmoji".equals(allowed)) {
            verify(gitLab.emojireactions(), never()).createIssuesNotesAwardEmoji(any(), anyLong(), anyLong(), any());
        }
        if (!"createRequestsAwardEmoji".equals(allowed)) {
            verify(gitLab.emojireactions(), never()).createRequestsAwardEmoji(any(), anyLong(), any());
        }
        if (!"createRequestsNotesAwardEmoji".equals(allowed)) {
            verify(gitLab.emojireactions(), never()).createRequestsNotesAwardEmoji(any(), anyLong(), anyLong(), any());
        }
    }
}
