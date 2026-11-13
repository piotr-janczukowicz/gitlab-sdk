package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserPublic;
import io.github.piotr_janczukowicz.gitlab.client.GitLabCaller;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;

/**
 * Entry point of the object layer. It wraps an existing {@link GitLab} client, so authentication, base URI, tier and
 * limits are configured on the client as usual; the SDK has no configuration of its own.
 */
public final class GitLabSdk {

    static {
        Internals.registerSdkAccess(GitLabSdk::gitLab);
    }

    private final GitLab gitlab;

    private GitLabSdk(final GitLab gitlab) {
        this.gitlab = requireNonNull(gitlab, "gitlab");
    }

    /**
     * Wraps a client.
     *
     * @param gitlab the client every object of this SDK calls
     * @return the entry point
     * @throws NullPointerException if {@code gitlab} is {@code null}
     */
    public static GitLabSdk of(final GitLab gitlab) {
        return new GitLabSdk(gitlab);
    }

    /**
     * Wraps a caller, so every object of this SDK calls the resources of {@code new GitLab(caller)}.
     *
     * @param caller the caller that executes the requests of every object of this SDK
     * @return the entry point
     * @throws NullPointerException if {@code caller} is {@code null}
     */
    public static GitLabSdk of(final GitLabCaller caller) {
        return of(new GitLab(requireNonNull(caller, "caller")));
    }

    /**
     * Wraps a client, the shortest way to start: it is the same as {@code of(new GitLab(GitLabCaller.of(client)))}. The
     * client stays yours; closing it is up to you.
     *
     * @param client the client that sends the requests of every object of this SDK
     * @return the entry point
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public static GitLabSdk of(final GitLabClient client) {
        return of(GitLabCaller.of(requireNonNull(client, "client")));
    }

    /**
     * Returns a handle to a project. No request is made until {@link Project#fetch()}.
     *
     * @param id the numeric id or the plain full path of the project, for example {@code my-group/my-project}; the core
     *     encodes the path, so do not encode it yourself
     * @return the project handle
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public Project project(final String id) {
        return new Project(this, requireNotBlank(id, "id"), null);
    }

    /**
     * Returns a handle to a group. No request is made until {@link Group#fetch()}.
     *
     * @param id the numeric id or the plain full path of the group, for example {@code my-group}; the core encodes the
     *     path
     * @return the group handle
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public Group group(final String id) {
        return new Group(this, requireNotBlank(id, "id"), null);
    }

    /**
     * Loads a user ({@code GET /users/{id}}).
     *
     * @param id the numeric id of the user
     * @return the user, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<User> user(final long id) throws IOException {
        return gitlab.users().get(id).map(loaded -> new User(this, loaded));
    }

    /**
     * Returns the account that the client signs in as.
     *
     * @return the current account
     */
    public Me me() {
        return new Me(this);
    }

    /**
     * Asks GitLab for the id of the account the client signs in as ({@code GET /user}).
     *
     * @return the id
     * @throws IOException if the transport fails
     */
    @SuppressWarnings("PMD.LawOfDemeter")
    long currentUserId() throws IOException {
        final GitLabUserPublic current = gitlab.users().getUser();
        return requireNonNull(current.id(), "id");
    }

    /**
     * Returns the wrapped client for the objects of this SDK.
     *
     * @return the client
     */
    GitLab gitLab() {
        return gitlab;
    }
}
