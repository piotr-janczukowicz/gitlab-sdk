package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.key;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.keyWithPush;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.otherRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabDeployKeysProject;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabDeployKeysProjectBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@UnitTest
class DeployKeyTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private DeployKey sut(final GitLabDeployKeysProject response) {
        return new DeployKey(GitLabSdk.of(gitLab).project(ID), response);
    }

    @Test
    void accessors_shouldReadResponse_whenPushAndExpiryAreSet() {
        final Instant expires = Instant.parse("2027-01-01T00:00:00Z");
        final DeployKey sut = sut(GitLabDeployKeysProjectBuilder.builder()
                .id(5L)
                .title("ci")
                .canPush(true)
                .expiresAt(expires)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(5L);
            softly.assertThat(sut.title()).isEqualTo("ci");
            softly.assertThat(sut.canPush()).isTrue();
            softly.assertThat(sut.expiresAt()).contains(expires);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
        });
    }

    @Test
    void accessors_shouldUseDefaults_whenOptionalFieldsAreMissing() {
        final DeployKey sut = sut(key(5L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.canPush()).isFalse();
            softly.assertThat(sut.expiresAt()).isEmpty();
        });
    }

    @Test
    void update_shouldReloadViaGet_whenCoreReturnsDifferentRecord() throws IOException {
        when(gitLab.deploykeys().project().update(eq(ID), eq(5L), any(GitLabProjectDeployKeysUpdatePayload.class)))
                .thenReturn(otherRecord(5L, "new"));
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.of(keyWithPush(5L, "new", true)));

        final DeployKey result =
                sut(key(5L, "ci")).update().title("new").canPush(true).send();

        final ArgumentCaptor<GitLabProjectDeployKeysUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeployKeysUpdatePayload.class);
        final InOrder order = inOrder(gitLab.deploykeys().project());
        order.verify(gitLab.deploykeys().project()).update(eq(ID), eq(5L), payload.capture());
        order.verify(gitLab.deploykeys().project()).get(ID, 5L);
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("new");
            softly.assertThat(payload.getValue().canPush()).isTrue();
            softly.assertThat(result.title()).isEqualTo("new");
            softly.assertThat(result.canPush()).isTrue();
        });
    }

    @Test
    void update_shouldSendOnlySetFields_whenOnlyTitleIsSet() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.of(key(5L, "new")));

        sut(key(5L, "ci")).update().title("new").send();

        final ArgumentCaptor<GitLabProjectDeployKeysUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeployKeysUpdatePayload.class);
        verify(gitLab.deploykeys().project()).update(eq(ID), eq(5L), payload.capture());
        assertThat(payload.getValue().canPush()).isNull();
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() {
        final DeployKeyUpdate update = sut(key(5L, "ci")).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void update_shouldThrowIllegalStateException_whenKeyIsGoneAfterUpdate() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.empty());
        final DeployKeyUpdate update = sut(key(5L, "ci")).update().canPush(false);

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void title_shouldThrowIllegalArgumentException_whenBlank() {
        final DeployKeyUpdate update = sut(key(5L, "ci")).update();

        final Throwable thrown = catchThrowable(() -> update.title(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void title_shouldThrowNullPointerException_whenNull() {
        final DeployKeyUpdate update = sut(key(5L, "ci")).update();

        final Throwable thrown = catchThrowable(() -> update.title(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void delete_shouldCallCoreDelete_whenCalled() throws IOException {
        sut(key(5L, "ci")).delete();

        verify(gitLab.deploykeys().project()).delete(ID, 5L);
    }

    @Test
    void refresh_shouldReturnLoadedKey_whenPresent() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.of(key(5L, "renamed")));

        final DeployKey refreshed = sut(key(5L, "ci")).refresh();

        assertThat(refreshed.title()).isEqualTo("renamed");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.empty());
        final DeployKey sut = sut(key(5L, "ci"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabDeployKeysProject record = key(5L, "ci");

        assertThat(sut(record).response()).isSameAs(record);
    }
}
