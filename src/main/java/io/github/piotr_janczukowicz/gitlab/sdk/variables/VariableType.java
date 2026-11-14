package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The type of a CI/CD variable. The values mirror the types the core accepts for project and group variables, with the
 * same constant names. A type in a response that is not listed here makes {@link Variable#type()} throw;
 * {@link Variable#typeName()} gives the raw name.
 */
public enum VariableType {
    /** An environment variable: the value is passed to the job as the value of the variable. */
    ENV_VAR("env_var"),
    /** A file variable: the value is written to a file and the variable holds the path of the file. */
    FILE("file");

    private final String value;

    VariableType(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the type as GitLab spells it, such as {@code env_var}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the type GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the type
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static VariableType from(final String value) {
        requireNonNull(value, "value");
        for (final VariableType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown variable type: " + value);
    }
}
