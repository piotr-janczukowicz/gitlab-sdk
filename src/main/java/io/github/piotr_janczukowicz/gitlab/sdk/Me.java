package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.io.IOException;

/**
 * The account the client signs in as. It is a handle: it holds no data from GitLab and makes no request until
 * {@link #profile()}.
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
}
