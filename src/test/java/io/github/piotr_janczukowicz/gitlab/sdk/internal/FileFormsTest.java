package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@UnitTest
class FileFormsTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<FileForms> constructor = FileForms.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void of_shouldHoldBranchContentAndMessage_whenContentIsEmpty() {
        final MultipartFormData form = FileForms.of("main", "", "Add file");

        final String body = form.body().mapBytes(bytes -> new String(bytes, StandardCharsets.UTF_8), () -> "");
        assertThat(body)
                .contains("name=\"branch\"")
                .contains("main")
                .contains("name=\"content\"")
                .contains("name=\"commit_message\"")
                .contains("Add file");
    }

    @Test
    void of_shouldThrowNullPointerException_whenContentIsNull() {
        final Throwable thrown = catchThrowable(() -> FileForms.of("main", null, "m"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_shouldThrowIllegalArgumentException_whenBranchIsBlank() {
        final Throwable thrown = catchThrowable(() -> FileForms.of(" ", "c", "m"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_shouldThrowIllegalArgumentException_whenMessageIsBlank() {
        final Throwable thrown = catchThrowable(() -> FileForms.of("main", "c", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
