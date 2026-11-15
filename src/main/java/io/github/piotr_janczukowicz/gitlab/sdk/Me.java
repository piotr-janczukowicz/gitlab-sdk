package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.keys.GitLabKeysCreateUserGpgKeyPayload;
import io.github.piotr_janczukowicz.gitlab.api.useremailaddresses.GitLabUserEmailAddressesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.usersshandgpgkeys.GitLabUserSSHAndGPGKeysCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.usersshandgpgkeys.GitLabUserSSHAndGPGKeysCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.account.Email;
import io.github.piotr_janczukowicz.gitlab.sdk.account.GpgKey;
import io.github.piotr_janczukowicz.gitlab.sdk.account.SshKey;
import java.io.IOException;
import java.time.Instant;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * The account the client signs in as. It is a handle: it holds no data from GitLab and makes no request until
 * {@link #profile()}. The keys and email addresses of the account are listed and added here; only the signed-in
 * account's own keys are reachable, and no other user's.
 */
public final class Me {

    private final GitLabSdk sdk;

    /**
     * Creates the handle. Applications normally get it from {@link GitLabSdk#me()}. Nothing is requested until a method
     * is called.
     *
     * @param sdk the SDK that makes the calls
     * @throws NullPointerException if {@code sdk} is {@code null}
     */
    public Me(final GitLabSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Loads the profile of the account ({@code GET /user} for its id, then {@code GET /users/{id}}) as a user.
     *
     * @return the current user
     * @throws IllegalStateException if GitLab no longer has the account
     * @throws IOException if the transport fails
     */
    public User profile() throws IOException {
        final long id = sdk.currentUserId();
        return sdk.user(id).orElseThrow(() -> new IllegalStateException("User " + id + " is gone"));
    }

    /**
     * Lists the SSH keys of the account ({@code GET /user/keys}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SshKey> sshKeys() throws IOException {
        return sdk.gitLab().usersshandgpgkeys().list().stream().map(item -> new SshKey(sdk, item));
    }

    /**
     * Adds an SSH key to the account ({@code POST /user/keys}). The usage type is left to GitLab's default.
     *
     * @param title the title of the key
     * @param key the public key
     * @param expiresAt when the key expires, or {@code null} for a key that does not expire
     * @return the added key, as GitLab answers
     * @throws NullPointerException if {@code title} or {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code key} is blank; no request is made
     * @throws IOException if the transport fails
     */
    public SshKey addSshKey(final String title, final String key, final @Nullable Instant expiresAt)
            throws IOException {
        final GitLabUserSSHAndGPGKeysCreatePayload payload = GitLabUserSSHAndGPGKeysCreatePayloadBuilder.builder()
                .title(requireNotBlank(title, "title"))
                .key(requireNotBlank(key, "key"))
                .expiresAt(expiresAt)
                .build();
        return new SshKey(sdk, sdk.gitLab().usersshandgpgkeys().create(payload));
    }

    /**
     * Lists the email addresses of the account ({@code GET /user/emails}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the addresses
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Email> emails() throws IOException {
        return sdk.gitLab().useremailaddresses().list().stream().map(item -> new Email(sdk, item));
    }

    /**
     * Adds an email address to the account ({@code POST /user/emails}). GitLab may send a confirmation mail, and the
     * address then stays unconfirmed until it is confirmed.
     *
     * @param email the address
     * @return the added address, as GitLab answers
     * @throws NullPointerException if {@code email} is {@code null}
     * @throws IllegalArgumentException if {@code email} is blank; no request is made
     * @throws IOException if the transport fails
     */
    public Email addEmail(final String email) throws IOException {
        final GitLabUserEmailAddressesCreatePayload payload =
                new GitLabUserEmailAddressesCreatePayload(requireNotBlank(email, "email"));
        return new Email(sdk, sdk.gitLab().useremailaddresses().create(payload));
    }

    /**
     * Lists the GPG keys of the account ({@code GET /user/gpg_keys}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GpgKey> gpgKeys() throws IOException {
        return sdk.gitLab().keys().listUserGpgKeys().stream().map(item -> new GpgKey(sdk, item));
    }

    /**
     * Adds a GPG key to the account ({@code POST /user/gpg_keys}).
     *
     * @param key the armored public key
     * @return the added key, as GitLab answers
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank; no request is made
     * @throws IOException if the transport fails
     */
    public GpgKey addGpgKey(final String key) throws IOException {
        final GitLabKeysCreateUserGpgKeyPayload payload =
                new GitLabKeysCreateUserGpgKeyPayload(requireNotBlank(key, "key"));
        return new GpgKey(sdk, sdk.gitLab().keys().createUserGpgKey(payload));
    }
}
