package io.github.piotr_janczukowicz.gitlab.sdk.deploy;

import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.created;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.token;
import static io.github.piotr_janczukowicz.gitlab.sdk.deploy.DeployFixtures.tokenWithScopes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployToken;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deploytokens.GitLabDeployTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class DeployTokenTest {

    private static final String PROJECT_ID = "my-group/demo";
    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    private DeployToken sut(final GitLabDeployToken response) {
        return new DeployToken(sdk.project(PROJECT_ID), response);
    }

    private GroupDeployToken groupSut(final GitLabDeployToken response) {
        return new GroupDeployToken(sdk.group(GROUP_ID), response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsAreSet() {
        final Instant expires = Instant.parse("2027-01-01T00:00:00Z");
        final DeployToken sut = sut(GitLabDeployTokenBuilder.builder()
                .id(9L)
                .name("ci")
                .username("deployer")
                .expiresAt(expires)
                .scopes(List.of("read_repository", "write_registry"))
                .revoked(true)
                .expired(true)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(9L);
            softly.assertThat(sut.name()).isEqualTo("ci");
            softly.assertThat(sut.username()).contains("deployer");
            softly.assertThat(sut.expiresAt()).contains(expires);
            softly.assertThat(sut.scopes())
                    .containsExactly(DeployTokenScope.READ_REPOSITORY, DeployTokenScope.WRITE_REGISTRY);
            softly.assertThat(sut.scopeNames()).containsExactly("read_repository", "write_registry");
            softly.assertThat(sut.isRevoked()).isTrue();
            softly.assertThat(sut.isExpired()).isTrue();
            softly.assertThat(sut.project().id()).isEqualTo(PROJECT_ID);
        });
    }

    @Test
    void accessors_shouldUseDefaults_whenOptionalFieldsAreMissing() {
        final DeployToken sut = sut(token(9L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.username()).isEmpty();
            softly.assertThat(sut.expiresAt()).isEmpty();
            softly.assertThat(sut.scopes()).isEmpty();
            softly.assertThat(sut.scopeNames()).isEmpty();
            softly.assertThat(sut.isRevoked()).isFalse();
            softly.assertThat(sut.isExpired()).isFalse();
        });
    }

    @Test
    void scopes_shouldThrowIllegalArgumentException_whenUnknownScope() {
        final DeployToken sut = sut(tokenWithScopes(9L, "ci", List.of("read_repository", "launch_rockets")));

        final Throwable thrown = catchThrowable(sut::scopes);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("launch_rockets");
        assertThat(sut.scopeNames()).containsExactly("read_repository", "launch_rockets");
    }

    @Test
    void delete_shouldCallCoreDelete_whenCalled() throws IOException {
        sut(token(9L, "ci")).delete();

        verify(gitLab.deploytokens().project()).delete(PROJECT_ID, 9L);
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabDeployToken record = token(9L, "ci");

        assertThat(sut(record).response()).isSameAs(record);
    }

    @Test
    void refresh_shouldReturnLoadedToken_whenPresent() throws IOException {
        when(gitLab.deploytokens().project().get(PROJECT_ID, 9L)).thenReturn(Optional.of(token(9L, "renamed")));

        final DeployToken refreshed = sut(token(9L, "ci")).refresh();

        assertThat(refreshed.name()).isEqualTo("renamed");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.deploytokens().project().get(PROJECT_ID, 9L)).thenReturn(Optional.empty());
        final DeployToken sut = sut(token(9L, "ci"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("9");
    }

    @Test
    void groupAccessors_shouldReadResponse_whenAllFieldsAreSet() {
        final GroupDeployToken sut = groupSut(GitLabDeployTokenBuilder.builder()
                .id(9L)
                .name("ci")
                .username("deployer")
                .scopes(List.of("read_package_registry"))
                .revoked(false)
                .expired(false)
                .build());

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(9L);
            softly.assertThat(sut.name()).isEqualTo("ci");
            softly.assertThat(sut.username()).contains("deployer");
            softly.assertThat(sut.expiresAt()).isEmpty();
            softly.assertThat(sut.scopes()).containsExactly(DeployTokenScope.READ_PACKAGE_REGISTRY);
            softly.assertThat(sut.scopeNames()).containsExactly("read_package_registry");
            softly.assertThat(sut.isRevoked()).isFalse();
            softly.assertThat(sut.isExpired()).isFalse();
            softly.assertThat(sut.group().id()).isEqualTo(GROUP_ID);
        });
    }

    @Test
    void groupScopes_shouldThrowIllegalArgumentException_whenUnknownScope() {
        final GroupDeployToken sut = groupSut(tokenWithScopes(9L, "ci", List.of("launch_rockets")));

        final Throwable thrown = catchThrowable(sut::scopes);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        assertThat(sut.scopeNames()).containsExactly("launch_rockets");
    }

    @Test
    void groupDelete_shouldCallCoreDelete_whenCalled() throws IOException {
        groupSut(token(9L, "ci")).delete();

        verify(gitLab.deploytokens().group()).delete(GROUP_ID, 9L);
    }

    @Test
    void groupResponse_shouldReturnRecord_whenCalled() {
        final GitLabDeployToken record = token(9L, "ci");

        assertThat(groupSut(record).response()).isSameAs(record);
    }

    @Test
    void groupRefresh_shouldReturnLoadedToken_whenPresent() throws IOException {
        when(gitLab.deploytokens().group().get(GROUP_ID, 9L)).thenReturn(Optional.of(token(9L, "renamed")));

        final GroupDeployToken refreshed = groupSut(token(9L, "ci")).refresh();

        assertThat(refreshed.name()).isEqualTo("renamed");
    }

    @Test
    void groupRefresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.deploytokens().group().get(GROUP_ID, 9L)).thenReturn(Optional.empty());
        final GroupDeployToken sut = groupSut(token(9L, "ci"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("9");
    }

    @Test
    void created_shouldExposeSecretOnlyThroughToken_whenProjectToken() {
        final CreatedDeployToken sut = new CreatedDeployToken(sdk.project(PROJECT_ID), created(9L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.token()).isEqualTo(SECRET);
            softly.assertThat(sut.deployToken().id()).isEqualTo(9L);
            softly.assertThat(sut.deployToken().name()).isEqualTo("ci");
            softly.assertThat(sut.deployToken().scopes()).containsExactly(DeployTokenScope.READ_REPOSITORY);
            softly.assertThat(sut.deployToken().project().id()).isEqualTo(PROJECT_ID);
            softly.assertThat(sut.deployToken().response().toString()).doesNotContain(SECRET);
        });
    }

    @Test
    void createdResponse_shouldReturnRecordWithRedactedToString_whenProjectToken() {
        final GitLabDeployTokenWithToken record = created(9L, "ci");

        final CreatedDeployToken sut = new CreatedDeployToken(sdk.project(PROJECT_ID), record);

        assertSoftly(softly -> {
            softly.assertThat(sut.response()).isSameAs(record);
            softly.assertThat(sut.response().toString()).doesNotContain(SECRET);
        });
    }

    @Test
    void groupCreatedResponse_shouldReturnRecordWithRedactedToString_whenGroupToken() {
        final GitLabDeployTokenWithToken record = created(9L, "ci");

        final GroupCreatedDeployToken sut = new GroupCreatedDeployToken(sdk.group(GROUP_ID), record);

        assertSoftly(softly -> {
            softly.assertThat(sut.response()).isSameAs(record);
            softly.assertThat(sut.response().toString()).doesNotContain(SECRET);
        });
    }

    @Test
    void createdToString_shouldNotContainSecret_whenProjectToken() {
        final CreatedDeployToken sut = new CreatedDeployToken(sdk.project(PROJECT_ID), created(9L, "ci"));

        assertThat(sut.toString()).doesNotContain(SECRET).contains("9").contains("ci");
    }

    @Test
    void groupCreated_shouldExposeSecretOnlyThroughToken_whenGroupToken() {
        final GroupCreatedDeployToken sut = new GroupCreatedDeployToken(sdk.group(GROUP_ID), created(9L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.token()).isEqualTo(SECRET);
            softly.assertThat(sut.deployToken().id()).isEqualTo(9L);
            softly.assertThat(sut.deployToken().group().id()).isEqualTo(GROUP_ID);
        });
    }

    @Test
    void groupCreatedToString_shouldNotContainSecret_whenGroupToken() {
        final GroupCreatedDeployToken sut = new GroupCreatedDeployToken(sdk.group(GROUP_ID), created(9L, "ci"));

        assertThat(sut.toString()).doesNotContain(SECRET).contains("9").contains("ci");
    }
}
