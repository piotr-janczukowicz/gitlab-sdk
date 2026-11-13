package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUserProfile;
import java.io.IOException;

/**
 * A GitLab user, as GitLab returned it. The object is immutable, and {@link #response()} gives the complete response
 * for every field the accessors here do not cover.
 */
public final class User {

    private final GitLabSdk sdk;
    private final GitLabUserProfile response;

    User(final GitLabSdk sdk, final GitLabUserProfile response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the numeric id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the username.
     *
     * @return the username
     */
    public String username() {
        return requireNonNull(response.username(), "username");
    }

    /**
     * Reloads the user ({@code GET /users/{id}}). This object is not changed.
     *
     * @return the reloaded user
     * @throws IllegalStateException if GitLab no longer has the user
     * @throws IOException if the transport fails
     */
    public User refresh() throws IOException {
        return sdk.user(id()).orElseThrow(() -> new IllegalStateException("User " + id() + " is gone"));
    }

    /**
     * Returns the complete response.
     *
     * @return the response record
     */
    public GitLabUserProfile response() {
        return response;
    }
}
