package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.attachment;
import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.bodyOf;
import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.listed;
import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.page;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisCreateFormat;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisUpdateFormat;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwikis.GitLabGroupGroupWikisUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupWikiTest {

    private static final String ID = "my-group";
    private static final byte[] PNG = {1, 2, 3};

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);
    private final Wiki sut = group.wiki();

    @Test
    void wiki_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).group(ID).wiki();

        verifyNoInteractions(untouched);
    }

    @Test
    void forGroup_shouldThrowNullPointerException_whenGroupIsNull() {
        final Throwable thrown = catchThrowable(() -> Wiki.forGroup(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void all_shouldLoadEachItem_whenListReturnsBasicRecords() throws IOException {
        when(gitLab.groupwikis().list(ID)).thenReturn(List.of(listed("gone"), listed("dir/page")));
        when(gitLab.groupwikis().get(ID, "gone")).thenReturn(Optional.empty());
        when(gitLab.groupwikis().get(ID, "dir/page")).thenReturn(Optional.of(page("dir/page")));

        final List<WikiPage> result = sut.all().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(WikiPage::slug).containsExactly("dir/page");
            softly.assertThat(result.getFirst().wiki()).isSameAs(sut);
        });
    }

    @Test
    void all_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier =
                new GitLabTierException("getApiV4GroupsIdWikis", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.groupwikis().list(ID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(sut::all);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void all_shouldPassForbiddenException_whenTierMissing() throws IOException {
        final GitLabForbiddenException forbidden = mock(GitLabForbiddenException.class);
        when(gitLab.groupwikis().list(ID)).thenThrow(forbidden);

        final Throwable thrown = catchThrowable(sut::all);

        assertThat(thrown).isSameAs(forbidden);
    }

    @Test
    void find_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.groupwikis().get(ID, "missing")).thenReturn(Optional.empty());

        final Optional<WikiPage> result = sut.find("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void find_shouldPassSlugUnchanged_whenSlugHasSlash() throws IOException {
        when(gitLab.groupwikis().get(ID, "dir/page")).thenReturn(Optional.of(page("dir/page")));

        final Optional<WikiPage> result = sut.find("dir/page");

        assertThat(result).map(WikiPage::slug).contains("dir/page");
    }

    @Test
    void create_shouldSendTitleContentAndFormat() throws IOException {
        final GitLabGroupGroupWikisCreatePayload expected = GitLabGroupGroupWikisCreatePayloadBuilder.builder()
                .title("Guide")
                .content(CONTENT)
                .format(GitLabGroupGroupWikisCreateFormat.ORG)
                .build();
        when(gitLab.groupwikis().create(ID, expected)).thenReturn(page("Guide"));

        final WikiPage created = sut.create("Guide", CONTENT, WikiFormat.ORG);

        assertThat(created.slug()).isEqualTo("Guide");
        verify(gitLab.groupwikis()).create(ID, expected);
    }

    @Test
    void create_shouldOmitFormat_whenNotGiven() throws IOException {
        final GitLabGroupGroupWikisCreatePayload expected = GitLabGroupGroupWikisCreatePayloadBuilder.builder()
                .title("Guide")
                .content(CONTENT)
                .build();
        when(gitLab.groupwikis().create(ID, expected)).thenReturn(page("Guide"));

        sut.create("Guide", CONTENT);

        verify(gitLab.groupwikis()).create(ID, expected);
    }

    @Test
    void update_shouldSendOnlySetFields() throws IOException {
        final GitLabGroupGroupWikisUpdatePayload expected = GitLabGroupGroupWikisUpdatePayloadBuilder.builder()
                .title("dir/renamed")
                .build();
        when(gitLab.groupwikis().update(ID, "dir/page", expected)).thenReturn(page("dir/renamed"));

        final WikiPage result = new WikiPage(sut, page("dir/page"))
                .update()
                .title("dir/renamed")
                .send();

        assertThat(result.slug()).isEqualTo("dir/renamed");
        verify(gitLab.groupwikis()).update(ID, "dir/page", expected);
    }

    @Test
    void update_shouldSendFormat_whenSet() throws IOException {
        final GitLabGroupGroupWikisUpdatePayload expected = GitLabGroupGroupWikisUpdatePayloadBuilder.builder()
                .content("* Org")
                .format(GitLabGroupGroupWikisUpdateFormat.ORG)
                .build();
        when(gitLab.groupwikis().update(ID, "home", expected)).thenReturn(page("home"));

        new WikiPage(sut, page("home"))
                .update()
                .content("* Org")
                .format(WikiFormat.ORG)
                .send();

        verify(gitLab.groupwikis()).update(ID, "home", expected);
    }

    @Test
    void delete_shouldCallCoreWithSlug() throws IOException {
        new WikiPage(sut, page("dir/page")).delete();

        verify(gitLab.groupwikis()).delete(ID, "dir/page");
    }

    @Test
    void uploadAttachment_shouldSendFilePartAndBranch() throws IOException {
        final GitLabWikiAttachment response = attachment();
        when(gitLab.groupwikis().createAttachment(eq(ID), any(MultipartFormData.class)))
                .thenReturn(response);

        final GitLabWikiAttachment result = sut.uploadAttachment("notes.txt", PNG, "main");

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.groupwikis()).createAttachment(eq(ID), form.capture());
        final String body = bodyOf(form.getValue());
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(response);
            softly.assertThat(body).contains("name=\"file\"; filename=\"notes.txt\"");
            softly.assertThat(body).contains("Content-Type: application/octet-stream");
            softly.assertThat(body).contains("name=\"branch\"\r\n\r\nmain\r\n");
        });
    }
}
