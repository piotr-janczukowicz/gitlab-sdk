package io.github.piotr_janczukowicz.gitlab.sdk.reactions;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAwardEmoji;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Note;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestNote;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * An emoji reaction (award emoji) on an {@link Issue}, a {@link Note}, a {@link MergeRequest} or a
 * {@link MergeRequestNote}, as GitLab returned it. The object is immutable, and {@link #response()} gives the complete
 * response for every field the accessors here do not cover.
 *
 * <p>One class serves the four kinds of owner, so unlike the other objects of this SDK a reaction has no parent
 * navigation: it does not offer a method that returns its owner. {@link #refresh()} and {@link #delete()} use the owner
 * the reaction was made on.
 */
public final class Reaction {

    private final ReactionTarget target;
    private final GitLabAwardEmoji response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param issue the issue the reaction was made on
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Reaction(final Issue issue, final GitLabAwardEmoji response) {
        this(ReactionTarget.of(requireNonNull(issue, "issue")), response);
    }

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param note the issue note the reaction was made on
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Reaction(final Note note, final GitLabAwardEmoji response) {
        this(ReactionTarget.of(requireNonNull(note, "note")), response);
    }

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param mergeRequest the merge request the reaction was made on
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Reaction(final MergeRequest mergeRequest, final GitLabAwardEmoji response) {
        this(ReactionTarget.of(requireNonNull(mergeRequest, "mergeRequest")), response);
    }

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param note the merge request note the reaction was made on
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Reaction(final MergeRequestNote note, final GitLabAwardEmoji response) {
        this(ReactionTarget.of(requireNonNull(note, "note")), response);
    }

    private Reaction(final ReactionTarget target, final GitLabAwardEmoji response) {
        this.target = target;
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the reaction (award) id.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the emoji name, for example {@code thumbsup}.
     *
     * @return the name, or empty when GitLab does not return one
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.name());
    }

    /**
     * Returns the user who reacted, as the core's record. The type comes from the core and is not wrapped by the SDK.
     *
     * @return the user, or empty when GitLab does not return one
     */
    public Optional<GitLabUserBasic> user() {
        return Optional.ofNullable(response.user());
    }

    /**
     * Returns when the reaction was created.
     *
     * @return the creation time, or empty when GitLab does not return one
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(response.createdAt());
    }

    /**
     * Loads the reaction again ({@code GET .../award_emoji/{award_id}} of the issue, issue note, merge request or merge
     * request note it was made on). This object is not changed.
     *
     * @return the reaction as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the reaction
     * @throws IOException if the transport fails
     */
    public Reaction refresh() throws IOException {
        return target.loader()
                .load(id())
                .map(loaded -> new Reaction(target, loaded))
                .orElseThrow(() -> new IllegalStateException("Reaction " + id() + " is gone"));
    }

    /**
     * Removes the reaction ({@code DELETE .../award_emoji/{award_id}} of the issue, issue note, merge request or merge
     * request note it was made on).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        target.remover().remove(id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabAwardEmoji response() {
        return response;
    }
}
