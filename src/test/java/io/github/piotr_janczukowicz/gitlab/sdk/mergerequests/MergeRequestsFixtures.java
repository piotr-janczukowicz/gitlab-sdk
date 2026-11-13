package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestApprovals;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestApprovalsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;

/** Builds the core records for the tests of the merge request, note and approval objects. */
final class MergeRequestsFixtures {

    private MergeRequestsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabMergeRequest mergeRequest(final long iid, final String title, final String state) {
        return GitLabMergeRequestBuilder.builder()
                .iid(iid)
                .title(title)
                .state(state)
                .build();
    }

    static GitLabMergeRequestBasic basic(final long iid) {
        return GitLabMergeRequestBasicBuilder.builder().iid(iid).title("t").build();
    }

    static GitLabMergeRequestApprovals approvals(final boolean approved, final int left) {
        return GitLabMergeRequestApprovalsBuilder.builder()
                .approved(approved)
                .approvalsLeft(left)
                .build();
    }

    static GitLabNote note(final long id, final String body) {
        return GitLabNoteBuilder.builder().id(id).body(body).build();
    }
}
