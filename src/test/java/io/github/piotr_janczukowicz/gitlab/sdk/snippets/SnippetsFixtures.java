package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippetBuilder;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabSnippetBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippet;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippetBuilder;
import java.util.List;
import java.util.Map;

/** Builds the core records for the tests of the snippet objects. */
final class SnippetsFixtures {

    static final String CONTENT = "puts 'secret content'";
    static final String RAW_URL = "https://example.com/-/snippets/1/raw/main/add.rb";

    private SnippetsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabUserBasic author() {
        return GitLabUserBasicBuilder.builder().id(1L).username("john_smith").build();
    }

    static List<Map<String, Object>> oneFile() {
        return List.of(Map.of("path", "add.rb", "raw_url", RAW_URL));
    }

    static GitLabProjectSnippet projectSnippet(final long id) {
        return GitLabProjectSnippetBuilder.builder()
                .id(id)
                .title("test " + id)
                .description("Ruby test snippet")
                .visibility("private")
                .author(author())
                .webUrl("http://example.com/example/example/snippets/" + id)
                .files(oneFile())
                .build();
    }

    static GitLabProjectSnippet bareProjectSnippet(final long id) {
        return GitLabProjectSnippetBuilder.builder().id(id).title("test " + id).build();
    }

    static GitLabPersonalSnippet personalSnippet(final long id) {
        return GitLabPersonalSnippetBuilder.builder()
                .id(id)
                .title("test " + id)
                .description("Ruby test snippet")
                .visibility("private")
                .author(author())
                .webUrl("http://example.com/snippets/" + id)
                .files(oneFile())
                .build();
    }

    static GitLabPersonalSnippet barePersonalSnippet(final long id) {
        return GitLabPersonalSnippetBuilder.builder().id(id).title("test " + id).build();
    }

    static GitLabSnippet listedSnippet(final long id) {
        return GitLabSnippetBuilder.builder()
                .id(id)
                .title("test " + id)
                .visibility("internal")
                .files(oneFile())
                .build();
    }
}
