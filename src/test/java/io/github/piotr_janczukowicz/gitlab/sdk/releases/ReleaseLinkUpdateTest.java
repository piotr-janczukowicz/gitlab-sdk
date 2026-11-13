package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.link;
import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.release;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksUpdateLinkType;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabProjectReleaseLinksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.releaselinks.GitLabReleaseLink;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

@UnitTest
class ReleaseLinkUpdateTest {

    private static final String ID = "42";
    private static final String TAG = "v1.0/rc";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Release release = new Release(GitLabSdk.of(gitLab).project(ID), release(TAG));
    private final ReleaseLink link = new ReleaseLink(release, link(5, "bin", "other"));

    private GitLabProjectReleaseLinksUpdatePayload sent() throws IOException {
        final ArgumentCaptor<GitLabProjectReleaseLinksUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectReleaseLinksUpdatePayload.class);
        verify(gitLab.releaselinks()).update(eq(ID), eq(TAG), eq(5L), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate(final GitLabReleaseLink answer) throws IOException {
        when(gitLab.releaselinks().update(eq(ID), eq(TAG), eq(5L), any(GitLabProjectReleaseLinksUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final ReleaseLinkUpdate update = link.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.releaselinks());
    }

    @Test
    void send_shouldSendOnlySetFields_whenNameAndUrlSet() throws IOException {
        stubUpdate(link(5, "new", "other"));

        link.update().name("new").url("https://dl.example/new").send();

        final GitLabProjectReleaseLinksUpdatePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("new");
            softly.assertThat(payload.url()).isEqualTo("https://dl.example/new");
            softly.assertThat(payload.linkType()).isNull();
            softly.assertThat(payload.filepath()).isNull();
            softly.assertThat(payload.directAssetPath()).isNull();
        });
    }

    @Test
    void send_shouldSendType_whenTypeSet() throws IOException {
        stubUpdate(link(5, "bin", "image"));

        link.update().type(ReleaseLinkType.IMAGE).send();

        final GitLabProjectReleaseLinksUpdatePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.linkType()).isEqualTo(GitLabProjectReleaseLinksUpdateLinkType.IMAGE);
            softly.assertThat(payload.name()).isNull();
        });
    }

    @ParameterizedTest
    @EnumSource(ReleaseLinkType.class)
    void type_shouldMapToCoreUpdateEnum_whenTypeIsAny(final ReleaseLinkType type) throws IOException {
        stubUpdate(link(5, "bin", type.value()));

        link.update().type(type).send();

        final GitLabProjectReleaseLinksUpdateLinkType sent = sent().linkType();
        assertSoftly(softly -> {
            softly.assertThat(sent).isEqualTo(GitLabProjectReleaseLinksUpdateLinkType.valueOf(type.name()));
            softly.assertThat(sent.wireValue()).isEqualTo(type.value());
        });
    }

    @Test
    void send_shouldReturnLinkFromAnswer_whenSent() throws IOException {
        final GitLabReleaseLink answer = link(5, "new", "other");
        stubUpdate(answer);

        final ReleaseLink result = link.update().name("new").send();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(answer);
            softly.assertThat(result.release()).isSameAs(release);
        });
        verify(gitLab.releaselinks(), never()).get(any(), any(), eq(5L));
    }

    @Test
    void setters_shouldThrowIllegalArgumentException_whenValueIsBlank() {
        final ReleaseLinkUpdate update = link.update();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> update.name(" ")).isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> update.url("")).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab.releaselinks());
    }

    @Test
    void setters_shouldThrowNullPointerException_whenValueIsNull() {
        final ReleaseLinkUpdate update = link.update();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> update.name(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.url(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.type(null)).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.releaselinks());
    }
}
