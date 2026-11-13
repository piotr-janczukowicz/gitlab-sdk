package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedRefAccess;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Conversions of the access entries shared by the protected branch and tag classes. */
final class ProtectedAccesses {

    private ProtectedAccesses() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * The access level numbers of the plain role rules. A rule of a user, group, deploy key or member role is skipped,
     * although GitLab sends a level with it, and so is an entry without a level. Never throws.
     */
    static List<Integer> values(final @Nullable List<GitLabProtectedRefAccess> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
                .filter(ProtectedAccesses::isPlainRole)
                .map(GitLabProtectedRefAccess::accessLevel)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * The levels of the entries; throws {@link IllegalArgumentException} for a number {@link ProtectedAccess} lacks.
     */
    static List<ProtectedAccess> levels(final @Nullable List<GitLabProtectedRefAccess> entries) {
        return values(entries).stream().map(ProtectedAccess::from).toList();
    }

    private static boolean isPlainRole(final GitLabProtectedRefAccess entry) {
        return entry.userId() == null
                && entry.groupId() == null
                && entry.deployKeyId() == null
                && entry.memberRoleId() == null;
    }
}
