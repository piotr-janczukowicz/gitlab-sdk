package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * How GitLab authenticates to the target of a remote mirror. The values mirror the core's enum for creating a remote
 * mirror, with the same constant names.
 */
public enum MirrorAuthMethod {
    /** Authenticate with an SSH key pair that GitLab generates for the mirror. */
    SSH_PUBLIC_KEY("ssh_public_key"),
    /** Authenticate with the user name and password in the URL of the mirror. */
    PASSWORD("password");

    private final String value;

    MirrorAuthMethod(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the method as GitLab spells it.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the method GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the method
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static MirrorAuthMethod from(final String value) {
        requireNonNull(value, "value");
        for (final MirrorAuthMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown mirror authentication method: " + value);
    }
}
