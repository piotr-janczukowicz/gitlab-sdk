package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNote;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelBuilder;

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
}
