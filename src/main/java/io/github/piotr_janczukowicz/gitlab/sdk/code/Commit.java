package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCherryPickPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsRevertPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A commit of a {@link Project}, as GitLab returned it. The object is immutable, and a commit never changes, so it has
 * no {@code refresh()}. {@link #response()} gives the complete response for every field the accessors here do not
 * cover. GitLab lists, cherry-picks and reverts with a smaller record, so every commit is loaded with {@code GET
 * /projects/{id}/repository/commits/{sha}} and all of them have the same shape.
 */
public final class Commit {

    private final Project project;
    private final GitLabCommitDetail response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Commit(final Project project, final GitLabCommitDetail response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the commit belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the full SHA of the commit.
     *
     * @return the SHA
     */
    public String sha() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the first line of the commit message.
     *
     * @return the title, or empty when GitLab does not return one
     */
    public Optional<String> title() {
        return Optional.ofNullable(response.title());
    }

    /**
     * Returns the commit message.
     *
     * @return the message, or empty when GitLab does not return one
     */
    public Optional<String> message() {
        return Optional.ofNullable(response.message());
    }

    /**
     * Lists the changed files of the commit ({@code GET /projects/{id}/repository/commits/{sha}/diff}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the diffs
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabDiff> diff() throws IOException {
        return gitLab().commits().listDiff(project.id(), sha()).stream();
    }

    /**
     * Cherry-picks the commit onto a branch ({@code POST /projects/{id}/repository/commits/{sha}/cherry_pick}) and
     * loads the new commit ({@code GET /projects/{id}/repository/commits/{sha}}). This object is not changed.
     *
     * @param branch the branch to cherry-pick onto
     * @return the new commit
     * @throws NullPointerException if {@code branch} is {@code null}
     * @throws IllegalArgumentException if {@code branch} is blank
     * @throws IllegalStateException if GitLab no longer has the new commit
     * @throws IOException if the transport fails
     */
    public Commit cherryPick(final String branch) throws IOException {
        final GitLabCommit created = gitLab().commits()
                .cherryPick(
                        project.id(),
                        sha(),
                        GitLabProjectCommitsCherryPickPayloadBuilder.builder()
                                .branch(requireNotBlank(branch, "branch"))
                                .build());
        return load(created);
    }

    /**
     * Reverts the commit on a branch ({@code POST /projects/{id}/repository/commits/{sha}/revert}) and loads the new
     * commit ({@code GET /projects/{id}/repository/commits/{sha}}). This object is not changed.
     *
     * @param branch the branch to revert on
     * @return the new commit
     * @throws NullPointerException if {@code branch} is {@code null}
     * @throws IllegalArgumentException if {@code branch} is blank
     * @throws IllegalStateException if GitLab no longer has the new commit
     * @throws IOException if the transport fails
     */
    public Commit revert(final String branch) throws IOException {
        final GitLabCommit created = gitLab().commits()
                .revert(
                        project.id(),
                        sha(),
                        GitLabProjectCommitsRevertPayloadBuilder.builder()
                                .branch(requireNotBlank(branch, "branch"))
                                .build());
        return load(created);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabCommitDetail response() {
        return response;
    }

    private Commit load(final GitLabCommit created) throws IOException {
        final String createdSha = requireNonNull(created.id(), "id");
        return project.commit(createdSha)
                .orElseThrow(() -> new IllegalStateException("Commit " + createdSha + " is gone"));
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
