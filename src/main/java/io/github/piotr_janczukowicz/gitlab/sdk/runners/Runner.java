package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerManager;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiJobBasicWithProject;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetails;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetailsRunnerType;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabInstanceRunnersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A runner, as GitLab returned it. The object is immutable: it keeps the data of the moment it was loaded, and
 * {@link #response()} gives the complete response of {@code GET /runners/{id}}. A runner that came from a project or
 * group list is loaded again, because the list holds only a summary: one extra request per item.
 *
 * <p>One class serves the instance, group and project runners, and a runner can be enabled in several projects, so
 * unlike most objects of this SDK a runner has no parent navigation: it does not offer a method that returns a project
 * or group. {@link #update()}, {@link #delete()}, {@link #managers()}, {@link #jobs()} and
 * {@link #resetAuthenticationToken()} always use the global runner id, even for a runner that came from a group list.
 * Runner registration, verification, the instance-level runner administration, runner creation through the
 * authenticated user, and the group runner get and delete are not wrapped; use the core client for them.
 */
public final class Runner {

    private final GitLabSdk sdk;
    private final GitLabCiRunnerDetails response;

    /**
     * Wraps a response. Applications normally get this object from {@link GitLabSdk#runner(long)}, a project or a
     * group; call the constructor only to wrap a response that was loaded some other way.
     *
     * @param sdk the SDK whose client the runner calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Runner(final GitLabSdk sdk, final GitLabCiRunnerDetails response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id of the runner, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
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
     * Returns the name the runner reports.
     *
     * @return the name, or empty when GitLab sent none
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the type of the runner.
     *
     * @return the type, or empty when GitLab sent none
     * @throws IllegalArgumentException if the SDK does not know the type; {@link #typeName()} gives the raw name
     */
    public Optional<RunnerType> type() {
        return typeName().map(RunnerType::from);
    }

    /**
     * Returns the type of the runner as the core reads it from the response, such as {@code project_type}. A type the
     * core does not know reads as {@code unrecognized}.
     *
     * @return the raw name, or empty when GitLab sent none
     */
    public Optional<String> typeName() {
        return Optional.ofNullable(response.runnerType()).map(GitLabCiRunnerDetailsRunnerType::wireValue);
    }

    /**
     * Tells whether the runner is online.
     *
     * @return {@code true} if GitLab reports it as online; {@code false} also when the flag is absent
     */
    public boolean isOnline() {
        return Boolean.TRUE.equals(response.online());
    }

    /**
     * Tells whether the runner is paused.
     *
     * @return {@code true} if GitLab reports it as paused; {@code false} also when the flag is absent
     */
    public boolean isPaused() {
        return Boolean.TRUE.equals(response.paused());
    }

    /**
     * Tells whether the runner is active.
     *
     * @return {@code true} if GitLab reports it as active; {@code false} also when the flag is absent
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(response.active());
    }

    /**
     * Returns the status as GitLab spells it, such as {@code online}, {@code offline}, {@code stale} or
     * {@code never_contacted}.
     *
     * @return the raw status
     */
    public String status() {
        return requireNonNull(response.status(), "status");
    }

    /**
     * Returns the tags of the runner. The response holds them in one string; the SDK splits it on commas and trims each
     * tag, and an empty or blank string gives an empty list. This mapping is not verified against a live GitLab.
     *
     * @return the tags, as an immutable list
     */
    public List<String> tagList() {
        final String tags = response.tagList();
        if (tags == null) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    /**
     * Returns the version of the runner software.
     *
     * @return the version, or empty when GitLab sent none
     */
    public Optional<String> version() {
        return Optional.ofNullable(response.version());
    }

    /**
     * Returns the time the runner last contacted GitLab.
     *
     * @return the time, or empty when GitLab sent none
     */
    public Optional<Instant> contactedAt() {
        return Optional.ofNullable(response.contactedAt());
    }

    /**
     * Starts a change of the runner. Nothing is sent until {@link RunnerUpdate#send()}.
     *
     * @return the update builder
     */
    public RunnerUpdate update() {
        return new RunnerUpdate(this);
    }

    /**
     * Loads the runner again ({@code GET /runners/{id}}). This object is not changed.
     *
     * @return the runner as GitLab holds it now
     * @throws IllegalStateException if the runner is gone
     * @throws IOException if the transport fails
     */
    public Runner refresh() throws IOException {
        return sdk.runner(id()).orElseThrow(() -> new IllegalStateException("Runner " + id() + " is gone"));
    }

    /**
     * Deletes the runner ({@code DELETE /runners/{id}}). This removes the runner everywhere, from every project and
     * group it is enabled in; to stop one project from using it, use {@link #disable(Project)}.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().runners().instance().deleteById(id());
    }

    /**
     * Disables the runner for a project ({@code DELETE /projects/{id}/runners/{runner_id}}). The runner itself stays.
     * This is only valid for a runner that is enabled in that project; GitLab refuses it for a runner that belongs to
     * the project alone.
     *
     * @param project the project to disable the runner for
     * @throws NullPointerException if {@code project} is {@code null}
     * @throws IOException if the transport fails
     */
    public void disable(final Project project) throws IOException {
        final Project target = requireNonNull(project, "project");
        Internals.gitLab(target).runners().project().delete(target.id(), id());
    }

    /**
     * Lists the managers of the runner ({@code GET /runners/{id}/managers}). The core's records are passed through.
     *
     * @return the managers, as an immutable list
     * @throws IOException if the transport fails
     */
    public List<GitLabCiRunnerManager> managers() throws IOException {
        return List.copyOf(gitLab().runners().instance().listManagers(id()));
    }

    /**
     * Lists the jobs the runner processed ({@code GET /runners/{id}/jobs}). The core's records are passed through.
     * Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the jobs
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabCiJobBasicWithProject> jobs() throws IOException {
        return gitLab().runners().instance().listJobs(id()).stream();
    }

    /**
     * Resets the authentication token of the runner ({@code POST /runners/{id}/reset_authentication_token}). The old
     * token stops working, and the new one is secret, do not log it.
     *
     * @return the new token
     * @throws IOException if the transport fails
     */
    public RegistrationToken resetAuthenticationToken() throws IOException {
        return new RegistrationToken(gitLab().runners().instance().resetAuthenticationTokenById(id()));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCiRunnerDetails response() {
        return response;
    }

    Runner apply(final GitLabInstanceRunnersUpdatePayload payload) throws IOException {
        return new Runner(sdk, gitLab().runners().instance().update(id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(sdk);
    }
}
