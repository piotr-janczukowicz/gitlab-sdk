package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLink;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;

/**
 * A link between two issues, as GitLab returned it. The object is immutable, and {@link #response()} gives the complete
 * response, which names both issues. A link is addressed through the issue it was loaded from.
 */
public final class IssueLink {

    private final Issue issue;
    private final GitLabIssueLink response;

    /**
     * Wraps a response. Applications normally get this object from the issue; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param issue the issue the link was loaded from
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public IssueLink(final Issue issue, final GitLabIssueLink response) {
        this.issue = requireNonNull(issue, "issue");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the issue the link was loaded from.
     *
     * @return the issue
     */
    public Issue issue() {
        return issue;
    }

    /**
     * Returns the id of the link.
     *
     * @return the link id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the kind of the link.
     *
     * @return the type
     * @throws IllegalArgumentException if GitLab returns a type this SDK does not know; {@link #typeName()} still
     *     returns it
     */
    public IssueLinkType type() {
        return IssueLinkType.from(typeName());
    }

    /**
     * Returns the kind of the link exactly as GitLab spelled it, such as {@code relates_to}. Use it when
     * {@link #type()} throws {@link IllegalArgumentException} because GitLab returned a type this SDK does not know
     * yet.
     *
     * @return the raw type name
     */
    public String typeName() {
        return requireNonNull(response.linkType(), "linkType");
    }

    /**
     * Deletes the link, not the issues ({@code DELETE /projects/{id}/issues/{issue_iid}/links/{issue_link_id}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        Internals.gitLab(issue.project()).issuelinks().delete(issue.project().id(), issue.iid(), String.valueOf(id()));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabIssueLink response() {
        return response;
    }
}
