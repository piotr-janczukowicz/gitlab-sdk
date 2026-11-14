package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsUpdateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsUpdateVisibility;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class SnippetVisibilityTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(SnippetVisibility.from("private")).isEqualTo(SnippetVisibility.PRIVATE);
        assertThat(SnippetVisibility.from("internal")).isEqualTo(SnippetVisibility.INTERNAL);
        assertThat(SnippetVisibility.from("public")).isEqualTo(SnippetVisibility.PUBLIC);
    }

    @Test
    void visibility_shouldThrowIllegalArgumentException_whenUnknown() {
        assertThatThrownBy(() -> SnippetVisibility.from("secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> SnippetVisibility.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void from_shouldMapEveryCoreVisibility_whenLoopingFourCoreEnums() {
        final List<String> wireValues = new ArrayList<>();
        for (final GitLabProjectProjectSnippetsCreateVisibility each :
                GitLabProjectProjectSnippetsCreateVisibility.values()) {
            wireValues.add(each.wireValue());
        }
        for (final GitLabProjectProjectSnippetsUpdateVisibility each :
                GitLabProjectProjectSnippetsUpdateVisibility.values()) {
            wireValues.add(each.wireValue());
        }
        for (final GitLabCodeSnippetsCreateVisibility each : GitLabCodeSnippetsCreateVisibility.values()) {
            wireValues.add(each.wireValue());
        }
        for (final GitLabCodeSnippetsUpdateVisibility each : GitLabCodeSnippetsUpdateVisibility.values()) {
            wireValues.add(each.wireValue());
        }

        for (final String wireValue : wireValues) {
            assertThat(SnippetVisibility.from(wireValue).value()).isEqualTo(wireValue);
        }
        assertThat(wireValues).hasSize(4 * SnippetVisibility.values().length);
    }

    @Test
    void value_shouldMatchEveryCoreEnum_whenEveryVisibilityIsMapped() {
        for (final SnippetVisibility visibility : SnippetVisibility.values()) {
            final String value = visibility.value();
            assertThat(GitLabProjectProjectSnippetsCreateVisibility.fromWire(value)
                            .name())
                    .isEqualTo(visibility.name());
            assertThat(GitLabProjectProjectSnippetsUpdateVisibility.fromWire(value)
                            .name())
                    .isEqualTo(visibility.name());
            assertThat(GitLabCodeSnippetsCreateVisibility.fromWire(value).name())
                    .isEqualTo(visibility.name());
            assertThat(GitLabCodeSnippetsUpdateVisibility.fromWire(value).name())
                    .isEqualTo(visibility.name());
        }
    }
}
