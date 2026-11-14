package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisCreateFormat;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisUpdateFormat;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisCreateFormat;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisUpdateFormat;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class WikiFormatTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(WikiFormat.from("markdown")).isEqualTo(WikiFormat.MARKDOWN);
        assertThat(WikiFormat.from("rdoc")).isEqualTo(WikiFormat.RDOC);
        assertThat(WikiFormat.from("asciidoc")).isEqualTo(WikiFormat.ASCIIDOC);
        assertThat(WikiFormat.from("org")).isEqualTo(WikiFormat.ORG);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> WikiFormat.from("textile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("textile");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> WikiFormat.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void from_shouldMapEveryCoreFormat_whenLoopingFourCoreEnums() {
        final List<String> wireValues = List.of(
                        GitLabProjectProjectWikisCreateFormat.values(),
                        GitLabProjectProjectWikisUpdateFormat.values(),
                        GitLabGroupGroupWikisCreateFormat.values(),
                        GitLabGroupGroupWikisUpdateFormat.values())
                .stream()
                .flatMap(Arrays::stream)
                .map(WikiFormatTest::wireValue)
                .toList();

        for (final String wireValue : wireValues) {
            assertThat(WikiFormat.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(4 * WikiFormat.values().length);
    }

    @Test
    void value_shouldMatchEveryCoreEnum_whenEveryFormatIsMapped() {
        for (final WikiFormat format : WikiFormat.values()) {
            assertThat(GitLabProjectProjectWikisCreateFormat.fromWire(format.value())
                            .name())
                    .isEqualTo(format.name());
            assertThat(GitLabProjectProjectWikisUpdateFormat.fromWire(format.value())
                            .name())
                    .isEqualTo(format.name());
            assertThat(GitLabGroupGroupWikisCreateFormat.fromWire(format.value())
                            .name())
                    .isEqualTo(format.name());
            assertThat(GitLabGroupGroupWikisUpdateFormat.fromWire(format.value())
                            .name())
                    .isEqualTo(format.name());
        }
    }

    private static String wireValue(final Enum<?> constant) {
        return switch (constant) {
            case GitLabProjectProjectWikisCreateFormat each -> each.wireValue();
            case GitLabProjectProjectWikisUpdateFormat each -> each.wireValue();
            case GitLabGroupGroupWikisCreateFormat each -> each.wireValue();
            case GitLabGroupGroupWikisUpdateFormat each -> each.wireValue();
            default -> throw new IllegalArgumentException("Not a wiki format: " + constant);
        };
    }
}
