package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
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
import io.github.piotr_janczukowicz.gitlab.api.keys.GitLabKeysCreateUserGpgKeyPayload;
import io.github.piotr_janczukowicz.gitlab.api.useremailaddresses.GitLabUserEmailAddressesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.usersshandgpgkeys.GitLabUserSSHAndGPGKeysCreatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.account.Email;
import io.github.piotr_janczukowicz.gitlab.sdk.account.GpgKey;
import io.github.piotr_janczukowicz.gitlab.sdk.account.SshKey;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MeAccountTest {

    private static final String PUBLIC_KEY = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIsecretlooking smoke@host";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Me sut = GitLabSdk.of(gitLab).me();

    @Test
    void sshKeys_shouldWrapEachKey_whenListed() throws IOException {
        final GitLabSSHKey key =
                GitLabSSHKeyBuilder.builder().id(3L).title("laptop").build();
        when(gitLab.usersshandgpgkeys().list().stream()).thenReturn(Stream.of(key));

        final List<SshKey> keys = sut.sshKeys().toList();

        assertThat(keys).singleElement().satisfies(found -> {
            assertThat(found.id()).isEqualTo(3L);
            assertThat(found.response()).isSameAs(key);
        });
    }

    @Test
    void sshKeys_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.usersshandgpgkeys().list().stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return GitLabSSHKeyBuilder.builder().id(id).build();
                }));

        final Stream<SshKey> keys = sut.sshKeys();
        final int beforeConsuming = consumed.get();
        final List<SshKey> first = keys.limit(1).toList();

        assertSoftly(softly -> {
            softly.assertThat(beforeConsuming).isZero();
            softly.assertThat(first).hasSize(1);
            softly.assertThat(consumed).hasValue(1);
        });
    }

    @Test
    void sshKeys_shouldThrowIllegalStateException_whenStreamIsConsumedTwice() throws IOException {
        when(gitLab.usersshandgpgkeys().list().stream()).thenReturn(Stream.empty());
        final Stream<SshKey> keys = sut.sshKeys();
        keys.toList();

        final Throwable thrown = catchThrowable(keys::toList);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addSshKey_shouldSendTitleKeyAndExpiry_whenCalled() throws IOException {
        final Instant expiresAt = Instant.parse("2027-01-01T00:00:00Z");
        final GitLabSSHKey created = GitLabSSHKeyBuilder.builder()
                .id(4L)
                .title("smoke")
                .key(PUBLIC_KEY)
                .build();
        when(gitLab.usersshandgpgkeys().create(any())).thenReturn(created);

        final SshKey key = sut.addSshKey("smoke", PUBLIC_KEY, expiresAt);

        final ArgumentCaptor<GitLabUserSSHAndGPGKeysCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabUserSSHAndGPGKeysCreatePayload.class);
        verify(gitLab.usersshandgpgkeys()).create(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("smoke");
            softly.assertThat(payload.getValue().key()).isEqualTo(PUBLIC_KEY);
            softly.assertThat(payload.getValue().expiresAt()).isEqualTo(expiresAt);
            softly.assertThat(key.id()).isEqualTo(4L);
        });
    }

    @Test
    void addSshKey_shouldAcceptNoExpiry_whenExpiryIsNull() throws IOException {
        when(gitLab.usersshandgpgkeys().create(any()))
                .thenReturn(GitLabSSHKeyBuilder.builder().id(4L).build());

        sut.addSshKey("smoke", PUBLIC_KEY, null);

        final ArgumentCaptor<GitLabUserSSHAndGPGKeysCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabUserSSHAndGPGKeysCreatePayload.class);
        verify(gitLab.usersshandgpgkeys()).create(payload.capture());
        assertThat(payload.getValue().expiresAt()).isNull();
    }

    @Test
    void addSshKey_shouldHideTheKeyFromToString_whenKeyIsCreated() throws IOException {
        when(gitLab.usersshandgpgkeys().create(any()))
                .thenReturn(GitLabSSHKeyBuilder.builder()
                        .id(4L)
                        .title("smoke")
                        .key(PUBLIC_KEY)
                        .build());

        final SshKey key = sut.addSshKey("smoke", PUBLIC_KEY, null);

        assertSoftly(softly -> {
            softly.assertThat(key.toString()).doesNotContain("secretlooking").doesNotContain(PUBLIC_KEY);
            softly.assertThat(key.toString()).contains("4").contains("smoke");
        });
    }

    @Test
    void addSshKey_shouldThrowIllegalArgumentException_whenTitleOrKeyIsBlank() {
        final Throwable blankTitle = catchThrowable(() -> sut.addSshKey(" ", PUBLIC_KEY, null));
        final Throwable blankKey = catchThrowable(() -> sut.addSshKey("smoke", "", null));

        assertSoftly(softly -> {
            softly.assertThat(blankTitle).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankKey).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void addSshKey_shouldThrowNullPointerException_whenTitleOrKeyIsNull() {
        final Throwable nullTitle = catchThrowable(() -> sut.addSshKey(null, PUBLIC_KEY, null));
        final Throwable nullKey = catchThrowable(() -> sut.addSshKey("smoke", null, null));

        assertSoftly(softly -> {
            softly.assertThat(nullTitle).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullKey).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void emails_shouldWrapEachAddress_whenListed() throws IOException {
        final GitLabEmail email =
                GitLabEmailBuilder.builder().id(5L).email("a@example.com").build();
        when(gitLab.useremailaddresses().list().stream()).thenReturn(Stream.of(email));

        final List<Email> emails = sut.emails().toList();

        assertThat(emails)
                .singleElement()
                .satisfies(found -> assertThat(found.address()).isEqualTo("a@example.com"));
    }

    @Test
    void addEmail_shouldSendAddress_whenCalled() throws IOException {
        when(gitLab.useremailaddresses().create(any()))
                .thenReturn(GitLabEmailBuilder.builder()
                        .id(5L)
                        .email("a@example.com")
                        .build());

        final Email created = sut.addEmail("a@example.com");

        final ArgumentCaptor<GitLabUserEmailAddressesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabUserEmailAddressesCreatePayload.class);
        verify(gitLab.useremailaddresses()).create(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().email()).isEqualTo("a@example.com");
            softly.assertThat(created.id()).isEqualTo(5L);
        });
    }

    @Test
    void addEmail_shouldThrowIllegalArgumentException_whenEmailIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addEmail(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gpgKeys_shouldWrapEachKey_whenListed() throws IOException {
        final GitLabGpgKey key = GitLabGpgKeyBuilder.builder().id(6L).build();
        when(gitLab.keys().listUserGpgKeys().stream()).thenReturn(Stream.of(key));

        final List<GpgKey> keys = sut.gpgKeys().toList();

        assertThat(keys)
                .singleElement()
                .satisfies(found -> assertThat(found.id()).isEqualTo(6L));
    }

    @Test
    void addGpgKey_shouldSendKeyAndHideItFromToString_whenCalled() throws IOException {
        final String armored = "-----BEGIN PGP PUBLIC KEY BLOCK-----secretlooking";
        when(gitLab.keys().createUserGpgKey(any()))
                .thenReturn(GitLabGpgKeyBuilder.builder().id(6L).key(armored).build());

        final GpgKey created = sut.addGpgKey(armored);

        final ArgumentCaptor<GitLabKeysCreateUserGpgKeyPayload> payload =
                ArgumentCaptor.forClass(GitLabKeysCreateUserGpgKeyPayload.class);
        verify(gitLab.keys()).createUserGpgKey(payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().key()).isEqualTo(armored);
            softly.assertThat(created.toString())
                    .doesNotContain("secretlooking")
                    .contains("6");
        });
    }

    @Test
    void addGpgKey_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addGpgKey(""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
