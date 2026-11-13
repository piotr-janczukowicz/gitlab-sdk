package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.created;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.key;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.keyWithPush;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.otherRecord;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.token;
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
import io.github.piotr_janczukowicz.gitlab.api.deploykeys.GitLabProjectDeployKeysCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabProjectDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectDeployTest {

    private static final String ID = "my-group/demo";
    private static final String PUBLIC_KEY = "ssh-ed25519 AAAAC3Nza demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private GitLabProjectDeployKeysCreatePayload keyPayload() throws IOException {
        final ArgumentCaptor<GitLabProjectDeployKeysCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeployKeysCreatePayload.class);
        verify(gitLab.deploykeys().project()).create(eq(ID), payload.capture());
        return payload.getValue();
    }

    private GitLabProjectDeployTokensCreatePayload tokenPayload() throws IOException {
        final ArgumentCaptor<GitLabProjectDeployTokensCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectDeployTokensCreatePayload.class);
        verify(gitLab.deploytokens().project()).create(eq(ID), payload.capture());
        return payload.getValue();
    }

    @Test
    void deployKeys_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.deploykeys().project().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return key(id, "k" + id);
                }));

        final Stream<DeployKey> keys = project.deployKeys();

        assertThat(consumed).hasValue(0);
        assertThat(keys.limit(1).toList())
                .singleElement()
                .satisfies(k -> assertThat(k.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.deploykeys().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void deployKey_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.empty());

        assertThat(project.deployKey(5L)).isEmpty();
    }

    @Test
    void deployKey_shouldReturnKey_whenPresent() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.of(key(5L, "ci")));

        assertThat(project.deployKey(5L))
                .get()
                .satisfies(k -> assertThat(k.title()).isEqualTo("ci"));
    }

    @Test
    void addDeployKey_shouldSendKeyTitleAndCanPush_whenCanPushGiven() throws IOException {
        when(gitLab.deploykeys().project().create(eq(ID), any(GitLabProjectDeployKeysCreatePayload.class)))
                .thenReturn(keyWithPush(5L, "ci", true));

        final DeployKey result = project.addDeployKey("ci", PUBLIC_KEY, true);

        final GitLabProjectDeployKeysCreatePayload payload = keyPayload();
        assertSoftly(softly -> {
            softly.assertThat(payload.key()).isEqualTo(PUBLIC_KEY);
            softly.assertThat(payload.title()).isEqualTo("ci");
            softly.assertThat(payload.canPush()).isTrue();
            softly.assertThat(payload.expiresAt()).isNull();
            softly.assertThat(result.id()).isEqualTo(5L);
            softly.assertThat(result.canPush()).isTrue();
        });
        verify(gitLab.deploykeys().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void addDeployKey_shouldLeaveCanPushUnset_whenCanPushIsNotGiven() throws IOException {
        when(gitLab.deploykeys().project().create(eq(ID), any(GitLabProjectDeployKeysCreatePayload.class)))
                .thenReturn(key(5L, "ci"));

        project.addDeployKey("ci", PUBLIC_KEY);

        final GitLabProjectDeployKeysCreatePayload payload = keyPayload();
        assertSoftly(softly -> {
            softly.assertThat(payload.key()).isEqualTo(PUBLIC_KEY);
            softly.assertThat(payload.title()).isEqualTo("ci");
            softly.assertThat(payload.canPush()).isNull();
        });
    }

    @Test
    void addDeployKey_shouldThrowIllegalArgumentException_whenTitleOrKeyIsBlank() {
        final Throwable blankTitle = catchThrowable(() -> project.addDeployKey(" ", PUBLIC_KEY));
        final Throwable blankKey = catchThrowable(() -> project.addDeployKey("ci", " "));

        assertSoftly(softly -> {
            softly.assertThat(blankTitle).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankKey).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void addDeployKey_shouldThrowNullPointerException_whenArgumentIsNull() {
        final Throwable nullTitle = catchThrowable(() -> project.addDeployKey(null, PUBLIC_KEY));
        final Throwable nullKey = catchThrowable(() -> project.addDeployKey("ci", null));

        assertSoftly(softly -> {
            softly.assertThat(nullTitle).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullKey).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void enableDeployKey_shouldReloadViaGet_whenCoreReturnsDifferentRecord() throws IOException {
        when(gitLab.deploykeys().project().enable(ID, 5L)).thenReturn(otherRecord(5L, "ci"));
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.of(keyWithPush(5L, "ci", true)));

        final DeployKey result = project.enableDeployKey(5L);

        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(5L);
            softly.assertThat(result.canPush()).isTrue();
        });
    }

    @Test
    void enableDeployKey_shouldThrowIllegalStateException_whenKeyIsGoneAfterEnable() throws IOException {
        when(gitLab.deploykeys().project().get(ID, 5L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> project.enableDeployKey(5L));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void deployTokens_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.deploytokens().project().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return token(id, "t" + id);
                }));

        final Stream<DeployToken> tokens = project.deployTokens();

        assertThat(consumed).hasValue(0);
        assertThat(tokens.limit(1).toList())
                .singleElement()
                .satisfies(t -> assertThat(t.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.deploytokens().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void deployToken_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.deploytokens().project().get(ID, 9L)).thenReturn(Optional.empty());

        assertThat(project.deployToken(9L)).isEmpty();
    }

    @Test
    void deployToken_shouldReturnToken_whenPresent() throws IOException {
        when(gitLab.deploytokens().project().get(ID, 9L)).thenReturn(Optional.of(token(9L, "ci")));

        assertThat(project.deployToken(9L))
                .get()
                .satisfies(t -> assertThat(t.name()).isEqualTo("ci"));
    }

    @Test
    void createDeployToken_shouldReturnSecret_whenScopesGiven() throws IOException {
        when(gitLab.deploytokens().project().create(eq(ID), any(GitLabProjectDeployTokensCreatePayload.class)))
                .thenReturn(created(9L, "ci"));

        final CreatedDeployToken result =
                project.createDeployToken("ci", DeployTokenScope.READ_REPOSITORY, DeployTokenScope.WRITE_REGISTRY);

        final GitLabProjectDeployTokensCreatePayload payload = tokenPayload();
        assertSoftly(softly -> {
            softly.assertThat(result.token()).isEqualTo(SECRET);
            softly.assertThat(result.deployToken().id()).isEqualTo(9L);
            softly.assertThat(payload.name()).isEqualTo("ci");
            softly.assertThat(payload.scopes())
                    .containsExactly(
                            GitLabProjectDeployTokensCreateScopes.READ_REPOSITORY,
                            GitLabProjectDeployTokensCreateScopes.WRITE_REGISTRY);
            softly.assertThat(payload.expiresAt()).isNull();
            softly.assertThat(payload.username()).isNull();
        });
    }

    @Test
    void createDeployToken_shouldThrowIllegalArgumentException_whenNoScope() {
        final Throwable none = catchThrowable(() -> project.createDeployToken("ci"));
        final Throwable nullArray = catchThrowable(() -> project.createDeployToken("ci", (DeployTokenScope[]) null));

        assertSoftly(softly -> {
            softly.assertThat(none).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullArray).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void createDeployToken_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.createDeployToken(" ", DeployTokenScope.READ_REPOSITORY));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void createDeployToken_shouldThrowNullPointerException_whenNameOrScopeIsNull() {
        final Throwable nullName =
                catchThrowable(() -> project.createDeployToken(null, DeployTokenScope.READ_REPOSITORY));
        final Throwable nullScope = catchThrowable(() -> project.createDeployToken("ci", (DeployTokenScope) null));

        assertSoftly(softly -> {
            softly.assertThat(nullName).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullScope).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
