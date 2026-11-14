package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.RAW_URL;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.author;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.bareProjectSnippet;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.projectSnippet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippet;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippetBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SnippetTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final ProjectSnippets snippets = project.snippets();

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new Snippet(null, projectSnippet(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new Snippet(snippets, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void accessors_shouldReadResponse_whenFieldsArePresent() {
        final GitLabProjectSnippet response = projectSnippet(5L);

        final Snippet sut = new Snippet(snippets, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(5L);
            softly.assertThat(sut.title()).isEqualTo("test 5");
            softly.assertThat(sut.description()).contains("Ruby test snippet");
            softly.assertThat(sut.visibility()).contains(SnippetVisibility.PRIVATE);
            softly.assertThat(sut.visibilityName()).contains("private");
            softly.assertThat(sut.author()).contains(author());
            softly.assertThat(sut.webUrl()).contains("http://example.com/example/example/snippets/5");
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenFieldsAreAbsent() {
        final Snippet sut = new Snippet(snippets, bareProjectSnippet(5L));

        assertSoftly(softly -> {
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.visibility()).isEmpty();
            softly.assertThat(sut.visibilityName()).isEmpty();
            softly.assertThat(sut.author()).isEmpty();
            softly.assertThat(sut.webUrl()).isEmpty();
        });
    }

    @Test
    void visibility_shouldThrowIllegalArgumentException_whenUnknown() {
        final Snippet sut = new Snippet(
                snippets,
                GitLabProjectSnippetBuilder.builder()
                        .id(5L)
                        .title("t")
                        .visibility("secret")
                        .build());

        final Throwable thrown = catchThrowable(sut::visibility);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.visibilityName()).contains("secret");
        });
    }

    @Test
    void files_shouldMapPathAndRawUrl_whenPresent() {
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        final List<SnippetFile> files = sut.files();

        assertSoftly(softly -> {
            softly.assertThat(files).extracting(SnippetFile::path).containsExactly("add.rb");
            softly.assertThat(files.getFirst().rawUrl()).contains(RAW_URL);
        });
    }

    @Test
    void files_shouldBeEmpty_whenAbsent() {
        final Snippet sut = new Snippet(snippets, bareProjectSnippet(5L));

        assertThat(sut.files()).isEmpty();
    }

    @Test
    void files_shouldSkipEntry_whenPathMissing() {
        final Map<String, Object> withNullPath = new HashMap<>();
        withNullPath.put("path", null);
        final Snippet sut = new Snippet(
                snippets,
                GitLabProjectSnippetBuilder.builder()
                        .id(5L)
                        .title("t")
                        .files(List.of(
                                Map.of("raw_url", RAW_URL),
                                Map.of("path", 12),
                                withNullPath,
                                Map.of("path", "kept.rb", "raw_url", 3)))
                        .build());

        final List<SnippetFile> files = sut.files();

        assertSoftly(softly -> {
            softly.assertThat(files).extracting(SnippetFile::path).containsExactly("kept.rb");
            softly.assertThat(files.getFirst().rawUrl()).isEmpty();
        });
    }

    @Test
    void content_shouldReturnCoreText() throws IOException {
        when(gitLab.projectsnippets().getRaw(ID, 5L)).thenReturn(CONTENT);
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        final String result = sut.content();

        assertThat(result).isEqualTo(CONTENT);
        verify(gitLab.projectsnippets(), times(1)).getRaw(ID, 5L);
    }

    @Test
    void fileContent_shouldPassRefAndPathUnchanged_whenPathHasSlash() throws IOException {
        when(gitLab.projectsnippets().getFilesRaw(ID, 5L, "feature/x", "dir/add.rb"))
                .thenReturn(CONTENT);
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        final String result = sut.fileContent("feature/x", "dir/add.rb");

        assertThat(result).isEqualTo(CONTENT);
        verify(gitLab.projectsnippets(), times(1)).getFilesRaw(ID, 5L, "feature/x", "dir/add.rb");
    }

    @Test
    void fileContent_shouldRejectBlankArguments_withoutCoreCall() throws IOException {
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.fileContent(" ", "add.rb")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.fileContent("main", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
        });
        verify(gitLab.projectsnippets(), never()).getFilesRaw(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void refresh_shouldReturnReloadedSnippet_whenStillPresent() throws IOException {
        final GitLabProjectSnippet reloaded = projectSnippet(5L);
        when(gitLab.projectsnippets().get(ID, 5L)).thenReturn(Optional.of(reloaded));
        final Snippet sut = new Snippet(snippets, bareProjectSnippet(5L));

        final Snippet result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(result.project()).isSameAs(project);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projectsnippets().get(ID, 5L)).thenReturn(Optional.empty());
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void delete_shouldCallCoreWithId() throws IOException {
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        sut.delete();

        verify(gitLab.projectsnippets(), times(1)).delete(ID, 5L);
    }

    @Test
    void update_shouldTargetThisSnippet_whenSent() throws IOException {
        when(gitLab.projectsnippets().update(eq(ID), eq(5L), any())).thenReturn(projectSnippet(5L));
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));

        sut.update().title("x").send();

        verify(gitLab.projectsnippets(), times(1)).update(eq(ID), eq(5L), any());
    }

    @Test
    void toString_shouldNotContainContent_whenObjectPrinted() throws IOException {
        when(gitLab.projectsnippets().getRaw(ID, 5L)).thenReturn(CONTENT);
        final Snippet sut = new Snippet(snippets, projectSnippet(5L));
        sut.content();

        final String printed = sut + " " + sut.files() + " " + snippets;

        assertSoftly(softly -> {
            softly.assertThat(printed).doesNotContain(CONTENT);
            softly.assertThat(printed).doesNotContain("Ruby test snippet");
        });
    }
}
