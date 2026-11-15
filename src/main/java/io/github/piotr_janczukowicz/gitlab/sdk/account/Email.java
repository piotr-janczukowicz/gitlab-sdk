package io.github.piotr_janczukowicz.gitlab.sdk.account;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabEmail;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/** An email address of the account the client signs in as, as GitLab returned it. The object is immutable. */
public final class Email {

    private final GitLabSdk sdk;
    private final GitLabEmail response;

    /**
     * Wraps a response. Applications normally get this object from {@code Me.emails()} or {@code Me.addEmail}; call the
     * constructor only to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Email(final GitLabSdk sdk, final GitLabEmail response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id of the address.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the address.
     *
     * @return the email address
     */
    public String address() {
        return requireNonNull(response.email(), "email");
    }

    /**
     * Returns when the address was confirmed.
     *
     * @return the time, or empty while it is unconfirmed
     */
    public Optional<Instant> confirmedAt() {
        return Optional.ofNullable(response.confirmedAt());
    }

    /**
     * Deletes the address ({@code DELETE /user/emails/{email_id}}). The core returns nothing for this call and does not
     * map HTTP 404, so an address that is already gone fails with the core's exception.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.gitLab(sdk).useremailaddresses().delete(id());
    }

    /**
     * Returns the complete response.
     *
     * @return the response record
     */
    public GitLabEmail response() {
        return response;
    }
}
