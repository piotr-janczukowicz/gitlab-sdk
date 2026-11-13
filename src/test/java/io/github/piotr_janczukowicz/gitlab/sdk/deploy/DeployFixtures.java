package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDeployKey;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDeployKeyBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabDeployKeysProject;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabDeployKeysProjectBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployToken;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenWithTokenBuilder;
import java.util.List;

/** Builds the core records for the tests of the deploy key and deploy token objects. */
final class DeployFixtures {

    static final String SECRET = "gldt-s3cr3t-value";

    private DeployFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabDeployKeysProject key(final long id, final String title) {
        return GitLabDeployKeysProjectBuilder.builder().id(id).title(title).build();
    }

    static GitLabDeployKeysProject keyWithPush(final long id, final String title, final boolean canPush) {
        return GitLabDeployKeysProjectBuilder.builder()
                .id(id)
                .title(title)
                .canPush(canPush)
                .build();
    }

    static GitLabDeployKey otherRecord(final long id, final String title) {
        return GitLabDeployKeyBuilder.builder().id(id).title(title).build();
    }

    static GitLabDeployToken token(final long id, final String name) {
        return GitLabDeployTokenBuilder.builder().id(id).name(name).build();
    }

    static GitLabDeployToken tokenWithScopes(final long id, final String name, final List<String> scopes) {
        return GitLabDeployTokenBuilder.builder()
                .id(id)
                .name(name)
                .scopes(scopes)
                .build();
    }

    static GitLabDeployTokenWithToken created(final long id, final String name) {
        return GitLabDeployTokenWithTokenBuilder.builder()
                .id(id)
                .name(name)
                .username("gitlab+deploy-token-" + id)
                .scopes(List.of("read_repository"))
                .token(SECRET)
                .build();
    }
}
