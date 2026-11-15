package io.github.piotr_janczukowicz.gitlab.sdk.account;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.keys.GitLabGpgKey;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * A GPG key of the account the client signs in as, as GitLab returned it. The object is immutable. The public key is
 * available through {@link #key()} and {@link #response()}, but it is never part of {@link #toString()}.
 */
public final class GpgKey {

    private final GitLabSdk sdk;
    private final GitLabGpgKey response;

    /**
     * Wraps a response. Applications normally get this object from {@code Me.gpgKeys()} or {@code Me.addGpgKey}; call
     * the constructor only to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GpgKey(final GitLabSdk sdk, final GitLabGpgKey response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id of the key.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the public key.
     *
     * @return the public key as GitLab returned it
     */
    public String key() {
        return requireNonNull(response.key(), "key");
    }

    /**
     * Returns when the key was added.
     *
     * @return the time, or empty when GitLab leaves it out
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(response.createdAt());
    }

    /**
     * Deletes the key ({@code DELETE /user/gpg_keys/{key_id}}). The core returns nothing for this call and does not map
     * HTTP 404, so a key that is already gone fails with the core's exception.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.gitLab(sdk).keys().deleteUserGpgKey(id());
    }

    /**
     * Returns the complete response. Like {@link #key()}, it holds the public key.
     *
     * @return the response record
     */
    public GitLabGpgKey response() {
        return response;
    }

    @Override
    public String toString() {
        return "GpgKey[id=" + response.id() + "]";
    }
}
