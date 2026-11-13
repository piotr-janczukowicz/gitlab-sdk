package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.release;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabRelease;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesListFilterBuilder;
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
class ProjectReleasesTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void releases_shouldListReleases_whenGitLabReturnsThem() throws IOException {
        when(gitLab.releases().list(ID).stream()).thenReturn(Stream.of(release("v1"), release("v2")));

        final List<Release> result = sut.releases().toList();

        assertThat(result).extracting(Release::tagName).containsExactly("v1", "v2");
    }

    @Test
    void releases_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.releases().list(ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(tag -> {
                    consumed.incrementAndGet();
                    return release(tag);
                }));

        final Stream<Release> stream = sut.releases();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void releases_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectReleasesListFilter filter =
                GitLabProjectReleasesListFilterBuilder.builder().build();
        when(gitLab.releases().list(ID, filter).stream()).thenReturn(Stream.of(release("v1")));

        final List<Release> result = sut.releases(filter).toList();

        assertThat(result).extracting(Release::tagName).containsExactly("v1");
    }

    @Test
    void releases_shouldThrowNullPointerException_whenFilterIsNull() {
        assertThatThrownBy(() -> sut.releases(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.releases());
    }

    @Test
    void release_shouldReturnLoadedRelease_whenGitLabHasIt() throws IOException {
        final GitLabRelease response = release("v1");
        when(gitLab.releases().get(ID, "v1")).thenReturn(Optional.of(response));

        final Optional<Release> found = sut.release("v1");

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void release_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.releases().get(ID, "gone")).thenReturn(Optional.empty());

        final Optional<Release> found = sut.release("gone");

        assertThat(found).isEmpty();
    }

    @Test
    void release_shouldPassTagNameWithSlashUnchanged_whenNameContainsSlash() throws IOException {
        when(gitLab.releases().get(ID, "v1.0/rc")).thenReturn(Optional.of(release("v1.0/rc")));

        final Optional<Release> found = sut.release("v1.0/rc");

        assertThat(found).isPresent();
        verify(gitLab.releases()).get(ID, "v1.0/rc");
    }

    @Test
    void release_shouldThrowIllegalArgumentException_whenTagNameIsBlank() {
        assertThatThrownBy(() -> sut.release(" ")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.releases());
    }

    @Test
    void release_shouldThrowNullPointerException_whenTagNameIsNull() {
        assertThatThrownBy(() -> sut.release(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.releases());
    }

    @Test
    void createRelease_shouldSendTagNameOnly_whenTagExists() throws IOException {
        final GitLabRelease created = release("v1");
        final GitLabProjectReleasesCreatePayload payload = GitLabProjectReleasesCreatePayloadBuilder.builder()
                .tagName("v1")
                .build();
        when(gitLab.releases().create(ID, payload)).thenReturn(created);

        final Release result = sut.createRelease("v1");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.project()).isSameAs(sut);
        });
    }

    @Test
    void createRelease_shouldSendTagNameAndRef_whenRefGiven() throws IOException {
        final GitLabRelease created = release("v1");
        final GitLabProjectReleasesCreatePayload payload = GitLabProjectReleasesCreatePayloadBuilder.builder()
                .tagName("v1")
                .ref("main")
                .build();
        when(gitLab.releases().create(ID, payload)).thenReturn(created);

        final Release result = sut.createRelease("v1", "main");

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void createRelease_shouldSendPayloadAsGiven_whenPayloadGiven() throws IOException {
        final GitLabRelease created = release("v1");
        final GitLabProjectReleasesCreatePayload payload = GitLabProjectReleasesCreatePayloadBuilder.builder()
                .tagName("v1")
                .name("One")
                .milestoneIds("3")
                .build();
        when(gitLab.releases().create(ID, payload)).thenReturn(created);

        final Release result = sut.createRelease(payload);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void createRelease_shouldThrowIllegalArgumentException_whenTagNameOrRefIsBlank() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.createRelease(" ")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.createRelease(" ", "main"))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.createRelease("v1", "")).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab.releases());
    }

    @Test
    void createRelease_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.createRelease((String) null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.createRelease(null, "main")).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.createRelease("v1", null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.createRelease((GitLabProjectReleasesCreatePayload) null))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.releases());
    }
}
