package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * A scope of a project or group access token: what the token may be used for. The values mirror the scopes the core
 * accepts when it creates a project or a group access token, with the same constant names; a unit test compares them
 * with both core enums. A scope in a response that is not listed here makes {@link AccessToken#scopes()} throw;
 * {@link AccessToken#scopeNames()} gives the raw names.
 */
public enum AccessTokenScope {
    /** Full read and write access to the API. */
    API("api"),
    /** Read access to the API. */
    READ_API("read_api"),
    /** Create runners. */
    CREATE_RUNNER("create_runner"),
    /** Manage runners. */
    MANAGE_RUNNER("manage_runner"),
    /** Call the Kubernetes agent proxy. */
    K8S_PROXY("k8s_proxy"),
    /** Rotate the token itself. */
    SELF_ROTATE("self_rotate"),
    /** Use the MCP server. */
    MCP("mcp"),
    /** Use the MCP server for Orbit. */
    MCP_ORBIT("mcp_orbit"),
    /** A token whose permissions are set per resource. */
    GRANULAR("granular"),
    /** Read the repository. */
    READ_REPOSITORY("read_repository"),
    /** Write to the repository. */
    WRITE_REPOSITORY("write_repository"),
    /** Pull container images. */
    READ_REGISTRY("read_registry"),
    /** Push container images. */
    WRITE_REGISTRY("write_registry"),
    /** Read from the dependency proxy (virtual registry). */
    READ_VIRTUAL_REGISTRY("read_virtual_registry"),
    /** Write to the dependency proxy (virtual registry). */
    WRITE_VIRTUAL_REGISTRY("write_virtual_registry"),
    /** Read observability data. */
    READ_OBSERVABILITY("read_observability"),
    /** Write observability data. */
    WRITE_OBSERVABILITY("write_observability"),
    /** Use the AI features. */
    AI_FEATURES("ai_features");

    private final String value;

    AccessTokenScope(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the scope as GitLab spells it, such as {@code read_repository}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the scope GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the scope
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static AccessTokenScope from(final String value) {
        requireNonNull(value, "value");
        for (final AccessTokenScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown access token scope: " + value);
    }
}
