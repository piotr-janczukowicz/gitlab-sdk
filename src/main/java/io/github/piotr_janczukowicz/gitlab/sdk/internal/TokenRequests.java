package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithTokenAccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.AccessTokenScope;
import java.util.Collection;
import java.util.List;

/** Validates the arguments of the creation of a project or a group access token before any request is made. */
public final class TokenRequests {

    private TokenRequests() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks that the answer of a creation can be decoded for a role. The core's response type has no value for the
     * Planner and the Security Manager roles, and GitLab returns the secret of a token only once, so a token created
     * with such a role would be lost when the answer fails to decode.
     *
     * @param level the role of the token
     * @return the role
     * @throws NullPointerException if {@code level} is {@code null}
     * @throws IllegalArgumentException if the core cannot decode an answer for the role
     */
    public static AccessLevel requireTokenLevel(final AccessLevel level) {
        requireNonNull(level, "level");
        try {
            GitLabResourceAccessTokenWithTokenAccessLevel.fromWire(level.value());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "The core cannot decode an access token with the level " + level
                            + "; use GUEST, REPORTER, DEVELOPER, MAINTAINER or OWNER",
                    e);
        }
        return level;
    }

    /**
     * Checks the scopes of a token and returns them as an immutable list.
     *
     * @param scopes the scopes
     * @return the scopes
     * @throws NullPointerException if the collection or a scope is {@code null}
     * @throws IllegalArgumentException if no scope is given
     */
    public static List<AccessTokenScope> requireScopes(final Collection<AccessTokenScope> scopes) {
        final List<AccessTokenScope> copy = List.copyOf(requireNonNull(scopes, "scopes"));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("At least one scope is required");
        }
        return copy;
    }
}
