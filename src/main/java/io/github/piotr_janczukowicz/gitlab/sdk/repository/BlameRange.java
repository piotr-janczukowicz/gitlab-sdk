package io.github.piotr_janczukowicz.gitlab.sdk.repository;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRange;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabBlameRangeCommit;
import java.util.List;

/**
 * A run of consecutive lines of a file that one commit last changed, as GitLab returned it. The object is immutable,
 * and {@link #response()} gives the complete response.
 */
public final class BlameRange {

    private final GitLabBlameRange response;

    /**
     * Wraps a response.
     *
     * @param response the response record the object is built from
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public BlameRange(final GitLabBlameRange response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the commit that last changed the lines.
     *
     * @return the commit
     */
    public GitLabBlameRangeCommit commit() {
        return requireNonNull(response.commit(), "commit");
    }

    /**
     * Returns the text of the lines.
     *
     * @return an unmodifiable list, empty when GitLab returns none
     */
    public List<String> lines() {
        final List<String> lines = response.lines();
        return lines == null ? List.of() : List.copyOf(lines);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabBlameRange response() {
        return response;
    }
}
