package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;

/**
 * A deploy token of a {@link Group} that was just created. GitLab returns the secret of a token only in this answer, so
 * {@link #token()} is the only place the SDK exposes it; keep it, because it cannot be loaded again. The secret is
 * never part of {@link #toString()}.
 */
public final class GroupCreatedDeployToken {

    private final GroupDeployToken deployToken;
    private final GitLabDeployTokenWithToken response;

    /**
     * Wraps the answer of a creation. Applications normally get this object from {@link Group#createDeployToken}.
     *
     * @param group the group the token belongs to
     * @param response the answer of the creation
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupCreatedDeployToken(final Group group, final GitLabDeployTokenWithToken response) {
        this.response = requireNonNull(response, "response");
        this.deployToken = new GroupDeployToken(requireNonNull(group, "group"), DeployValues.withoutSecret(response));
    }

    /**
     * Returns the token without its secret.
     *
     * @return the deploy token
     */
    public GroupDeployToken deployToken() {
        return deployToken;
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
     * Returns the complete answer of the creation. The core redacts the secret in the record's {@code toString()}, but
     * the record's {@code token()} still holds it, as {@link #token()} does; keep the record out of logs and serialized
     * output.
     *
     * @return the response record
     */
    public GitLabDeployTokenWithToken response() {
        return response;
    }

    @Override
    public String toString() {
        return DeployValues.describe("GroupCreatedDeployToken", response);
    }
}
