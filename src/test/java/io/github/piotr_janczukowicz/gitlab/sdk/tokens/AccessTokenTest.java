package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.EXPIRES;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.LAST_USED;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.created;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.fullToken;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.token;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.tokenWithScopes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabProjectAccessTokensRotatePayload;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class AccessTokenTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private AccessToken sut(final GitLabResourceAccessToken response) {
        return new AccessToken(GitLabSdk.of(gitLab).project(ID), response);
    }

    private GitLabProjectAccessTokensRotatePayload sentRotate() throws IOException {
        final ArgumentCaptor<GitLabProjectAccessTokensRotatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectAccessTokensRotatePayload.class);
        verify(gitLab.accesstokens().project()).rotate(eq(ID), eq("7"), payload.capture());
        return payload.getValue();
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final AccessToken sut = sut(fullToken(7L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(7L);
            softly.assertThat(sut.name()).isEqualTo("ci");
            softly.assertThat(sut.project().id()).isEqualTo(ID);
            softly.assertThat(sut.description()).contains("for the pipeline");
            softly.assertThat(sut.scopes()).containsExactly(AccessTokenScope.API, AccessTokenScope.READ_REPOSITORY);
            softly.assertThat(sut.scopeNames()).containsExactly("api", "read_repository");
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.isRevoked()).isFalse();
            softly.assertThat(sut.expiresAt()).contains(EXPIRES);
            softly.assertThat(sut.lastUsedAt()).contains(LAST_USED);
            softly.assertThat(sut.accessLevel()).contains(AccessLevel.MAINTAINER);
            softly.assertThat(sut.userId()).contains(99L);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenResponseLeavesFieldsOut() {
        final AccessToken sut = sut(token(7L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.scopes()).isEmpty();
            softly.assertThat(sut.scopeNames()).isEmpty();
            softly.assertThat(sut.isActive()).isFalse();
            softly.assertThat(sut.isRevoked()).isFalse();
            softly.assertThat(sut.expiresAt()).isEmpty();
            softly.assertThat(sut.lastUsedAt()).isEmpty();
            softly.assertThat(sut.accessLevel()).isEmpty();
            softly.assertThat(sut.userId()).isEmpty();
        });
    }

    @Test
    void scopes_shouldThrowIllegalArgumentException_whenScopeIsUnknownButScopeNamesWork() {
        final AccessToken sut = sut(tokenWithScopes(7L, List.of("api", "future_scope")));

        final Throwable thrown = catchThrowable(sut::scopes);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.scopeNames()).containsExactly("api", "future_scope");
        });
    }

    @Test
    void rotate_shouldSendNoExpiry_whenNoDateIsGiven() throws IOException {
        when(gitLab.accesstokens().project().rotate(eq(ID), eq("7"), any(GitLabProjectAccessTokensRotatePayload.class)))
                .thenReturn(created(8L, "ci"));

        final CreatedAccessToken result = sut(token(7L, "ci")).rotate();

        final java.time.LocalDate sentExpiry = sentRotate().expiresAt();
        assertSoftly(softly -> {
            softly.assertThat(sentExpiry).isNull();
            softly.assertThat(result.token()).isEqualTo(SECRET);
            softly.assertThat(result.accessToken().id()).isEqualTo(8L);
        });
    }

    @Test
    void rotate_shouldSendExpiry_whenDateIsGiven() throws IOException {
        when(gitLab.accesstokens().project().rotate(eq(ID), eq("7"), any(GitLabProjectAccessTokensRotatePayload.class)))
                .thenReturn(created(8L, "ci"));

        sut(token(7L, "ci")).rotate(LocalDate.of(2027, 1, 31));

        assertThat(sentRotate().expiresAt()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    @Test
    void rotate_shouldThrowNullPointerException_whenDateIsNull() {
        final AccessToken sut = sut(token(7L, "ci"));

        final Throwable thrown = catchThrowable(() -> sut.rotate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void revoke_shouldDeleteByStringId_whenCalled() throws IOException {
        sut(token(7L, "ci")).revoke();

        verify(gitLab.accesstokens().project()).delete(ID, "7");
    }

    @Test
    void refresh_shouldReturnLoadedToken_whenPresent() throws IOException {
        when(gitLab.accesstokens().project().get(ID, "7")).thenReturn(Optional.of(fullToken(7L, "fresh")));

        final AccessToken refreshed = sut(token(7L, "old")).refresh();

        assertThat(refreshed.name()).isEqualTo("fresh");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.accesstokens().project().get(ID, "7")).thenReturn(Optional.empty());
        final AccessToken sut = sut(token(7L, "ci"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabResourceAccessToken record = token(7L, "ci");

        assertThat(sut(record).response()).isSameAs(record);
    }
}
