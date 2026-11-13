package io.github.piotr_janczukowicz.gitlab.sdk.releases;

import static io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleasesFixtures.release;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabRelease;
import io.github.piotr_janczukowicz.gitlab.api.releases.GitLabProjectReleasesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ReleaseUpdateTest {

    private static final String ID = "42";
    private static final String TAG = "v1.0/rc";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Release release = new Release(GitLabSdk.of(gitLab).project(ID), release(TAG));

    private GitLabProjectReleasesUpdatePayload sent() throws IOException {
        final ArgumentCaptor<GitLabProjectReleasesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectReleasesUpdatePayload.class);
        verify(gitLab.releases()).update(eq(ID), eq(TAG), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate(final GitLabRelease answer) throws IOException {
        when(gitLab.releases().update(eq(ID), eq(TAG), any(GitLabProjectReleasesUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final ReleaseUpdate update = release.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.releases());
    }

    @Test
    void send_shouldSendOnlySetFields_whenNameAndDescriptionSet() throws IOException {
        stubUpdate(release(TAG));

        release.update().name("New").description("Notes").send();

        final GitLabProjectReleasesUpdatePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("New");
            softly.assertThat(payload.description()).isEqualTo("Notes");
            softly.assertThat(payload.releasedAt()).isNull();
            softly.assertThat(payload.milestones()).isNull();
            softly.assertThat(payload.milestoneIds()).isNull();
        });
    }

    @Test
    void send_shouldSendReleasedAtAndMilestones_whenSet() throws IOException {
        final Instant at = Instant.parse("2026-05-06T07:08:09Z");
        stubUpdate(release(TAG));

        release.update().releasedAt(at).milestones(List.of("m1", "m2")).send();

        final GitLabProjectReleasesUpdatePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.releasedAt()).isEqualTo(at);
            softly.assertThat(payload.milestones()).containsExactly("m1", "m2");
            softly.assertThat(payload.name()).isNull();
        });
    }

    @Test
    void send_shouldSendEmptyMilestones_whenEmptyListGiven() throws IOException {
        stubUpdate(release(TAG));

        release.update().milestones(List.of()).send();

        assertThat(sent().milestones()).isEmpty();
    }

    @Test
    void send_shouldReturnReleaseFromAnswer_whenSent() throws IOException {
        final GitLabRelease answer = release(TAG);
        stubUpdate(answer);

        final Release result = release.update().name("New").send();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(answer);
            softly.assertThat(result.project()).isSameAs(release.project());
        });
        verify(gitLab.releases(), org.mockito.Mockito.never()).get(any(), any());
    }

    @Test
    void setters_shouldThrowIllegalArgumentException_whenValueIsBlank() {
        final ReleaseUpdate update = release.update();

        assertSoftly(softly ->
                softly.assertThatThrownBy(() -> update.name(" ")).isInstanceOf(IllegalArgumentException.class));
        verifyNoInteractions(gitLab.releases());
    }

    @Test
    void setters_shouldThrowNullPointerException_whenValueIsNull() {
        final ReleaseUpdate update = release.update();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> update.name(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.description(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.releasedAt(null)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> update.milestones(null)).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.releases());
    }
}
