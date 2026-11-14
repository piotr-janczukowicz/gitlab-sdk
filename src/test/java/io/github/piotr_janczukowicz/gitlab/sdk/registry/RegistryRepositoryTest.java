package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.details;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.fullDetails;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.fullRepository;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.groupRepository;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.listedTag;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.loadedProject;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryRepository;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabProjectContainerRegistryDeleteTagFilter;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabProjectContainerRegistryDeleteTagFilterBuilder;
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
class RegistryRepositoryTest {

    private static final String ID = "42";
    private static final long REPOSITORY_ID = 77L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final Project project = sdk.project(ID);

    private RegistryRepository owned(final GitLabContainerRegistryRepository response) {
        return new RegistryRepository(project, response);
    }

    private RegistryRepository grouped(final GitLabContainerRegistryRepository response) {
        return new RegistryRepository(sdk, response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabContainerRegistryRepository response = fullRepository(REPOSITORY_ID);

        final RegistryRepository sut = owned(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(REPOSITORY_ID);
            softly.assertThat(sut.name()).isEqualTo("app");
            softly.assertThat(sut.path()).isEqualTo("my-group/demo/app");
            softly.assertThat(sut.location()).isEqualTo("registry.example.com/my-group/demo/app");
            softly.assertThat(sut.tagsCount()).contains(3);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void tagsCount_shouldBeEmpty_whenAbsent() {
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        assertThat(sut.tagsCount()).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new RegistryRepository((Project) null, repository(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new RegistryRepository(project, null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new RegistryRepository((GitLabSdk) null, repository(1L))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new RegistryRepository(sdk, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void project_shouldReturnOwnerWithoutRequest_whenBuiltByProject() throws IOException {
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Project result = sut.project();

        assertThat(result).isSameAs(project);
        verifyNoInteractions(gitLab);
    }

    @Test
    void project_shouldLoadThroughProjectId_whenBuiltBySdk() throws IOException {
        when(gitLab.projects().project().get("99")).thenReturn(Optional.of(loadedProject(99L)));
        final RegistryRepository sut = grouped(groupRepository(REPOSITORY_ID, 99L));

        final Project result = sut.project();

        assertThat(result.id()).isEqualTo("99");
    }

    @Test
    void project_shouldThrowIllegalStateException_whenProjectIdAbsent() {
        final RegistryRepository sut = grouped(repository(REPOSITORY_ID));

        final Throwable thrown = catchThrowable(sut::project);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("project id");
    }

    @Test
    void project_shouldThrowIllegalStateException_whenProjectIsGone() throws IOException {
        when(gitLab.projects().project().get("99")).thenReturn(Optional.empty());
        final RegistryRepository sut = grouped(groupRepository(REPOSITORY_ID, 99L));

        final Throwable thrown = catchThrowable(sut::project);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("99");
    }

    @Test
    void refresh_shouldUseRepositoryIdNotProjectId_whenCalled() throws IOException {
        final GitLabContainerRegistryRepository fresh = fullRepository(REPOSITORY_ID);
        when(gitLab.containerregistry().get("77")).thenReturn(Optional.of(fresh));
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final RegistryRepository result = sut.refresh();

        final Project owner = result.project();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(fresh);
            softly.assertThat(owner).isSameAs(project);
        });
        verify(gitLab.containerregistry(), never()).get(ID);
    }

    @Test
    void refresh_shouldKeepProjectId_whenBuiltBySdkAndAnswerHasNone() throws IOException {
        when(gitLab.containerregistry().get("77")).thenReturn(Optional.of(repository(REPOSITORY_ID)));
        when(gitLab.containerregistry().listTags("99", REPOSITORY_ID).stream()).thenReturn(Stream.of(listedTag("v1")));
        when(gitLab.containerregistry().getTag("99", REPOSITORY_ID, "v1")).thenReturn(Optional.of(details("v1")));
        final RegistryRepository sut = grouped(groupRepository(REPOSITORY_ID, 99L));

        final List<RegistryTag> tags = sut.refresh().tags().toList();

        assertThat(tags).extracting(RegistryTag::name).containsExactly("v1");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.containerregistry().get("77")).thenReturn(Optional.empty());
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("77");
    }

    @Test
    void tags_shouldLoadEachItem_whenListReturnsSmallerRecord() throws IOException {
        when(gitLab.containerregistry().listTags(ID, REPOSITORY_ID).stream())
                .thenReturn(Stream.of(listedTag("v1"), listedTag("v2")));
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v1")).thenReturn(Optional.of(details("v1")));
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v2")).thenReturn(Optional.of(fullDetails("v2")));
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final List<RegistryTag> result = sut.tags().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(RegistryTag::name).containsExactly("v1", "v2");
            softly.assertThat(result.get(1).response()).isEqualTo(fullDetails("v2"));
            softly.assertThat(result.get(1).repository()).isSameAs(sut);
        });
    }

    @Test
    void tags_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.containerregistry().listTags(ID, REPOSITORY_ID).stream())
                .thenReturn(Stream.of(listedTag("v1"), listedTag("v2"), listedTag("v3")));
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v1")).thenReturn(Optional.of(details("v1")));
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v2")).thenReturn(Optional.empty());
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v3")).thenReturn(Optional.of(details("v3")));
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final List<RegistryTag> result = sut.tags().toList();

        assertThat(result).extracting(RegistryTag::name).containsExactly("v1", "v3");
    }

    @Test
    void tags_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.containerregistry().listTags(ID, REPOSITORY_ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return listedTag(name);
                }));
        when(gitLab.containerregistry().getTag(any(), anyLong(), any())).thenReturn(Optional.of(details("a")));
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Stream<RegistryTag> stream = sut.tags();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void tags_shouldThrowIllegalStateException_whenProjectIdAbsent() throws IOException {
        final RegistryRepository sut = grouped(repository(REPOSITORY_ID));

        final Throwable thrown = catchThrowable(sut::tags);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void tag_shouldReturnTag_whenFound() throws IOException {
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v1")).thenReturn(Optional.of(fullDetails("v1")));
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Optional<RegistryTag> result = sut.tag("v1");

        assertThat(result)
                .hasValueSatisfying(found -> assertThat(found.digest()).contains("sha256:abc"));
    }

    @Test
    void tag_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v1")).thenReturn(Optional.empty());
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Optional<RegistryTag> result = sut.tag("v1");

        assertThat(result).isEmpty();
    }

    @Test
    void tag_shouldPassNameUnchanged_whenItHasSlashAndColon() throws IOException {
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "release/1:rc"))
                .thenReturn(Optional.of(details("release/1:rc")));
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Optional<RegistryTag> result = sut.tag("release/1:rc");

        assertThat(result).hasValueSatisfying(found -> assertThat(found.name()).isEqualTo("release/1:rc"));
        verify(gitLab.containerregistry()).getTag(ID, REPOSITORY_ID, "release/1:rc");
    }

    @Test
    void tag_shouldThrowBeforeRequest_whenNameIsNullOrBlank() throws IOException {
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.tag(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.tag(" "))).isInstanceOf(IllegalArgumentException.class);
        });
        verify(gitLab.containerregistry(), never()).getTag(any(), anyLong(), any());
    }

    @Test
    void delete_shouldCallCore_whenBuiltByProject() throws IOException {
        owned(repository(REPOSITORY_ID)).delete();

        verify(gitLab.containerregistry()).delete(ID, REPOSITORY_ID);
    }

    @Test
    void delete_shouldUseProjectId_whenBuiltBySdk() throws IOException {
        grouped(groupRepository(REPOSITORY_ID, 99L)).delete();

        verify(gitLab.containerregistry()).delete("99", REPOSITORY_ID);
    }

    @Test
    void delete_shouldThrowIllegalStateException_whenProjectIdAbsent() throws IOException {
        final RegistryRepository sut = grouped(repository(REPOSITORY_ID));

        final Throwable thrown = catchThrowable(sut::delete);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.containerregistry(), never()).delete(any(), anyLong());
    }

    @Test
    void deleteTags_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectContainerRegistryDeleteTagFilter filter =
                GitLabProjectContainerRegistryDeleteTagFilterBuilder.builder()
                        .nameRegexDelete("v1.*")
                        .keepN(2)
                        .build();

        owned(repository(REPOSITORY_ID)).deleteTags(filter);

        verify(gitLab.containerregistry()).deleteTag(ID, REPOSITORY_ID, filter);
    }

    @Test
    void deleteTags_shouldThrowNullPointerException_whenFilterIsNull() throws IOException {
        final RegistryRepository sut = owned(repository(REPOSITORY_ID));

        final Throwable thrown = catchThrowable(() -> sut.deleteTags(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.containerregistry(), never())
                .deleteTag(any(), anyLong(), any(GitLabProjectContainerRegistryDeleteTagFilter.class));
    }
}
