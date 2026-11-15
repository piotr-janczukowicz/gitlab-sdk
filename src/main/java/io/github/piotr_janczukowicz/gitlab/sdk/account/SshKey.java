package io.github.piotr_janczukowicz.gitlab.sdk.account;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSSHKey;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * An SSH key of the account the client signs in as, as GitLab returned it. The object is immutable. The public key is
 * available through {@link #key()} and {@link #response()}, but it is never part of {@link #toString()}.
 */
public final class SshKey {

    private final GitLabSdk sdk;
    private final GitLabSSHKey response;

    /**
     * Wraps a response. Applications normally get this object from {@code Me.sshKeys()} or {@code Me.addSshKey}; call
     * the constructor only to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public SshKey(final GitLabSdk sdk, final GitLabSSHKey response) {
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
     * Returns the title of the key.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
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
     * Returns when the key expires.
     *
     * @return the expiry, or empty when the key does not expire
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(response.expiresAt());
    }

    /**
     * Deletes the key ({@code DELETE /user/keys/{key_id}}). A key that is already gone is not an error: the core maps
     * HTTP 404 to an empty result, which is ignored.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.gitLab(sdk).usersshandgpgkeys().delete(id());
    }

    /**
     * Returns the complete response. Like {@link #key()}, it holds the public key.
     *
     * @return the response record
     */
    public GitLabSSHKey response() {
        return response;
    }

    @Override
    public String toString() {
        return "SshKey[id=" + response.id() + ", title=" + response.title() + "]";
    }
}
