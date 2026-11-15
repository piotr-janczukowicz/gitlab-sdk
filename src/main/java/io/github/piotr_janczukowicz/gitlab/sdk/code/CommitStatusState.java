package io.github.piotr_janczukowicz.gitlab.sdk.code;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateState;
import java.util.Locale;

/**
 * The state of a status that {@link Commit#addStatus(CommitStatusState)} sets: exactly the six states the core accepts
 * for it.
 */
public enum CommitStatusState {
    /** The status is pending. */
    PENDING,
    /** The status is running. */
    RUNNING,
    /** The status is success. */
    SUCCESS,
    /** The status is failed. */
    FAILED,
    /** The status is canceled. */
    CANCELED,
    /** The status is skipped. */
    SKIPPED;

    /**
     * Returns the name of the state as GitLab spells it, such as {@code success}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    GitLabProjectCommitsCreateState toCore() {
        return GitLabProjectCommitsCreateState.fromWire(value());
    }
}
