package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class DeployTokenScopeTest {

    @ParameterizedTest
    @EnumSource(DeployTokenScope.class)
    void value_shouldMatchTheCoreConstantOfTheSameName_whenAnyScope(final DeployTokenScope scope) {
        assertSoftly(softly -> {
            softly.assertThat(GitLabProjectDeployTokensCreateScopes.valueOf(scope.name())
                            .wireValue())
                    .isEqualTo(scope.value());
            softly.assertThat(GitLabGroupDeployTokensCreateScopes.valueOf(scope.name())
                            .wireValue())
                    .isEqualTo(scope.value());
            softly.assertThat(DeployTokenScope.from(scope.value())).isSameAs(scope);
        });
    }

    @Test
    void values_shouldMirrorTheCoreValues_whenCounted() {
        assertThat(DeployTokenScope.values())
                .hasSize(GitLabProjectDeployTokensCreateScopes.values().length)
                .hasSize(GitLabGroupDeployTokensCreateScopes.values().length);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenUnknown() {
        final Throwable thrown = catchThrowable(() -> DeployTokenScope.from("read_everything"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("read_everything");
    }

    @Test
    void from_shouldThrowNullPointerException_whenNull() {
        final Throwable thrown = catchThrowable(() -> DeployTokenScope.from(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
