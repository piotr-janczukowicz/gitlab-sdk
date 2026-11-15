package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;

/**
 * An access token of a {@link Project} that was just created or rotated. GitLab returns the secret of a token only in
 * this answer, so {@link #token()} is the only place the SDK exposes it; keep it, because it cannot be loaded again.
 * The secret is never part of {@link #toString()}.
 */
public final class CreatedAccessToken {

    private final AccessToken accessToken;
    private final GitLabResourceAccessTokenWithToken response;

    /**
     * Wraps the answer of a creation or a rotation. Applications normally get this object from
     * {@link Project#createAccessToken} or {@link AccessToken#rotate()}.
     *
     * @param project the project the token belongs to
     * @param response the answer of the creation
     * @throws NullPointerException if an argument is {@code null}
     */
    public CreatedAccessToken(final Project project, final GitLabResourceAccessTokenWithToken response) {
        this.response = requireNonNull(response, "response");
        this.accessToken = new AccessToken(requireNonNull(project, "project"), TokenValues.withoutSecret(response));
    }

    /**
     * Returns the token without its secret.
     *
     * @return the access token
     */
    public AccessToken accessToken() {
        return accessToken;
    }

    /**
     * Returns the secret of the token. This is the only time GitLab returns it.
     *
     * @return the secret
     */
    public String token() {
        return requireNonNull(response.token(), "token");
    }

    /**
     * Returns the complete answer of the creation. The core redacts the secret in the record's {@code toString()} (a
     * unit test checks it), but the record's {@code token()} still holds it, as {@link #token()} does; keep the record
     * out of logs and serialized output.
     *
     * @return the response record
     */
    public GitLabResourceAccessTokenWithToken response() {
        return response;
    }

    @Override
    public String toString() {
        return TokenValues.describe("CreatedAccessToken", response);
    }
}
