package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabCiPipelineScheduleDetails;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreateVariablePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesCreateVariableType;
import io.github.piotr_janczukowicz.gitlab.api.pipelineschedules.GitLabProjectPipelineSchedulesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Pipeline;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.VariableType;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A pipeline schedule of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response of {@code GET
 * /projects/{id}/pipeline_schedules/{pipeline_schedule_id}}. A schedule that came from a list is loaded again, because
 * the list holds only a summary. The schedule's inputs, its pipelines list and its trigger endpoint are not wrapped;
 * use the core client for them. Pipeline schedules of a project that GitLab does not offer them for answer with the
 * core's exceptions, which pass through.
 */
public final class PipelineSchedule {

    private final Project project;
    private final GitLabCiPipelineScheduleDetails response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public PipelineSchedule(final Project project, final GitLabCiPipelineScheduleDetails response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the schedule belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the schedule, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    public String description() {
        return requireNonNull(response.description(), "description");
    }

    /**
     * Returns the branch or tag the schedule runs for.
     *
     * @return the ref
     */
    public String ref() {
        return requireNonNull(response.ref(), "ref");
    }

    /**
     * Returns the cron expression, as GitLab sent it.
     *
     * @return the cron expression
     */
    public String cron() {
        return requireNonNull(response.cron(), "cron");
    }

    /**
     * Returns the time zone of the cron expression.
     *
     * @return the time zone name, or empty when GitLab sent none
     */
    public Optional<String> cronTimezone() {
        return Optional.ofNullable(response.cronTimezone());
    }

    /**
     * Tells whether the schedule is active.
     *
     * @return {@code true} if GitLab reports it as active; {@code false} also when the flag is absent
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(response.active());
    }

    /**
     * Returns the time of the next run.
     *
     * @return the time, or empty when GitLab sent none
     */
    public Optional<Instant> nextRunAt() {
        return Optional.ofNullable(response.nextRunAt());
    }

    /**
     * Returns the owner of the schedule, the user the pipelines run as.
     *
     * @return the owner, or empty when GitLab sent none
     */
    public Optional<GitLabUserBasic> owner() {
        return Optional.ofNullable(response.owner());
    }

    /**
     * Loads the last pipeline the schedule started ({@code GET /projects/{id}/pipelines/{pipeline_id}}): one request
     * each time it is called.
     *
     * @return the pipeline, or empty when the schedule has not run or the pipeline is gone
     * @throws IOException if the transport fails
     */
    public Optional<Pipeline> lastPipeline() throws IOException {
        final GitLabCiPipelineBasic last = response.lastPipeline();
        if (last == null) {
            return Optional.empty();
        }
        return project.pipeline(requireNonNull(last.id(), "lastPipeline.id"));
    }

    /**
     * Returns the variables of the schedule, built from the response; no request is made. The list does not reflect
     * variables added, changed or deleted since this object was loaded: call {@link #refresh()} for the current ones.
     * The values are secrets, do not log them.
     *
     * @return the variables, as an immutable list
     */
    public List<ScheduleVariable> variables() {
        return Optional.ofNullable(response.variables()).orElse(List.of()).stream()
                .map(listed -> new ScheduleVariable(this, listed))
                .toList();
    }

    /**
     * Starts a change of the schedule. Nothing is sent until {@link PipelineScheduleUpdate#send()}.
     *
     * @return the update builder
     */
    public PipelineScheduleUpdate update() {
        return new PipelineScheduleUpdate(this);
    }

    /**
     * Makes the authenticated user the owner of the schedule ({@code POST
     * /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/take_ownership}). This object is not changed.
     *
     * @return the schedule as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public PipelineSchedule takeOwnership() throws IOException {
        return new PipelineSchedule(project, gitLab().pipelineschedules().takeOwnership(project.id(), id()));
    }

    /**
     * Starts a pipeline of the schedule now ({@code POST
     * /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/play}). GitLab queues the run, so the pipeline may
     * appear a moment later.
     *
     * @throws IOException if the transport fails
     */
    public void play() throws IOException {
        gitLab().pipelineschedules().play(project.id(), id());
    }

    /**
     * Adds a variable of the default type ({@code POST
     * /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/variables}). The value is secret, do not log it; an
     * empty value is sent as given.
     *
     * @param key the key of the variable
     * @param value the value
     * @return the new variable
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public ScheduleVariable addVariable(final String key, final String value) throws IOException {
        return create(GitLabProjectPipelineSchedulesCreateVariablePayloadBuilder.builder()
                .key(requireNotBlank(key, "key"))
                .value(requireNonNull(value, "value")));
    }

    /**
     * Adds a variable of a type ({@code POST /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/variables}). The
     * value is secret, do not log it; an empty value is sent as given.
     *
     * @param key the key of the variable
     * @param value the value
     * @param type the type
     * @return the new variable
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public ScheduleVariable addVariable(final String key, final String value, final VariableType type)
            throws IOException {
        return create(GitLabProjectPipelineSchedulesCreateVariablePayloadBuilder.builder()
                .key(requireNotBlank(key, "key"))
                .value(requireNonNull(value, "value"))
                .variableType(GitLabProjectPipelineSchedulesCreateVariableType.valueOf(
                        requireNonNull(type, "type").name())));
    }

    /**
     * Loads a variable by its key ({@code GET
     * /projects/{id}/pipeline_schedules/{pipeline_schedule_id}/variables/{key}}).
     *
     * @param key the key of the variable
     * @return the variable, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     * @throws IOException if the transport fails
     */
    public Optional<ScheduleVariable> variable(final String key) throws IOException {
        return gitLab().pipelineschedules()
                .getVariable(project.id(), id(), requireNotBlank(key, "key"))
                .map(loaded -> new ScheduleVariable(this, loaded));
    }

    /**
     * Deletes the schedule ({@code DELETE /projects/{id}/pipeline_schedules/{pipeline_schedule_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().pipelineschedules().delete(project.id(), id());
    }

    /**
     * Loads the schedule again ({@code GET /projects/{id}/pipeline_schedules/{pipeline_schedule_id}}). This object is
     * not changed.
     *
     * @return the schedule as GitLab holds it now
     * @throws IllegalStateException if the schedule is gone
     * @throws IOException if the transport fails
     */
    public PipelineSchedule refresh() throws IOException {
        return project.pipelineSchedule(id())
                .orElseThrow(() -> new IllegalStateException("Pipeline schedule " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from. It holds the secret variable values; the records print
     * them redacted.
     *
     * @return the response record
     */
    public GitLabCiPipelineScheduleDetails response() {
        return response;
    }

    PipelineSchedule apply(final GitLabProjectPipelineSchedulesUpdatePayload payload) throws IOException {
        return new PipelineSchedule(project, gitLab().pipelineschedules().update(project.id(), id(), payload));
    }

    private ScheduleVariable create(final GitLabProjectPipelineSchedulesCreateVariablePayloadBuilder payload)
            throws IOException {
        return new ScheduleVariable(
                this, gitLab().pipelineschedules().createVariable(project.id(), id(), payload.build()));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
