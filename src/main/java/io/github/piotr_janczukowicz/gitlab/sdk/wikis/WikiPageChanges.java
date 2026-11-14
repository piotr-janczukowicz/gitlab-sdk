package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import org.jspecify.annotations.Nullable;

/**
 * The fields a {@link WikiPageUpdate} sends; a {@code null} field is not sent. {@link #toString()} never prints the
 * content, which can hold anything the user wrote.
 *
 * @param title the new title, or {@code null}
 * @param content the new content, or {@code null}
 * @param format the new format, or {@code null}
 */
record WikiPageChanges(
        @Nullable String title,
        @Nullable String content,
        @Nullable WikiFormat format) {

    @Override
    public String toString() {
        return "WikiPageChanges[title=" + title + ", format=" + format + ", content "
                + (content == null ? "unset" : "set") + "]";
    }
}
