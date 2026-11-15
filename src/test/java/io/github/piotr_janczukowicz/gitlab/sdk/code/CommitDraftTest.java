package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class CommitDraftTest {

    private static final String ID = "my-group/my-project";
    private static final String ACTION_PART = "name=\"actions[][action]\"";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final CommitDraft sut = project.createCommit("feature", "Change things");

    private static String bodyOf(final MultipartFormData form) {
        return form.body().mapBytes(bytes -> new String(bytes, StandardCharsets.UTF_8), () -> "");
    }

    private static int count(final String body, final String part) {
        return body.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Test
    void send_shouldPostEveryActionInOrder_whenDraftHasSeveralActions() throws IOException {
        final GitLabCommitDetail created = commit("abc123", "Change things");
        when(gitLab.commits().createCommits(eq(ID), any(MultipartFormData.class)))
                .thenReturn(created);

        final Commit result = sut.create("a.txt", "hello")
                .create("b.bin", new byte[] {0, -1, 10, 13})
                .update("c.txt", "new")
                .update("d.bin", new byte[] {1, 2})
                .delete("e.txt")
                .move("f.txt", "g.txt")
                .send();

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.commits()).createCommits(eq(ID), form.capture());
        final String body = bodyOf(form.getValue());
        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.project()).isSameAs(project);
            softly.assertThat(body)
                    .containsSubsequence(
                            "name=\"branch\"",
                            "feature",
                            "name=\"commit_message\"",
                            "Change things",
                            ACTION_PART,
                            "create",
                            "a.txt",
                            "hello",
                            ACTION_PART,
                            "create",
                            "b.bin",
                            "AP8KDQ==",
                            ACTION_PART,
                            "update",
                            "c.txt",
                            "new",
                            ACTION_PART,
                            "update",
                            "d.bin",
                            "AQI=",
                            ACTION_PART,
                            "delete",
                            "e.txt",
                            ACTION_PART,
                            "move",
                            "g.txt",
                            "name=\"actions[][previous_path]\"",
                            "f.txt");
            softly.assertThat(count(body, ACTION_PART)).isEqualTo(6);
            softly.assertThat(count(body, "name=\"actions[][encoding]\"")).isEqualTo(2);
            softly.assertThat(count(body, "name=\"actions[][content]\"")).isEqualTo(4);
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenDraftHasNoAction() throws IOException {
        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("action");
        verify(gitLab.commits(), never()).createCommits(any(), any());
    }

    @Test
    void send_shouldThrowIllegalStateException_whenGitLabAnswersWithoutId() throws IOException {
        when(gitLab.commits().createCommits(eq(ID), any(MultipartFormData.class)))
                .thenReturn(GitLabCommitDetailBuilder.builder().build());

        final Throwable thrown = catchThrowable(() -> sut.delete("a.txt").send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("id");
    }

    @Test
    void create_shouldThrowNullPointerException_whenContentIsNull() {
        final Throwable text = catchThrowable(() -> sut.create("a.txt", (String) null));
        final Throwable bytes = catchThrowable(() -> sut.create("a.txt", (byte[]) null));
        final Throwable updateText = catchThrowable(() -> sut.update("a.txt", (String) null));
        final Throwable updateBytes = catchThrowable(() -> sut.update("a.txt", (byte[]) null));

        assertSoftly(softly -> {
            softly.assertThat(text).isInstanceOf(NullPointerException.class);
            softly.assertThat(bytes).isInstanceOf(NullPointerException.class);
            softly.assertThat(updateText).isInstanceOf(NullPointerException.class);
            softly.assertThat(updateBytes).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void send_shouldRefuseDraft_whenOnlyInvalidActionsWereTried() throws IOException {
        catchThrowable(() -> sut.create("a.txt", (String) null));

        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.commits(), never()).createCommits(any(), any());
    }

    @Test
    void actions_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable create = catchThrowable(() -> sut.create(" ", "x"));
        final Throwable delete = catchThrowable(() -> sut.delete(" "));
        final Throwable move = catchThrowable(() -> sut.move(" ", "b"));

        assertSoftly(softly -> {
            softly.assertThat(create).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(delete).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(move).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void createCommit_shouldThrowBeforeAnyRequest_whenBranchOrMessageIsInvalid() throws IOException {
        final Throwable nullBranch = catchThrowable(() -> project.createCommit(null, "m"));
        final Throwable blankBranch = catchThrowable(() -> project.createCommit(" ", "m"));
        final Throwable nullMessage = catchThrowable(() -> project.createCommit("b", null));
        final Throwable blankMessage = catchThrowable(() -> project.createCommit("b", " "));

        assertSoftly(softly -> {
            softly.assertThat(nullBranch).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankBranch).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullMessage).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankMessage).isInstanceOf(IllegalArgumentException.class);
        });
        verify(gitLab.commits(), never()).createCommits(any(), any());
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenProjectIsNull() {
        final Throwable thrown = catchThrowable(() -> new CommitDraft(null, "b", "m"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
