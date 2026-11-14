package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentApproval;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentExtended;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsApprovalPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsApprovalStatus;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabProjectDeploymentsUpdateStatus;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A deployment of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response of {@code GET /projects/{id}/deployments/{id}}. A
 * deployment that came from a list is loaded again, because the list holds only a summary.
 */
public final class Deployment {

    private final Project project;
    private final GitLabDeploymentExtended response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Deployment(final Project project, final GitLabDeploymentExtended response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the deployment belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the deployment, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the project-internal id of the deployment.
     *
     * @return the iid
     */
    public long iid() {
        return requireNonNull(response.iid(), "iid");
    }

    /**
     * Returns the ref (branch or tag name) the deployment was made from.
     *
     * @return the ref
     */
    public String ref() {
        return requireNonNull(response.ref(), "ref");
    }

    /**
     * Returns the commit SHA the deployment was made from.
     *
     * @return the SHA
     */
    public String sha() {
        return requireNonNull(response.sha(), "sha");
    }

    /**
     * Returns the status of the deployment.
     *
     * @return the status
     * @throws IllegalArgumentException if GitLab sent a status the SDK does not know; {@link #statusName()} still works
     */
    public DeploymentStatus status() {
        return DeploymentStatus.from(statusName());
    }

    /**
     * Returns the status of the deployment as GitLab spells it, such as {@code success}.
     *
     * @return the raw status name
     */
    public String statusName() {
        return requireNonNull(response.status(), "status");
    }

    /**
     * Sets the status of the deployment ({@code PUT /projects/{id}/deployments/{deployment_id}}). This object is not
     * changed.
     *
     * @param status the new status; GitLab accepts only {@link DeploymentStatus#RUNNING},
     *     {@link DeploymentStatus#SUCCESS}, {@link DeploymentStatus#FAILED} and {@link DeploymentStatus#CANCELED}
     * @return the deployment as GitLab returned it after the update
     * @throws NullPointerException if {@code status} is {@code null}
     * @throws IllegalArgumentException if GitLab does not accept the status; no request is made
     * @throws IOException if the transport fails
     */
    public Deployment updateStatus(final DeploymentStatus status) throws IOException {
        if (!requireNonNull(status, "status").isSettable()) {
            throw new IllegalArgumentException(
                    "A deployment status can be set to running, success, failed or canceled, not " + status.value());
        }
        final GitLabDeploymentExtended answer = gitLab().deployments()
                .update(
                        project.id(),
                        id(),
                        GitLabProjectDeploymentsUpdatePayloadBuilder.builder()
                                .status(GitLabProjectDeploymentsUpdateStatus.valueOf(status.name()))
                                .build());
        return new Deployment(project, answer);
    }

    /**
     * Approves the deployment ({@code POST /projects/{id}/deployments/{deployment_id}/approval}). This needs GitLab
     * Premium; on a lower tier the core throws its tier exception, which passes through.
     *
     * @param comment the comment, or {@code null} to send none
     * @return the approval as GitLab returned it
     * @throws IllegalArgumentException if {@code comment} is blank
     * @throws IOException if the transport fails
     */
    public GitLabDeploymentApproval approve(final @Nullable String comment) throws IOException {
        return decide(GitLabProjectDeploymentsApprovalStatus.APPROVED, comment);
    }

    /**
     * Rejects the deployment ({@code POST /projects/{id}/deployments/{deployment_id}/approval}). This needs GitLab
     * Premium; on a lower tier the core throws its tier exception, which passes through.
     *
     * @param comment the comment, or {@code null} to send none
     * @return the approval as GitLab returned it
     * @throws IllegalArgumentException if {@code comment} is blank
     * @throws IOException if the transport fails
     */
    public GitLabDeploymentApproval reject(final @Nullable String comment) throws IOException {
        return decide(GitLabProjectDeploymentsApprovalStatus.REJECTED, comment);
    }

    /**
     * Lists the merge requests deployed by this deployment ({@code GET
     * /projects/{id}/deployments/{deployment_id}/merge_requests}). GitLab lists a summary of each merge request, so
     * each one is loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, and
     * one that is gone by then is skipped. A merge request is loaded from the project GitLab names in the summary; when
     * it names none, from {@link #project()}. Transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return gitLab().deployments().listMergeRequests(project.id(), id()).stream()
                .map(listed -> Lookups.find(
                        () -> projectOf(listed.projectId()).mergeRequest(requireNonNull(listed.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Deletes the deployment ({@code DELETE /projects/{id}/deployments/{deployment_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().deployments().delete(project.id(), id());
    }

    /**
     * Loads the deployment again ({@code GET /projects/{id}/deployments/{deployment_id}}). This object is not changed.
     *
     * @return the deployment as GitLab holds it now
     * @throws IllegalStateException if the deployment is gone
     * @throws IOException if the transport fails
     */
    public Deployment refresh() throws IOException {
        return project.deployment(id()).orElseThrow(() -> new IllegalStateException("Deployment " + id() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabDeploymentExtended response() {
        return response;
    }

    private GitLabDeploymentApproval decide(
            final GitLabProjectDeploymentsApprovalStatus status, final @Nullable String comment) throws IOException {
        final GitLabProjectDeploymentsApprovalPayloadBuilder payload =
                GitLabProjectDeploymentsApprovalPayloadBuilder.builder().status(status);
        if (comment != null) {
            payload.comment(requireNotBlank(comment, "comment"));
        }
        return gitLab().deployments().approval(project.id(), id(), payload.build());
    }

    private Project projectOf(final @Nullable Long listedIn) {
        if (listedIn == null || project.id().equals(String.valueOf(listedIn))) {
            return project;
        }
        return Internals.sdk(project).project(String.valueOf(listedIn));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
