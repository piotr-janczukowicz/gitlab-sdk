package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.personalSnippet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateFile;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateFileAction;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateVisibility;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PersonalSnippetUpdateTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final PersonalSnippet snippet =
            new PersonalSnippet(GitLabSdk.of(gitLab).snippets(), personalSnippet(42L));
    private final ArgumentCaptor<GitLabCodeSnippetsUpdatePayload> sent =
            ArgumentCaptor.forClass(GitLabCodeSnippetsUpdatePayload.class);

    @Test
    void constructor_shouldThrowNullPointerException_whenSnippetIsNull() {
        final Throwable thrown = catchThrowable(() -> new PersonalSnippetUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() throws IOException {
        final Throwable thrown = catchThrowable(() -> snippet.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.codesnippets(), never()).update(anyLong(), any());
    }

    @Test
    void update_shouldSendOnlySetFields_whenTitleDescriptionAndVisibilitySet() throws IOException {
        when(gitLab.codesnippets().update(eq(42L), sent.capture())).thenReturn(personalSnippet(42L));

        final PersonalSnippet result = snippet.update()
                .title("renamed")
                .description("new description")
                .visibility(SnippetVisibility.PUBLIC)
                .send();

        final GitLabCodeSnippetsUpdatePayload payload = sent.getValue();
        assertSoftly(softly -> {
            softly.assertThat(payload.title()).isEqualTo("renamed");
            softly.assertThat(payload.description()).isEqualTo("new description");
            softly.assertThat(payload.visibility()).isEqualTo(GitLabCodeSnippetsUpdateVisibility.PUBLIC);
            softly.assertThat(payload.files()).isNull();
            softly.assertThat(payload.content()).isNull();
            softly.assertThat(payload.fileName()).isNull();
            softly.assertThat(result.id()).isEqualTo(42L);
        });
        verify(gitLab.codesnippets(), times(1)).update(eq(42L), any());
    }

    @Test
    void update_shouldSendFileActionsInCallOrder() throws IOException {
        when(gitLab.codesnippets().update(eq(42L), sent.capture())).thenReturn(personalSnippet(42L));

        snippet.update()
                .addFile("dir/new.rb", CONTENT)
                .moveFile("a.rb", "b.rb")
                .updateFile("add.rb", "changed")
                .deleteFile("old.rb")
                .send();

        final GitLabCodeSnippetsUpdatePayload payload = sent.getValue();
        assertSoftly(softly -> {
            softly.assertThat(payload.files())
                    .containsExactly(
                            new GitLabCodeSnippetsUpdateFile(
                                    GitLabCodeSnippetsUpdateFileAction.CREATE, CONTENT, "dir/new.rb", null),
                            new GitLabCodeSnippetsUpdateFile(
                                    GitLabCodeSnippetsUpdateFileAction.MOVE, null, "b.rb", "a.rb"),
                            new GitLabCodeSnippetsUpdateFile(
                                    GitLabCodeSnippetsUpdateFileAction.UPDATE, "changed", "add.rb", null),
                            new GitLabCodeSnippetsUpdateFile(
                                    GitLabCodeSnippetsUpdateFileAction.DELETE, null, "old.rb", null));
            softly.assertThat(payload.title()).isNull();
            softly.assertThat(payload.description()).isNull();
            softly.assertThat(payload.visibility()).isNull();
            softly.assertThat(payload.content()).isNull();
            softly.assertThat(payload.fileName()).isNull();
        });
    }

    @Test
    void moveFile_shouldSendPreviousPath() throws IOException {
        when(gitLab.codesnippets().update(eq(42L), sent.capture())).thenReturn(personalSnippet(42L));

        snippet.update().moveFile("dir/a.rb", "dir/b.rb").send();

        final GitLabCodeSnippetsUpdateFile file = sent.getValue().files().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(file.action()).isEqualTo(GitLabCodeSnippetsUpdateFileAction.MOVE);
            softly.assertThat(file.previousPath()).isEqualTo("dir/a.rb");
            softly.assertThat(file.filePath()).isEqualTo("dir/b.rb");
            softly.assertThat(file.content()).isNull();
        });
    }

    @Test
    void moveFile_shouldRejectBlankPreviousPath_withoutCoreCall() throws IOException {
        final PersonalSnippetUpdate sut = snippet.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.moveFile(" ", "b.rb")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.moveFile("a.rb", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(sut::send)).isInstanceOf(IllegalStateException.class);
        });
        verify(gitLab.codesnippets(), never()).update(anyLong(), any());
    }

    @Test
    void addFile_shouldRejectNullContent_withoutCoreCall() throws IOException {
        final PersonalSnippetUpdate sut = snippet.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.addFile("a.rb", null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.updateFile("a.rb", null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.deleteFile(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.title(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.visibility(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(sut::send)).isInstanceOf(IllegalStateException.class);
        });
        verify(gitLab.codesnippets(), never()).update(anyLong(), any());
    }

    @Test
    void toString_shouldNotContainContent_whenObjectPrinted() {
        final PersonalSnippetUpdate sut =
                snippet.update().updateFile("a.rb", CONTENT).description("secret description");

        final String printed = sut.toString();

        assertSoftly(softly -> {
            softly.assertThat(printed).doesNotContain(CONTENT);
            softly.assertThat(printed).doesNotContain("secret description");
        });
    }
}
