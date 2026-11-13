package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to a {@link DeployKey} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}.
 */
public final class DeployKeyUpdate {

    private final DeployKey deployKey;
    private final GitLabProjectDeployKeysUpdatePayloadBuilder payload =
            GitLabProjectDeployKeysUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param deployKey the key to change
     * @throws NullPointerException if {@code deployKey} is {@code null}
     */
    public DeployKeyUpdate(final DeployKey deployKey) {
        this.deployKey = requireNonNull(deployKey, "deployKey");
    }

    /**
     * Sets the title.
     *
     * @param title the title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public DeployKeyUpdate title(final String title) {
        payload.title(requireNotBlank(title, "title"));
        return changed();
    }

    /**
     * Sets whether the key may push to the repository.
     *
     * @param canPush {@code true} to allow pushing
     * @return this update
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public DeployKeyUpdate canPush(final boolean canPush) {
        payload.canPush(canPush);
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/deploy_keys/{key_id}}) and loads the key again, because the core's
     * answer to the update is a different record than the key. The key this update was made from is not changed.
     *
     * @return the key as GitLab holds it after the update
     * @throws IllegalStateException if nothing was set, or the key is gone
     * @throws IOException if the transport fails
     */
    public DeployKey send() throws IOException {
        state.requireChange();
        return deployKey.apply(payload.build());
    }

    private DeployKeyUpdate changed() {
        state.mark();
        return this;
    }
}
