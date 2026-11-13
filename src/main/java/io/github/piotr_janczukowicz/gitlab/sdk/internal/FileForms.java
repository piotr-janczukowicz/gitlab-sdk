package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.util.Base64;

/** Builds the multipart form the core takes to create or update a repository file. */
public final class FileForms {

    private static final String BRANCH = "branch";
    private static final String CONTENT = "content";

    private FileForms() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds the form of a file commit.
     *
     * @param branch the branch to commit to
     * @param content the content, sent as UTF-8 text; may be empty
     * @param message the commit message
     * @return the form
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code branch} or {@code message} is blank
     */
    public static MultipartFormData of(final String branch, final String content, final String message) {
        return MultipartFormData.builder()
                .text(BRANCH, requireNotBlank(branch, BRANCH))
                .text(CONTENT, requireNonNull(content, CONTENT))
                .text("commit_message", requireNotBlank(message, "message"))
                .build();
    }

    /**
     * Builds the form of a file commit with binary content, sent Base64-encoded with {@code encoding=base64}.
     *
     * @param branch the branch to commit to
     * @param content the content; may be empty
     * @param message the commit message
     * @return the form
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code branch} or {@code message} is blank
     */
    public static MultipartFormData of(final String branch, final byte[] content, final String message) {
        return MultipartFormData.builder()
                .text(BRANCH, requireNotBlank(branch, BRANCH))
                .text("encoding", "base64")
                .text(CONTENT, Base64.getEncoder().encodeToString(requireNonNull(content, CONTENT)))
                .text("commit_message", requireNotBlank(message, "message"))
                .build();
    }
}
