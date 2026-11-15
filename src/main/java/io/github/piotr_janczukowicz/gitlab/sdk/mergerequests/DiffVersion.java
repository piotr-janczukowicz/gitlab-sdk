package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabMergeRequestDiff;
import java.time.Instant;
import java.util.Optional;

/**
 * One version of the diff of a {@link MergeRequest}, as GitLab lists it: GitLab makes a new version whenever the source
 * branch changes. The object is immutable, and {@link #response()} gives the complete response. The files and commits
 * of a version come from {@link MergeRequest#version(long)}.
 */
public final class DiffVersion {

    private final GitLabMergeRequestDiff response;

    /**
     * Wraps a response. Applications normally get this object from the merge request; call the constructor only to wrap
     * a response that was loaded some other way.
     *
     * @param response the response record the object is built from
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public DiffVersion(final GitLabMergeRequestDiff response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the version id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the commit the source branch pointed to in this version.
     *
     * @return the SHA, or empty when GitLab does not return one
     */
    public Optional<String> headCommitSha() {
        return Optional.ofNullable(response.headCommitSha());
    }

    /**
     * Returns the commit the source branch forked from in this version.
     *
     * @return the SHA, or empty when GitLab does not return one
     */
    public Optional<String> baseCommitSha() {
        return Optional.ofNullable(response.baseCommitSha());
    }

    /**
     * Returns the commit of the target branch the diff starts from in this version.
     *
     * @return the SHA, or empty when GitLab does not return one
     */
    public Optional<String> startCommitSha() {
        return Optional.ofNullable(response.startCommitSha());
    }

    /**
     * Returns when GitLab made the version.
     *
     * @return the creation time, or empty when GitLab does not return it
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(response.createdAt());
    }

    /**
     * Returns the state of the version as GitLab spells it, such as {@code collected}.
     *
     * @return the state, or empty when GitLab does not return one
     */
    public Optional<String> state() {
        return Optional.ofNullable(response.state());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabMergeRequestDiff response() {
        return response;
    }
}
