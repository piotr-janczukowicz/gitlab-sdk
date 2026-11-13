package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.io.IOException;

/** The account the client signs in as. It makes no request until {@link #fetch()}. */
public final class Me {

    private final GitLabSdk sdk;

    Me(final GitLabSdk sdk) {
        this.sdk = requireNonNull(sdk, "sdk");
    }

    /**
     * Loads the current account ({@code GET /user}, then {@code GET /users/{id}}) as a user.
     *
     * @return the current user
     * @throws IllegalStateException if GitLab no longer has the account
     * @throws IOException if the transport fails
     */
    public User fetch() throws IOException {
        final long id = sdk.currentUserId();
        return sdk.user(id).orElseThrow(() -> new IllegalStateException("User " + id + " is gone"));
    }
}
