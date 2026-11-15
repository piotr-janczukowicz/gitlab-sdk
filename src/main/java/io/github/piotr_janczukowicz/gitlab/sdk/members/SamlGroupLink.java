package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSamlGroupLink;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import java.io.IOException;
import java.util.Optional;

/**
 * A link between a SAML group and a {@link Group}, as GitLab returned it. Per GitLab's API description, SAML group
 * links are a Premium feature: on a lower tier the core's {@code GitLabTierException} passes through. The object is
 * immutable, and {@link #response()} gives the complete response.
 */
public final class SamlGroupLink {

    private final Group group;
    private final GitLabSamlGroupLink response;

    /**
     * Wraps a response. Applications normally get this object from the group; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public SamlGroupLink(final Group group, final GitLabSamlGroupLink response) {
        this.group = requireNonNull(group, "group");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the group the link belongs to.
     *
     * @return the group
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the name of the SAML group.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the role the members of the SAML group get.
     *
     * @return the role
     * @throws IllegalArgumentException if the response holds a level the SDK does not know; use {@link #response()}
     */
    public AccessLevel accessLevel() {
        return AccessLevel.from(requireNonNull(response.accessLevel(), "accessLevel"));
    }

    /**
     * Returns the name of the SAML provider the link applies to.
     *
     * @return the provider, or empty when the response has none
     */
    public Optional<String> provider() {
        return Optional.ofNullable(response.provider());
    }

    /**
     * Deletes the link ({@code DELETE /groups/{id}/saml_group_links/{saml_group_name}}), with the provider of the link
     * when it has one.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final Optional<String> provider = provider();
        if (provider.isPresent()) {
            group.deleteSamlLink(name(), provider.get());
        } else {
            group.deleteSamlLink(name());
        }
    }

    /**
     * Loads the link again ({@code GET /groups/{id}/saml_group_links/{saml_group_name}}). This object is not changed.
     *
     * @return the link as GitLab holds it now
     * @throws IllegalStateException if the link is gone
     * @throws IOException if the transport fails
     */
    public SamlGroupLink refresh() throws IOException {
        final Optional<String> provider = provider();
        final Optional<SamlGroupLink> loaded =
                provider.isPresent() ? group.samlLink(name(), provider.get()) : group.samlLink(name());
        return loaded.orElseThrow(() -> new IllegalStateException("SAML group link " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabSamlGroupLink response() {
        return response;
    }
}
