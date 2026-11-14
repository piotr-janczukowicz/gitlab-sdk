package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.CONTENT;
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
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateFile;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateFileAction;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateVisibility;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class SnippetUpdateTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Snippet snippet = new Snippet(project.snippets(), projectSnippet(5L));
    private final ArgumentCaptor<GitLabProjectProjectSnippetsUpdatePayload> sent =
            ArgumentCaptor.forClass(GitLabProjectProjectSnippetsUpdatePayload.class);

    @Test
    void constructor_shouldThrowNullPointerException_whenSnippetIsNull() {
        final Throwable thrown = catchThrowable(() -> new SnippetUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() throws IOException {
        final Throwable thrown = catchThrowable(() -> snippet.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.projectsnippets(), never()).update(anyString(), anyLong(), any());
    }

    @Test
    void update_shouldSendOnlySetFields_whenTitleDescriptionAndVisibilitySet() throws IOException {
        when(gitLab.projectsnippets().update(eq(ID), eq(5L), sent.capture())).thenReturn(projectSnippet(5L));

        final Snippet result = snippet.update()
                .title("renamed")
                .description("")
                .visibility(SnippetVisibility.INTERNAL)
                .send();

        final GitLabProjectProjectSnippetsUpdatePayload payload = sent.getValue();
        assertSoftly(softly -> {
            softly.assertThat(payload.title()).isEqualTo("renamed");
            softly.assertThat(payload.description()).isEmpty();
            softly.assertThat(payload.visibility()).isEqualTo(GitLabProjectProjectSnippetsUpdateVisibility.INTERNAL);
            softly.assertThat(payload.files()).isNull();
            softly.assertThat(payload.content()).isNull();
            softly.assertThat(payload.fileName()).isNull();
            softly.assertThat(result.project()).isSameAs(project);
        });
        verify(gitLab.projectsnippets(), times(1)).update(eq(ID), eq(5L), any());
    }

    @Test
    void update_shouldSendFileActionsInCallOrder() throws IOException {
        when(gitLab.projectsnippets().update(eq(ID), eq(5L), sent.capture())).thenReturn(projectSnippet(5L));

        snippet.update()
                .deleteFile("old.rb")
                .addFile("dir/new.rb", CONTENT)
                .updateFile("add.rb", "changed")
                .moveFile("a.rb", "b.rb")
                .send();

        final GitLabProjectProjectSnippetsUpdatePayload payload = sent.getValue();
        assertSoftly(softly -> {
            softly.assertThat(payload.files())
                    .containsExactly(
                            new GitLabProjectProjectSnippetsUpdateFile(
                                    GitLabProjectProjectSnippetsUpdateFileAction.DELETE, null, "old.rb", null),
                            new GitLabProjectProjectSnippetsUpdateFile(
                                    GitLabProjectProjectSnippetsUpdateFileAction.CREATE, CONTENT, "dir/new.rb", null),
                            new GitLabProjectProjectSnippetsUpdateFile(
                                    GitLabProjectProjectSnippetsUpdateFileAction.UPDATE, "changed", "add.rb", null),
                            new GitLabProjectProjectSnippetsUpdateFile(
                                    GitLabProjectProjectSnippetsUpdateFileAction.MOVE, null, "b.rb", "a.rb"));
            softly.assertThat(payload.title()).isNull();
            softly.assertThat(payload.visibility()).isNull();
            softly.assertThat(payload.content()).isNull();
            softly.assertThat(payload.fileName()).isNull();
        });
    }

    @Test
    void moveFile_shouldSendPreviousPath() throws IOException {
        when(gitLab.projectsnippets().update(eq(ID), eq(5L), sent.capture())).thenReturn(projectSnippet(5L));

        snippet.update().moveFile("dir/a.rb", "dir/b.rb").send();

        final GitLabProjectProjectSnippetsUpdateFile file =
                sent.getValue().files().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(file.action()).isEqualTo(GitLabProjectProjectSnippetsUpdateFileAction.MOVE);
            softly.assertThat(file.previousPath()).isEqualTo("dir/a.rb");
            softly.assertThat(file.filePath()).isEqualTo("dir/b.rb");
            softly.assertThat(file.content()).isNull();
        });
    }

    @Test
    void moveFile_shouldRejectBlankPreviousPath_withoutCoreCall() throws IOException {
        final SnippetUpdate sut = snippet.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.moveFile(" ", "b.rb")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.moveFile(null, "b.rb")))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.moveFile("a.rb", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(sut::send)).isInstanceOf(IllegalStateException.class);
        });
        verify(gitLab.projectsnippets(), never()).update(anyString(), anyLong(), any());
    }

    @Test
    void addFile_shouldRejectNullContent_withoutCoreCall() throws IOException {
        final SnippetUpdate sut = snippet.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.addFile("a.rb", null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.addFile(" ", CONTENT)))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.updateFile("a.rb", null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.updateFile(" ", CONTENT)))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.deleteFile(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.title(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.visibility(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(sut::send)).isInstanceOf(IllegalStateException.class);
        });
        verify(gitLab.projectsnippets(), never()).update(anyString(), anyLong(), any());
    }

    @Test
    void toString_shouldNotContainContent_whenObjectPrinted() {
        final SnippetUpdate sut = snippet.update().addFile("a.rb", CONTENT).description("secret description");

        final String printed = sut.toString();

        assertSoftly(softly -> {
            softly.assertThat(printed).doesNotContain(CONTENT);
            softly.assertThat(printed).doesNotContain("secret description");
        });
    }
}
