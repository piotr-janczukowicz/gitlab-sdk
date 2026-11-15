package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.UPDATED;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.URL_WITH_CREDENTIALS;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.fullRemote;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.remote;
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
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabRemoteMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RemoteMirrorTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private RemoteMirror sut(final GitLabRemoteMirror response) {
        return new RemoteMirror(GitLabSdk.of(gitLab).project(ID), response);
    }

    private GitLabProjectRemoteMirrorsUpdatePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabProjectRemoteMirrorsUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectRemoteMirrorsUpdatePayload.class);
        verify(gitLab.remotemirrors()).update(eq(ID), eq("5"), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.remotemirrors().update(eq(ID), eq("5"), any(GitLabProjectRemoteMirrorsUpdatePayload.class)))
                .thenReturn(fullRemote(5L));
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final RemoteMirror sut = sut(fullRemote(5L));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(5L);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
            softly.assertThat(sut.url()).isEqualTo(URL_WITH_CREDENTIALS);
            softly.assertThat(sut.isEnabled()).isTrue();
            softly.assertThat(sut.updateStatus()).contains("finished");
            softly.assertThat(sut.lastUpdateAt()).contains(UPDATED);
            softly.assertThat(sut.lastUpdateStartedAt()).contains(UPDATED.minusSeconds(5));
            softly.assertThat(sut.lastSuccessfulUpdateAt()).contains(UPDATED);
            softly.assertThat(sut.lastError()).contains("none yet");
            softly.assertThat(sut.onlyProtectedBranches()).isTrue();
            softly.assertThat(sut.keepDivergentRefs()).isTrue();
            softly.assertThat(sut.authMethod()).contains("password");
            softly.assertThat(sut.hostKeyFingerprints()).containsExactly("SHA256:abc", "SHA256:def");
            softly.assertThat(sut.mirrorBranchRegex()).contains("^release/");
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenResponseLeavesFieldsOut() {
        final RemoteMirror sut = sut(remote(5L));

        assertSoftly(softly -> {
            softly.assertThat(sut.isEnabled()).isFalse();
            softly.assertThat(sut.updateStatus()).isEmpty();
            softly.assertThat(sut.lastUpdateAt()).isEmpty();
            softly.assertThat(sut.lastUpdateStartedAt()).isEmpty();
            softly.assertThat(sut.lastSuccessfulUpdateAt()).isEmpty();
            softly.assertThat(sut.lastError()).isEmpty();
            softly.assertThat(sut.onlyProtectedBranches()).isFalse();
            softly.assertThat(sut.keepDivergentRefs()).isFalse();
            softly.assertThat(sut.authMethod()).isEmpty();
            softly.assertThat(sut.hostKeyFingerprints()).isEmpty();
            softly.assertThat(sut.mirrorBranchRegex()).isEmpty();
        });
    }

    @Test
    void toString_shouldNotContainTheAddress_whenPrinted() {
        assertThat(sut(remote(5L)).toString())
                .doesNotContain("not-a-real-password")
                .doesNotContain("mirror-user");
    }

    @Test
    void send_shouldSendOnlySetFields_whenEnabledIsSet() throws IOException {
        stubUpdate();

        final RemoteMirror result = sut(remote(5L)).update().enabled(true).send();

        final GitLabProjectRemoteMirrorsUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.enabled()).isTrue();
            softly.assertThat(payload.authMethod()).isNull();
            softly.assertThat(payload.keepDivergentRefs()).isNull();
            softly.assertThat(payload.onlyProtectedBranches()).isNull();
            softly.assertThat(payload.mirrorBranchRegex()).isNull();
            softly.assertThat(payload.hostKeys()).isNull();
            softly.assertThat(result.id()).isEqualTo(5L);
        });
    }

    @Test
    void send_shouldSendEveryField_whenAllAreSet() throws IOException {
        stubUpdate();

        sut(remote(5L))
                .update()
                .enabled(false)
                .authMethod(MirrorAuthMethod.SSH_PUBLIC_KEY)
                .keepDivergentRefs(true)
                .onlyProtectedBranches(true)
                .mirrorBranchRegex("^main$")
                .hostKeys(List.of("host-key-one", "host-key-two"))
                .send();

        final GitLabProjectRemoteMirrorsUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.enabled()).isFalse();
            softly.assertThat(payload.authMethod()).isEqualTo("ssh_public_key");
            softly.assertThat(payload.keepDivergentRefs()).isTrue();
            softly.assertThat(payload.onlyProtectedBranches()).isTrue();
            softly.assertThat(payload.mirrorBranchRegex()).isEqualTo("^main$");
            softly.assertThat(payload.hostKeys()).containsExactly("host-key-one", "host-key-two");
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final RemoteMirrorUpdate update = sut(remote(5L)).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void setters_shouldThrowOnBadArguments_whenNullOrBlank() {
        final RemoteMirrorUpdate update = sut(remote(5L)).update();

        final Throwable nullMethod = catchThrowable(() -> update.authMethod(null));
        final Throwable blankRegex = catchThrowable(() -> update.mirrorBranchRegex(" "));
        final Throwable blankKey = catchThrowable(() -> update.hostKeys(List.of(" ")));
        final Throwable nullKeys = catchThrowable(() -> update.hostKeys(null));

        assertSoftly(softly -> {
            softly.assertThat(nullMethod).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankRegex).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankKey).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullKeys).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void sync_shouldCallCoreSync_whenCalled() throws IOException {
        sut(remote(5L)).sync();

        verify(gitLab.remotemirrors()).sync(ID, "5");
    }

    @Test
    void delete_shouldCallCoreDeleteByStringId_whenCalled() throws IOException {
        sut(remote(5L)).delete();

        verify(gitLab.remotemirrors()).delete(ID, "5");
    }

    @Test
    void refresh_shouldReturnLoadedMirror_whenPresent() throws IOException {
        when(gitLab.remotemirrors().get(ID, "5")).thenReturn(Optional.of(fullRemote(5L)));

        final RemoteMirror refreshed = sut(remote(5L)).refresh();

        assertThat(refreshed.isEnabled()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.remotemirrors().get(ID, "5")).thenReturn(Optional.empty());
        final RemoteMirror sut = sut(remote(5L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabRemoteMirror record = remote(5L);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
