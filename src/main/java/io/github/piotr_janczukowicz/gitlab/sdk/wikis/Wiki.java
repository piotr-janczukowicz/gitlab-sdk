package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * The wiki of one project or group. {@code Project.wiki()} and {@code Group.wiki()} are the usual way in; this type
 * exists so both owner kinds share the same page objects. A group wiki needs GitLab Premium: on a lower tier GitLab
 * answers 403 or 404, and a client configured with a lower tier refuses the call with the core's
 * {@code GitLabTierException} before any request. Each method names the project request; the group request is the same
 * with {@code /groups/{id}} in place of {@code /projects/{id}}.
 *
 * <p>A slug is passed to the core as GitLab returns it, not URL-encoded: {@code dir/page} stays as it is, and the core
 * encodes it in the request path. An already encoded slug would be encoded twice.
 */
public final class Wiki {

    private static final String SLUG = "slug";

    private final WikiGateway gateway;

    private Wiki(final WikiGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Returns the wiki of a project; no request is made.
     *
     * @param project the project
     * @return the wiki
     * @throws NullPointerException if {@code project} is {@code null}
     */
    public static Wiki forProject(final Project project) {
        requireNonNull(project, "project");
        return new Wiki(new ProjectWikiGateway(Internals.gitLab(project), project.id()));
    }

    /**
     * Returns the wiki of a group (Premium); no request is made.
     *
     * @param group the group
     * @return the wiki
     * @throws NullPointerException if {@code group} is {@code null}
     */
    public static Wiki forGroup(final Group group) {
        requireNonNull(group, "group");
        return new Wiki(new GroupWikiGateway(Internals.gitLab(group), group.id()));
    }

    /**
     * Lists the pages of the wiki ({@code GET /projects/{id}/wikis}). GitLab lists every page in one response, without
     * the content, so the list is loaded at once and each page is loaded again ({@code GET
     * /projects/{id}/wikis/{slug}}) only as the stream reaches it: one extra request per page, and a page that is gone
     * by then is skipped. The stream can be used once; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the pages
     * @throws IOException if the list cannot be loaded
     */
    public Stream<WikiPage> all() throws IOException {
        return gateway.pages().stream()
                .map(listed -> Lookups.find(() -> find(requireNonNull(listed.slug(), SLUG))))
                .flatMap(Optional::stream);
    }

    /**
     * Loads one page ({@code GET /projects/{id}/wikis/{slug}}).
     *
     * @param slug the slug as GitLab returns it, not URL-encoded, such as {@code dir/page}
     * @return the page, or empty when GitLab answers HTTP 404; empty also when the wiki is not available
     * @throws NullPointerException if {@code slug} is {@code null}
     * @throws IllegalArgumentException if {@code slug} is blank
     * @throws IOException if the transport fails
     */
    public Optional<WikiPage> find(final String slug) throws IOException {
        return gateway.page(requireNotBlank(slug, SLUG)).map(loaded -> new WikiPage(this, loaded));
    }

    /**
     * Creates a page in GitLab's default format, Markdown unless the instance says otherwise ({@code POST
     * /projects/{id}/wikis}). GitLab derives the slug from the title; a slash in the title puts the page in a
     * directory.
     *
     * @param title the title
     * @param content the content, sent as given
     * @return the page as GitLab returned it
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     */
    public WikiPage create(final String title, final String content) throws IOException {
        return created(title, content, null);
    }

    /**
     * Creates a page in a format ({@code POST /projects/{id}/wikis}), as {@link #create(String, String)} describes.
     *
     * @param title the title
     * @param content the content, sent as given
     * @param format the markup format
     * @return the page as GitLab returned it
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws IOException if the transport fails
     */
    public WikiPage create(final String title, final String content, final WikiFormat format) throws IOException {
        return created(title, content, requireNonNull(format, "format"));
    }

    /**
     * Uploads a file to the wiki repository ({@code POST /projects/{id}/wikis/attachments}) on its default branch. The
     * file is sent as the form part {@code file} with the content type {@code application/octet-stream}. GitLab offers
     * no call that removes an uploaded file. The core's record is passed through; its {@code link().markdown()} is the
     * text to put in a page.
     *
     * @param fileName the name of the file
     * @param content the bytes of the file
     * @return the attachment as GitLab returned it
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code fileName} is blank
     * @throws IOException if the transport fails
     */
    public GitLabWikiAttachment uploadAttachment(final String fileName, final byte[] content) throws IOException {
        return gateway.upload(attachmentForm(fileName, content).build());
    }

    /**
     * Uploads a file to a branch of the wiki repository ({@code POST /projects/{id}/wikis/attachments}), as
     * {@link #uploadAttachment(String, byte[])} describes.
     *
     * @param fileName the name of the file
     * @param content the bytes of the file
     * @param branch the branch of the wiki repository
     * @return the attachment as GitLab returned it
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code fileName} or {@code branch} is blank
     * @throws IOException if the transport fails
     */
    public GitLabWikiAttachment uploadAttachment(final String fileName, final byte[] content, final String branch)
            throws IOException {
        final MultipartFormData.Builder form = attachmentForm(fileName, content);
        return gateway.upload(
                form.text("branch", requireNotBlank(branch, "branch")).build());
    }

    WikiGateway gateway() {
        return gateway;
    }

    private WikiPage created(final String title, final String content, final @Nullable WikiFormat format)
            throws IOException {
        final String checkedTitle = requireNotBlank(title, "title");
        final String checkedContent = requireNonNull(content, "content");
        return new WikiPage(this, gateway.create(checkedTitle, checkedContent, format));
    }

    private static MultipartFormData.Builder attachmentForm(final String fileName, final byte[] content) {
        return MultipartFormData.builder()
                .file(
                        "file",
                        requireNotBlank(fileName, "fileName"),
                        "application/octet-stream",
                        requireNonNull(content, "content"));
    }
}
