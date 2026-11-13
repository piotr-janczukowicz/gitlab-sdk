package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployToken;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenWithToken;
import java.util.List;

/** Conversions shared by the project and group deploy token classes. */
final class DeployValues {

    private DeployValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    static List<DeployTokenScope> scopes(final GitLabDeployToken response) {
        return scopeNames(response).stream().map(DeployTokenScope::fromWire).toList();
    }

    static List<String> scopeNames(final GitLabDeployToken response) {
        final List<String> names = response.scopes();
        return names == null ? List.of() : List.copyOf(names);
    }

    static GitLabDeployToken withoutSecret(final GitLabDeployTokenWithToken created) {
        return new GitLabDeployToken(
                created.id(),
                created.name(),
                created.username(),
                created.expiresAt(),
                created.scopes(),
                created.revoked(),
                created.expired());
    }

    static String describe(final String type, final GitLabDeployTokenWithToken created) {
        return type + "[id=" + created.id() + ", name=" + created.name() + ", token=<redacted>]";
    }
}
