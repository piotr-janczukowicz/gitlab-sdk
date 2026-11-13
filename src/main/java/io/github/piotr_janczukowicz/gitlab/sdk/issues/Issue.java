package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesNotesPayload;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesNotesPayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An issue of a {@link Project}, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. An issue
 * is addressed by its {@code iid}, the number within its project.
 */
public final class Issue {

    private final Project project;
    private final GitLabIssue response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Issue(final Project project, final GitLabIssue response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the issue belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the issue number within its project.
     *
     * @return the {@code iid}
     */
    public long iid() {
        return requireNonNull(response.iid(), "iid");
    }

    /**
     * Returns the issue title.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
    }

    /**
     * Returns the issue description in Markdown.
     *
     * @return the description, or empty when GitLab does not return one
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Returns the issue state.
     *
     * @return the state
     * @throws IllegalArgumentException if GitLab returns a state other than {@code opened} or {@code closed}
     */
    public IssueState state() {
        return IssueState.from(requireNonNull(response.state(), "state"));
    }

    /**
     * Returns the names of the labels of the issue.
     *
     * @return the label names, empty when there are none
     */
    public List<String> labels() {
        return Optional.ofNullable(response.labels()).orElse(List.of());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link IssueUpdate#send()}.
     *
     * @return the update builder
     */
    public IssueUpdate update() {
        return new IssueUpdate(this);
    }

    /**
     * Closes the issue ({@code PUT /projects/{id}/issues/{issue_iid}} with {@code state_event=close}). This object is
     * not changed.
     *
     * @return the issue as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Issue close() throws IOException {
        return changeState(GitLabProjectIssuesUpdateStateEvent.CLOSE);
    }

    /**
     * Reopens the issue ({@code PUT /projects/{id}/issues/{issue_iid}} with {@code state_event=reopen}). This object is
     * not changed.
     *
     * @return the issue as GitLab returned it after the change
     * @throws IOException if the transport fails
     */
    public Issue reopen() throws IOException {
        return changeState(GitLabProjectIssuesUpdateStateEvent.REOPEN);
    }

    /**
     * Adds a note to the issue ({@code POST /projects/{id}/issues/{issue_iid}/notes}). The note path names the issue by
     * its {@code iid}, not by its global id.
     *
     * @param body the note text in Markdown
     * @return the created note
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is blank
     * @throws IOException if the transport fails
     */
    public Note comment(final String body) throws IOException {
        final GitLabProjectNotesCreateIssuesNotesPayload payload =
                GitLabProjectNotesCreateIssuesNotesPayloadBuilder.builder()
                        .body(requireNotBlank(body, "body"))
                        .build();
        return new Note(this, gitLab().notes().project().createIssuesNotes(project.id(), iid(), payload));
    }

    /**
     * Lists the notes of the issue ({@code GET /projects/{id}/issues/{issue_iid}/notes}), system notes included. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the notes
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Note> notes() throws IOException {
        return gitLab().notes().project().listIssuesNotes(project.id(), iid()).stream()
                .map(note -> new Note(this, note));
    }

    /**
     * Deletes the issue ({@code DELETE /projects/{id}/issues/{issue_iid}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().issues().project().delete(project.id(), iid());
    }

    /**
     * Loads the issue again ({@code GET /projects/{id}/issues/{issue_iid}}). This object is not changed.
     *
     * @return the issue as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the issue
     * @throws IOException if the transport fails
     */
    public Issue refresh() throws IOException {
        return project.issue(iid()).orElseThrow(() -> new IllegalStateException("Issue " + iid() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabIssue response() {
        return response;
    }

    Issue apply(final GitLabProjectIssuesUpdatePayload payload) throws IOException {
        return new Issue(project, gitLab().issues().project().update(project.id(), iid(), payload));
    }

    private Issue changeState(final GitLabProjectIssuesUpdateStateEvent event) throws IOException {
        return apply(GitLabProjectIssuesUpdatePayloadBuilder.builder()
                .stateEvent(event)
                .build());
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
