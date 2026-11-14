package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiResetTokenResult;
import java.time.Instant;
import java.util.Optional;

/**
 * A runner token that GitLab returned after a reset: the registration token of a project or group, or the
 * authentication token of a {@link Runner}. GitLab returns the secret only in this answer, so {@link #token()} is the
 * only place the SDK exposes it. The secret is never part of {@link #toString()}.
 */
public final class RegistrationToken {

    private final GitLabCiResetTokenResult response;

    /**
     * Wraps the answer of a reset. Applications normally get this object from the reset methods of the owner.
     *
     * @param response the answer of the reset
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public RegistrationToken(final GitLabCiResetTokenResult response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the new token. The token is secret, do not log it.
     *
     * @return the token
     */
    public String token() {
        return requireNonNull(response.token(), "token");
    }

    /**
     * Returns the time the token expires.
     *
     * @return the time, or empty when GitLab sent none
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(response.tokenExpiresAt());
    }

    /**
     * Returns the complete answer of the reset. The core redacts the token in the record's {@code toString()}, but the
     * record's {@code token()} still holds it; keep the record out of logs and serialized output.
     *
     * @return the response record
     */
    public GitLabCiResetTokenResult response() {
        return response;
    }

    @Override
    public String toString() {
        return "RegistrationToken[token=<redacted>, expiresAt=" + response.tokenExpiresAt() + "]";
    }
}
