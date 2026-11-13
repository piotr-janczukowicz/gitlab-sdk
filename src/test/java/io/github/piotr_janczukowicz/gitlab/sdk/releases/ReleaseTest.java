package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.link;
import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.release;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabRelease;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabReleaseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksCreateLinkType;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLink;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ReleaseTest {

    private static final String ID = "my-group/my-project";
    private static final String TAG = "v1.0/rc";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabRelease response = release(TAG);
    private final Release sut = new Release(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenReleaseIsLoaded() {
        final Instant released = Instant.parse("2026-01-02T03:04:05Z");
        final GitLabRelease full = GitLabReleaseBuilder.builder()
                .tagName(TAG)
                .name("One")
                .description("Notes")
                .releasedAt(released)
                .upcomingRelease(true)
                .build();

        final Release result = new Release(project, full);

        assertSoftly(softly -> {
            softly.assertThat(result.tagName()).isEqualTo(TAG);
            softly.assertThat(result.name()).contains("One");
            softly.assertThat(result.description()).contains("Notes");
            softly.assertThat(result.releasedAt()).contains(released);
            softly.assertThat(result.isUpcoming()).isTrue();
            softly.assertThat(result.project()).isSameAs(project);
            softly.assertThat(result.response()).isSameAs(full);
        });
    }

    @Test
    void description_shouldBeEmpty_whenAbsent() {
        final Release result =
                new Release(project, GitLabReleaseBuilder.builder().tagName(TAG).build());

        assertSoftly(softly -> {
            softly.assertThat(result.description()).isEmpty();
            softly.assertThat(result.name()).isEmpty();
            softly.assertThat(result.releasedAt()).isEmpty();
        });
    }

    @Test
    void isUpcoming_shouldBeFalse_whenFlagIsAbsent() {
        assertThat(sut.isUpcoming()).isFalse();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> new Release(null, response)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> new Release(project, null)).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void refresh_shouldReturnReloadedRelease_whenGitLabHasIt() throws IOException {
        final GitLabRelease reloaded = release(TAG);
        when(gitLab.releases().get(ID, TAG)).thenReturn(Optional.of(reloaded));

        final Release result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.releases().get(ID, TAG)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Release " + TAG + " is gone");
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        when(gitLab.releases().delete(ID, TAG)).thenReturn(Optional.of(response));

        sut.delete();

        verify(gitLab.releases()).delete(ID, TAG);
    }

    @Test
    void collectEvidence_shouldReturnNewRelease_whenCalled() throws IOException {
        final GitLabRelease withEvidence = release(TAG);
        when(gitLab.releases().evidence(ID, TAG)).thenReturn(withEvidence);

        final Release result = sut.collectEvidence();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(withEvidence);
            softly.assertThat(result.project()).isSameAs(project);
        });
    }

    @Test
    void links_shouldListLinks_whenGitLabReturnsThem() throws IOException {
        when(gitLab.releaselinks().list(ID, TAG).stream())
                .thenReturn(Stream.of(link(1, "a", "other"), link(2, "b", "package")));

        final List<ReleaseLink> result = sut.links().toList();

        assertThat(result).extracting(ReleaseLink::id).containsExactly(1L, 2L);
    }

    @Test
    void links_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.releaselinks().list(ID, TAG).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return link(id, "n", "other");
                }));

        final Stream<ReleaseLink> stream = sut.links();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void link_shouldReturnLoadedLink_whenGitLabHasIt() throws IOException {
        final GitLabReleaseLink loaded = link(5, "a", "other");
        when(gitLab.releaselinks().get(ID, TAG, 5L)).thenReturn(Optional.of(loaded));

        final Optional<ReleaseLink> found = sut.link(5);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(found.orElseThrow().release()).isSameAs(sut);
        });
    }

    @Test
    void link_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.releaselinks().get(ID, TAG, 5L)).thenReturn(Optional.empty());

        final Optional<ReleaseLink> found = sut.link(5);

        assertThat(found).isEmpty();
    }

    @Test
    void addLink_shouldSendNameAndUrl_whenNoType() throws IOException {
        final GitLabReleaseLink created = link(9, "bin", "other");
        final GitLabProjectReleaseLinksCreatePayload payload = GitLabProjectReleaseLinksCreatePayloadBuilder.builder()
                .name("bin")
                .url("https://dl.example/bin")
                .build();
        when(gitLab.releaselinks().create(ID, TAG, payload)).thenReturn(created);

        final ReleaseLink result = sut.addLink("bin", "https://dl.example/bin");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.release()).isSameAs(sut);
        });
    }

    @Test
    void addLink_shouldSendNameUrlAndType_whenPayloadGiven() throws IOException {
        final GitLabReleaseLink created = link(9, "bin", "package");
        final GitLabProjectReleaseLinksCreatePayload payload = GitLabProjectReleaseLinksCreatePayloadBuilder.builder()
                .name("bin")
                .url("https://dl.example/bin")
                .linkType(GitLabProjectReleaseLinksCreateLinkType.PACKAGE)
                .build();
        when(gitLab.releaselinks().create(ID, TAG, payload)).thenReturn(created);

        final ReleaseLink result = sut.addLink(payload);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void addLink_shouldThrowIllegalArgumentException_whenNameOrUrlIsBlank() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.addLink(" ", "https://x")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> sut.addLink("n", "")).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab.releaselinks());
    }

    @Test
    void addLink_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.addLink(null, "https://x")).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.addLink("n", null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.addLink((GitLabProjectReleaseLinksCreatePayload) null))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.releaselinks());
    }
}
