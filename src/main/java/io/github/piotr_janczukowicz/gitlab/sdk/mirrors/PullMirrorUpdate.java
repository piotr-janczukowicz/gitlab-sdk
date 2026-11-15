package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabProjectPullMirroringUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link PullMirror} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The object has no {@code toString()}, so a password set here is never printed.
 */
public final class PullMirrorUpdate {

    private final PullMirror mirror;
    private final GitLabProjectPullMirroringUpdatePayloadBuilder payload =
            GitLabProjectPullMirroringUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param mirror the mirror to change
     * @throws NullPointerException if {@code mirror} is {@code null}
     */
    public PullMirrorUpdate(final PullMirror mirror) {
        this.mirror = requireNonNull(mirror, "mirror");
    }

    /**
     * Switches the mirror on or off.
     *
     * @param enabled {@code true} to switch it on
     * @return this update
     */
    public PullMirrorUpdate enabled(final boolean enabled) {
        payload.enabled(enabled);
        return changed();
    }

    /**
     * Sets the address GitLab pulls from.
     *
     * @param url the address
     * @return this update
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is blank
     */
    public PullMirrorUpdate url(final String url) {
        payload.url(requireNotBlank(url, "url"));
        return changed();
    }

    /**
     * Sets the user name GitLab authenticates with.
     *
     * @param user the user name
     * @return this update
     * @throws NullPointerException if {@code user} is {@code null}
     */
    public PullMirrorUpdate authUser(final String user) {
        payload.authUser(requireNonNull(user, "user"));
        return changed();
    }

    /**
     * Sets the password GitLab authenticates with. It is write-only and is never printed by this SDK.
     *
     * @param password the password
     * @return this update
     * @throws NullPointerException if {@code password} is {@code null}
     */
    public PullMirrorUpdate authPassword(final String password) {
        payload.authPassword(requireNonNull(password, "password"));
        return changed();
    }

    /**
     * Sets whether a pull starts the pipelines of the project.
     *
     * @param trigger {@code true} to start them
     * @return this update
     */
    public PullMirrorUpdate mirrorTriggerBuilds(final boolean trigger) {
        payload.mirrorTriggerBuilds(trigger);
        return changed();
    }

    /**
     * Sets whether only protected branches are mirrored.
     *
     * @param onlyProtected {@code true} to mirror protected branches only
     * @return this update
     */
    public PullMirrorUpdate onlyMirrorProtectedBranches(final boolean onlyProtected) {
        payload.onlyMirrorProtectedBranches(onlyProtected);
        return changed();
    }

    /**
     * Sets whether a pull overwrites branches that diverged.
     *
     * @param overwrite {@code true} to overwrite them
     * @return this update
     */
    public PullMirrorUpdate mirrorOverwritesDivergedBranches(final boolean overwrite) {
        payload.mirrorOverwritesDivergedBranches(overwrite);
        return changed();
    }

    /**
     * Sets the regular expression that selects the branches to mirror.
     *
     * @param regex the expression
     * @return this update
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public PullMirrorUpdate mirrorBranchRegex(final String regex) {
        payload.mirrorBranchRegex(requireNotBlank(regex, "mirrorBranchRegex"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/mirror/pull}). The mirror this update was made from is not changed.
     *
     * @return the mirror as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public PullMirror send() throws IOException {
        state.requireChange();
        return mirror.apply(payload.build());
    }

    private PullMirrorUpdate changed() {
        state.mark();
        return this;
    }
}
