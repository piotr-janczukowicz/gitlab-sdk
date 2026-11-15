package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.EXPIRES;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.created;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CreatedAccessTokenTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private CreatedAccessToken sut(final GitLabResourceAccessTokenWithToken response) {
        return new CreatedAccessToken(GitLabSdk.of(gitLab).project(ID), response);
    }

    @Test
    void token_shouldReturnSecret_whenCreated() {
        assertThat(sut(created(8L, "ci")).token()).isEqualTo(SECRET);
    }

    @Test
    void accessToken_shouldCarryTheFieldsOfTheAnswer_whenCreated() {
        final AccessToken token = sut(created(8L, "ci")).accessToken();

        assertSoftly(softly -> {
            softly.assertThat(token.id()).isEqualTo(8L);
            softly.assertThat(token.name()).isEqualTo("ci");
            softly.assertThat(token.project().id()).isEqualTo(ID);
            softly.assertThat(token.scopes()).containsExactly(AccessTokenScope.API);
            softly.assertThat(token.isActive()).isTrue();
            softly.assertThat(token.expiresAt()).contains(EXPIRES);
            softly.assertThat(token.accessLevel()).contains(AccessLevel.DEVELOPER);
        });
    }

    @Test
    void toString_shouldNotContainSecret_whenPrinted() {
        final CreatedAccessToken sut = sut(created(8L, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.toString())
                    .doesNotContain(SECRET)
                    .contains("8")
                    .contains("ci");
            softly.assertThat(sut.accessToken().toString()).doesNotContain(SECRET);
            softly.assertThat(sut.accessToken().response().toString()).doesNotContain(SECRET);
        });
    }

    @Test
    void toString_shouldNotContainSecret_whenTheCoreRecordIsPrinted() {
        final GitLabResourceAccessTokenWithToken record = created(8L, "ci");

        assertSoftly(softly -> {
            softly.assertThat(record.toString()).doesNotContain(SECRET);
            softly.assertThat(sut(record).response().toString()).doesNotContain(SECRET);
        });
    }

    @Test
    void response_shouldReturnTheAnswerWithTheSecret_whenCalled() {
        final GitLabResourceAccessTokenWithToken record = created(8L, "ci");

        assertThat(sut(record).response()).isSameAs(record);
    }
}
