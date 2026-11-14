package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * One file of a snippet, as listed in the snippet's {@code files}: its path and the URL of its raw content. The content
 * itself is read with {@code fileContent(ref, path)} on the snippet.
 */
public final class SnippetFile {

    private final String path;
    private final @Nullable String rawUrl;

    SnippetFile(final String path, final @Nullable String rawUrl) {
        this.path = requireNonNull(path, "path");
        this.rawUrl = rawUrl;
    }

    /**
     * Returns the path of the file in the snippet repository, such as {@code dir/add.rb}.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the URL of the raw content of the file.
     *
     * @return the URL, or empty when GitLab sent none
     */
    public Optional<String> rawUrl() {
        return Optional.ofNullable(rawUrl);
    }
}
