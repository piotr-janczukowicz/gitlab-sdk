package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.http.MultipartFormData;

/** Builds the multipart form the core takes to create or update a repository file. */
public final class FileForms {

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
                .text("branch", requireNotBlank(branch, "branch"))
                .text("content", requireNonNull(content, "content"))
                .text("commit_message", requireNotBlank(message, "message"))
                .build();
    }
}
