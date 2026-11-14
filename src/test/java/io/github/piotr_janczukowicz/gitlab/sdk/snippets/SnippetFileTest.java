package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.RAW_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SnippetFileTest {

    @Test
    void accessors_shouldReturnPathAndRawUrl_whenBothGiven() {
        final SnippetFile sut = new SnippetFile("dir/add.rb", RAW_URL);

        assertSoftly(softly -> {
            softly.assertThat(sut.path()).isEqualTo("dir/add.rb");
            softly.assertThat(sut.rawUrl()).contains(RAW_URL);
        });
    }

    @Test
    void rawUrl_shouldBeEmpty_whenAbsent() {
        final SnippetFile sut = new SnippetFile("add.rb", null);

        assertThat(sut.rawUrl()).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenPathIsNull() {
        final Throwable thrown = catchThrowable(() -> new SnippetFile(null, RAW_URL));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
