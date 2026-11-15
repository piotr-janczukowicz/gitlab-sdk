package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitActionTest {

    private static String bodyOf(final CommitAction action) {
        final MultipartFormData.Builder form = MultipartFormData.builder();
        action.writeTo(form);
        return form.build().body().mapBytes(bytes -> new String(bytes, StandardCharsets.UTF_8), () -> "");
    }

    @Test
    void writeTo_shouldSendTextContentWithoutEncoding_whenCreatingFromText() {
        final CommitAction sut = CommitAction.create("a/b.txt", "hello");

        final String body = bodyOf(sut);

        assertSoftly(softly -> {
            softly.assertThat(body)
                    .containsSubsequence(
                            "name=\"actions[][action]\"",
                            "create",
                            "name=\"actions[][file_path]\"",
                            "a/b.txt",
                            "name=\"actions[][content]\"",
                            "hello");
            softly.assertThat(body).doesNotContain("encoding").doesNotContain("previous_path");
        });
    }

    @Test
    void writeTo_shouldSendBase64WithEncoding_whenCreatingFromBytes() {
        final CommitAction sut = CommitAction.create("a.bin", new byte[] {0, -1, 10, 13});

        final String body = bodyOf(sut);

        assertThat(body)
                .containsSubsequence(
                        "name=\"actions[][action]\"",
                        "create",
                        "name=\"actions[][file_path]\"",
                        "a.bin",
                        "name=\"actions[][encoding]\"",
                        "base64",
                        "name=\"actions[][content]\"",
                        "AP8KDQ==");
    }

    @Test
    void writeTo_shouldSendEmptyContent_whenBytesAreEmpty() {
        final CommitAction sut = CommitAction.update("a.bin", new byte[0]);

        final String body = bodyOf(sut);

        assertThat(body)
                .containsSubsequence("update", "name=\"actions[][encoding]\"", "base64", "name=\"actions[][content]\"");
    }

    @Test
    void writeTo_shouldSendUpdateWithText_whenUpdatingFromText() {
        final CommitAction sut = CommitAction.update("a.txt", "new");

        final String body = bodyOf(sut);

        assertThat(body)
                .containsSubsequence(
                        "name=\"actions[][action]\"", "update", "name=\"actions[][file_path]\"", "a.txt", "new");
    }

    @Test
    void writeTo_shouldSendOnlyActionAndPath_whenDeleting() {
        final CommitAction sut = CommitAction.delete("old.txt");

        final String body = bodyOf(sut);

        assertSoftly(softly -> {
            softly.assertThat(body).containsSubsequence("delete", "name=\"actions[][file_path]\"", "old.txt");
            softly.assertThat(body).doesNotContain("content").doesNotContain("previous_path");
        });
    }

    @Test
    void writeTo_shouldSendPreviousPathAfterPath_whenMoving() {
        final CommitAction sut = CommitAction.move("old.txt", "new.txt");

        final String body = bodyOf(sut);

        assertThat(body)
                .containsSubsequence(
                        "name=\"actions[][action]\"",
                        "move",
                        "name=\"actions[][file_path]\"",
                        "new.txt",
                        "name=\"actions[][previous_path]\"",
                        "old.txt");
    }

    @Test
    void factories_shouldThrowNullPointerException_whenContentIsNull() {
        final Throwable text = catchThrowable(() -> CommitAction.create("a.txt", (String) null));
        final Throwable bytes = catchThrowable(() -> CommitAction.create("a.txt", (byte[]) null));
        final Throwable updateText = catchThrowable(() -> CommitAction.update("a.txt", (String) null));
        final Throwable updateBytes = catchThrowable(() -> CommitAction.update("a.txt", (byte[]) null));

        assertSoftly(softly -> {
            softly.assertThat(text).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
            softly.assertThat(bytes).isInstanceOf(NullPointerException.class).hasMessageContaining("content");
            softly.assertThat(updateText).isInstanceOf(NullPointerException.class);
            softly.assertThat(updateBytes).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void factories_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable create = catchThrowable(() -> CommitAction.create(" ", "x"));
        final Throwable createBytes = catchThrowable(() -> CommitAction.create(" ", new byte[0]));
        final Throwable update = catchThrowable(() -> CommitAction.update(" ", "x"));
        final Throwable updateBytes = catchThrowable(() -> CommitAction.update(" ", new byte[0]));
        final Throwable delete = catchThrowable(() -> CommitAction.delete(" "));
        final Throwable moveFrom = catchThrowable(() -> CommitAction.move(" ", "b"));
        final Throwable moveTo = catchThrowable(() -> CommitAction.move("a", " "));

        assertSoftly(softly -> {
            softly.assertThat(create).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(createBytes).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(update).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(updateBytes).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(delete).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(moveFrom).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(moveTo).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void factories_shouldThrowNullPointerException_whenPathIsNull() {
        final Throwable delete = catchThrowable(() -> CommitAction.delete(null));
        final Throwable move = catchThrowable(() -> CommitAction.move("a", null));

        assertSoftly(softly -> {
            softly.assertThat(delete).isInstanceOf(NullPointerException.class);
            softly.assertThat(move).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void create_shouldKeepContentAsGiven_whenArrayChangesAfterwards() {
        final byte[] content = {1, 2};
        final CommitAction sut = CommitAction.create("a.bin", content);
        content[0] = 9;

        final String body = bodyOf(sut);

        assertThat(body).contains("AQI=");
    }
}
