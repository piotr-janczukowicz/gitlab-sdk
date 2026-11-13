package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.created;
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
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabGroupDeployTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupDeployTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    @Test
    void deployTokens_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.deploytokens().group().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return token(id, "t" + id);
                }));

        final Stream<GroupDeployToken> tokens = group.deployTokens();

        assertThat(consumed).hasValue(0);
        assertThat(tokens.limit(1).toList())
                .singleElement()
                .satisfies(t -> assertThat(t.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.deploytokens().group(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void deployToken_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.deploytokens().group().get(ID, 9L)).thenReturn(Optional.empty());

        assertThat(group.deployToken(9L)).isEmpty();
    }

    @Test
    void deployToken_shouldReturnToken_whenPresent() throws IOException {
        when(gitLab.deploytokens().group().get(ID, 9L)).thenReturn(Optional.of(token(9L, "ci")));

        assertThat(group.deployToken(9L))
                .get()
                .satisfies(t -> assertThat(t.name()).isEqualTo("ci"));
    }

    @Test
    void createDeployToken_shouldReturnSecret_whenScopesGiven() throws IOException {
        when(gitLab.deploytokens().group().create(eq(ID), any(GitLabGroupDeployTokensCreatePayload.class)))
                .thenReturn(created(9L, "ci"));

        final GroupCreatedDeployToken result =
                group.createDeployToken("ci", DeployTokenScope.READ_REPOSITORY, DeployTokenScope.WRITE_REGISTRY);

        final ArgumentCaptor<GitLabGroupDeployTokensCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupDeployTokensCreatePayload.class);
        verify(gitLab.deploytokens().group()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(result.token()).isEqualTo(SECRET);
            softly.assertThat(result.deployToken().id()).isEqualTo(9L);
            softly.assertThat(payload.getValue().name()).isEqualTo("ci");
            softly.assertThat(payload.getValue().scopes())
                    .containsExactly(
                            GitLabGroupDeployTokensCreateScopes.READ_REPOSITORY,
                            GitLabGroupDeployTokensCreateScopes.WRITE_REGISTRY);
        });
    }

    @Test
    void createDeployToken_shouldThrowIllegalArgumentException_whenNoScope() {
        final Throwable none = catchThrowable(() -> group.createDeployToken("ci"));
        final Throwable nullArray = catchThrowable(() -> group.createDeployToken("ci", (DeployTokenScope[]) null));

        assertSoftly(softly -> {
            softly.assertThat(none).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullArray).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void createDeployToken_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> group.createDeployToken(" ", DeployTokenScope.READ_REPOSITORY));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void createDeployToken_shouldThrowNullPointerException_whenNameOrScopeIsNull() {
        final Throwable nullName =
                catchThrowable(() -> group.createDeployToken(null, DeployTokenScope.READ_REPOSITORY));
        final Throwable nullScope = catchThrowable(() -> group.createDeployToken("ci", (DeployTokenScope) null));

        assertSoftly(softly -> {
            softly.assertThat(nullName).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullScope).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
