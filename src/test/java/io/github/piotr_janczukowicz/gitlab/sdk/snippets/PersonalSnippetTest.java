package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.RAW_URL;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.author;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.barePersonalSnippet;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.personalSnippet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippetBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PersonalSnippetTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final PersonalSnippets snippets = GitLabSdk.of(gitLab).snippets();

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new PersonalSnippet(null, personalSnippet(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new PersonalSnippet(snippets, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void accessors_shouldReadResponse_whenFieldsArePresent() {
        final GitLabPersonalSnippet response = personalSnippet(42L);

        final PersonalSnippet sut = new PersonalSnippet(snippets, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(42L);
            softly.assertThat(sut.title()).isEqualTo("test 42");
            softly.assertThat(sut.description()).contains("Ruby test snippet");
            softly.assertThat(sut.visibility()).contains(SnippetVisibility.PRIVATE);
            softly.assertThat(sut.visibilityName()).contains("private");
            softly.assertThat(sut.author()).contains(author());
            softly.assertThat(sut.webUrl()).contains("http://example.com/snippets/42");
            softly.assertThat(sut.files()).extracting(SnippetFile::path).containsExactly("add.rb");
            softly.assertThat(sut.files().getFirst().rawUrl()).contains(RAW_URL);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenFieldsAreAbsent() {
        final PersonalSnippet sut = new PersonalSnippet(snippets, barePersonalSnippet(42L));

        assertSoftly(softly -> {
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.visibility()).isEmpty();
            softly.assertThat(sut.visibilityName()).isEmpty();
            softly.assertThat(sut.author()).isEmpty();
            softly.assertThat(sut.webUrl()).isEmpty();
            softly.assertThat(sut.files()).isEmpty();
        });
    }

    @Test
    void visibility_shouldThrowIllegalArgumentException_whenUnknown() {
        final PersonalSnippet sut = new PersonalSnippet(
                snippets,
                GitLabPersonalSnippetBuilder.builder()
                        .id(42L)
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
    void files_shouldSkipEntry_whenPathMissing() {
        final PersonalSnippet sut = new PersonalSnippet(
                snippets,
                GitLabPersonalSnippetBuilder.builder()
                        .id(42L)
                        .title("t")
                        .files(List.of(Map.of("raw_url", RAW_URL), Map.of("path", "kept.rb")))
                        .build());

        assertThat(sut.files()).extracting(SnippetFile::path).containsExactly("kept.rb");
    }

    @Test
    void content_shouldReturnCoreText() throws IOException {
        when(gitLab.codesnippets().getRaw(42L)).thenReturn(CONTENT);
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));

        final String result = sut.content();

        assertThat(result).isEqualTo(CONTENT);
        verify(gitLab.codesnippets(), times(1)).getRaw(42L);
    }

    @Test
    void fileContent_shouldPassRefAndPathUnchanged_whenPathHasSlash() throws IOException {
        when(gitLab.codesnippets().getFilesRaw("42", "feature/x", "dir/add.rb")).thenReturn(CONTENT);
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));

        final String result = sut.fileContent("feature/x", "dir/add.rb");

        assertThat(result).isEqualTo(CONTENT);
        verify(gitLab.codesnippets(), times(1)).getFilesRaw("42", "feature/x", "dir/add.rb");
    }

    @Test
    void fileContent_shouldRejectBlankArguments_withoutCoreCall() throws IOException {
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.fileContent(" ", "add.rb")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.fileContent("main", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
        });
        verify(gitLab.codesnippets(), never()).getFilesRaw(anyString(), anyString(), anyString());
    }

    @Test
    void refresh_shouldReturnReloadedSnippet_whenStillPresent() throws IOException {
        final GitLabPersonalSnippet reloaded = personalSnippet(42L);
        when(gitLab.codesnippets().get(42L)).thenReturn(Optional.of(reloaded));
        final PersonalSnippet sut = new PersonalSnippet(snippets, barePersonalSnippet(42L));

        final PersonalSnippet result = sut.refresh();

        assertThat(result.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.codesnippets().get(42L)).thenReturn(Optional.empty());
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("42");
    }

    @Test
    void delete_shouldReturnVoid_whenCoreReturnsOptional() throws IOException {
        when(gitLab.codesnippets().delete(42L)).thenReturn(Optional.of(personalSnippet(42L)));
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));

        sut.delete();

        verify(gitLab.codesnippets(), times(1)).delete(42L);
    }

    @Test
    void update_shouldTargetThisSnippet_whenSent() throws IOException {
        when(gitLab.codesnippets().update(eq(42L), any())).thenReturn(personalSnippet(42L));
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));

        sut.update().title("x").send();

        verify(gitLab.codesnippets(), times(1)).update(eq(42L), any());
    }

    @Test
    void toString_shouldNotContainContent_whenObjectPrinted() throws IOException {
        when(gitLab.codesnippets().getRaw(42L)).thenReturn(CONTENT);
        final PersonalSnippet sut = new PersonalSnippet(snippets, personalSnippet(42L));
        sut.content();

        final String printed = sut + " " + sut.files() + " " + snippets;

        assertSoftly(softly -> {
            softly.assertThat(printed).doesNotContain(CONTENT);
            softly.assertThat(printed).doesNotContain("Ruby test snippet");
        });
    }
}
