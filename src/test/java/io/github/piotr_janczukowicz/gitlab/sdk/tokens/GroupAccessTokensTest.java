package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.created;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.token;
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
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensCreateScopes;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensListFilter;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabGroupAccessTokensListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupAccessTokensTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    private GitLabGroupAccessTokensCreatePayload sentCreate() throws IOException {
        final ArgumentCaptor<GitLabGroupAccessTokensCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupAccessTokensCreatePayload.class);
        verify(gitLab.accesstokens().group()).create(eq(ID), payload.capture());
        return payload.getValue();
    }

    private void stubCreate() throws IOException {
        when(gitLab.accesstokens().group().create(eq(ID), any(GitLabGroupAccessTokensCreatePayload.class)))
                .thenReturn(created(8L, "ci"));
    }

    @Test
    void accessTokens_shouldWrapEveryToken_whenGitLabListsTwo() throws IOException {
        when(gitLab.accesstokens().group().list(ID)).thenReturn(List.of(token(1L, "a"), token(2L, "b")));

        final List<GroupAccessToken> tokens = group.accessTokens().toList();

        assertThat(tokens).extracting(GroupAccessToken::id).containsExactly(1L, 2L);
        assertThat(tokens).extracting(t -> t.group().id()).containsOnly(ID);
    }

    @Test
    void accessTokens_shouldPassFilterToCore_whenFilterIsGiven() throws IOException {
        final GitLabGroupAccessTokensListFilter filter =
                GitLabGroupAccessTokensListFilterBuilder.builder().search("ci").build();
        when(gitLab.accesstokens().group().list(ID, filter)).thenReturn(List.of(token(3L, "ci")));

        final List<GroupAccessToken> tokens = group.accessTokens(filter).toList();

        assertThat(tokens).extracting(GroupAccessToken::id).containsExactly(3L);
    }

    @Test
    void accessTokens_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> group.accessTokens(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void accessToken_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.accesstokens().group().get(ID, "5")).thenReturn(Optional.empty());

        assertThat(group.accessToken(5L)).isEmpty();
    }

    @Test
    void accessToken_shouldReturnToken_whenPresent() throws IOException {
        when(gitLab.accesstokens().group().get(ID, "5")).thenReturn(Optional.of(token(5L, "ci")));

        assertThat(group.accessToken(5L))
                .get()
                .satisfies(t -> assertThat(t.name()).isEqualTo("ci"));
    }

    @Test
    void createAccessToken_shouldSendNameScopesExpiryAndLevel_whenAllGiven() throws IOException {
        stubCreate();

        final CreatedGroupAccessToken result = group.createAccessToken(
                "ci",
                List.of(AccessTokenScope.API, AccessTokenScope.READ_REPOSITORY),
                LocalDate.of(2026, 12, 31),
                AccessLevel.DEVELOPER);

        final GitLabGroupAccessTokensCreatePayload payload = sentCreate();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("ci");
            softly.assertThat(payload.scopes())
                    .containsExactly(
                            GitLabGroupAccessTokensCreateScopes.API,
                            GitLabGroupAccessTokensCreateScopes.READ_REPOSITORY);
            softly.assertThat(payload.expiresAt()).isEqualTo(LocalDate.of(2026, 12, 31));
            softly.assertThat(payload.accessLevel()).isEqualTo(GitLabGroupAccessTokensCreateAccessLevel.DEVELOPER);
            softly.assertThat(result.token()).isEqualTo(SECRET);
            softly.assertThat(result.accessToken().id()).isEqualTo(8L);
        });
    }

    @Test
    void createAccessToken_shouldSendNoExpiry_whenExpiryIsNull() throws IOException {
        stubCreate();

        group.createAccessToken("ci", List.of(AccessTokenScope.API), null, AccessLevel.MAINTAINER);

        assertThat(sentCreate().expiresAt()).isNull();
    }

    @Test
    void createAccessToken_shouldThrowIllegalArgumentException_whenNameBlankOrNoScope() {
        final Throwable blank = catchThrowable(
                () -> group.createAccessToken(" ", List.of(AccessTokenScope.API), null, AccessLevel.DEVELOPER));
        final Throwable none =
                catchThrowable(() -> group.createAccessToken("ci", List.of(), null, AccessLevel.DEVELOPER));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(none).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void createAccessToken_shouldThrowNullPointerException_whenArgumentIsNull() {
        final Throwable nullScopes =
                catchThrowable(() -> group.createAccessToken("ci", null, null, AccessLevel.DEVELOPER));
        final Throwable nullScope = catchThrowable(() -> group.createAccessToken(
                "ci", java.util.Arrays.asList(AccessTokenScope.API, null), null, AccessLevel.DEVELOPER));
        final Throwable nullLevel =
                catchThrowable(() -> group.createAccessToken("ci", List.of(AccessTokenScope.API), null, null));

        assertSoftly(softly -> {
            softly.assertThat(nullScopes).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullScope).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullLevel).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void createAccessToken_shouldRefuseBeforeTheRequest_whenLevelCannotBeDecodedFromTheAnswer() {
        final Throwable planner = catchThrowable(
                () -> group.createAccessToken("ci", List.of(AccessTokenScope.API), null, AccessLevel.PLANNER));
        final Throwable minimal = catchThrowable(
                () -> group.createAccessToken("ci", List.of(AccessTokenScope.API), null, AccessLevel.MINIMAL_ACCESS));

        assertSoftly(softly -> {
            softly.assertThat(planner).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(minimal).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
