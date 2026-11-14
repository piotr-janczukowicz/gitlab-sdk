package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Values and conversions shared by the project and the personal snippet classes. */
final class SnippetValues {

    static final String TITLE = "title";
    static final String VISIBILITY = "visibility";
    static final String FILE_PATH = "filePath";
    static final String CONTENT = "content";
    static final String IS_GONE = " is gone";

    private static final String PATH = "path";

    private SnippetValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads the {@code files} of a snippet response: each entry's {@code path} and {@code raw_url}. An entry whose
     * {@code path} is absent or not text is skipped; a {@code raw_url} that is not text is treated as absent.
     */
    static List<SnippetFile> files(final @Nullable List<Map<String, Object>> files) {
        if (files == null) {
            return List.of();
        }
        final List<SnippetFile> result = new ArrayList<>(files.size());
        for (final Map<String, Object> entry : files) {
            if (entry.get(PATH) instanceof String path) {
                final Object rawUrl = entry.get("raw_url");
                result.add(new SnippetFile(path, rawUrl instanceof String text ? text : null));
            }
        }
        return List.copyOf(result);
    }

    static Optional<SnippetVisibility> visibility(final @Nullable String name) {
        return Optional.ofNullable(name).map(SnippetVisibility::from);
    }

    /**
     * One file change of an update, with the wire name of its action ({@code create}, {@code update}, {@code delete} or
     * {@code move}). Its {@code toString()} never prints the content.
     */
    record FileChange(
            String action,
            String path,
            @Nullable String previousPath,
            @Nullable String content) {

        @Override
        public String toString() {
            return "FileChange[action=" + action + ", path=" + path + ", previousPath=" + previousPath + "]";
        }
    }

    /**
     * The changes collected by a snippet update builder, validated as they are set. File changes keep their call order.
     * Its {@code toString()} is the default one, so it never prints a description or content.
     */
    static final class Changes {

        private final UpdateState state = new UpdateState();
        private final List<FileChange> files = new ArrayList<>();
        private @Nullable String title;
        private @Nullable String description;
        private @Nullable SnippetVisibility visibility;

        void setTitle(final String value) {
            title = requireNotBlank(value, TITLE);
            state.mark();
        }

        void setDescription(final String value) {
            description = requireNonNull(value, "description");
            state.mark();
        }

        void setVisibility(final SnippetVisibility value) {
            visibility = requireNonNull(value, VISIBILITY);
            state.mark();
        }

        void addFile(final String path, final String content) {
            file(new FileChange("create", requireNotBlank(path, PATH), null, requireNonNull(content, CONTENT)));
        }

        void updateFile(final String path, final String content) {
            file(new FileChange("update", requireNotBlank(path, PATH), null, requireNonNull(content, CONTENT)));
        }

        void deleteFile(final String path) {
            file(new FileChange("delete", requireNotBlank(path, PATH), null, null));
        }

        void moveFile(final String previousPath, final String path) {
            final String checkedPrevious = requireNotBlank(previousPath, "previousPath");
            file(new FileChange("move", requireNotBlank(path, PATH), checkedPrevious, null));
        }

        void file(final FileChange change) {
            files.add(change);
            state.mark();
        }

        void requireChange() {
            state.requireChange();
        }

        @Nullable String title() {
            return title;
        }

        @Nullable String description() {
            return description;
        }

        @Nullable SnippetVisibility visibility() {
            return visibility;
        }

        List<FileChange> files() {
            return List.copyOf(files);
        }
    }
}
