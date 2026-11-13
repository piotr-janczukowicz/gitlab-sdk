package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLink;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The sharing of a project with a group, as GitLab answers it. The object is immutable and has no refresh; unshare with
 * {@link Project#unshare(long)}. No method returns it yet; sharing arrives with a later release.
 */
public final class ProjectGroupLink {

    private final GitLabProjectGroupLink response;

    ProjectGroupLink(final GitLabProjectGroupLink response) {
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id of the group the project is shared with.
     *
     * @return the group id
     * @throws NullPointerException if GitLab's answer has no group id
     */
    public long groupId() {
        return requireNonNull(response.groupId(), "groupId");
    }

    /**
     * Returns the date the sharing expires.
     *
     * @return the date, or empty when the sharing does not expire
     */
    public Optional<LocalDate> expiresAt() {
        return Optional.ofNullable(response.expiresAt());
    }

    /**
     * Returns the complete response.
     *
     * @return the response record
     */
    public GitLabProjectGroupLink response() {
        return response;
    }
}
