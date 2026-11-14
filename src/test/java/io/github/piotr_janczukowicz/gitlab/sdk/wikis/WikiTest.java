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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiAttachment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabWikiPage;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisCreateFormat;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectwikis.GitLabProjectProjectWikisCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class WikiTest {

    private static final String ID = "my-group/my-project";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Wiki sut = project.wiki();

    @Test
    void wiki_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).project(ID).wiki();

        verifyNoInteractions(untouched);
    }

    @Test
    void forProject_shouldThrowNullPointerException_whenProjectIsNull() {
        final Throwable thrown = catchThrowable(() -> Wiki.forProject(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void all_shouldLoadEachItem_whenListReturnsBasicRecords() throws IOException {
        when(gitLab.projectwikis().list(ID)).thenReturn(List.of(listed("home"), listed("dir/page")));
        when(gitLab.projectwikis().get(ID, "home")).thenReturn(Optional.of(page("home")));
        when(gitLab.projectwikis().get(ID, "dir/page")).thenReturn(Optional.of(page("dir/page")));

        final List<WikiPage> result = sut.all().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(WikiPage::slug).containsExactly("home", "dir/page");
            softly.assertThat(result.getFirst().content()).contains(CONTENT);
            softly.assertThat(result.getFirst().wiki()).isSameAs(sut);
        });
    }

    @Test
    void all_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.projectwikis().list(ID)).thenReturn(List.of(listed("gone"), listed("home")));
        when(gitLab.projectwikis().get(ID, "gone")).thenReturn(Optional.empty());
        when(gitLab.projectwikis().get(ID, "home")).thenReturn(Optional.of(page("home")));

        final List<WikiPage> result = sut.all().toList();

        assertThat(result).extracting(WikiPage::slug).containsExactly("home");
    }

    @Test
    void all_shouldBeLazy_whenNotConsumed() throws IOException {
        when(gitLab.projectwikis().list(ID)).thenReturn(List.of(listed("home"), listed("other")));

        final Stream<WikiPage> result = sut.all();

        assertThat(result).isNotNull();
        verify(gitLab.projectwikis()).list(ID);
        verify(gitLab.projectwikis(), never()).get(anyString(), anyString());
    }

    @Test
    void all_shouldThrowUncheckedIoException_whenItemLoadFails() throws IOException {
        final IOException failure = new IOException("connection reset");
        when(gitLab.projectwikis().list(ID)).thenReturn(List.of(listed("home")));
        when(gitLab.projectwikis().get(ID, "home")).thenThrow(failure);

        final Throwable thrown = catchThrowable(() -> sut.all().toList());

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasCause(failure);
    }

    @Test
    void find_shouldReturnPage_whenGitLabHasIt() throws IOException {
        final GitLabWikiPage response = page("home");
        when(gitLab.projectwikis().get(ID, "home")).thenReturn(Optional.of(response));

        final Optional<WikiPage> result = sut.find("home");

        assertSoftly(softly -> {
            softly.assertThat(result).map(WikiPage::response).contains(response);
            softly.assertThat(result).map(WikiPage::wiki).contains(sut);
        });
    }

    @Test
    void find_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectwikis().get(ID, "missing")).thenReturn(Optional.empty());

        final Optional<WikiPage> result = sut.find("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void find_shouldPassSlugUnchanged_whenSlugHasSlash() throws IOException {
        when(gitLab.projectwikis().get(ID, "dir/page")).thenReturn(Optional.of(page("dir/page")));

        final Optional<WikiPage> result = sut.find("dir/page");

        assertThat(result).map(WikiPage::slug).contains("dir/page");
        verify(gitLab.projectwikis()).get(ID, "dir/page");
    }

    @Test
    void find_shouldThrowIllegalArgumentException_whenSlugIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.find(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.projectwikis(), never()).get(anyString(), anyString());
    }

    @Test
    void create_shouldSendTitleContentAndFormat() throws IOException {
        final GitLabProjectProjectWikisCreatePayload expected = GitLabProjectProjectWikisCreatePayloadBuilder.builder()
                .title("Guide")
                .content(CONTENT)
                .format(GitLabProjectProjectWikisCreateFormat.ASCIIDOC)
                .build();
        when(gitLab.projectwikis().create(ID, expected)).thenReturn(page("Guide"));

        final WikiPage created = sut.create("Guide", CONTENT, WikiFormat.ASCIIDOC);

        assertSoftly(softly -> {
            softly.assertThat(created.slug()).isEqualTo("Guide");
            softly.assertThat(created.wiki()).isSameAs(sut);
        });
        verify(gitLab.projectwikis()).create(ID, expected);
    }

    @Test
    void create_shouldOmitFormat_whenNotGiven() throws IOException {
        final GitLabProjectProjectWikisCreatePayload expected = GitLabProjectProjectWikisCreatePayloadBuilder.builder()
                .title("Guide")
                .content("")
                .build();
        when(gitLab.projectwikis().create(ID, expected)).thenReturn(page("Guide"));

        final WikiPage created = sut.create("Guide", "");

        assertThat(created.slug()).isEqualTo("Guide");
        verify(gitLab.projectwikis()).create(ID, expected);
    }

    @Test
    void create_shouldRejectInvalidArguments_withoutCoreCall() throws IOException {
        final Throwable blankTitle = catchThrowable(() -> sut.create(" ", CONTENT));
        final Throwable nullContent = catchThrowable(() -> sut.create("Guide", null));
        final Throwable nullFormat = catchThrowable(() -> sut.create("Guide", CONTENT, null));

        assertSoftly(softly -> {
            softly.assertThat(blankTitle).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullContent).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullFormat).isInstanceOf(NullPointerException.class);
        });
        verify(gitLab.projectwikis(), never()).create(anyString(), any());
    }

    @Test
    void uploadAttachment_shouldSendFilePartAndBranch() throws IOException {
        final GitLabWikiAttachment response = attachment();
        when(gitLab.projectwikis().createAttachment(eq(ID), any(MultipartFormData.class)))
                .thenReturn(response);

        final GitLabWikiAttachment result = sut.uploadAttachment("dk.png", PNG, "main");

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.projectwikis()).createAttachment(eq(ID), form.capture());
        final String wire = new String(
                form.getValue().body().mapBytes(bytes -> bytes, () -> new byte[0]), StandardCharsets.ISO_8859_1);
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(response);
            softly.assertThat(wire).contains("name=\"file\"; filename=\"dk.png\"");
            softly.assertThat(wire).contains("Content-Type: application/octet-stream");
            softly.assertThat(wire).contains(new String(PNG, StandardCharsets.ISO_8859_1));
            softly.assertThat(wire).contains("name=\"branch\"\r\n\r\nmain\r\n");
        });
    }

    @Test
    void uploadAttachment_shouldSendNoBranch_whenNotGiven() throws IOException {
        when(gitLab.projectwikis().createAttachment(eq(ID), any(MultipartFormData.class)))
                .thenReturn(attachment());

        sut.uploadAttachment("dk.png", PNG);

        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.projectwikis()).createAttachment(eq(ID), form.capture());
        final String body = bodyOf(form.getValue());
        assertSoftly(softly -> {
            softly.assertThat(body).contains("name=\"file\"; filename=\"dk.png\"");
            softly.assertThat(body).doesNotContain("name=\"branch\"");
        });
    }

    @Test
    void uploadAttachment_shouldRejectBlankFileName_withoutCoreCall() throws IOException {
        final Throwable blankName = catchThrowable(() -> sut.uploadAttachment(" ", PNG));
        final Throwable nullContent = catchThrowable(() -> sut.uploadAttachment("dk.png", null));
        final Throwable blankBranch = catchThrowable(() -> sut.uploadAttachment("dk.png", PNG, " "));

        assertSoftly(softly -> {
            softly.assertThat(blankName).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullContent).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankBranch).isInstanceOf(IllegalArgumentException.class);
        });
        verify(gitLab.projectwikis(), never()).createAttachment(anyString(), any());
    }
}
