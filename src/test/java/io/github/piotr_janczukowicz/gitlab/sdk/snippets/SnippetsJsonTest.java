package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabSnippet;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippet;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class SnippetsJsonTest {

    private static final String ID = "my-group/my-project";

    /**
     * A legacy single-file project snippet, shaped after the REST docs example of {@code GET
     * /projects/:id/snippets/:snippet_id} (values after the docs example, the id changed: title {@code test},
     * {@code file_name} {@code add.rb}, no {@code files} and no {@code visibility}). The page could only be read as a
     * summary, so the values follow the docs but the text is not a verbatim copy.
     */
    private static final String LEGACY = """
            {
              "id": 2,
              "title": "test",
              "file_name": "add.rb",
              "description": "Ruby test snippet",
              "author": {
                "id": 1,
                "username": "john_smith",
                "email": "john@example.com",
                "name": "John Smith",
                "state": "active",
                "created_at": "2012-05-23T08:00:58Z"
              },
              "updated_at": "2012-06-28T10:52:04Z",
              "created_at": "2012-06-28T10:52:04Z",
              "imported": false,
              "imported_from": "none",
              "project_id": 1,
              "web_url": "http://example.com/example/example/snippets/2",
              "raw_url": "http://example.com/example/example/snippets/2/raw",
              "ssh_url_to_repo": "ssh://git@gitlab.example.com:example/example/snippets/2.git",
              "http_url_to_repo": "https://gitlab.example.com/example/example/snippets/2.git"
            }""";

    /**
     * A multi-file snippet with {@code files: [{"path", "raw_url"}]}: the same JSON the core's decode test
     * ({@code WikisSnippetsDependenciesAuditResponsesDecodingTest.SNIPPET} in gitlab-rest-client) uses, shaped after
     * the REST docs examples of a single snippet and of a created personal snippet.
     */
    private static final String WITH_FILES = """
            {"id": 1, "title": "test", "file_name": "add.rb", "description": "Ruby test snippet",
             "visibility": "private", "imported": false, "imported_from": "none", "project_id": 3,
             "author": {"id": 1, "username": "john_smith", "name": "John Smith", "state": "active",
                        "avatar_url": "http://example.com/avatar.png", "web_url": "http://example.com/john_smith"},
             "updated_at": "2026-11-13T10:00:00.000Z", "created_at": "2026-11-13T09:00:00.000+01:00",
             "web_url": "http://example.com/example/example/snippets/1",
             "raw_url": "http://example.com/example/example/snippets/1/raw",
             "ssh_url_to_repo": "ssh://git@example.com/snippets/1.git",
             "http_url_to_repo": "https://example.com/snippets/1.git",
             "files": [{"path": "add.rb", "raw_url": "https://example.com/-/snippets/1/raw/main/add.rb"}]}""";

    /**
     * The list of the signed-in user's snippets, shaped after the REST docs example of {@code GET /snippets} (snippets
     * 42 and 41 by {@code user0}, one with a {@code project_id}); the {@code files} entry of 42 is an addition.
     */
    private static final String LIST = """
            [
              {
                "id": 42,
                "title": "Voluptatem iure ut qui aut et consequatur quaerat.",
                "file_name": "mclaughlin.rb",
                "description": null,
                "visibility": "internal",
                "author": {"id": 22, "name": "User 0", "username": "user0", "state": "active"},
                "updated_at": "2018-09-18T01:12:26.383Z",
                "created_at": "2018-09-18T01:12:26.383Z",
                "project_id": null,
                "web_url": "http://example.com/snippets/42",
                "raw_url": "http://example.com/snippets/42/raw",
                "files": [{"path": "mclaughlin.rb", "raw_url": "http://example.com/-/snippets/42/raw/main/mclaughlin.rb"}]
              },
              {
                "id": 41,
                "title": "Ut praesentium non et atque.",
                "file_name": "ondrickaemard.rb",
                "description": null,
                "visibility": "internal",
                "author": {"id": 22, "name": "User 0", "username": "user0", "state": "active"},
                "updated_at": "2018-09-18T01:12:26.360Z",
                "created_at": "2018-09-18T01:12:26.360Z",
                "project_id": 1,
                "web_url": "http://example.com/gitlab-org/gitlab-test/snippets/41",
                "raw_url": "http://example.com/gitlab-org/gitlab-test/snippets/41/raw"
              }
            ]""";

    private static <T> T decode(final String json, final Class<T> type) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), type);
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenDocsExample() {
        final ProjectSnippets snippets =
                GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)).project(ID).snippets();
        final Snippet legacy = new Snippet(snippets, decode(LEGACY, GitLabProjectSnippet.class));
        final Snippet withFiles = new Snippet(snippets, decode(WITH_FILES, GitLabProjectSnippet.class));

        assertSoftly(softly -> {
            softly.assertThat(legacy.id()).isEqualTo(2L);
            softly.assertThat(legacy.title()).isEqualTo("test");
            softly.assertThat(legacy.description()).contains("Ruby test snippet");
            softly.assertThat(legacy.visibility()).isEmpty();
            softly.assertThat(legacy.author().map(GitLabUserBasic::username)).contains("john_smith");
            softly.assertThat(legacy.webUrl()).contains("http://example.com/example/example/snippets/2");
            softly.assertThat(legacy.files()).isEmpty();
            softly.assertThat(legacy.response().fileName()).isEqualTo("add.rb");
            softly.assertThat(legacy.response().createdAt()).isEqualTo(Instant.parse("2012-06-28T10:52:04Z"));
            softly.assertThat(withFiles.visibility()).contains(SnippetVisibility.PRIVATE);
            softly.assertThat(withFiles.files()).extracting(SnippetFile::path).containsExactly("add.rb");
            softly.assertThat(withFiles.files().getFirst().rawUrl())
                    .contains("https://example.com/-/snippets/1/raw/main/add.rb");
        });
    }

    @Test
    void accessors_shouldReadRealShapedPersonalJson_whenDocsExample() {
        final PersonalSnippet sut = new PersonalSnippet(
                GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)).snippets(),
                decode(WITH_FILES, GitLabPersonalSnippet.class));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(1L);
            softly.assertThat(sut.visibilityName()).contains("private");
            softly.assertThat(sut.files()).extracting(SnippetFile::path).containsExactly("add.rb");
        });
    }

    @Test
    void all_shouldReadRealShapedList_whenDocsExample() throws IOException {
        final List<GitLabSnippet> listed = Arrays.asList(decode(LIST, GitLabSnippet[].class));
        final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitLab.codesnippets().list().stream()).thenReturn(listed.stream());

        final List<PersonalSnippet> snippets =
                GitLabSdk.of(gitLab).snippets().all().toList();

        assertSoftly(softly -> {
            softly.assertThat(snippets).extracting(PersonalSnippet::id).containsExactly(42L, 41L);
            softly.assertThat(snippets.getFirst().visibility()).contains(SnippetVisibility.INTERNAL);
            softly.assertThat(snippets.getFirst().description()).isEmpty();
            softly.assertThat(snippets.getFirst().files())
                    .extracting(SnippetFile::path)
                    .containsExactly("mclaughlin.rb");
            softly.assertThat(snippets.getLast().files()).isEmpty();
            softly.assertThat(snippets.getLast().response().projectId()).isEqualTo(1L);
        });
    }
}
