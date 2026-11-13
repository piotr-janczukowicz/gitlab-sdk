package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabRelease;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabReleaseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLink;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLinkBuilder;

/** Builds the core records for the tests of the release objects. */
final class ReleasesFixtures {

    private ReleasesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabRelease release(final String tagName) {
        return GitLabReleaseBuilder.builder()
                .tagName(tagName)
                .name("Release " + tagName)
                .build();
    }

    static GitLabReleaseLink link(final long id, final String name, final String linkType) {
        return GitLabReleaseLinkBuilder.builder()
                .id(id)
                .name(name)
                .url("https://dl.example/" + name)
                .linkType(linkType)
                .build();
    }
}
