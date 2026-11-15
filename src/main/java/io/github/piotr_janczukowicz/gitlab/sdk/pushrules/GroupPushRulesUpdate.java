package io.github.piotr_janczukowicz.gitlab.sdk.pushrules;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupGroupPushRulesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;

/**
 * Collects changes to {@link GroupPushRules} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. The core's payload has no way to send a null, so a field cannot be cleared.
 */
public final class GroupPushRulesUpdate {

    private final GroupPushRules rules;
    private final GitLabGroupGroupPushRulesUpdatePayloadBuilder payload =
            GitLabGroupGroupPushRulesUpdatePayloadBuilder.builder();
    private final UpdateState state = new UpdateState();

    /**
     * Starts a change of one object. Applications normally get it from the {@code update()} method of that object.
     * Nothing is sent until {@code send()}.
     *
     * @param rules the rules to change
     * @throws NullPointerException if {@code rules} is {@code null}
     */
    public GroupPushRulesUpdate(final GroupPushRules rules) {
        this.rules = requireNonNull(rules, "rules");
    }

    /**
     * Sets the regular expression every commit message must match.
     *
     * @param regex the expression
     * @return this update
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public GroupPushRulesUpdate commitMessageRegex(final String regex) {
        payload.commitMessageRegex(requireNotBlank(regex, "commitMessageRegex"));
        return changed();
    }

    /**
     * Sets the regular expression no commit message may match.
     *
     * @param regex the expression
     * @return this update
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public GroupPushRulesUpdate commitMessageNegativeRegex(final String regex) {
        payload.commitMessageNegativeRegex(requireNotBlank(regex, "commitMessageNegativeRegex"));
        return changed();
    }

    /**
     * Sets the regular expression every branch name must match.
     *
     * @param regex the expression
     * @return this update
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public GroupPushRulesUpdate branchNameRegex(final String regex) {
        payload.branchNameRegex(requireNotBlank(regex, "branchNameRegex"));
        return changed();
    }

    /**
     * Sets the regular expression every commit author email must match.
     *
     * @param regex the expression
     * @return this update
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public GroupPushRulesUpdate authorEmailRegex(final String regex) {
        payload.authorEmailRegex(requireNotBlank(regex, "authorEmailRegex"));
        return changed();
    }

    /**
     * Sets the regular expression no committed file name may match.
     *
     * @param regex the expression
     * @return this update
     * @throws NullPointerException if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public GroupPushRulesUpdate fileNameRegex(final String regex) {
        payload.fileNameRegex(requireNotBlank(regex, "fileNameRegex"));
        return changed();
    }

    /**
     * Sets the largest file a push may add, in megabytes.
     *
     * @param megabytes the limit
     * @return this update
     * @throws IllegalArgumentException if {@code megabytes} is negative
     */
    public GroupPushRulesUpdate maxFileSize(final long megabytes) {
        if (megabytes < 0) {
            throw new IllegalArgumentException("maxFileSize must not be negative");
        }
        payload.maxFileSize(megabytes);
        return changed();
    }

    /**
     * Sets whether tags may not be deleted.
     *
     * @param enabled {@code true} to forbid deleting tags
     * @return this update
     */
    public GroupPushRulesUpdate denyDeleteTag(final boolean enabled) {
        payload.denyDeleteTag(enabled);
        return changed();
    }

    /**
     * Sets whether the author of a commit must be a member of the group.
     *
     * @param enabled {@code true} to require it
     * @return this update
     */
    public GroupPushRulesUpdate memberCheck(final boolean enabled) {
        payload.memberCheck(enabled);
        return changed();
    }

    /**
     * Sets whether files that may hold secrets are rejected.
     *
     * @param enabled {@code true} to reject them
     * @return this update
     */
    public GroupPushRulesUpdate preventSecrets(final boolean enabled) {
        payload.preventSecrets(enabled);
        return changed();
    }

    /**
     * Sets whether the committer must be the user who pushes.
     *
     * @param enabled {@code true} to require it
     * @return this update
     */
    public GroupPushRulesUpdate commitCommitterCheck(final boolean enabled) {
        payload.commitCommitterCheck(enabled);
        return changed();
    }

    /**
     * Sets whether the committer name must match the name of the user who pushes.
     *
     * @param enabled {@code true} to require it
     * @return this update
     */
    public GroupPushRulesUpdate commitCommitterNameCheck(final boolean enabled) {
        payload.commitCommitterNameCheck(enabled);
        return changed();
    }

    /**
     * Sets whether unsigned commits are rejected.
     *
     * @param enabled {@code true} to reject them
     * @return this update
     */
    public GroupPushRulesUpdate rejectUnsignedCommits(final boolean enabled) {
        payload.rejectUnsignedCommits(enabled);
        return changed();
    }

    /**
     * Sets whether commits without a DCO sign-off are rejected.
     *
     * @param enabled {@code true} to reject them
     * @return this update
     */
    public GroupPushRulesUpdate rejectNonDcoCommits(final boolean enabled) {
        payload.rejectNonDcoCommits(enabled);
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /groups/{id}/push_rule}). The rules this update was made from are not changed.
     *
     * @return the rules as GitLab returned them after the update
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public GroupPushRules send() throws IOException {
        state.requireChange();
        return rules.apply(payload.build());
    }

    private GroupPushRulesUpdate changed() {
        state.mark();
        return this;
    }
}
