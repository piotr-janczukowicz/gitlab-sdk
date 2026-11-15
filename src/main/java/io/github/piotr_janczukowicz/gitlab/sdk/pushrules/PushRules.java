package io.github.piotr_janczukowicz.gitlab.sdk.pushrules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectProjectPushRulesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectPushRule;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * The push rules of a {@link Project}, as GitLab returned them. A project has at most one set of push rules. Per
 * GitLab's API description, push rules are a Premium feature: on a lower tier the core's {@code GitLabTierException}
 * passes through. The object is immutable, and {@link #response()} gives the complete response. A flag the response
 * leaves out counts as {@code false}.
 */
public final class PushRules {

    private final Project project;
    private final GitLabProjectPushRule response;

    /**
     * Wraps a response. Applications normally get this object from the project; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public PushRules(final Project project, final GitLabProjectPushRule response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the rules belong to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the id of the set of rules.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns when the rules were created.
     *
     * @return the creation time, or empty when the response has none
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(response.createdAt());
    }

    /**
     * Returns the regular expression every commit message must match.
     *
     * @return the expression, or empty when the response has none
     */
    public Optional<String> commitMessageRegex() {
        return Optional.ofNullable(response.commitMessageRegex());
    }

    /**
     * Returns the regular expression no commit message may match.
     *
     * @return the expression, or empty when the response has none
     */
    public Optional<String> commitMessageNegativeRegex() {
        return Optional.ofNullable(response.commitMessageNegativeRegex());
    }

    /**
     * Returns the regular expression every branch name must match.
     *
     * @return the expression, or empty when the response has none
     */
    public Optional<String> branchNameRegex() {
        return Optional.ofNullable(response.branchNameRegex());
    }

    /**
     * Returns the regular expression every commit author email must match.
     *
     * @return the expression, or empty when the response has none
     */
    public Optional<String> authorEmailRegex() {
        return Optional.ofNullable(response.authorEmailRegex());
    }

    /**
     * Returns the regular expression no committed file name may match.
     *
     * @return the expression, or empty when the response has none
     */
    public Optional<String> fileNameRegex() {
        return Optional.ofNullable(response.fileNameRegex());
    }

    /**
     * Returns the largest file a push may add, in megabytes.
     *
     * @return the limit, or empty when the response has none
     */
    public Optional<Long> maxFileSize() {
        return Optional.ofNullable(response.maxFileSize());
    }

    /**
     * Tells whether tags may not be deleted.
     *
     * @return {@code true} when the response says so
     */
    public boolean denyDeleteTag() {
        return Boolean.TRUE.equals(response.denyDeleteTag());
    }

    /**
     * Tells whether the author of a commit must be a member of the project.
     *
     * @return {@code true} when the response says so
     */
    public boolean memberCheck() {
        return Boolean.TRUE.equals(response.memberCheck());
    }

    /**
     * Tells whether files that may hold secrets are rejected.
     *
     * @return {@code true} when the response says so
     */
    public boolean preventSecrets() {
        return Boolean.TRUE.equals(response.preventSecrets());
    }

    /**
     * Tells whether the committer must be the user who pushes.
     *
     * @return {@code true} when the response says so
     */
    public boolean commitCommitterCheck() {
        return Boolean.TRUE.equals(response.commitCommitterCheck());
    }

    /**
     * Tells whether the committer name must match the name of the user who pushes.
     *
     * @return {@code true} when the response says so
     */
    public boolean commitCommitterNameCheck() {
        return Boolean.TRUE.equals(response.commitCommitterNameCheck());
    }

    /**
     * Tells whether unsigned commits are rejected.
     *
     * @return {@code true} when the response says so
     */
    public boolean rejectUnsignedCommits() {
        return Boolean.TRUE.equals(response.rejectUnsignedCommits());
    }

    /**
     * Tells whether commits without a DCO sign-off are rejected.
     *
     * @return {@code true} when the response says so
     */
    public boolean rejectNonDcoCommits() {
        return Boolean.TRUE.equals(response.rejectNonDcoCommits());
    }

    /**
     * Starts a change of the rules. Nothing is sent until {@link PushRulesUpdate#send()}.
     *
     * @return the update builder
     */
    public PushRulesUpdate update() {
        return new PushRulesUpdate(this);
    }

    /**
     * Deletes the push rules of the project ({@code DELETE /projects/{id}/push_rule}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().projectpushrules().delete(project.id());
    }

    /**
     * Loads the rules again ({@code GET /projects/{id}/push_rule}). This object is not changed.
     *
     * @return the rules as GitLab holds them now
     * @throws IllegalStateException if the project no longer has push rules
     * @throws IOException if the transport fails
     */
    public PushRules refresh() throws IOException {
        return project.pushRules().orElseThrow(() -> new IllegalStateException("Push rules are gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabProjectPushRule response() {
        return response;
    }

    PushRules apply(final GitLabProjectProjectPushRulesUpdatePayload payload) throws IOException {
        return new PushRules(project, gitLab().projectpushrules().update(project.id(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
