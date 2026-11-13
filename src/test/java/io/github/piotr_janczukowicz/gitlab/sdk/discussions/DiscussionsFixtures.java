package io.github.piotr_janczukowicz.gitlab.sdk.discussions;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussion;
import io.github.piotr_janczukowicz.gitlab.api.discussions.GitLabDiscussionBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import java.util.List;

/** Builds the core records for the tests of the discussion objects. */
final class DiscussionsFixtures {

    private DiscussionsFixtures() {
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

    static GitLabNote note(final long id, final String body) {
        return GitLabNoteBuilder.builder().id(id).body(body).build();
    }

    static GitLabDiscussion discussion(final String id, final GitLabNote... notes) {
        return GitLabDiscussionBuilder.builder()
                .id(id)
                .individualNote(false)
                .resolvable(true)
                .resolved(false)
                .notes(List.of(notes))
                .build();
    }
}
