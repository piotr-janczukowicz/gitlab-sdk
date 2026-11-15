package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUserAssociationsCount;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUserProfile;
import java.io.IOException;
import java.util.Optional;

/**
 * A GitLab user, as GitLab returned it. The object is immutable, and {@link #response()} gives the complete response
 * for every field the accessors here do not cover.
 */
public final class User {

    private final GitLabSdk sdk;
    private final GitLabUserProfile response;

    /**
     * Wraps a response. Applications normally get this object from {@link GitLabSdk#user(long)} or
     * {@link Me#profile()}; call the constructor only to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK that makes the calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public User(final GitLabSdk sdk, final GitLabUserProfile response) {
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
     * Counts the groups, projects, issues and merge requests associated with the user ({@code GET
     * /users/{id}/associations_count}). Per GitLab's API description, administrators can query any user and other users
     * only themselves; the SDK does not check this, so for anyone else GitLab's refusal reaches the caller as the
     * core's exception. Not verified live.
     *
     * @return the counts, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitLabUserAssociationsCount> associationsCount() throws IOException {
        return sdk.gitLab().users().getAssociationsCount(id());
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
