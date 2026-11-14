package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListPackagesPackageType;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabProjectPackagesListPackageType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class PackageTypeTest {

    @Test
    void from_shouldMapWireValues_whenDocumented() {
        assertThat(PackageType.from("npm")).isEqualTo(PackageType.NPM);
        assertThat(PackageType.from("terraform_module")).isEqualTo(PackageType.TERRAFORM_MODULE);
        assertThat(PackageType.from("ml_model")).isEqualTo(PackageType.ML_MODEL);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        assertThatThrownBy(() -> PackageType.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void from_shouldThrowNullPointerException_whenValueIsNull() {
        assertThatThrownBy(() -> PackageType.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void value_shouldMatchCoreWireValue_whenEveryTypeIsMapped() {
        for (final PackageType type : PackageType.values()) {
            assertThat(GitLabProjectPackagesListPackageType.valueOf(type.name()).wireValue())
                    .isEqualTo(type.value());
            assertThat(GitLabGroupGroupsListPackagesPackageType.valueOf(type.name())
                            .wireValue())
                    .isEqualTo(type.value());
        }
    }

    @Test
    void values_shouldMatchCoreEnums_whenCompared() {
        final String[] names =
                Arrays.stream(PackageType.values()).map(Enum::name).toArray(String[]::new);

        assertThat(Arrays.stream(GitLabProjectPackagesListPackageType.values()).map(Enum::name))
                .containsExactlyInAnyOrder(names);
        assertThat(Arrays.stream(GitLabGroupGroupsListPackagesPackageType.values())
                        .map(Enum::name))
                .containsExactlyInAnyOrder(names);
    }
}
