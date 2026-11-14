package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.gitlab.sdk.internal.FrozenValues.frozenMap;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * A wiki page, as GitLab returned it. The object is immutable: it keeps the data of the moment it was loaded, and
 * {@link #response()} gives the complete response. {@link #wiki()} returns the wiki the page belongs to. Its
 * {@code toString()} is the default one, so it never prints the content.
 */
public final class WikiPage {

    private final Wiki wiki;
    private final GitLabWikiPage response;

    /**
     * Wraps a response. Applications normally get this object from a {@link Wiki}; call the constructor only to wrap a
     * response that was loaded some other way.
     *
     * @param wiki the wiki the page belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public WikiPage(final Wiki wiki, final GitLabWikiPage response) {
        this.wiki = requireNonNull(wiki, "wiki");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the slug, which addresses the page in the requests. It is GitLab's value, not URL-encoded, such as
     * {@code dir/page}; GitLab derives it from the title, so a new title is expected to give a new slug.
     *
     * @return the slug
     */
    public String slug() {
        return requireNonNull(response.slug(), "slug");
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String title() {
        return requireNonNull(response.title(), "title");
    }

    /**
     * Returns the content.
     *
     * @return the content, or empty when GitLab sent none
     */
    public Optional<String> content() {
        return Optional.ofNullable(response.content());
    }

    /**
     * Returns the markup format.
     *
     * @return the format, or empty when GitLab sent none
     * @throws IllegalArgumentException if the SDK does not know the format; {@link #formatName()} gives the raw name
     */
    public Optional<WikiFormat> format() {
        return formatName().map(WikiFormat::from);
    }

    /**
     * Returns the markup format as GitLab spells it, such as {@code markdown}.
     *
     * @return the raw name, or empty when GitLab sent none
     */
    public Optional<String> formatName() {
        return Optional.ofNullable(response.format());
    }

    /**
     * Returns the encoding of the content, such as {@code UTF-8}.
     *
     * @return the encoding, or empty when GitLab sent none
     */
    public Optional<String> encoding() {
        return Optional.ofNullable(response.encoding());
    }

    /**
     * Returns the front matter of the page: the metadata block at its top, as GitLab parsed it. Nested objects are maps
     * and arrays are lists; a key with a JSON {@code null} keeps a {@code null} value.
     *
     * @return an immutable copy, nested maps and lists included; empty when GitLab sent none
     */
    public Map<String, Object> frontMatter() {
        final Map<String, Object> frontMatter = response.frontMatter();
        return frontMatter == null ? Map.of() : frozenMap(frontMatter);
    }

    /**
     * Returns the wiki the page belongs to, without a request.
     *
     * @return the wiki
     */
    public Wiki wiki() {
        return wiki;
    }

    /**
     * Loads the page again by its slug ({@code GET /projects/{id}/wikis/{slug}}, or the group variant). This object is
     * not changed.
     *
     * @return the page as GitLab holds it now
     * @throws IllegalStateException if the page is gone
     * @throws IOException if the transport fails
     */
    public WikiPage refresh() throws IOException {
        final String slug = slug();
        return wiki.find(slug).orElseThrow(() -> new IllegalStateException("Wiki page " + slug + " is gone"));
    }

    /**
     * Starts a change of the page. Nothing is sent until {@link WikiPageUpdate#send()}.
     *
     * @return the update builder
     */
    public WikiPageUpdate update() {
        return new WikiPageUpdate(this);
    }

    /**
     * Deletes the page ({@code DELETE /projects/{id}/wikis/{slug}}, or the group variant).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        wiki.gateway().delete(slug());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabWikiPage response() {
        return response;
    }

    WikiPage apply(final WikiPageChanges changes) throws IOException {
        return new WikiPage(wiki, wiki.gateway().update(slug(), changes));
    }
}
