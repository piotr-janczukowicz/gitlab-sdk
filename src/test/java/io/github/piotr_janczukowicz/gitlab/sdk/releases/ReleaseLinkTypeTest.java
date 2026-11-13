package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksCreateLinkType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class ReleaseLinkTypeTest {

    @ParameterizedTest
    @EnumSource(ReleaseLinkType.class)
    void value_shouldMatchCoreWireValue_whenTypeIsAny(final ReleaseLinkType type) {
        final String wire =
                GitLabProjectReleaseLinksCreateLinkType.valueOf(type.name()).wireValue();

        assertThat(type.value()).isEqualTo(wire);
        assertThat(ReleaseLinkType.from(wire)).isEqualTo(type);
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> ReleaseLinkType.from(null)).isInstanceOf(NullPointerException.class);
    }
}
