package io.github.piotr_janczukowicz.gitlab.sdk.repository;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabContributor;

/**
 * A contributor of a repository, as GitLab returned it. The object is immutable, and {@link #response()} gives the
 * complete response.
 */
public final class Contributor {

    private final GitLabContributor response;

    /**
     * Wraps a response.
     *
     * @param response the response record the object is built from
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public Contributor(final GitLabContributor response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the name of the contributor.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the email address of the contributor.
     *
     * @return the email address
     */
    public String email() {
        return requireNonNull(response.email(), "email");
    }

    /**
     * Returns the number of commits, which does not include merge commits.
     *
     * @return the commit count
     */
    public int commits() {
        return requireNonNull(response.commits(), "commits");
    }

    /**
     * Returns the number of added lines.
     *
     * @return the additions
     */
    public int additions() {
        return requireNonNull(response.additions(), "additions");
    }

    /**
     * Returns the number of deleted lines.
     *
     * @return the deletions
     */
    public int deletions() {
        return requireNonNull(response.deletions(), "deletions");
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabContributor response() {
        return response;
    }
}
