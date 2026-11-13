package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.link;
import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.release;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLink;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class ReleaseLinkTest {

    private static final String ID = "my-group/my-project";
    private static final String TAG = "v1.0/rc";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Release release = new Release(GitLabSdk.of(gitLab).project(ID), release(TAG));
    private final GitLabReleaseLink response = link(5, "bin", "package");
    private final ReleaseLink sut = new ReleaseLink(release, response);

    @Test
    void accessors_shouldReturnResponseValues_whenLinkIsLoaded() {
        final GitLabReleaseLink full = GitLabReleaseLinkBuilder.builder()
                .id(5L)
                .name("bin")
                .url("https://dl.example/bin")
                .directAssetUrl("https://gl.example/direct/bin")
                .linkType("runbook")
                .build();

        final ReleaseLink result = new ReleaseLink(release, full);

        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(5L);
            softly.assertThat(result.name()).isEqualTo("bin");
            softly.assertThat(result.url()).isEqualTo("https://dl.example/bin");
            softly.assertThat(result.directAssetUrl()).contains("https://gl.example/direct/bin");
            softly.assertThat(result.type()).contains(ReleaseLinkType.RUNBOOK);
            softly.assertThat(result.typeName()).contains("runbook");
            softly.assertThat(result.release()).isSameAs(release);
            softly.assertThat(result.response()).isSameAs(full);
        });
    }

    @Test
    void directAssetUrl_shouldBeEmpty_whenAbsent() {
        assertThat(sut.directAssetUrl()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"other,OTHER", "runbook,RUNBOOK", "package,PACKAGE", "image,IMAGE"})
    void type_shouldMapValue_whenKnown(final String wire, final ReleaseLinkType expected) {
        final ReleaseLink result = new ReleaseLink(release, link(1, "a", wire));

        assertThat(result.type()).contains(expected);
    }

    @Test
    void type_shouldThrowIllegalArgumentException_whenUnknown() {
        final ReleaseLink result = new ReleaseLink(release, link(1, "a", "sbom"));

        final Throwable thrown = catchThrowable(result::type);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(thrown).hasMessageContaining("sbom");
            softly.assertThat(result.typeName()).contains("sbom");
        });
    }

    @Test
    void type_shouldBeEmpty_whenAbsent() {
        final ReleaseLink result = new ReleaseLink(
                release, GitLabReleaseLinkBuilder.builder().id(1L).build());

        assertSoftly(softly -> {
            softly.assertThat(result.type()).isEmpty();
            softly.assertThat(result.typeName()).isEmpty();
        });
    }

    @Test
    void refresh_shouldReturnReloadedLink_whenGitLabHasIt() throws IOException {
        final GitLabReleaseLink reloaded = link(5, "bin", "image");
        when(gitLab.releaselinks().get(ID, TAG, 5L)).thenReturn(Optional.of(reloaded));

        final ReleaseLink result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.releaselinks().get(ID, TAG, 5L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Release link 5 is gone");
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        when(gitLab.releaselinks().delete(ID, TAG, 5L)).thenReturn(Optional.of(response));

        sut.delete();

        verify(gitLab.releaselinks()).delete(ID, TAG, 5L);
    }
}
