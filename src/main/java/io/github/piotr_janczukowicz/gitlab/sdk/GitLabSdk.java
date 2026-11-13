package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.client.GitLabCaller;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;

/**
 * Entry point of the object layer. It wraps an existing {@link GitLab} client, so authentication, flavor, version and
 * limits are configured on the client as usual; the SDK has no configuration of its own.
 */
public final class GitLabSdk {

    static {
        Internals.registerSdkAccess(GitLabSdk::github);
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
     * Returns the wrapped client for the objects of this SDK.
     *
     * @return the client
     */
    GitLab github() {
        return gitlab;
    }
}
