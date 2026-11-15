package io.github.piotr_janczukowicz.gitlab.sdk.repository;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabCompare;
import java.util.List;
import java.util.Optional;

/**
 * The result of comparing two refs, as GitLab returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class Comparison {

    private final GitLabCompare response;

    /**
     * Wraps a response.
     *
     * @param response the response record the object is built from
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public Comparison(final GitLabCompare response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the last commit of the compared range.
     *
     * @return the commit, or empty when GitLab does not return one
     */
    public Optional<GitLabCommit> commit() {
        return Optional.ofNullable(response.commit());
    }

    /**
     * Returns the commits of the range. When {@link #isTimedOut()} is {@code true} the commits are still complete.
     *
     * @return an unmodifiable list, empty when GitLab returns none
     */
    public List<GitLabCommit> commits() {
        final List<GitLabCommit> commits = response.commits();
        return commits == null ? List.of() : List.copyOf(commits);
    }

    /**
     * Returns the diffs of the range. When {@link #isTimedOut()} is {@code true} the list may be incomplete.
     *
     * @return an unmodifiable list, empty when GitLab returns none
     */
    public List<GitLabDiff> diffs() {
        final List<GitLabDiff> diffs = response.diffs();
        return diffs == null ? List.of() : List.copyOf(diffs);
    }

    /**
     * Tells whether the comparison exceeded GitLab's size limits.
     *
     * @return {@code true} if GitLab reports a timeout
     */
    public boolean isTimedOut() {
        return Boolean.TRUE.equals(response.compareTimeout());
    }

    /**
     * Tells whether both refs point to the same commit.
     *
     * @return {@code true} if GitLab reports the same ref
     */
    public boolean isSameRef() {
        return Boolean.TRUE.equals(response.compareSameRef());
    }

    /**
     * Returns the web address of the comparison.
     *
     * @return the URL, or empty when GitLab does not return one
     */
    public Optional<String> webUrl() {
        return Optional.ofNullable(response.webUrl());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCompare response() {
        return response;
    }
}
