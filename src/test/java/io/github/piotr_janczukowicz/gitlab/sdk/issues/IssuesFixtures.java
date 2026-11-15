package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabelBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStats;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStatsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNamespaceBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNamespaceBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLink;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabRelatedIssue;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabRelatedIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalogBuilder;
import org.jspecify.annotations.Nullable;

/** Builds the core records for the tests of the issue, note and label objects. */
final class IssuesFixtures {

    private IssuesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabIssue issue(final long iid, final String title, final String state) {
        return GitLabIssueBuilder.builder().iid(iid).title(title).state(state).build();
    }

    static GitLabIssuableTimeStats timeStats(final int estimate, final int spent) {
        return GitLabIssuableTimeStatsBuilder.builder()
                .timeEstimate(estimate)
                .totalTimeSpent(spent)
                .humanTimeEstimate("1h")
                .humanTotalTimeSpent("30m")
                .build();
    }

    static GitLabIssueLink issueLink(final long id, final String linkType) {
        return GitLabIssueLinkBuilder.builder().id(id).linkType(linkType).build();
    }

    static GitLabRelatedIssue relatedIssue(final @Nullable Long linkId) {
        return GitLabRelatedIssueBuilder.builder().issueLinkId(linkId).build();
    }

    static GitLabRelatedIssue relatedIssue(final @Nullable Long linkId, final long iid, final long projectId) {
        return GitLabRelatedIssueBuilder.builder()
                .issueLinkId(linkId)
                .iid(iid)
                .projectId(projectId)
                .build();
    }

    static GitLabUserBasic user(final long id) {
        return GitLabUserBasicBuilder.builder().id(id).build();
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

    static GitLabNote note(final long id, final String body) {
        return GitLabNoteBuilder.builder().id(id).body(body).build();
    }

    static GitLabProjectLabel label(final String name, final String color) {
        return GitLabProjectLabelBuilder.builder().name(name).color(color).build();
    }

    static GitLabGroupLabel groupLabel(final String name, final String color) {
        return GitLabGroupLabelBuilder.builder().name(name).color(color).build();
    }

    static GitLabProjectWithAccessAndCatalog projectIn(final String kind, final Long namespaceId, final String path) {
        final GitLabNamespaceBasic namespace = GitLabNamespaceBasicBuilder.builder()
                .kind(kind)
                .id(namespaceId)
                .fullPath(path)
                .build();
        return GitLabProjectWithAccessAndCatalogBuilder.builder()
                .namespace(namespace)
                .build();
    }

    static GitLabProjectWithAccessAndCatalog projectWithoutNamespace() {
        return GitLabProjectWithAccessAndCatalogBuilder.builder().build();
    }
}
