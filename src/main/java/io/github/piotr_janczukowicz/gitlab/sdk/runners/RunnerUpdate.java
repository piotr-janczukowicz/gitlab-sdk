package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabInstanceRunnersUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import java.util.List;

/**
 * Collects changes to a {@link Runner} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. Text is sent as given, an empty string included; whether GitLab clears the
 * value is unverified live. The access level is not offered here; use the core client for it.
 */
public final class RunnerUpdate {

    private final Runner runner;
    private final GitLabInstanceRunnersUpdatePayloadBuilder payload =
            GitLabInstanceRunnersUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param runner the runner to change
     * @throws NullPointerException if {@code runner} is {@code null}
     */
    public RunnerUpdate(final Runner runner) {
        this.runner = requireNonNull(runner, "runner");
    }

    /**
     * Sets the description. An empty description is sent as given.
     *
     * @param description the description
     * @return this update
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public RunnerUpdate description(final String description) {
        payload.description(requireNonNull(description, "description"));
        return changed();
    }

    /**
     * Sets whether the runner is paused, that is, does not pick up jobs.
     *
     * @param paused {@code true} to pause, {@code false} to resume
     * @return this update
     */
    public RunnerUpdate paused(final boolean paused) {
        payload.paused(paused);
        return changed();
    }

    /**
     * Sets whether the runner is active. GitLab deprecates this flag in favour of {@link #paused(boolean)}.
     *
     * @param active {@code true} to activate, {@code false} to deactivate
     * @return this update
     */
    public RunnerUpdate active(final boolean active) {
        payload.active(active);
        return changed();
    }

    /**
     * Sets the tags of the runner, replacing the existing ones. The list is copied, so later changes to it are not
     * sent; it goes to the core as a list, as the update request takes it, and an empty list is sent as given.
     *
     * @param tags the tags
     * @return this update
     * @throws NullPointerException if {@code tags} is {@code null} or holds a {@code null} element
     */
    public RunnerUpdate tags(final List<String> tags) {
        payload.tagList(List.copyOf(requireNonNull(tags, "tags")));
        return changed();
    }

    /**
     * Sets whether the runner picks up jobs that have no tags.
     *
     * @param runUntagged {@code true} to pick up untagged jobs
     * @return this update
     */
    public RunnerUpdate runUntagged(final boolean runUntagged) {
        payload.runUntagged(runUntagged);
        return changed();
    }

    /**
     * Sets whether the runner is locked to its current project.
     *
     * @param locked {@code true} to lock the runner
     * @return this update
     */
    public RunnerUpdate locked(final boolean locked) {
        payload.locked(locked);
        return changed();
    }

    /**
     * Sets the maximum job timeout of the runner, in seconds, sent as given.
     *
     * @param maximumTimeout the timeout in seconds
     * @return this update
     */
    public RunnerUpdate maximumTimeout(final int maximumTimeout) {
        payload.maximumTimeout(maximumTimeout);
        return changed();
    }

    /**
     * Sets the maintenance note. An empty note is sent as given.
     *
     * @param maintenanceNote the note
     * @return this update
     * @throws NullPointerException if {@code maintenanceNote} is {@code null}
     */
    public RunnerUpdate maintenanceNote(final String maintenanceNote) {
        payload.maintenanceNote(requireNonNull(maintenanceNote, "maintenanceNote"));
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /runners/{id}}). The runner this update was made from is not changed.
     *
     * @return the runner as GitLab returned it after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public Runner send() throws IOException {
        state.requireChange();
        return runner.apply(payload.build());
    }

    private RunnerUpdate changed() {
        state.mark();
        return this;
    }
}
