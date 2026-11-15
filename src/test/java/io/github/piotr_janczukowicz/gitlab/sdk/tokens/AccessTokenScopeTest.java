package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensCreateScopes;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class AccessTokenScopeTest {

    @ParameterizedTest
    @EnumSource(AccessTokenScope.class)
    void value_shouldMatchBothCoreEnums_whenScopeIsKnown(final AccessTokenScope scope) {
        final GitLabProjectAccessTokensCreateScopes project =
                GitLabProjectAccessTokensCreateScopes.valueOf(scope.name());
        final GitLabGroupAccessTokensCreateScopes group = GitLabGroupAccessTokensCreateScopes.valueOf(scope.name());

        assertThat(scope.value()).isEqualTo(project.wireValue()).isEqualTo(group.wireValue());
    }

    @Test
    void values_shouldCoverEveryScopeOfTheProjectEnum_whenCompared() {
        final List<String> sdk = Arrays.stream(AccessTokenScope.values())
                .map(AccessTokenScope::name)
                .toList();
        final List<String> project = Arrays.stream(GitLabProjectAccessTokensCreateScopes.values())
                .map(Enum::name)
                .toList();
        final List<String> group = Arrays.stream(GitLabGroupAccessTokensCreateScopes.values())
                .map(Enum::name)
                .toList();

        assertThat(sdk).containsExactlyInAnyOrderElementsOf(project).containsExactlyInAnyOrderElementsOf(group);
    }

    @Test
    void from_shouldReturnScope_whenNameIsKnown() {
        assertThat(AccessTokenScope.from("read_repository")).isEqualTo(AccessTokenScope.READ_REPOSITORY);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenNameIsUnknown() {
        final Throwable thrown = catchThrowable(() -> AccessTokenScope.from("nope"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nope");
    }

    @Test
    void from_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> AccessTokenScope.from(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
