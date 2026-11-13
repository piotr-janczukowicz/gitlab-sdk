package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * A scope of a deploy token: what the token may be used for. The values mirror the scopes the core accepts for project
 * and group deploy tokens. A scope in a response that is not listed here makes {@link DeployToken#scopes()} throw;
 * {@link DeployToken#scopeNames()} gives the raw names.
 */
public enum DeployTokenScope {
    /** Clone and pull the repository. */
    READ_REPOSITORY("read_repository"),
    /** Pull container images from the container registry. */
    READ_REGISTRY("read_registry"),
    /** Push container images to the container registry. */
    WRITE_REGISTRY("write_registry"),
    /** Pull packages from the package registry. */
    READ_PACKAGE_REGISTRY("read_package_registry"),
    /** Push packages to the package registry. */
    WRITE_PACKAGE_REGISTRY("write_package_registry"),
    /** Read from the dependency proxy (virtual registry). */
    READ_VIRTUAL_REGISTRY("read_virtual_registry"),
    /** Write to the dependency proxy (virtual registry). */
    WRITE_VIRTUAL_REGISTRY("write_virtual_registry");

    private final String wireValue;

    DeployTokenScope(final String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the name of the scope as GitLab spells it, such as {@code read_repository}.
     *
     * @return the name GitLab uses
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Finds the scope GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the scope
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static DeployTokenScope fromWire(final String value) {
        requireNonNull(value, "value");
        for (final DeployTokenScope scope : values()) {
            if (scope.wireValue.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown deploy token scope: " + value);
    }
}
