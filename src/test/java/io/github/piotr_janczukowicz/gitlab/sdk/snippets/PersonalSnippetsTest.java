package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.author;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.listedSnippet;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.oneFile;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.personalSnippet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreateFile;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabCodeSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabPersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabSnippet;
import io.github.piotr_janczukowicz.gitlab.api.codesnippets.GitLabSnippetBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PersonalSnippetsTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final PersonalSnippets sut = sdk.snippets();

    @Test
    void snippets_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).snippets();

        verifyNoInteractions(untouched);
    }

    @Test
    void forSdk_shouldThrowNullPointerException_whenSdkIsNull() {
        final Throwable thrown = catchThrowable(() -> PersonalSnippets.forSdk(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void all_shouldCopyEveryComponentWithoutRequest_whenPersonalListReturnsListRecord() throws Exception {
        final GitLabSnippet listed = GitLabSnippetBuilder.builder()
                .id(42L)
                .title("Ut praesentium non et atque.")
                .description("description")
                .visibility("internal")
                .author(author())
                .createdAt(Instant.parse("2026-11-13T09:00:00Z"))
                .updatedAt(Instant.parse("2026-11-13T10:00:00Z"))
                .projectId(1L)
                .webUrl("http://example.com/snippets/42")
                .rawUrl("http://example.com/snippets/42/raw")
                .sshUrlToRepo("ssh://git@example.com/snippets/42.git")
                .httpUrlToRepo("https://example.com/snippets/42.git")
                .fileName("ondrickaemard.rb")
                .files(oneFile())
                .imported(false)
                .importedFrom("none")
                .repositoryStorage("default")
                .build();
        when(gitLab.codesnippets().list().stream()).thenReturn(Stream.of(listed));

        final GitLabPersonalSnippet copied = sut.all().toList().getFirst().response();

        final RecordComponent[] from = GitLabSnippet.class.getRecordComponents();
        final RecordComponent[] to = GitLabPersonalSnippet.class.getRecordComponents();
        assertThat(Arrays.stream(to).map(RecordComponent::getName).toList())
                .isEqualTo(Arrays.stream(from).map(RecordComponent::getName).toList());
        assertThat(Arrays.stream(to).map(RecordComponent::getGenericType).toList())
                .isEqualTo(
                        Arrays.stream(from).map(RecordComponent::getGenericType).toList());
        for (int i = 0; i < from.length; i++) {
            final Object value = from[i].getAccessor().invoke(listed);
            assertThat(value).as(from[i].getName()).isNotNull();
            assertThat(to[i].getAccessor().invoke(copied)).as(to[i].getName()).isEqualTo(value);
        }
        verify(gitLab.codesnippets(), never()).get(anyLong());
    }

    @Test
    void all_shouldBeLazy_whenNotConsumed() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.codesnippets().list().stream()).thenReturn(Stream.of(1L, 2L).map(id -> {
            consumed.incrementAndGet();
            return listedSnippet(id);
        }));

        final Stream<PersonalSnippet> result = sut.all();

        assertThat(result).isNotNull();
        assertThat(consumed).hasValue(0);
        verify(gitLab.codesnippets(), never()).get(anyLong());
    }

    @Test
    void all_shouldWrapEachListedSnippet_whenListed() throws IOException {
        when(gitLab.codesnippets().list().stream()).thenReturn(Stream.of(listedSnippet(1L), listedSnippet(2L)));

        final List<PersonalSnippet> result = sut.all().toList();

        assertThat(result).extracting(PersonalSnippet::id).containsExactly(1L, 2L);
        verify(gitLab.codesnippets().list(), times(1)).stream();
    }

    @Test
    void find_shouldReturnSnippet_whenGitLabHasIt() throws IOException {
        final GitLabPersonalSnippet response = personalSnippet(7L);
        when(gitLab.codesnippets().get(7L)).thenReturn(Optional.of(response));

        final Optional<PersonalSnippet> result = sut.find(7L);

        assertThat(result).map(PersonalSnippet::response).contains(response);
    }

    @Test
    void find_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.codesnippets().get(7L)).thenReturn(Optional.empty());

        final Optional<PersonalSnippet> result = sut.find(7L);

        assertThat(result).isEmpty();
    }

    @Test
    void create_shouldSendFilesArray_whenSingleFile() throws IOException {
        final ArgumentCaptor<GitLabCodeSnippetsCreatePayload> sent =
                ArgumentCaptor.forClass(GitLabCodeSnippetsCreatePayload.class);
        when(gitLab.codesnippets().create(sent.capture())).thenReturn(personalSnippet(3L));

        final PersonalSnippet result = sut.create("test", SnippetVisibility.PRIVATE, "dir/add.rb", CONTENT);

        final GitLabCodeSnippetsCreatePayload payload = sent.getValue();
        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(3L);
            softly.assertThat(payload.title()).isEqualTo("test");
            softly.assertThat(payload.files()).containsExactly(new GitLabCodeSnippetsCreateFile("dir/add.rb", CONTENT));
            softly.assertThat(payload.content()).isNull();
            softly.assertThat(payload.fileName()).isNull();
            softly.assertThat(payload.description()).isNull();
        });
        verify(gitLab.codesnippets(), times(1)).create(any());
    }

    @Test
    void create_shouldSendVisibility() throws IOException {
        final ArgumentCaptor<GitLabCodeSnippetsCreatePayload> sent =
                ArgumentCaptor.forClass(GitLabCodeSnippetsCreatePayload.class);
        when(gitLab.codesnippets().create(sent.capture())).thenReturn(personalSnippet(3L));

        for (final SnippetVisibility visibility : SnippetVisibility.values()) {
            sut.create("test", visibility, "add.rb", CONTENT);
        }

        assertThat(sent.getAllValues())
                .extracting(GitLabCodeSnippetsCreatePayload::visibility)
                .containsExactly(
                        GitLabCodeSnippetsCreateVisibility.PRIVATE,
                        GitLabCodeSnippetsCreateVisibility.INTERNAL,
                        GitLabCodeSnippetsCreateVisibility.PUBLIC);
    }

    @Test
    void create_shouldRejectInvalidArguments_withoutCoreCall() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.create(" ", SnippetVisibility.PRIVATE, "add.rb", CONTENT)))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.create("test", null, "add.rb", CONTENT)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.create("test", SnippetVisibility.PRIVATE, " ", CONTENT)))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> sut.create("test", SnippetVisibility.PRIVATE, "add.rb", null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.codesnippets());
    }

    @Test
    void create_shouldPassPayloadThrough_whenPayloadGiven() throws IOException {
        final GitLabCodeSnippetsCreatePayload payload = GitLabCodeSnippetsCreatePayloadBuilder.builder()
                .title("two files")
                .files(List.of(
                        new GitLabCodeSnippetsCreateFile("a.txt", "a"), new GitLabCodeSnippetsCreateFile("b.txt", "b")))
                .build();
        when(gitLab.codesnippets().create(payload)).thenReturn(personalSnippet(4L));

        final PersonalSnippet result = sut.create(payload);

        assertThat(result.id()).isEqualTo(4L);
        verify(gitLab.codesnippets(), times(1)).create(payload);
    }

    @Test
    void create_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.create(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.codesnippets());
    }
}
