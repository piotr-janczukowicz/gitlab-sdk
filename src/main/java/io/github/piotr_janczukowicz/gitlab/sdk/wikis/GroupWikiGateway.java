package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBasic;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisApi;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisCreateFormat;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisUpdateFormat;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** The wiki calls for a group (Premium; the core's tier guard applies when a tier is configured). */
final class GroupWikiGateway implements WikiGateway {

    private final GitLab gitLab;
    private final String id;

    GroupWikiGateway(final GitLab gitLab, final String id) {
        this.gitLab = gitLab;
        this.id = id;
    }

    @Override
    public List<GitLabWikiPageBasic> pages() throws IOException {
        return api().list(id);
    }

    @Override
    public Optional<GitLabWikiPage> page(final String slug) throws IOException {
        return api().get(id, slug);
    }

    @Override
    public GitLabWikiPage create(final String title, final String content, final @Nullable WikiFormat format)
            throws IOException {
        return api().create(
                        id,
                        GitLabGroupGroupWikisCreatePayloadBuilder.builder()
                                .title(title)
                                .content(content)
                                .format(
                                        format == null
                                                ? null
                                                : GitLabGroupGroupWikisCreateFormat.fromWire(format.value()))
                                .build());
    }

    @Override
    public GitLabWikiPage update(final String slug, final WikiPageChanges changes) throws IOException {
        final WikiFormat format = changes.format();
        return api().update(
                        id,
                        slug,
                        GitLabGroupGroupWikisUpdatePayloadBuilder.builder()
                                .title(changes.title())
                                .content(changes.content())
                                .format(
                                        format == null
                                                ? null
                                                : GitLabGroupGroupWikisUpdateFormat.fromWire(format.value()))
                                .build());
    }

    @Override
    public void delete(final String slug) throws IOException {
        api().delete(id, slug);
    }

    @Override
    public GitLabWikiAttachment upload(final MultipartFormData form) throws IOException {
        return api().createAttachment(id, form);
    }

    private GitLabGroupGroupWikisApi api() {
        return gitLab.groupwikis();
    }
}
