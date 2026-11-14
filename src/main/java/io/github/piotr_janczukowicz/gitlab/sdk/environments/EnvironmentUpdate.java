package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsUpdateTier;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.IOException;

/**
 * Collects changes to an {@link Environment} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The Kubernetes namespace, the Flux resource path and the cluster agent can be
 * set or cleared; clearing sends JSON {@code null}, and the last call for a field wins. GitLab's behavior for the
 * clearing on a real instance is not yet verified live. The name cannot be changed, and the auto-stop setting is not
 * offered here.
 */
public final class EnvironmentUpdate {

    private final Environment environment;
    private final GitLabProjectEnvironmentsUpdatePayloadBuilder payload =
            GitLabProjectEnvironmentsUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param environment the environment to change
     * @throws NullPointerException if {@code environment} is {@code null}
     */
    public EnvironmentUpdate(final Environment environment) {
        this.environment = requireNonNull(environment, "environment");
    }

    /**
     * Sets the external URL. An empty URL is sent as given.
     *
     * @param externalUrl the URL
     * @return this update
     * @throws NullPointerException if {@code externalUrl} is {@code null}
     */
    public EnvironmentUpdate externalUrl(final String externalUrl) {
        payload.externalUrl(requireNonNull(externalUrl, "externalUrl"));
        return changed();
    }

    /**
     * Sets the tier.
     *
     * @param tier the tier
     * @return this update
     * @throws NullPointerException if {@code tier} is {@code null}
     */
    public EnvironmentUpdate tier(final EnvironmentTier tier) {
        payload.tier(GitLabProjectEnvironmentsUpdateTier.valueOf(
                requireNonNull(tier, "tier").name()));
        return changed();
    }

    /**
     * Sets the description. An empty description is sent as given.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public EnvironmentUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets the Kubernetes namespace. The last call of this method or of {@link #clearKubernetesNamespace()} wins.
     *
     * @param namespace the namespace
     * @return this update
     * @throws NullPointerException if {@code namespace} is {@code null}
     */
    public EnvironmentUpdate kubernetesNamespace(final String namespace) {
        payload.kubernetesNamespace(OptionalNull.of(requireNonNull(namespace, "namespace")));
        return changed();
    }

    /**
     * Removes the Kubernetes namespace. The request is a {@code PUT} with {@code "kubernetes_namespace": null}. The
     * last call of this method or of {@link #kubernetesNamespace(String)} wins.
     *
     * @return this update
     */
    public EnvironmentUpdate clearKubernetesNamespace() {
        payload.kubernetesNamespace(OptionalNull.ofNull());
        return changed();
    }

    /**
     * Sets the path of the Flux resource. The last call of this method or of {@link #clearFluxResourcePath()} wins.
     *
     * @param path the path
     * @return this update
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public EnvironmentUpdate fluxResourcePath(final String path) {
        payload.fluxResourcePath(OptionalNull.of(requireNonNull(path, "path")));
        return changed();
    }

    /**
     * Removes the path of the Flux resource. The request is a {@code PUT} with {@code "flux_resource_path": null}. The
     * last call of this method or of {@link #fluxResourcePath(String)} wins.
     *
     * @return this update
     */
    public EnvironmentUpdate clearFluxResourcePath() {
        payload.fluxResourcePath(OptionalNull.ofNull());
        return changed();
    }

    /**
     * Sets the cluster agent. The last call of this method or of {@link #clearClusterAgent()} wins.
     *
     * @param clusterAgentId the id of the cluster agent
     * @return this update
     */
    public EnvironmentUpdate clusterAgentId(final long clusterAgentId) {
        payload.clusterAgentId(OptionalNull.of(clusterAgentId));
        return changed();
    }

    /**
     * Removes the cluster agent. The request is a {@code PUT} with {@code "cluster_agent_id": null}. The last call of
     * this method or of {@link #clusterAgentId(long)} wins.
     *
     * @return this update
     */
    public EnvironmentUpdate clearClusterAgent() {
        payload.clusterAgentId(OptionalNull.ofNull());
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/environments/{environment_id}}). The environment this update was
     * made from is not changed.
     *
     * @return the environment as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Environment send() throws IOException {
        state.requireChange();
        return environment.apply(payload.build());
    }

    private EnvironmentUpdate changed() {
        state.mark();
        return this;
    }
}
