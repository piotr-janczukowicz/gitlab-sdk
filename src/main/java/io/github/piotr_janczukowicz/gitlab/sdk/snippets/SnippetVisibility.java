package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * Who can see a snippet: the three levels GitLab documents for creating and updating a project or personal snippet.
 * GitLab.com disables internal visibility for new snippets (per the GitLab docs). A visibility in a response that is
 * not listed here makes {@code visibility()} throw; {@code visibilityName()} gives the raw name.
 */
public enum SnippetVisibility {
    /** Private: the most restricted level. */
    PRIVATE("private"),
    /** Every signed-in user; GitLab.com disables it for new snippets (per the GitLab docs). */
    INTERNAL("internal"),
    /** Everyone. */
    PUBLIC("public");

    private final String value;

    SnippetVisibility(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the visibility as GitLab spells it, such as {@code private}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the visibility GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the visibility
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static SnippetVisibility from(final String value) {
        requireNonNull(value, "value");
        for (final SnippetVisibility visibility : values()) {
            if (visibility.value.equals(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown snippet visibility: " + value);
    }
}
