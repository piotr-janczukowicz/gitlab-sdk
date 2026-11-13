package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.tag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabProjectTagsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTag;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectTagsTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void tags_shouldListTags_whenGitLabReturnsThem() throws IOException {
        when(gitLab.tags().list(ID).stream()).thenReturn(Stream.of(tag("v1", "a"), tag("v2", "b")));

        final List<Tag> result = sut.tags().toList();

        assertThat(result).extracting(Tag::name).containsExactly("v1", "v2");
    }

    @Test
    void tags_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.tags().list(ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return tag(name, "sha");
                }));

        final Stream<Tag> stream = sut.tags();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void tags_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectTagsListFilter filter =
                GitLabProjectTagsListFilterBuilder.builder().search("^v").build();
        when(gitLab.tags().list(ID, filter).stream()).thenReturn(Stream.of(tag("v1", "a")));

        final List<Tag> result = sut.tags(filter).toList();

        assertThat(result).extracting(Tag::name).containsExactly("v1");
    }

    @Test
    void tags_shouldThrowNullPointerException_whenFilterIsNull() {
        assertThatThrownBy(() -> sut.tags(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.tags());
    }

    @Test
    void tag_shouldReturnLoadedTag_whenGitLabHasIt() throws IOException {
        final GitLabTag response = tag("v1", "a");
        when(gitLab.tags().get(ID, "v1")).thenReturn(Optional.of(response));

        final Optional<Tag> found = sut.tag("v1");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void tag_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.tags().get(ID, "gone")).thenReturn(Optional.empty());

        final Optional<Tag> found = sut.tag("gone");

        assertThat(found).isEmpty();
    }

    @Test
    void tag_shouldPassNameWithSlashUnchanged_whenNameContainsSlash() throws IOException {
        when(gitLab.tags().get(ID, "release/1.x")).thenReturn(Optional.of(tag("release/1.x", "a")));

        final Optional<Tag> found = sut.tag("release/1.x");

        assertThat(found).isPresent();
        verify(gitLab.tags()).get(ID, "release/1.x");
    }

    @Test
    void tag_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        assertThatThrownBy(() -> sut.tag(" ")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.tags());
    }

    @Test
    void tag_shouldThrowNullPointerException_whenNameIsNull() {
        assertThatThrownBy(() -> sut.tag(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.tags());
    }

    @Test
    void createTag_shouldSendNameRefAndMessage_whenMessageGiven() throws IOException {
        final GitLabTag created = tag("v1", "a");
        final GitLabProjectTagsCreatePayload payload = GitLabProjectTagsCreatePayloadBuilder.builder()
                .tagName("v1")
                .ref("main")
                .message("First")
                .build();
        when(gitLab.tags().create(ID, payload)).thenReturn(created);

        final Tag result = sut.createTag("v1", "main", "First");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.project()).isSameAs(sut);
        });
    }

    @Test
    void createTag_shouldSendNameAndRefOnly_whenNoMessage() throws IOException {
        final GitLabTag created = tag("v1", "a");
        final GitLabProjectTagsCreatePayload payload = GitLabProjectTagsCreatePayloadBuilder.builder()
                .tagName("v1")
                .ref("main")
                .build();
        when(gitLab.tags().create(ID, payload)).thenReturn(created);

        final Tag result = sut.createTag("v1", "main");

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void createTag_shouldThrowIllegalArgumentException_whenNameOrRefIsBlank() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.createTag(" ", "main")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.createTag("v1", "")).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab.tags());
    }

    @Test
    void createTag_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.createTag(null, "main")).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.createTag("v1", null)).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.tags());
    }
}
