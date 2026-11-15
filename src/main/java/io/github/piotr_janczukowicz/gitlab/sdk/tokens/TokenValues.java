package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessToken;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenResourceType;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import java.util.List;
import java.util.Optional;

/** Values and conversions shared by the classes of the tokens package. */
final class TokenValues {

    /** The end of the message of a {@code refresh()} that finds the object gone. */
    static final String IS_GONE = " is gone";

    private TokenValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    static List<String> scopeNames(final GitLabResourceAccessToken response) {
        final List<String> names = response.scopes();
        return names == null ? List.of() : List.copyOf(names);
    }

    static List<AccessTokenScope> scopes(final GitLabResourceAccessToken response) {
        return scopeNames(response).stream().map(AccessTokenScope::from).toList();
    }

    static Optional<AccessLevel> level(final GitLabResourceAccessToken response) {
        return Optional.ofNullable(response.accessLevel()).map(level -> AccessLevel.from(level.wireValue()));
    }

    static GitLabResourceAccessToken withoutSecret(final GitLabResourceAccessTokenWithToken created) {
        return new GitLabResourceAccessToken(
                created.id(),
                created.name(),
                created.revoked(),
                created.createdAt(),
                created.description(),
                created.scopes(),
                created.userId(),
                created.lastUsedAt(),
                created.lastUsedIps(),
                created.granularScopes(),
                created.active(),
                created.granular(),
                created.expiresAt(),
                created.accessLevel() == null
                        ? null
                        : GitLabResourceAccessTokenAccessLevel.valueOf(
                                created.accessLevel().name()),
                created.resourceType() == null
                        ? null
                        : GitLabResourceAccessTokenResourceType.valueOf(
                                created.resourceType().name()),
                created.resourceId());
    }

    static String describe(final String type, final GitLabResourceAccessTokenWithToken created) {
        return type + "[id=" + created.id() + ", name=" + created.name() + ", token=<redacted>]";
    }
}
