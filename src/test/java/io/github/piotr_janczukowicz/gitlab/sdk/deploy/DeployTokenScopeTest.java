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
    void wireValue_shouldBeKnownToTheCore_whenAnyScope(final DeployTokenScope scope) {
        assertSoftly(softly -> {
            softly.assertThat(GitLabProjectDeployTokensCreateScopes.fromWire(scope.wireValue()))
                    .hasToString(scope.name());
            softly.assertThat(GitLabGroupDeployTokensCreateScopes.fromWire(scope.wireValue()))
                    .hasToString(scope.name());
            softly.assertThat(DeployTokenScope.fromWire(scope.wireValue())).isSameAs(scope);
        });
    }

    @Test
    void values_shouldMirrorTheCoreValues_whenCounted() {
        assertThat(DeployTokenScope.values())
                .hasSize(GitLabProjectDeployTokensCreateScopes.values().length)
                .hasSize(GitLabGroupDeployTokensCreateScopes.values().length);
    }

    @Test
    void fromWire_shouldThrowIllegalArgumentException_whenUnknown() {
        final Throwable thrown = catchThrowable(() -> DeployTokenScope.fromWire("read_everything"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("read_everything");
    }

    @Test
    void fromWire_shouldThrowNullPointerException_whenNull() {
        final Throwable thrown = catchThrowable(() -> DeployTokenScope.fromWire(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
