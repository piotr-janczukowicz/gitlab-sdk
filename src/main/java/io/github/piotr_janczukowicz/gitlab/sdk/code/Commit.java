package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabBasicRef;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitSignature;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCherryPickPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentPayload;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListStatusesFilter;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListStatusesFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsRevertPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A commit of a {@link Project}, as GitLab returned it. The object is immutable, and a commit never changes, so it has
 * no {@code refresh()}. {@link #response()} gives the complete response for every field the accessors here do not
 * cover. GitLab lists, cherry-picks and reverts with a smaller record, so every commit is loaded with {@code GET
 * /projects/{id}/repository/commits/{sha}} and all of them have the same shape.
 */
public final class Commit {

    private static final int FIRST_LINE = 1;

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
     * Lists the statuses of the commit ({@code GET /projects/{id}/repository/commits/{sha}/statuses}). No parameter is
     * sent, so GitLab uses its defaults: per GitLab's API description, the {@code all} parameter is "If true, includes
     * all statuses instead of only the latest", the {@code ref} "Defaults to the default branch", and the statuses are
     * sorted by {@code id} in ascending order ("Defaults to asc"). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the statuses
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CommitStatus> statuses() throws IOException {
        return gitLab().commits().listStatuses(project.id(), sha()).stream().map(each -> new CommitStatus(this, each));
    }

    /**
     * Lists the statuses of the commit that match the arguments ({@code GET
     * /projects/{id}/repository/commits/{sha}/statuses}). A {@code null} {@code ref} or {@code name} leaves that
     * parameter out, so GitLab uses its default for the ref (the default branch). {@code all} is sent only when it is
     * {@code true}; {@code false} leaves it out, so GitLab lists only the latest status of each name. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param ref the branch or tag the statuses refer to, or {@code null} for GitLab's default
     * @param name the name of the status, or {@code null} for every name
     * @param all {@code true} to list every status instead of only the latest
     * @return the statuses
     * @throws IllegalArgumentException if {@code ref} or {@code name} is blank
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CommitStatus> statuses(final @Nullable String ref, final @Nullable String name, final boolean all)
            throws IOException {
        final GitLabProjectCommitsListStatusesFilter filter = GitLabProjectCommitsListStatusesFilterBuilder.builder()
                .ref(ref == null ? null : requireNotBlank(ref, "ref"))
                .name(name == null ? null : requireNotBlank(name, "name"))
                .all(all ? Boolean.TRUE : null)
                .build();
        return gitLab().commits().listStatuses(project.id(), sha(), filter).stream()
                .map(each -> new CommitStatus(this, each));
    }

    /**
     * Sets the status of the commit to a state ({@code POST /projects/{id}/statuses/{sha}}). Only the state is sent, so
     * GitLab uses its defaults for the rest; use {@link #addStatus(GitLabProjectCommitsCreatePayload)} to set a name, a
     * ref, a description, a target URL, a coverage or a pipeline. GitLab creates the status, or updates the one with
     * the same name and ref, and answers with it. This object is not changed.
     *
     * @param state the state
     * @return the status, as GitLab answered
     * @throws NullPointerException if {@code state} is {@code null}
     * @throws IOException if the transport fails
     */
    public CommitStatus addStatus(final CommitStatusState state) throws IOException {
        return addStatus(GitLabProjectCommitsCreatePayloadBuilder.builder()
                .state(requireNonNull(state, "state").toCore())
                .build());
    }

    /**
     * Sets the status of the commit from the payload of the core ({@code POST /projects/{id}/statuses/{sha}}). GitLab
     * creates the status, or updates the one with the same name and ref, and answers with it. This object is not
     * changed.
     *
     * @param payload the payload
     * @return the status, as GitLab answered
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public CommitStatus addStatus(final GitLabProjectCommitsCreatePayload payload) throws IOException {
        return new CommitStatus(
                this, gitLab().commits().create(project.id(), sha(), requireNonNull(payload, "payload")));
    }

    /**
     * Lists the comments of the commit ({@code GET /projects/{id}/repository/commits/{sha}/comments}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the comments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CommitComment> comments() throws IOException {
        return gitLab().commits().listComments(project.id(), sha()).stream().map(each -> new CommitComment(this, each));
    }

    /**
     * Comments on the commit as a whole ({@code POST /projects/{id}/repository/commits/{sha}/comments}). The core
     * requires a line type, so {@code line_type=new}, the default GitLab's API description gives, is sent; no path and
     * no line are sent.
     *
     * @param body the text of the comment
     * @return the comment, as GitLab answered
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public CommitComment comment(final String body) throws IOException {
        final GitLabProjectCommitsCreateCommentPayload payload =
                GitLabProjectCommitsCreateCommentPayloadBuilder.builder()
                        .note(requireNotBlank(body, "body"))
                        .lineType(CommitLineType.NEW.toCore())
                        .build();
        return new CommitComment(this, gitLab().commits().createComment(project.id(), sha(), payload));
    }

    /**
     * Comments on a line of a file of the commit ({@code POST /projects/{id}/repository/commits/{sha}/comments}).
     *
     * @param body the text of the comment
     * @param path the path of the file within the repository
     * @param line the line, counted from 1
     * @param lineType the side of the diff the line is on
     * @return the comment, as GitLab answered
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code body} or {@code path} is blank or {@code line} is below 1
     * @throws IOException if the transport fails
     */
    public CommitComment comment(final String body, final String path, final int line, final CommitLineType lineType)
            throws IOException {
        if (line < FIRST_LINE) {
            throw new IllegalArgumentException("line must be 1 or later");
        }
        final GitLabProjectCommitsCreateCommentPayload payload =
                GitLabProjectCommitsCreateCommentPayloadBuilder.builder()
                        .note(requireNotBlank(body, "body"))
                        .path(requireNotBlank(path, "path"))
                        .line(line)
                        .lineType(requireNonNull(lineType, "lineType").toCore())
                        .build();
        return new CommitComment(this, gitLab().commits().createComment(project.id(), sha(), payload));
    }

    /**
     * Lists the merge requests of the project that contain the commit ({@code GET
     * /projects/{id}/repository/commits/{sha}/merge_requests}). GitLab lists a summary of each merge request, so each
     * one is loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) as the stream reaches it, and one
     * that is gone by then is skipped. No {@code state} is sent, so per GitLab's API description merge requests of
     * every state are returned. Pages are fetched only as the stream is consumed; transport failures during iteration
     * are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return gitLab().commits().listMergeRequests(project.id(), sha()).stream()
                .map(listed -> Lookups.find(() -> project.mergeRequest(requireNonNull(listed.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Lists the branches and tags the commit is pushed to ({@code GET /projects/{id}/repository/commits/{sha}/refs}).
     * No {@code type} is sent, so GitLab uses its default, {@code all} (branches and tags). Pages are fetched only as
     * the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the core's records, each with a type and a name
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitLabBasicRef> refs() throws IOException {
        return gitLab().commits().listRefs(project.id(), sha()).stream();
    }

    /**
     * Loads the signature of the commit ({@code GET /projects/{id}/repository/commits/{sha}/signature}).
     *
     * @return the signature, or empty when GitLab answers HTTP 404, which covers a commit without a signature as well
     *     as an unknown commit
     * @throws IOException if the transport fails
     */
    public Optional<GitLabCommitSignature> signature() throws IOException {
        return gitLab().commits().getSignature(project.id(), sha());
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
