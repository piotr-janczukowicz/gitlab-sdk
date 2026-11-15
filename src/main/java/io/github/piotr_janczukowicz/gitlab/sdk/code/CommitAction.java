package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.util.Base64;
import org.jspecify.annotations.Nullable;

/**
 * One file change of a multi-file commit, written as the {@code actions[][...]} fields of the form-encoded commit
 * request. Binary content is sent Base64-encoded with {@code encoding=base64}, text as it is, as for
 * {@link RepositoryFile}.
 */
final class CommitAction {

    private static final String PATH = "path";
    private static final String CONTENT = "content";
    private static final String CREATE = "create";
    private static final String UPDATE = "update";

    private final String action;
    private final String path;
    private final @Nullable String previousPath;
    private final @Nullable String content;
    private final boolean base64;

    private CommitAction(
            final String action,
            final String path,
            final @Nullable String previousPath,
            final @Nullable String content,
            final boolean base64) {
        this.action = action;
        this.path = path;
        this.previousPath = previousPath;
        this.content = content;
        this.base64 = base64;
    }

    static CommitAction create(final String path, final String content) {
        return new CommitAction(CREATE, requireNotBlank(path, PATH), null, requireNonNull(content, CONTENT), false);
    }

    static CommitAction create(final String path, final byte[] content) {
        return new CommitAction(CREATE, requireNotBlank(path, PATH), null, encoded(content), true);
    }

    static CommitAction update(final String path, final String content) {
        return new CommitAction(UPDATE, requireNotBlank(path, PATH), null, requireNonNull(content, CONTENT), false);
    }

    static CommitAction update(final String path, final byte[] content) {
        return new CommitAction(UPDATE, requireNotBlank(path, PATH), null, encoded(content), true);
    }

    static CommitAction delete(final String path) {
        return new CommitAction("delete", requireNotBlank(path, PATH), null, null, false);
    }

    static CommitAction move(final String previousPath, final String path) {
        return new CommitAction(
                "move", requireNotBlank(path, PATH), requireNotBlank(previousPath, "previousPath"), null, false);
    }

    void writeTo(final MultipartFormData.Builder form) {
        form.text("actions[][action]", action).text("actions[][file_path]", path);
        if (previousPath != null) {
            form.text("actions[][previous_path]", previousPath);
        }
        if (base64) {
            form.text("actions[][encoding]", "base64");
        }
        if (content != null) {
            form.text("actions[][content]", content);
        }
    }

    private static String encoded(final byte[] content) {
        return Base64.getEncoder().encodeToString(requireNonNull(content, CONTENT));
    }
}
