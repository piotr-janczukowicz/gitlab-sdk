package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBasic;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class WikisJsonTest {

    private static final String ID = "my-group/my-project";

    /**
     * The page of the REST docs ({@code GET /projects/:id/wikis/:slug}), verbatim in its first five properties; the
     * {@code wiki_page_meta_id} and the {@code front_matter} object (with a null, an array and a nested object) are
     * additions, because the docs example has neither.
     */
    private static final String PAGE = """
            {
              "content" : "home page",
              "format" : "markdown",
              "slug" : "home",
              "title" : "home",
              "encoding": "UTF-8",
              "wiki_page_meta_id": 21,
              "front_matter": {
                "title": "home",
                "draft": null,
                "tags": ["a", "b"],
                "meta": {"level": 2}
              }
            }""";

    /**
     * The list of the REST docs ({@code GET /projects/:id/wikis}, shaped with {@code with_content=1}), verbatim in its
     * three pages; the fourth page, without content and with a slash in its slug, is an addition.
     */
    private static final String LIST = """
            [
              {
                "content" : "Here is an instruction how to deploy this project.",
                "format" : "markdown",
                "slug" : "deploy",
                "title" : "deploy",
                "encoding": "UTF-8"
              },
              {
                "content" : "Our development process is described here.",
                "format" : "markdown",
                "slug" : "development",
                "title" : "development",
                "encoding": "UTF-8"
              },
              {
                "content" : "*  [Deploy](deploy)\\n*  [Development](development)",
                "format" : "markdown",
                "slug" : "home",
                "title" : "home",
                "encoding": "UTF-8"
              },
              {
                "format" : "rdoc",
                "slug" : "dir/page",
                "title" : "dir/page",
                "wiki_page_meta_id": 22
              }
            ]""";

    /** The attachment of the REST docs ({@code POST /projects/:id/wikis/attachments}), verbatim. */
    private static final String ATTACHMENT = """
            {
              "file_name" : "dk.png",
              "file_path" : "uploads/6a061c4cf9f1c28cb22c384b4b8d4e3c/dk.png",
              "branch" : "main",
              "link" : {
                "url" : "uploads/6a061c4cf9f1c28cb22c384b4b8d4e3c/dk.png",
                "markdown" : "![A description of the attachment](uploads/6a061c4cf9f1c28cb22c384b4b8d4e3c/dk.png)"
              }
            }""";

    private static <T> T decode(final String json, final Class<T> type) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), type);
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenDocsExample() {
        final GitLabWikiPage response = decode(PAGE, GitLabWikiPage.class);
        final WikiPage sut = new WikiPage(
                GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)).project(ID).wiki(), response);

        assertSoftly(softly -> {
            softly.assertThat(sut.slug()).isEqualTo("home");
            softly.assertThat(sut.title()).isEqualTo("home");
            softly.assertThat(sut.content()).contains("home page");
            softly.assertThat(sut.format()).contains(WikiFormat.MARKDOWN);
            softly.assertThat(sut.encoding()).contains("UTF-8");
            softly.assertThat(sut.response().wikiPageMetaId()).isEqualTo(21L);
            softly.assertThat(sut.frontMatter()).containsEntry("title", "home");
            softly.assertThat(sut.frontMatter()).containsEntry("draft", null);
            softly.assertThat(sut.frontMatter()).containsEntry("tags", List.of("a", "b"));
            softly.assertThat(sut.frontMatter()).containsEntry("meta", Map.of("level", 2));
        });
    }

    @Test
    void all_shouldReadRealShapedList_whenDocsExample() throws IOException {
        final List<GitLabWikiPageBasic> listed = List.of(decode(LIST, GitLabWikiPageBasic[].class));
        final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitLab.projectwikis().list(ID)).thenReturn(listed);
        when(gitLab.projectwikis().get(ID, "deploy")).thenReturn(Optional.empty());
        when(gitLab.projectwikis().get(ID, "development")).thenReturn(Optional.empty());
        when(gitLab.projectwikis().get(ID, "home")).thenReturn(Optional.of(decode(PAGE, GitLabWikiPage.class)));
        when(gitLab.projectwikis().get(ID, "dir/page")).thenReturn(Optional.empty());

        final List<WikiPage> pages =
                GitLabSdk.of(gitLab).project(ID).wiki().all().toList();

        assertSoftly(softly -> {
            softly.assertThat(listed)
                    .extracting(GitLabWikiPageBasic::slug)
                    .containsExactly("deploy", "development", "home", "dir/page");
            softly.assertThat(listed.getFirst().format()).isEqualTo("markdown");
            softly.assertThat(listed.getLast().wikiPageMetaId()).isEqualTo(22L);
            softly.assertThat(pages).extracting(WikiPage::slug).containsExactly("home");
        });
    }

    @Test
    void attachment_shouldDecodeRealShapedRecord_whenDocsExample() {
        final GitLabWikiAttachment decoded = decode(ATTACHMENT, GitLabWikiAttachment.class);

        assertSoftly(softly -> {
            softly.assertThat(decoded.fileName()).isEqualTo("dk.png");
            softly.assertThat(decoded.filePath()).isEqualTo("uploads/6a061c4cf9f1c28cb22c384b4b8d4e3c/dk.png");
            softly.assertThat(decoded.branch()).isEqualTo("main");
            softly.assertThat(decoded.link().markdown())
                    .isEqualTo("![A description of the attachment](uploads/6a061c4cf9f1c28cb22c384b4b8d4e3c/dk.png)");
        });
    }
}
