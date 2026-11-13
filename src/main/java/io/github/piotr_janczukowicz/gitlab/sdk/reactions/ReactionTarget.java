package io.github.piotr_janczukowicz.gitlab.sdk.reactions;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmoji;
import io.github.piotr_janczukowicz.gitlab.api.emojireactions.GitLabProjectEmojiReactionsApi;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Note;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestNote;
import java.io.IOException;
import java.util.Optional;

/**
 * The core calls that reload and delete a reaction of one owner kind (issue, issue note, merge request or merge request
 * note). It keeps {@link Reaction} one class for the four kinds. A target addresses the award by its id and uses the
 * ids of the owner the reaction was made on.
 *
 * @param loader loads one award of the owner
 * @param remover deletes one award of the owner
 */
record ReactionTarget(Loader loader, Remover remover) {

    static ReactionTarget of(final Issue issue) {
        final Project project = issue.project();
        final String id = project.id();
        final long iid = issue.iid();
        return new ReactionTarget(
                awardId -> api(project).getIssuesAwardEmoji(id, iid, awardId),
                awardId -> api(project).deleteIssuesAwardEmoji(id, iid, awardId));
    }

    static ReactionTarget of(final Note note) {
        final Issue issue = note.issue();
        final Project project = issue.project();
        final String id = project.id();
        final long iid = issue.iid();
        final long noteId = note.id();
        return new ReactionTarget(
                awardId -> api(project).getIssuesNotesAwardEmoji(id, iid, noteId, awardId),
                awardId -> api(project).deleteIssuesNotesAwardEmoji(id, iid, noteId, awardId));
    }

    static ReactionTarget of(final MergeRequest mergeRequest) {
        final Project project = mergeRequest.project();
        final String id = project.id();
        final long iid = mergeRequest.iid();
        return new ReactionTarget(
                awardId -> api(project).getRequestsAwardEmoji(id, iid, awardId),
                awardId -> api(project).deleteRequestsAwardEmoji(id, iid, awardId));
    }

    static ReactionTarget of(final MergeRequestNote note) {
        final MergeRequest mergeRequest = note.mergeRequest();
        final Project project = mergeRequest.project();
        final String id = project.id();
        final long iid = mergeRequest.iid();
        final long noteId = note.id();
        return new ReactionTarget(
                awardId -> api(project).getRequestsNotesAwardEmoji(id, iid, noteId, awardId),
                awardId -> api(project).deleteRequestsNotesAwardEmoji(id, iid, noteId, awardId));
    }

    private static GitLabProjectEmojiReactionsApi api(final Project project) {
        return Internals.gitLab(project).emojireactions();
    }

    /** Loads one award of the owner. */
    @FunctionalInterface
    interface Loader {
        Optional<GitLabAwardEmoji> load(long awardId) throws IOException;
    }

    /** Deletes one award of the owner. */
    @FunctionalInterface
    interface Remover {
        void remove(long awardId) throws IOException;
    }
}
