package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabEnvironment;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsStopPayload;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsStopPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An environment of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment
 * it was loaded, and {@link #response()} gives the complete response. GitLab lists, loads, creates, updates and stops
 * an environment with the same record, so none of them needs an extra request. Protected environments (Premium) are not
 * wrapped; use the core client for them.
 */
public final class Environment {

    private final Project project;
    private final GitLabEnvironment response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Environment(final Project project, final GitLabEnvironment response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the environment belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the environment, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the name of the environment.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the slug of the environment.
     *
     * @return the slug
     */
    public String slug() {
        return requireNonNull(response.slug(), "slug");
    }

    /**
     * Returns the external URL of the environment.
     *
     * @return the URL, or empty when GitLab sent none
     */
    public Optional<String> externalUrl() {
        return Optional.ofNullable(response.externalUrl());
    }

    /**
     * Returns the tier of the environment.
     *
     * @return the tier, or empty when GitLab sent none
     * @throws IllegalArgumentException if GitLab sent a tier the SDK does not know; {@link #tierName()} still works
     */
    public Optional<EnvironmentTier> tier() {
        return tierName().map(EnvironmentTier::from);
    }

    /**
     * Returns the tier of the environment as GitLab spells it, such as {@code production}.
     *
     * @return the raw tier name, or empty when GitLab sent none
     */
    public Optional<String> tierName() {
        return Optional.ofNullable(response.tier());
    }

    /**
     * Returns the state of the environment as GitLab spells it: {@code available}, {@code stopping} or {@code stopped}.
     *
     * @return the raw state
     */
    public String state() {
        return requireNonNull(response.state(), "state");
    }

    /**
     * Returns the description.
     *
     * @return the description, or empty when GitLab sent none
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the Kubernetes namespace of the environment.
     *
     * @return the namespace, or empty when GitLab sent none
     */
    public Optional<String> kubernetesNamespace() {
        return Optional.ofNullable(response.kubernetesNamespace());
    }

    /**
     * Returns the path of the Flux resource of the environment.
     *
     * @return the path, or empty when GitLab sent none
     */
    public Optional<String> fluxResourcePath() {
        return Optional.ofNullable(response.fluxResourcePath());
    }

    /**
     * Starts a change of the environment. Nothing is sent until {@link EnvironmentUpdate#send()}. The name cannot be
     * changed.
     *
     * @return the update builder
     */
    public EnvironmentUpdate update() {
        return new EnvironmentUpdate(this);
    }

    /**
     * Stops the environment ({@code POST /projects/{id}/environments/{environment_id}/stop}) without {@code force}.
     * This object is not changed.
     *
     * @return the environment as GitLab returned it after the stop
     * @throws IOException if the transport fails
     */
    public Environment stop() throws IOException {
        return sendStop(GitLabProjectEnvironmentsStopPayloadBuilder.builder().build());
    }

    /**
     * Stops the environment ({@code POST /projects/{id}/environments/{environment_id}/stop}) with {@code force}. This
     * object is not changed.
     *
     * @param force {@code true} to stop the environment without running its stop action
     * @return the environment as GitLab returned it after the stop
     * @throws IOException if the transport fails
     */
    public Environment stop(final boolean force) throws IOException {
        return sendStop(GitLabProjectEnvironmentsStopPayloadBuilder.builder()
                .force(force)
                .build());
    }

    /**
     * Deletes the environment ({@code DELETE /projects/{id}/environments/{environment_id}}). GitLab deletes only a
     * stopped environment.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().environments().delete(project.id(), id());
    }

    /**
     * Lists the deployments of the environment ({@code GET /projects/{id}/deployments} with {@code environment}).
     * GitLab matches the environment by its name, not by its id. GitLab lists a summary of each deployment, so each one
     * is loaded ({@code GET /projects/{id}/deployments/{deployment_id}}) as the stream reaches it, which costs one
     * request per item, and one that is gone by then is skipped. Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the deployments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Deployment> deployments() throws IOException {
        return project.deployments(GitLabProjectDeploymentsListFilterBuilder.builder()
                .environment(name())
                .build());
    }

    /**
     * Loads the environment again ({@code GET /projects/{id}/environments/{environment_id}}). This object is not
     * changed.
     *
     * @return the environment as GitLab holds it now
     * @throws IllegalStateException if the environment is gone
     * @throws IOException if the transport fails
     */
    public Environment refresh() throws IOException {
        return project.environment(id())
                .orElseThrow(() -> new IllegalStateException("Environment " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabEnvironment response() {
        return response;
    }

    Environment apply(final GitLabProjectEnvironmentsUpdatePayload payload) throws IOException {
        return new Environment(project, gitLab().environments().update(project.id(), id(), payload));
    }

    private Environment sendStop(final GitLabProjectEnvironmentsStopPayload payload) throws IOException {
        return new Environment(project, gitLab().environments().stop(project.id(), id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
