package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

@UnitTest
class ManagementFormsTest {

    private final ManagementForms sut = new ManagementForms();

    static byte[] bytesOf(final MultipartFormData form) {
        return form.body().mapBytes(bytes -> bytes, () -> new byte[0]);
    }

    static String textOf(final MultipartFormData form) {
        return new String(bytesOf(form), StandardCharsets.UTF_8);
    }

    /**
     * Returns the value of a named text part of a multipart body, so a test asserts the field name and value together
     * instead of a bare substring.
     */
    static Optional<String> fieldOf(final String body, final String name) {
        final Matcher matcher = Pattern.compile(
                        "Content-Disposition: form-data; name=\"" + Pattern.quote(name)
                                + "\"\r\n(?:[^\r\n]+\r\n)*\r\n(.*?)\r\n--",
                        Pattern.DOTALL)
                .matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    @Test
    void put_shouldSendUtf8_whenValueIsNonAscii() {
        sut.put("name", "zażółć");

        final byte[] body = bytesOf(sut.build());

        assertThat(new String(body, StandardCharsets.ISO_8859_1))
                .contains(new String("zażółć".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
    }

    @Test
    void fieldOf_shouldReturnExactValues_whenFieldsArePut() {
        sut.put("visibility", "internal");
        sut.put("description", "");
        sut.put("initialize_with_readme", "true");

        final String body = textOf(sut.build());

        assertSoftly(softly -> {
            softly.assertThat(fieldOf(body, "visibility")).contains("internal");
            softly.assertThat(fieldOf(body, "description")).contains("");
            softly.assertThat(fieldOf(body, "initialize_with_readme")).contains("true");
            softly.assertThat(fieldOf(body, "path")).isEmpty();
        });
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
