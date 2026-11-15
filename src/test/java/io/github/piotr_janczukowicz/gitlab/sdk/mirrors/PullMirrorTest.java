package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.UPDATED;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.URL_WITH_CREDENTIALS;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.barePull;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.fullPull;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.pull;
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
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabProjectPullMirroringUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabPullMirror;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PullMirrorTest {

    private static final String ID = "my-group/demo";
    private static final String PASSWORD = "not-a-real-password-for-the-mirror";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private PullMirror sut(final GitLabPullMirror response) {
        return new PullMirror(GitLabSdk.of(gitLab).project(ID), response);
    }

    private GitLabProjectPullMirroringUpdatePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabProjectPullMirroringUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPullMirroringUpdatePayload.class);
        verify(gitLab.pullmirroring()).update(eq(ID), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.pullmirroring().update(eq(ID), any(GitLabProjectPullMirroringUpdatePayload.class)))
                .thenReturn(fullPull(9L));
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final PullMirror sut = sut(fullPull(9L));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(9L);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
            softly.assertThat(sut.url()).contains(URL_WITH_CREDENTIALS);
            softly.assertThat(sut.isEnabled()).isTrue();
            softly.assertThat(sut.updateStatus()).contains("finished");
            softly.assertThat(sut.lastError()).contains("none yet");
            softly.assertThat(sut.lastUpdateAt()).contains(UPDATED);
            softly.assertThat(sut.lastUpdateStartedAt()).contains(UPDATED.minusSeconds(5));
            softly.assertThat(sut.lastSuccessfulUpdateAt()).contains(UPDATED);
            softly.assertThat(sut.mirrorTriggerBuilds()).isTrue();
            softly.assertThat(sut.onlyMirrorProtectedBranches()).isTrue();
            softly.assertThat(sut.mirrorOverwritesDivergedBranches()).isTrue();
            softly.assertThat(sut.mirrorBranchRegex()).contains("^release/");
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenResponseLeavesFieldsOut() {
        final PullMirror sut = sut(barePull(9L));

        assertSoftly(softly -> {
            softly.assertThat(sut.url()).isEmpty();
            softly.assertThat(sut.isEnabled()).isFalse();
            softly.assertThat(sut.updateStatus()).isEmpty();
            softly.assertThat(sut.lastError()).isEmpty();
            softly.assertThat(sut.lastUpdateAt()).isEmpty();
            softly.assertThat(sut.mirrorTriggerBuilds()).isFalse();
            softly.assertThat(sut.onlyMirrorProtectedBranches()).isFalse();
            softly.assertThat(sut.mirrorOverwritesDivergedBranches()).isFalse();
            softly.assertThat(sut.mirrorBranchRegex()).isEmpty();
        });
    }

    @Test
    void toString_shouldNotContainTheAddress_whenPrinted() {
        assertThat(sut(pull(9L)).toString())
                .doesNotContain("not-a-real-password")
                .doesNotContain("mirror-user");
    }

    @Test
    void send_shouldSendOnlySetFields_whenEnabledIsSet() throws IOException {
        stubUpdate();

        final PullMirror result = sut(pull(9L)).update().enabled(true).send();

        final GitLabProjectPullMirroringUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.enabled()).isTrue();
            softly.assertThat(payload.url()).isNull();
            softly.assertThat(payload.authUser()).isNull();
            softly.assertThat(payload.authPassword()).isNull();
            softly.assertThat(payload.mirrorTriggerBuilds()).isNull();
            softly.assertThat(payload.onlyMirrorProtectedBranches()).isNull();
            softly.assertThat(payload.mirrorOverwritesDivergedBranches()).isNull();
            softly.assertThat(payload.mirrorBranchRegex()).isNull();
            softly.assertThat(result.id()).isEqualTo(9L);
        });
    }

    @Test
    void send_shouldSendEveryField_whenAllAreSet() throws IOException {
        stubUpdate();

        sut(pull(9L))
                .update()
                .enabled(false)
                .url("https://other.example.test/repo.git")
                .authUser("mirror-user")
                .authPassword(PASSWORD)
                .mirrorTriggerBuilds(true)
                .onlyMirrorProtectedBranches(true)
                .mirrorOverwritesDivergedBranches(true)
                .mirrorBranchRegex("^main$")
                .send();

        final GitLabProjectPullMirroringUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.enabled()).isFalse();
            softly.assertThat(payload.url()).isEqualTo("https://other.example.test/repo.git");
            softly.assertThat(payload.authUser()).isEqualTo("mirror-user");
            softly.assertThat(payload.authPassword()).isEqualTo(PASSWORD);
            softly.assertThat(payload.mirrorTriggerBuilds()).isTrue();
            softly.assertThat(payload.onlyMirrorProtectedBranches()).isTrue();
            softly.assertThat(payload.mirrorOverwritesDivergedBranches()).isTrue();
            softly.assertThat(payload.mirrorBranchRegex()).isEqualTo("^main$");
        });
    }

    @Test
    void update_shouldNotPrintThePassword_whenToStringIsCalled() {
        final PullMirrorUpdate update = sut(pull(9L)).update().authPassword(PASSWORD);

        assertThat(update.toString()).doesNotContain(PASSWORD);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final PullMirrorUpdate update = sut(pull(9L)).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void setters_shouldThrowOnBadArguments_whenNullOrBlank() {
        final PullMirrorUpdate update = sut(pull(9L)).update();

        final Throwable blankUrl = catchThrowable(() -> update.url(" "));
        final Throwable nullUser = catchThrowable(() -> update.authUser(null));
        final Throwable nullPassword = catchThrowable(() -> update.authPassword(null));
        final Throwable blankRegex = catchThrowable(() -> update.mirrorBranchRegex(" "));

        assertSoftly(softly -> {
            softly.assertThat(blankUrl).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullUser).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullPassword).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankRegex).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void refresh_shouldReturnLoadedMirror_whenPresent() throws IOException {
        when(gitLab.pullmirroring().get(ID)).thenReturn(fullPull(9L));

        final PullMirror refreshed = sut(pull(9L)).refresh();

        assertThat(refreshed.isEnabled()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.pullmirroring().get(ID)).thenThrow(mock(GitLabNotFoundException.class));
        final PullMirror sut = sut(pull(9L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabPullMirror record = pull(9L);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
