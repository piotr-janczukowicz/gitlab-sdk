package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLdapGroupLink;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import java.io.IOException;
import java.util.Optional;

/**
 * A link between an LDAP group (by CN or by filter) and a {@link Group}, as GitLab returned it. Per GitLab's API
 * description, LDAP group links are a Premium feature: on a lower tier the core's {@code GitLabTierException} passes
 * through. The object is immutable, and {@link #response()} gives the complete response. A link has no id and GitLab
 * has no call to load a single one, so it has no {@code refresh()}.
 */
public final class LdapGroupLink {

    private final Group group;
    private final GitLabLdapGroupLink response;

    /**
     * Wraps a response. Applications normally get this object from the group; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param group the group the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public LdapGroupLink(final Group group, final GitLabLdapGroupLink response) {
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
     * Returns the CN of the LDAP group.
     *
     * @return the CN, or empty when the link is made by filter
     */
    public Optional<String> cn() {
        return Optional.ofNullable(response.cn());
    }

    /**
     * Returns the LDAP filter of the link.
     *
     * @return the filter, or empty when the link is made by CN
     */
    public Optional<String> filter() {
        return Optional.ofNullable(response.filter());
    }

    /**
     * Returns the name of the LDAP provider.
     *
     * @return the provider
     */
    public String provider() {
        return requireNonNull(response.provider(), "provider");
    }

    /**
     * Returns the role the members of the LDAP group get.
     *
     * @return the role
     * @throws IllegalArgumentException if the response holds a level the SDK does not know; use {@link #response()}
     */
    public AccessLevel accessLevel() {
        return AccessLevel.from(requireNonNull(response.groupAccess(), "groupAccess"));
    }

    /**
     * Deletes the link ({@code DELETE /groups/{id}/ldap_group_links} with the provider and the CN or the filter).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final Optional<String> cn = cn();
        if (cn.isPresent()) {
            group.deleteLdapLink(provider(), cn.get());
        } else {
            group.deleteLdapFilterLink(
                    provider(),
                    filter().orElseThrow(() -> new IllegalStateException("The link has no CN and no filter")));
        }
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabLdapGroupLink response() {
        return response;
    }
}
