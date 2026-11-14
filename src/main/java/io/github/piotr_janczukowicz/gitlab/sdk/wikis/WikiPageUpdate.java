package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.sdk.internal.UpdateState;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * Collects changes to a {@link WikiPage} and sends them in one request. Only the fields that were set are sent, and
 * nothing is sent until {@link #send()}. GitLab's REST docs require the title or the content on an update, so a change
 * of the format alone is expected to be refused; the SDK sends it as set and GitLab's answer passes through (unverified
 * live). The front matter is not offered here; use the core client for it.
 */
public final class WikiPageUpdate {

    private final WikiPage page;
    private final UpdateState state = new UpdateState();
    private @Nullable String title;
    private @Nullable String content;
    private @Nullable WikiFormat format;

    /**
     * Starts a change of one page. Applications normally get it from {@link WikiPage#update()}. Nothing is sent until
     * {@code send()}.
     *
     * @param page the page to change
     * @throws NullPointerException if {@code page} is {@code null}
     */
    public WikiPageUpdate(final WikiPage page) {
        this.page = requireNonNull(page, "page");
    }

    /**
     * Sets the title. GitLab derives the slug from the title, so the page {@link #send()} returns has a new slug, while
     * the request still names the old one.
     *
     * @param title the title
     * @return this update
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public WikiPageUpdate title(final String title) {
        this.title = requireNotBlank(title, "title");
        return changed();
    }

    /**
     * Sets the content, sent as given.
     *
     * @param content the content
     * @return this update
     * @throws NullPointerException if {@code content} is {@code null}
     */
    public WikiPageUpdate content(final String content) {
        this.content = requireNonNull(content, "content");
        return changed();
    }

    /**
     * Sets the markup format.
     *
     * @param format the format
     * @return this update
     * @throws NullPointerException if {@code format} is {@code null}
     */
    public WikiPageUpdate format(final WikiFormat format) {
        this.format = requireNonNull(format, "format");
        return changed();
    }

    /**
     * Sends the changes ({@code PUT /projects/{id}/wikis/{slug}}, or the group variant) to the slug of the page this
     * update was made from. That page is not changed.
     *
     * @return the page as GitLab returned it after the update, with its new slug when the title changed
     * @throws IllegalStateException if nothing was set
     * @throws IOException if the transport fails
     */
    public WikiPage send() throws IOException {
        state.requireChange();
        return page.apply(new WikiPageChanges(title, content, format));
    }

    private WikiPageUpdate changed() {
        state.mark();
        return this;
    }
}
