package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensRotatePayload;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensRotatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * An access token of a {@link Project}, as GitLab returned it. The object is immutable, and {@link #response()} gives
 * the complete response. The secret of a token is returned only when the token is created or rotated, and only by
 * {@link CreatedAccessToken#token()}; GitLab never returns it again.
 */
public final class AccessToken {

    private final Project project;
    private final GitLabResourceAccessToken response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public AccessToken(final Project project, final GitLabResourceAccessToken response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the token belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the token.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the name of the token.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the description of the token.
     *
     * @return the description, or empty when the response has none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the scopes of the token.
     *
     * @return the scopes; empty when the response has none
     * @throws IllegalArgumentException if the response holds a scope the SDK does not know; use {@link #scopeNames()}
     */
    public List<AccessTokenScope> scopes() {
        return TokenValues.scopes(response);
    }

    /**
     * Returns the scopes of the token as GitLab spells them. This never fails for a scope the SDK does not know.
     *
     * @return the scope names; empty when the response has none
     */
    public List<String> scopeNames() {
        return TokenValues.scopeNames(response);
    }

    /**
     * Tells whether the token is active.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(response.active());
    }

    /**
     * Tells whether the token is revoked.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean isRevoked() {
        return Boolean.TRUE.equals(response.revoked());
    }

    /**
     * Returns when the token expires.
     *
     * @return the expiry, or empty when the response has none
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(response.expiresAt());
    }

    /**
     * Returns when the token was last used.
     *
     * @return the time, or empty when the response has none
     */
    public Optional<Instant> lastUsedAt() {
        return Optional.ofNullable(response.lastUsedAt());
    }

    /**
     * Returns the role the token has in the project.
     *
     * @return the role, or empty when the response has none
     */
    public Optional<AccessLevel> accessLevel() {
        return TokenValues.level(response);
    }

    /**
     * Returns the id of the bot user the token acts as.
     *
     * @return the user id, or empty when the response has none
     */
    public Optional<Long> userId() {
        return Optional.ofNullable(response.userId());
    }

    /**
     * Rotates the token with GitLab's default expiry ({@code POST /projects/{id}/access_tokens/{token_id}/rotate}). The
     * old token stops working and the answer holds the new secret, which GitLab returns only once.
     *
     * @return the new token with its secret
     * @throws IOException if the transport fails
     */
    public CreatedAccessToken rotate() throws IOException {
        return rotateWith(
                GitLabProjectAccessTokensRotatePayloadBuilder.builder().build());
    }

    /**
     * Rotates the token and sets when the new one expires ({@code POST
     * /projects/{id}/access_tokens/{token_id}/rotate}). The old token stops working and the answer holds the new
     * secret, which GitLab returns only once.
     *
     * @param expiresAt the day the new token expires
     * @return the new token with its secret
     * @throws NullPointerException if {@code expiresAt} is {@code null}
     * @throws IOException if the transport fails
     */
    public CreatedAccessToken rotate(final LocalDate expiresAt) throws IOException {
        return rotateWith(GitLabProjectAccessTokensRotatePayloadBuilder.builder()
                .expiresAt(requireNonNull(expiresAt, "expiresAt"))
                .build());
    }

    /**
     * Revokes the token ({@code DELETE /projects/{id}/access_tokens/{token_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void revoke() throws IOException {
        gitLab().accesstokens().project().delete(project.id(), String.valueOf(id()));
    }

    /**
     * Loads the token again ({@code GET /projects/{id}/access_tokens/{token_id}}). This object is not changed.
     *
     * @return the token as GitLab holds it now
     * @throws IllegalStateException if the token is gone
     * @throws IOException if the transport fails
     */
    public AccessToken refresh() throws IOException {
        return project.accessToken(id())
                .orElseThrow(() -> new IllegalStateException("Access token " + id() + TokenValues.IS_GONE));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabResourceAccessToken response() {
        return response;
    }

    private CreatedAccessToken rotateWith(final GitLabProjectAccessTokensRotatePayload payload) throws IOException {
        return new CreatedAccessToken(
                project, gitLab().accesstokens().project().rotate(project.id(), String.valueOf(id()), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
