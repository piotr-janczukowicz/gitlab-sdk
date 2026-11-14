package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariableBuilder;

/** Builds the core records for the tests of the variable objects. */
final class VariablesFixtures {

    static final String SECRET = "s3cr3t-value";

    private VariablesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabCiVariable variable(final String key, final String value) {
        return GitLabCiVariableBuilder.builder()
                .key(key)
                .value(value)
                .variableType("env_var")
                .build();
    }

    static GitLabCiVariable scoped(final String key, final String value, final String scope) {
        return GitLabCiVariableBuilder.builder()
                .key(key)
                .value(value)
                .variableType("env_var")
                .environmentScope(scope)
                .build();
    }

    static GitLabCiVariable withType(final String key, final String type) {
        return GitLabCiVariableBuilder.builder().key(key).variableType(type).build();
    }
}
