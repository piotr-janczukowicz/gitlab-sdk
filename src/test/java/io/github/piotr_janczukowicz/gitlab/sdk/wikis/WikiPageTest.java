package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.bare;
import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.page;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisUpdateFormat;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class WikiPageTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Wiki wiki = GitLabSdk.of(gitLab).project(ID).wiki();

    @Test
    void accessors_shouldReturnResponseValues_whenPageIsLoaded() {
        final GitLabWikiPage response = page("dir/page");

        final WikiPage sut = new WikiPage(wiki, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.slug()).isEqualTo("dir/page");
            softly.assertThat(sut.title()).isEqualTo("dir/page");
            softly.assertThat(sut.content()).contains(CONTENT);
            softly.assertThat(sut.format()).contains(WikiFormat.MARKDOWN);
            softly.assertThat(sut.formatName()).contains("markdown");
            softly.assertThat(sut.encoding()).contains("UTF-8");
            softly.assertThat(sut.wiki()).isSameAs(wiki);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenGitLabSentNoValue() {
        final WikiPage sut = new WikiPage(wiki, bare("home"));

        assertSoftly(softly -> {
            softly.assertThat(sut.content()).isEmpty();
            softly.assertThat(sut.format()).isEmpty();
            softly.assertThat(sut.formatName()).isEmpty();
            softly.assertThat(sut.encoding()).isEmpty();
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        final Throwable nullWiki = catchThrowable(() -> new WikiPage(null, page("home")));
        final Throwable nullResponse = catchThrowable(() -> new WikiPage(wiki, null));

        assertSoftly(softly -> {
            softly.assertThat(nullWiki).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullResponse).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void format_shouldThrowIllegalArgumentException_whenUnknown() {
        final WikiPage sut = new WikiPage(
                wiki,
                GitLabWikiPageBuilder.builder(page("home")).format("textile").build());

        final Throwable thrown = catchThrowable(sut::format);

        assertSoftly(softly -> {
            softly.assertThat(thrown)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("textile");
            softly.assertThat(sut.formatName()).contains("textile");
        });
    }

    @Test
    void frontMatter_shouldBeImmutableAndEmpty_whenAbsent() {
        final WikiPage sut = new WikiPage(wiki, page("home"));

        final Map<String, Object> frontMatter = sut.frontMatter();
        final Throwable thrown = catchThrowable(() -> frontMatter.put("title", "x"));

        assertSoftly(softly -> {
            softly.assertThat(frontMatter).isEmpty();
            softly.assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void frontMatter_shouldBeImmutableCopy_whenPresent() {
        final Map<String, Object> nested = new HashMap<>();
        nested.put("level", 2);
        final List<Object> tags = new ArrayList<>(List.of("a"));
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("title", "Home");
        source.put("draft", null);
        source.put("meta", nested);
        source.put("tags", tags);
        final WikiPage sut = new WikiPage(
                wiki,
                GitLabWikiPageBuilder.builder(page("home")).frontMatter(source).build());

        final Map<String, Object> frontMatter = sut.frontMatter();
        source.put("later", "x");
        nested.put("later", "x");
        tags.add("b");

        assertSoftly(softly -> {
            softly.assertThat(frontMatter).containsOnlyKeys("title", "draft", "meta", "tags");
            softly.assertThat(frontMatter).containsEntry("draft", null);
            softly.assertThat(frontMatter.get("meta")).isEqualTo(Map.of("level", 2));
            softly.assertThat(frontMatter.get("tags")).isEqualTo(List.of("a"));
            softly.assertThat(catchThrowable(() -> frontMatter.remove("title")))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(() -> ((Map<?, ?>) frontMatter.get("meta")).clear()))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThat(catchThrowable(() -> ((List<?>) frontMatter.get("tags")).clear()))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void refresh_shouldReturnLoadedPage_whenGitLabHasIt() throws IOException {
        final GitLabWikiPage reloaded = page("home");
        when(gitLab.projectwikis().get(ID, "home")).thenReturn(Optional.of(reloaded));

        final WikiPage result = new WikiPage(wiki, bare("home")).refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.wiki()).isSameAs(wiki);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projectwikis().get(ID, "home")).thenReturn(Optional.empty());
        final WikiPage sut = new WikiPage(wiki, page("home"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("home");
    }

    @Test
    void update_shouldSendOnlySetFields() throws IOException {
        final GitLabProjectProjectWikisUpdatePayload expected = GitLabProjectProjectWikisUpdatePayloadBuilder.builder()
                .content("new text")
                .build();
        when(gitLab.projectwikis().update(ID, "home", expected)).thenReturn(page("home"));

        final WikiPage result =
                new WikiPage(wiki, page("home")).update().content("new text").send();

        assertThat(result.slug()).isEqualTo("home");
        verify(gitLab.projectwikis()).update(ID, "home", expected);
    }

    @Test
    void update_shouldSendFormat_whenSet() throws IOException {
        final GitLabProjectProjectWikisUpdatePayload expected = GitLabProjectProjectWikisUpdatePayloadBuilder.builder()
                .content("= Title")
                .format(GitLabProjectProjectWikisUpdateFormat.ASCIIDOC)
                .build();
        when(gitLab.projectwikis().update(ID, "home", expected)).thenReturn(page("home"));

        new WikiPage(wiki, page("home"))
                .update()
                .content("= Title")
                .format(WikiFormat.ASCIIDOC)
                .send();

        verify(gitLab.projectwikis()).update(ID, "home", expected);
    }

    @Test
    void update_shouldTargetOldSlugAndReturnNewSlug_whenTitleChanges() throws IOException {
        final GitLabProjectProjectWikisUpdatePayload expected = GitLabProjectProjectWikisUpdatePayloadBuilder.builder()
                .title("dir/renamed")
                .build();
        when(gitLab.projectwikis().update(ID, "dir/page", expected)).thenReturn(page("dir/renamed"));
        final WikiPage sut = new WikiPage(wiki, page("dir/page"));

        final WikiPage result = sut.update().title("dir/renamed").send();

        assertSoftly(softly -> {
            softly.assertThat(result.slug()).isEqualTo("dir/renamed");
            softly.assertThat(result.wiki()).isSameAs(wiki);
            softly.assertThat(sut.slug()).isEqualTo("dir/page");
        });
        verify(gitLab.projectwikis()).update(ID, "dir/page", expected);
    }

    @Test
    void delete_shouldCallCoreWithSlug() throws IOException {
        new WikiPage(wiki, page("dir/page")).delete();

        verify(gitLab.projectwikis()).delete(ID, "dir/page");
    }
}
