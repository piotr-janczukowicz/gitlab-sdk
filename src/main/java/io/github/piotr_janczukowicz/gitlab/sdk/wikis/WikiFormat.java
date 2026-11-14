package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The markup format of a {@link WikiPage}: the four formats GitLab documents for creating and updating a page. A format
 * in a response that is not listed here makes {@link WikiPage#format()} throw; {@link WikiPage#formatName()} gives the
 * raw name.
 */
public enum WikiFormat {
    /** Markdown, GitLab's default. */
    MARKDOWN("markdown"),
    /** RDoc. */
    RDOC("rdoc"),
    /** AsciiDoc. */
    ASCIIDOC("asciidoc"),
    /** Org mode. */
    ORG("org");

    private final String value;

    WikiFormat(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the format as GitLab spells it, such as {@code markdown}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the format GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the format
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static WikiFormat from(final String value) {
        requireNonNull(value, "value");
        for (final WikiFormat format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown wiki format: " + value);
    }
}
