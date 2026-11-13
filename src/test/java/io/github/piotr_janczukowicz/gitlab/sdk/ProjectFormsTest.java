package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectFormsTest {

    private final ProjectForms sut = new ProjectForms();

    static byte[] bytesOf(final MultipartFormData form) {
        return form.body().mapBytes(bytes -> bytes, () -> new byte[0]);
    }

    static String textOf(final MultipartFormData form) {
        return new String(bytesOf(form), StandardCharsets.UTF_8);
    }

    @Test
    void put_shouldSendUtf8_whenValueIsNonAscii() {
        sut.put("name", "zażółć");

        final byte[] body = bytesOf(sut.build());

        assertThat(new String(body, StandardCharsets.ISO_8859_1))
                .contains(new String("zażółć".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
    }

    @Test
    void build_shouldOmitUnsetFields_whenOnlyOneFieldIsPut() {
        sut.put("name", "demo");

        final String body = textOf(sut.build());

        assertThat(body).contains("name=\"name\"").doesNotContain("name=\"path\"");
    }

    @Test
    void build_shouldKeepInsertionOrder_whenSeveralFieldsArePut() {
        sut.put("path", "p");
        sut.put("name", "n");

        final String body = textOf(sut.build());

        assertThat(body.indexOf("name=\"path\"")).isLessThan(body.indexOf("name=\"name\""));
    }

    @Test
    void build_shouldThrowIllegalStateException_whenNothingWasPut() {
        final Throwable thrown = catchThrowable(sut::build);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }
}
