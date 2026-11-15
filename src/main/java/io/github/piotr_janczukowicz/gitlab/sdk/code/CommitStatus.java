package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitStatus;
import java.util.Optional;

/**
 * A status of a {@link Commit}, as GitLab returned it. The object is immutable; {@link #response()} gives the complete
 * response for every field the accessors here do not cover (the dates, the coverage, the pipeline, the author).
 */
public final class CommitStatus {

    private final Commit commit;
    private final GitLabCommitStatus response;

    /**
     * Wraps a response. Applications normally get this object from the commit; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param commit the commit the status belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CommitStatus(final Commit commit, final GitLabCommitStatus response) {
        this.commit = requireNonNull(commit, "commit");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the commit the status belongs to.
     *
     * @return the commit
     */
    public Commit commit() {
        return commit;
    }

    /**
     * Returns the id of the status.
     *
     * @return the id, or empty when GitLab does not return one
     */
    public Optional<Long> id() {
        return Optional.ofNullable(response.id());
    }

    /**
     * Returns the status as GitLab spells it. A listed status can be any status of a CI job, such as {@code created} or
     * {@code manual}, not only a state that {@link CommitStatusState} can set, so the raw name is returned.
     *
     * @return the status, or empty when GitLab does not return one
     */
    public Optional<String> status() {
        return Optional.ofNullable(response.status());
    }

    /**
     * Returns the name that tells this status from the statuses of other systems.
     *
     * @return the name, or empty when GitLab does not return one
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the branch or tag the status refers to.
     *
     * @return the ref, or empty when GitLab does not return one
     */
    public Optional<String> ref() {
        return Optional.ofNullable(response.ref());
    }

    /**
     * Returns the short description of the status.
     *
     * @return the description, or empty when GitLab does not return one
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the URL associated with the status.
     *
     * @return the URL, or empty when GitLab does not return one
     */
    public Optional<String> targetUrl() {
        return Optional.ofNullable(response.targetUrl());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCommitStatus response() {
        return response;
    }
}
