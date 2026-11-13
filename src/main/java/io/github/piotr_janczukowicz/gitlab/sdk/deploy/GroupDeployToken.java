package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployToken;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A deploy token of a {@link Group}, as GitLab returned it. The object is immutable, and {@link #response()} gives the
 * complete response. The secret of the token is returned only when the token is created, and only by
 * {@link GroupCreatedDeployToken#token()}; GitLab never returns it again.
 */
public final class GroupDeployToken {

    private final Group group;
    private final GitLabDeployToken response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupDeployToken(final Group group, final GitLabDeployToken response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the token belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
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
     * Returns the user name that goes with the token.
     *
     * @return the user name, or empty when the response has none
     */
    public Optional<String> username() {
        return Optional.ofNullable(response.username());
    }

    /**
     * Returns the scopes of the token.
     *
     * @return the scopes; empty when the response has none
     * @throws IllegalArgumentException if the response holds a scope the SDK does not know; use {@link #scopeNames()}
     */
    public List<DeployTokenScope> scopes() {
        return DeployValues.scopes(response);
    }

    /**
     * Returns the scopes of the token as GitLab spells them. This never fails for a scope the SDK does not know.
     *
     * @return the scope names; empty when the response has none
     */
    public List<String> scopeNames() {
        return DeployValues.scopeNames(response);
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
     * Tells whether the token is expired.
     *
     * @return {@code true} when the response says so; a missing flag counts as {@code false}
     */
    public boolean isExpired() {
        return Boolean.TRUE.equals(response.expired());
    }

    /**
     * Returns when the token expires.
     *
     * @return the expiry, or empty when the token does not expire
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(response.expiresAt());
    }

    /**
     * Deletes the token ({@code DELETE /groups/{id}/deploy_tokens/{token_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().deploytokens().group().delete(group.id(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabDeployToken response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
