package io.github.piotr_janczukowicz.gitlab.sdk.badges;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBadge;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBadgeBuilder;

/** Builds the core records for the tests of the badge objects. */
final class BadgesFixtures {

    static final String LINK = "https://example.test/pipelines/%{project_path}";
    static final String IMAGE = "https://example.test/badge.svg";

    private BadgesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabBadge badge(final long id) {
        return GitLabBadgeBuilder.builder().id(id).linkUrl(LINK).imageUrl(IMAGE).build();
    }

    static GitLabBadge namedBadge(final long id, final String name) {
        return GitLabBadgeBuilder.builder()
                .id(id)
                .name(name)
                .linkUrl(LINK)
                .imageUrl(IMAGE)
                .renderedLinkUrl("https://example.test/pipelines/demo")
                .renderedImageUrl("https://example.test/rendered.svg")
                .kind("project")
                .build();
    }
}
