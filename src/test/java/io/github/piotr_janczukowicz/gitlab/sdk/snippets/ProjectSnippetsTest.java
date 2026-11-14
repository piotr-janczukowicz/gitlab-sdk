package io.github.piotr_janczukowicz.gitlab.sdk.snippets;

import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.CONTENT;
import static io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetsFixtures.projectSnippet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateFile;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateFileBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectSnippet;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectSnippetsTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final ProjectSnippets sut = project.snippets();

    @Test
    void snippets_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).project(ID).snippets();

        verifyNoInteractions(untouched);
    }

    @Test
    void forProject_shouldThrowNullPointerException_whenProjectIsNull() {
        final Throwable thrown = catchThrowable(() -> ProjectSnippets.forProject(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void all_shouldWrapEachListedSnippet_whenListed() throws IOException {
        when(gitLab.projectsnippets().list(ID).stream()).thenReturn(Stream.of(projectSnippet(1L), projectSnippet(2L)));

        final List<Snippet> result = sut.all().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Snippet::id).containsExactly(1L, 2L);
            softly.assertThat(result.getFirst().project()).isSameAs(project);
        });
        verify(gitLab.projectsnippets().list(ID), times(1)).stream();
        verify(gitLab.projectsnippets(), never()).get(anyString(), anyLong());
    }

    @Test
    void all_shouldBeLazy_whenNotConsumed() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectsnippets().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return projectSnippet(id);
                }));

        final Stream<Snippet> result = sut.all();

        assertThat(result).isNotNull();
        assertThat(consumed).hasValue(0);
        verify(gitLab.projectsnippets(), never()).get(anyString(), anyLong());
    }

    @Test
    void find_shouldReturnSnippet_whenGitLabHasIt() throws IOException {
        final GitLabProjectSnippet response = projectSnippet(7L);
        when(gitLab.projectsnippets().get(ID, 7L)).thenReturn(Optional.of(response));

        final Optional<Snippet> result = sut.find(7L);

        assertSoftly(softly -> {
            softly.assertThat(result).map(Snippet::response).contains(response);
            softly.assertThat(result).map(Snippet::project).contains(project);
        });
    }

    @Test
    void find_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectsnippets().get(ID, 7L)).thenReturn(Optional.empty());

        final Optional<Snippet> result = sut.find(7L);

        assertThat(result).isEmpty();
    }

    @Test
    void create_shouldSendFilesArray_whenSingleFile() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectSnippetsCreatePayload> sent =
                ArgumentCaptor.forClass(GitLabProjectProjectSnippetsCreatePayload.class);
        when(gitLab.projectsnippets().create(eq(ID), sent.capture())).thenReturn(projectSnippet(3L));

        final Snippet result = sut.create("test", SnippetVisibility.PRIVATE, "dir/add.rb", CONTENT);

        final GitLabProjectProjectSnippetsCreatePayload payload = sent.getValue();
        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(3L);
            softly.assertThat(payload.title()).isEqualTo("test");
            softly.assertThat(payload.files())
                    .containsExactly(new GitLabProjectProjectSnippetsCreateFile("dir/add.rb", CONTENT));
            softly.assertThat(payload.content()).isNull();
            softly.assertThat(payload.fileName()).isNull();
            softly.assertThat(payload.description()).isNull();
        });
        verify(gitLab.projectsnippets(), times(1)).create(eq(ID), any());
    }

    @Test
    void create_shouldSendVisibility() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectSnippetsCreatePayload> sent =
                ArgumentCaptor.forClass(GitLabProjectProjectSnippetsCreatePayload.class);
        when(gitLab.projectsnippets().create(eq(ID), sent.capture())).thenReturn(projectSnippet(3L));

        for (final SnippetVisibility visibility : SnippetVisibility.values()) {
            sut.create("test", visibility, "add.rb", CONTENT);
        }

        assertThat(sent.getAllValues())
                .extracting(GitLabProjectProjectSnippetsCreatePayload::visibility)
                .containsExactly(
                        GitLabProjectProjectSnippetsCreateVisibility.PRIVATE,
                        GitLabProjectProjectSnippetsCreateVisibility.INTERNAL,
                        GitLabProjectProjectSnippetsCreateVisibility.PUBLIC);
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
        verifyNoInteractions(gitLab.projectsnippets());
    }

    @Test
    void create_shouldPassPayloadThrough_whenPayloadGiven() throws IOException {
        final GitLabProjectProjectSnippetsCreatePayload payload =
                GitLabProjectProjectSnippetsCreatePayloadBuilder.builder()
                        .title("two files")
                        .visibility(GitLabProjectProjectSnippetsCreateVisibility.PRIVATE)
                        .files(List.of(
                                GitLabProjectProjectSnippetsCreateFileBuilder.builder()
                                        .filePath("a.txt")
                                        .content("a")
                                        .build(),
                                GitLabProjectProjectSnippetsCreateFileBuilder.builder()
                                        .filePath("b.txt")
                                        .content("b")
                                        .build()))
                        .build();
        when(gitLab.projectsnippets().create(ID, payload)).thenReturn(projectSnippet(4L));

        final Snippet result = sut.create(payload);

        assertThat(result.id()).isEqualTo(4L);
        verify(gitLab.projectsnippets(), times(1)).create(ID, payload);
    }

    @Test
    void create_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.create(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.projectsnippets());
    }
}
