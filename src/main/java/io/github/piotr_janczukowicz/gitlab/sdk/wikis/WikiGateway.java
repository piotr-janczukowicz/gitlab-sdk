package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPageBasic;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The wiki calls for one project or group. GitLab has separate endpoints, and separate core payloads, for each owner;
 * this interface hides which one is used.
 */
interface WikiGateway {

    List<GitLabWikiPageBasic> pages() throws IOException;

    Optional<GitLabWikiPage> page(String slug) throws IOException;

    GitLabWikiPage create(String title, String content, @Nullable WikiFormat format) throws IOException;

    GitLabWikiPage update(String slug, WikiPageChanges changes) throws IOException;

    void delete(String slug) throws IOException;

    GitLabWikiAttachment upload(MultipartFormData form) throws IOException;
}
