package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.List;

/**
 * Collects changes to a {@link RemoteMirror} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The address of a mirror cannot be changed here: the core's payload has no
 * field for it. The object has no {@code toString()}.
 */
public final class RemoteMirrorUpdate {

    private final RemoteMirror mirror;
    private final GitLabProjectRemoteMirrorsUpdatePayloadBuilder payload =
            GitLabProjectRemoteMirrorsUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param mirror the mirror to change
     * @throws NullPointerException if {@code mirror} is {@code null}
     */
    public RemoteMirrorUpdate(final RemoteMirror mirror) {
        this.mirror = requireNonNull(mirror, "mirror");
    }

    /**
     * Switches the mirror on or off.
     *
     * @param enabled {@code true} to switch it on
     * @return this update
     */
    public RemoteMirrorUpdate enabled(final boolean enabled) {
        payload.enabled(enabled);
        return changed();
    }

    /**
     * Sets how GitLab authenticates to the target.
     *
     * @param method the method
     * @return this update
     * @throws NullPointerException if {@code method} is {@code null}
     */
    public RemoteMirrorUpdate authMethod(final MirrorAuthMethod method) {
        payload.authMethod(requireNonNull(method, "method").value());
        return changed();
    }

    /**
     * Sets whether refs that diverged are kept on the target.
     *
     * @param keep {@code true} to keep them
     * @return this update
     */
    public RemoteMirrorUpdate keepDivergentRefs(final boolean keep) {
        payload.keepDivergentRefs(keep);
        return changed();
    }

    /**
     * Sets whether only protected branches are mirrored.
     *
     * @param onlyProtected {@code true} to mirror protected branches only
     * @return this update
     */
    public RemoteMirrorUpdate onlyProtectedBranches(final boolean onlyProtected) {
        payload.onlyProtectedBranches(onlyProtected);
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
    public RemoteMirrorUpdate mirrorBranchRegex(final String regex) {
        payload.mirrorBranchRegex(requireNotBlank(regex, "mirrorBranchRegex"));
        return changed();
    }

    /**
     * Replaces the SSH host keys GitLab trusts for the target.
     *
     * @param hostKeys the host keys
     * @return this update
     * @throws NullPointerException if {@code hostKeys} or a key is {@code null}
     * @throws IllegalArgumentException if a key is blank
     */
    public RemoteMirrorUpdate hostKeys(final List<String> hostKeys) {
        payload.hostKeys(Arguments.requireNotBlankElements(hostKeys, "hostKeys"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/remote_mirrors/{mirror_id}}). The mirror this update was made from
     * is not changed.
     *
     * @return the mirror as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public RemoteMirror send() throws IOException {
        state.requireChange();
        return mirror.apply(payload.build());
    }

    private RemoteMirrorUpdate changed() {
        state.mark();
        return this;
    }
}
