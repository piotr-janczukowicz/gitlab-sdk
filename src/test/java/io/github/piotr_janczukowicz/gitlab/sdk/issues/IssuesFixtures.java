package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabelBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNamespaceBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNamespaceBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalogBuilder;

/** Builds the core records for the tests of the issue, note and label objects. */
final class IssuesFixtures {

    private IssuesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabIssue issue(final long iid, final String title, final String state) {
        return GitLabIssueBuilder.builder().iid(iid).title(title).state(state).build();
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
