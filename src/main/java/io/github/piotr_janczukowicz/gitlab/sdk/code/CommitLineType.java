package io.github.piotr_janczukowicz.gitlab.sdk.code;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentLineType;
import java.util.Locale;

/** The side of a diff a line comment refers to: the new version of the file or the old one. */
public enum CommitLineType {
    /** The line of the new version of the file; GitLab calls this type {@code new}. */
    NEW,
    /** The line of the old version of the file; GitLab calls this type {@code old}. */
    OLD;

    /**
     * Returns the name of the type as GitLab spells it, {@code new} or {@code old}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    GitLabProjectCommitsCreateCommentLineType toCore() {
        return GitLabProjectCommitsCreateCommentLineType.fromWire(value());
    }
}
