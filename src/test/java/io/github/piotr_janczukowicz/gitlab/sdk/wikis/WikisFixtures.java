package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachmentBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBuilder;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.nio.charset.StandardCharsets;

/** Builds the core records for the tests of the wiki objects. */
final class WikisFixtures {

    static final String CONTENT = "# Home\n\nWelcome";

    private WikisFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * The title of a fixture page: the last segment of the slug. How GitLab titles a nested page is unverified, so no
     * test relies on the title holding the full path.
     */
    static String titleOf(final String slug) {
        return slug.substring(slug.lastIndexOf('/') + 1);
    }

    static GitLabWikiPageBasic listed(final String slug) {
        return GitLabWikiPageBasicBuilder.builder()
                .slug(slug)
                .title(titleOf(slug))
                .format("markdown")
                .build();
    }

    static GitLabWikiPage page(final String slug) {
        return GitLabWikiPageBuilder.builder()
                .slug(slug)
                .title(titleOf(slug))
                .format("markdown")
                .content(CONTENT)
                .encoding("UTF-8")
                .build();
    }

    static GitLabWikiPage bare(final String slug) {
        return GitLabWikiPageBuilder.builder().slug(slug).title(titleOf(slug)).build();
    }

    static GitLabWikiAttachment attachment() {
        return GitLabWikiAttachmentBuilder.builder()
                .fileName("dk.png")
                .filePath("uploads/6a061c4cf9f1c28cb22c384b4b8d4e3c/dk.png")
                .branch("main")
                .build();
    }

    static String bodyOf(final MultipartFormData form) {
        return form.body().mapBytes(bytes -> new String(bytes, StandardCharsets.UTF_8), () -> "");
    }
}
