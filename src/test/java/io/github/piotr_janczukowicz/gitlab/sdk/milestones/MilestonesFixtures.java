package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssueBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssueBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestoneBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import org.jspecify.annotations.Nullable;

/** Builds the core records for the tests of the milestone objects. */
final class MilestonesFixtures {

    private MilestonesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabMilestone milestone(final long id, final String title) {
        return GitLabMilestoneBuilder.builder()
                .id(id)
                .iid(id + 100)
                .title(title)
                .state("active")
                .build();
    }

    static GitLabIssueBasic issueBasic(final long iid, final @Nullable Long projectId) {
        return GitLabIssueBasicBuilder.builder().iid(iid).projectId(projectId).build();
    }

    static GitLabIssue issue(final long iid) {
        return GitLabIssueBuilder.builder()
                .iid(iid)
                .title("i" + iid)
                .state("opened")
                .build();
    }

    static GitLabMergeRequestBasic mergeRequestBasic(final long iid, final @Nullable Long projectId) {
        return GitLabMergeRequestBasicBuilder.builder()
                .iid(iid)
                .projectId(projectId)
                .build();
    }

    static GitLabMergeRequest mergeRequest(final long iid) {
        return GitLabMergeRequestBuilder.builder()
                .iid(iid)
                .title("m" + iid)
                .state("opened")
                .build();
    }
}
