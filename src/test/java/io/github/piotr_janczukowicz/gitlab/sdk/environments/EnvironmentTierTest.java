package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsCreateTier;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsUpdateTier;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class EnvironmentTierTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(EnvironmentTier.from("production")).isEqualTo(EnvironmentTier.PRODUCTION);
        assertThat(EnvironmentTier.from("staging")).isEqualTo(EnvironmentTier.STAGING);
        assertThat(EnvironmentTier.from("testing")).isEqualTo(EnvironmentTier.TESTING);
        assertThat(EnvironmentTier.from("development")).isEqualTo(EnvironmentTier.DEVELOPMENT);
        assertThat(EnvironmentTier.from("other")).isEqualTo(EnvironmentTier.OTHER);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> EnvironmentTier.from("qa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qa");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> EnvironmentTier.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void value_shouldMatchCoreWireValue_whenEveryTierIsMapped() {
        for (final EnvironmentTier tier : EnvironmentTier.values()) {
            assertThat(GitLabProjectEnvironmentsCreateTier.valueOf(tier.name()).wireValue())
                    .isEqualTo(tier.value());
            assertThat(GitLabProjectEnvironmentsUpdateTier.valueOf(tier.name()).wireValue())
                    .isEqualTo(tier.value());
        }
    }

    @Test
    void values_shouldCoverEveryCoreTier_whenCoreEnumsAreChecked() {
        final String[] names =
                Arrays.stream(EnvironmentTier.values()).map(Enum::name).toArray(String[]::new);

        assertThat(Arrays.stream(GitLabProjectEnvironmentsCreateTier.values()).map(Enum::name))
                .containsExactlyInAnyOrder(names);
        assertThat(Arrays.stream(GitLabProjectEnvironmentsUpdateTier.values()).map(Enum::name))
                .containsExactlyInAnyOrder(names);
    }
}
