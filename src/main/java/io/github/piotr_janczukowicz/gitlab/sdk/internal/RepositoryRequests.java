package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesListTreeFilter;
import io.github.piotr_janczukowicz.gitlab.api.repositories.GitLabProjectRepositoriesListTreeFilterBuilder;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Validates the arguments of the repository reads and builds the core's request values from them. */
public final class RepositoryRequests {

    private static final int MIN_MERGE_BASE_REFS = 2;
    private static final byte LINE_FEED = '\n';

    private RepositoryRequests() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds the filter of a tree listing. {@code null} leaves a parameter out, so GitLab uses its default.
     *
     * @param path the directory, or {@code null} for the root
     * @param ref the branch, tag or commit, or {@code null} for the default branch
     * @param recursive whether to list nested directories too
     * @return the filter
     * @throws IllegalArgumentException if {@code path} or {@code ref} is blank
     */
    public static GitLabProjectRepositoriesListTreeFilter treeFilter(
            final @Nullable String path, final @Nullable String ref, final boolean recursive) {
        return GitLabProjectRepositoriesListTreeFilterBuilder.builder()
                .path(path == null ? null : requireNotBlank(path, "path"))
                .ref(ref == null ? null : requireNotBlank(ref, "ref"))
                .recursive(recursive)
                .build();
    }

    /**
     * Checks the refs of a merge base lookup.
     *
     * @param refs the refs
     * @return the refs as an immutable list
     * @throws NullPointerException if {@code refs} or an element is {@code null}
     * @throws IllegalArgumentException if fewer than two refs are given or one is blank
     */
    public static List<String> mergeBaseRefs(final String... refs) {
        final List<String> checked =
                Arguments.requireNotBlankElements(Arrays.asList(requireNonNull(refs, "refs")), "refs");
        if (checked.size() < MIN_MERGE_BASE_REFS) {
            throw new IllegalArgumentException("refs needs at least two refs");
        }
        return checked;
    }

    /**
     * Counts the lines of a file the way a blame numbers them: a final line break does not start another line.
     *
     * @param content the content of the file
     * @return the number of lines, zero for an empty file
     */
    public static int lineCount(final byte[] content) {
        int lines = 0;
        for (final byte b : content) {
            if (b == LINE_FEED) {
                lines++;
            }
        }
        if (content.length > 0 && content[content.length - 1] != LINE_FEED) {
            lines++;
        }
        return lines;
    }
}
