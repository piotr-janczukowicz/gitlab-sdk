package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLink;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The sharing of a project with a group, as GitLab answers it. The object is immutable and has no refresh; unshare with
 * {@link Project#unshare(long)}. {@link Project#share(long,
 * io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel)} returns it.
 */
public final class ProjectGroupLink {

    private final Project project;
    private final GitLabProjectGroupLink response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ProjectGroupLink(final Project project, final GitLabProjectGroupLink response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project that is shared.
     *
     * @return the project
     */
    public Project project() {
        return project;
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
