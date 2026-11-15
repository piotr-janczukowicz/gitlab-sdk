package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitNote;
import java.util.Optional;

/**
 * A comment on a {@link Commit}, as GitLab returned it. The object is immutable; {@link #response()} gives the complete
 * response for every field the accessors here do not cover (the author and the creation time).
 */
public final class CommitComment {

    private final Commit commit;
    private final GitLabCommitNote response;

    /**
     * Wraps a response. Applications normally get this object from the commit; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param commit the commit the comment belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public CommitComment(final Commit commit, final GitLabCommitNote response) {
        this.commit = requireNonNull(commit, "commit");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the commit the comment belongs to.
     *
     * @return the commit
     */
    public Commit commit() {
        return commit;
    }

    /**
     * Returns the text of the comment.
     *
     * @return the text, or empty when GitLab does not return one
     */
    public Optional<String> body() {
        return Optional.ofNullable(response.note());
    }

    /**
     * Returns the path of the file the comment is on.
     *
     * @return the path, or empty for a comment on the commit as a whole
     */
    public Optional<String> path() {
        return Optional.ofNullable(response.path());
    }

    /**
     * Returns the line the comment is on.
     *
     * @return the line, or empty for a comment on the commit as a whole
     */
    public Optional<Integer> line() {
        return Optional.ofNullable(response.line());
    }

    /**
     * Returns the side of the diff the line is on, as GitLab spells it ({@code new} or {@code old}).
     *
     * @return the line type, or empty when GitLab does not return one
     */
    public Optional<String> lineType() {
        return Optional.ofNullable(response.lineType());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCommitNote response() {
        return response;
    }
}
