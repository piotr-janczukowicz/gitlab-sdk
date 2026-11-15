package io.github.piotr_janczukowicz.gitlab.sdk.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabEmail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabEmailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSSHKey;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSSHKeyBuilder;
import io.github.piotr_janczukowicz.gitlab.api.keys.GitLabGpgKey;
import io.github.piotr_janczukowicz.gitlab.api.keys.GitLabGpgKeyBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class AccountKeysTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    @Test
    void sshKeyDelete_shouldCallDeleteWithTheKeyId_whenCalled() throws IOException {
        final SshKey sut = new SshKey(sdk, GitLabSSHKeyBuilder.builder().id(3L).build());

        sut.delete();

        verify(gitLab.usersshandgpgkeys()).delete(3L);
    }

    @Test
    void sshKeyDelete_shouldNotFail_whenKeyIsAlreadyGone() throws IOException {
        when(gitLab.usersshandgpgkeys().delete(3L)).thenReturn(Optional.empty());
        final SshKey sut = new SshKey(sdk, GitLabSSHKeyBuilder.builder().id(3L).build());

        final Throwable thrown = catchThrowable(sut::delete);

        assertThat(thrown).isNull();
    }

    @Test
    void sshKeyAccessors_shouldReadTheResponse_whenCalled() {
        final Instant expires = Instant.parse("2027-01-01T00:00:00Z");
        final GitLabSSHKey response = GitLabSSHKeyBuilder.builder()
                .id(3L)
                .title("laptop")
                .key("ssh-ed25519 AAAA")
                .expiresAt(expires)
                .build();

        final SshKey sut = new SshKey(sdk, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(3L);
            softly.assertThat(sut.title()).isEqualTo("laptop");
            softly.assertThat(sut.key()).isEqualTo("ssh-ed25519 AAAA");
            softly.assertThat(sut.expiresAt()).contains(expires);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void sshKeyExpiresAt_shouldBeEmpty_whenKeyDoesNotExpire() {
        final SshKey sut = new SshKey(sdk, GitLabSSHKeyBuilder.builder().id(3L).build());

        assertThat(sut.expiresAt()).isEmpty();
    }

    @Test
    void sshKeyConstructor_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        final GitLabSSHKey response = GitLabSSHKeyBuilder.builder().id(3L).build();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new SshKey(null, response)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new SshKey(sdk, null))).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void emailDelete_shouldCallDeleteWithTheEmailId_whenCalled() throws IOException {
        final Email sut = new Email(sdk, GitLabEmailBuilder.builder().id(5L).build());

        sut.delete();

        verify(gitLab.useremailaddresses()).delete(5L);
    }

    @Test
    void emailAccessors_shouldReadTheResponse_whenCalled() {
        final Instant confirmed = Instant.parse("2026-01-01T00:00:00Z");
        final GitLabEmail response = GitLabEmailBuilder.builder()
                .id(5L)
                .email("a@example.com")
                .confirmedAt(confirmed)
                .build();

        final Email sut = new Email(sdk, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(5L);
            softly.assertThat(sut.address()).isEqualTo("a@example.com");
            softly.assertThat(sut.confirmedAt()).contains(confirmed);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void emailConfirmedAt_shouldBeEmpty_whenUnconfirmed() {
        final Email sut = new Email(sdk, GitLabEmailBuilder.builder().id(5L).build());

        assertThat(sut.confirmedAt()).isEmpty();
    }

    @Test
    void emailConstructor_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        final GitLabEmail response = GitLabEmailBuilder.builder().id(5L).build();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new Email(null, response))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new Email(sdk, null))).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void gpgKeyDelete_shouldCallDeleteWithTheKeyId_whenCalled() throws IOException {
        final GpgKey sut = new GpgKey(sdk, GitLabGpgKeyBuilder.builder().id(6L).build());

        sut.delete();

        verify(gitLab.keys()).deleteUserGpgKey(6L);
    }

    @Test
    void gpgKeyAccessors_shouldReadTheResponse_whenCalled() {
        final Instant created = Instant.parse("2026-01-01T00:00:00Z");
        final GitLabGpgKey response = GitLabGpgKeyBuilder.builder()
                .id(6L)
                .key("armored")
                .createdAt(created)
                .build();

        final GpgKey sut = new GpgKey(sdk, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(6L);
            softly.assertThat(sut.key()).isEqualTo("armored");
            softly.assertThat(sut.createdAt()).contains(created);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void gpgKeyCreatedAt_shouldBeEmpty_whenGitLabLeavesItOut() {
        final GpgKey sut = new GpgKey(sdk, GitLabGpgKeyBuilder.builder().id(6L).build());

        assertThat(sut.createdAt()).isEmpty();
    }

    @Test
    void gpgKeyConstructor_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        final GitLabGpgKey response = GitLabGpgKeyBuilder.builder().id(6L).build();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new GpgKey(null, response)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new GpgKey(sdk, null))).isInstanceOf(NullPointerException.class);
        });
    }
}
