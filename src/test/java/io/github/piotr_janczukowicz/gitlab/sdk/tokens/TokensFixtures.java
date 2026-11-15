package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessToken;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenBuilder;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenResourceType;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithTokenAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithTokenBuilder;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithTokenResourceType;
import java.time.Instant;
import java.util.List;

/** Builds the core records for the tests of the access token objects. */
final class TokensFixtures {

    /** A made-up value; it is not a token of any GitLab instance. */
    static final String SECRET = "not-a-real-secret-0123456789";

    static final Instant EXPIRES = Instant.parse("2026-12-31T00:00:00Z");
    static final Instant LAST_USED = Instant.parse("2026-06-01T10:00:00Z");

    private TokensFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabResourceAccessToken token(final long id, final String name) {
        return GitLabResourceAccessTokenBuilder.builder().id(id).name(name).build();
    }

    static GitLabResourceAccessToken fullToken(final long id, final String name) {
        return GitLabResourceAccessTokenBuilder.builder()
                .id(id)
                .name(name)
                .description("for the pipeline")
                .scopes(List.of("api", "read_repository"))
                .active(true)
                .revoked(false)
                .userId(99L)
                .expiresAt(EXPIRES)
                .lastUsedAt(LAST_USED)
                .accessLevel(GitLabResourceAccessTokenAccessLevel.MAINTAINER)
                .resourceType(GitLabResourceAccessTokenResourceType.PROJECT)
                .resourceId(42L)
                .build();
    }

    static GitLabResourceAccessToken tokenWithScopes(final long id, final List<String> scopes) {
        return GitLabResourceAccessTokenBuilder.builder()
                .id(id)
                .name("t")
                .scopes(scopes)
                .build();
    }

    static GitLabResourceAccessTokenWithToken created(final long id, final String name) {
        return GitLabResourceAccessTokenWithTokenBuilder.builder()
                .id(id)
                .name(name)
                .scopes(List.of("api"))
                .active(true)
                .revoked(false)
                .expiresAt(EXPIRES)
                .accessLevel(GitLabResourceAccessTokenWithTokenAccessLevel.DEVELOPER)
                .resourceType(GitLabResourceAccessTokenWithTokenResourceType.PROJECT)
                .resourceId(42L)
                .token(SECRET)
                .build();
    }
}
