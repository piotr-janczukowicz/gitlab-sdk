package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

@UnitTest
class SnippetValuesTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<SnippetValues> constructor = SnippetValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void files_shouldBeEmpty_whenAbsent() {
        assertThat(SnippetValues.files(null)).isEmpty();
    }

    @Test
    void toString_shouldNotContainContent_whenObjectPrinted() {
        final SnippetValues.FileChange change = new SnippetValues.FileChange("create", "a.rb", null, "secret content");
        final SnippetValues.Changes changes = new SnippetValues.Changes();
        changes.setTitle("title");
        changes.setDescription("secret description");
        changes.file(change);

        final String printed = change + " " + changes;

        assertSoftly(softly -> {
            softly.assertThat(printed).doesNotContain("secret content");
            softly.assertThat(printed).doesNotContain("secret description");
            softly.assertThat(change.toString()).contains("create").contains("a.rb");
        });
    }
}
