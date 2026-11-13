package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.List;

/**
 * A protected branch of a {@link Group}, as GitLab returned it. Premium: every group-level protected branch operation
 * needs the Premium tier, and the core's tier guard decides. The object is immutable: it keeps the data of the moment
 * it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * The branch is addressed by its name, which may be a wildcard and may contain slashes; the name reaches the core
 * client unchanged and the core encodes it. User-, group-, deploy-key- and member-role-based rules are not wrapped:
 * they are left out of the access lists and stay available from {@link #response()}.
 */
public final class GroupProtectedBranch {

    private final Group group;
    private final GitLabGroupProtectedBranch response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public GroupProtectedBranch(final Group group, final GitLabGroupProtectedBranch response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the branch belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the name or wildcard of the protected branch.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Tells whether force pushes are allowed.
     *
     * @return {@code true} if GitLab reports force pushes as allowed; a missing flag is {@code false}
     */
    public boolean allowsForcePush() {
        return Boolean.TRUE.equals(response.allowForcePush());
    }

    /**
     * Tells whether merge requests need the approval of a code owner.
     *
     * @return {@code true} if GitLab reports the approval as required; a missing flag is {@code false}
     */
    public boolean requiresCodeOwnerApproval() {
        return Boolean.TRUE.equals(response.codeOwnerApprovalRequired());
    }

    /**
     * Returns the access levels allowed to push. Only role-based rules are listed; rules of a user, a group, a deploy
     * key or a member role are skipped and stay available from {@link #response()}.
     *
     * @return the levels, empty when GitLab returned none
     * @throws IllegalArgumentException if GitLab returns a level the SDK does not know; {@link #pushAccessValues()}
     *     still gives the numbers
     */
    public List<ProtectedAccess> pushAccess() {
        return ProtectedAccesses.levels(response.pushAccessLevels());
    }

    /**
     * Returns the access level numbers allowed to push as GitLab returned them, an escape hatch for a level
     * {@link ProtectedAccess} does not know. Only role-based rules are listed; rules of a user, a group, a deploy key
     * or a member role are skipped and stay available from {@link #response()}.
     *
     * @return the numbers, empty when GitLab returned none
     */
    public List<Integer> pushAccessValues() {
        return ProtectedAccesses.values(response.pushAccessLevels());
    }

    /**
     * Returns the access levels allowed to merge. Only role-based rules are listed; rules of a user, a group, a deploy
     * key or a member role are skipped and stay available from {@link #response()}.
     *
     * @return the levels, empty when GitLab returned none
     * @throws IllegalArgumentException if GitLab returns a level the SDK does not know; {@link #mergeAccessValues()}
     *     still gives the numbers
     */
    public List<ProtectedAccess> mergeAccess() {
        return ProtectedAccesses.levels(response.mergeAccessLevels());
    }

    /**
     * Returns the access level numbers allowed to merge as GitLab returned them, an escape hatch for a level
     * {@link ProtectedAccess} does not know. Only role-based rules are listed; rules of a user, a group, a deploy key
     * or a member role are skipped and stay available from {@link #response()}.
     *
     * @return the numbers, empty when GitLab returned none
     */
    public List<Integer> mergeAccessValues() {
        return ProtectedAccesses.values(response.mergeAccessLevels());
    }

    /**
     * Allows or forbids force pushes ({@code PATCH /groups/{id}/protected_branches/{name}}). Premium. This object is
     * not changed.
     *
     * @param allow {@code true} to allow force pushes
     * @return the branch as GitLab answered
     * @throws IOException if the transport fails
     */
    public GroupProtectedBranch allowForcePush(final boolean allow) throws IOException {
        return update(GitLabGroupGroupLevelProtectedBranchesUpdatePayloadBuilder.builder()
                .allowForcePush(allow)
                .build());
    }

    /**
     * Requires or waives the approval of a code owner ({@code PATCH /groups/{id}/protected_branches/{name}}). Premium.
     * This object is not changed.
     *
     * @param required {@code true} to require the approval
     * @return the branch as GitLab answered
     * @throws IOException if the transport fails
     */
    public GroupProtectedBranch requireCodeOwnerApproval(final boolean required) throws IOException {
        return update(GitLabGroupGroupLevelProtectedBranchesUpdatePayloadBuilder.builder()
                .codeOwnerApprovalRequired(required)
                .build());
    }

    /**
     * Removes the protection ({@code DELETE /groups/{id}/protected_branches/{name}}). Premium. The branches stay.
     *
     * @throws IOException if the transport fails
     */
    public void unprotect() throws IOException {
        gitLab().grouplevelprotectedbranches().delete(group.id(), name());
    }

    /**
     * Loads the protected branch again ({@code GET /groups/{id}/protected_branches/{name}}). Premium. This object is
     * not changed.
     *
     * @return the protected branch as GitLab holds it now
     * @throws IllegalStateException if the branch is no longer protected
     * @throws IOException if the transport fails
     */
    public GroupProtectedBranch refresh() throws IOException {
        return group.protectedBranch(name())
                .orElseThrow(() -> new IllegalStateException("Protected branch " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabGroupProtectedBranch response() {
        return response;
    }

    private GroupProtectedBranch update(final GitLabGroupGroupLevelProtectedBranchesUpdatePayload payload)
            throws IOException {
        return new GroupProtectedBranch(
                group, gitLab().grouplevelprotectedbranches().update(group.id(), name(), payload));
    }

    private GitLab gitLab() {
        return Internals.gitLab(group);
    }
}
