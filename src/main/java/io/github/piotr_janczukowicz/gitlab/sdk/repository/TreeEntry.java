package io.github.piotr_janczukowicz.gitlab.sdk.repository;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabTreeObject;
import java.util.Optional;

/**
 * An entry of a repository tree, as GitLab returned it. The object is immutable, and {@link #response()} gives the
 * complete response for every field the accessors here do not cover.
 */
public final class TreeEntry {

    private static final String TREE = "tree";
    private static final String BLOB = "blob";

    private final GitLabTreeObject response;

    /**
     * Wraps a response.
     *
     * @param response the response record the object is built from
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public TreeEntry(final GitLabTreeObject response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the path of the entry within the repository.
     *
     * @return the path
     */
    public String path() {
        return requireNonNull(response.path(), "path");
    }

    /**
     * Returns the name of the entry.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the object id (SHA) of the entry.
     *
     * @return the id, or empty when GitLab does not return one
     */
    public Optional<String> id() {
        return Optional.ofNullable(response.id());
    }

    /**
     * Returns the file mode of the entry, for example {@code 100644}.
     *
     * @return the mode, or empty when GitLab does not return one
     */
    public Optional<String> mode() {
        return Optional.ofNullable(response.mode());
    }

    /**
     * Tells whether the entry is a directory (GitLab type {@code tree}).
     *
     * @return {@code true} for a directory
     */
    public boolean isDirectory() {
        return TREE.equals(response.type());
    }

    /**
     * Tells whether the entry is a file (GitLab type {@code blob}). A submodule (type {@code commit}) is neither a file
     * nor a directory.
     *
     * @return {@code true} for a file
     */
    public boolean isFile() {
        return BLOB.equals(response.type());
    }

    /**
     * Returns the last commit that changed the entry. GitLab returns it only when asked, and this SDK does not ask.
     *
     * @return the commit, or empty when GitLab does not return one
     */
    public Optional<GitLabCommit> lastCommit() {
        return Optional.ofNullable(response.lastCommit());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabTreeObject response() {
        return response;
    }
}
