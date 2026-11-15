package io.github.piotr_janczukowicz.gitlab.sdk.tokens;

import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.tokens.TokensFixtures.created;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessToken;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenResourceType;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithToken;
import io.github.piotr_janczukowicz.gitlab.api.accesstokens.GitLabResourceAccessTokenWithTokenBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

@UnitTest
class TokenValuesTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<TokenValues> constructor = TokenValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withoutSecret_shouldCopyEveryFieldAndKeepTheEnumsByName_whenAnswerIsComplete() {
        final GitLabResourceAccessTokenWithToken answer = created(8L, "ci");

        final GitLabResourceAccessToken copy = TokenValues.withoutSecret(answer);

        assertSoftly(softly -> {
            softly.assertThat(copy.id()).isEqualTo(8L);
            softly.assertThat(copy.name()).isEqualTo("ci");
            softly.assertThat(copy.scopes()).isEqualTo(answer.scopes());
            softly.assertThat(copy.active()).isTrue();
            softly.assertThat(copy.revoked()).isFalse();
            softly.assertThat(copy.expiresAt()).isEqualTo(answer.expiresAt());
            softly.assertThat(copy.resourceId()).isEqualTo(42L);
            softly.assertThat(copy.accessLevel()).isEqualTo(GitLabResourceAccessTokenAccessLevel.DEVELOPER);
            softly.assertThat(copy.resourceType()).isEqualTo(GitLabResourceAccessTokenResourceType.PROJECT);
        });
    }

    @Test
    void withoutSecret_shouldKeepNullEnumsNull_whenAnswerLeavesThemOut() {
        final GitLabResourceAccessTokenWithToken answer = GitLabResourceAccessTokenWithTokenBuilder.builder()
                .id(8L)
                .name("ci")
                .token(SECRET)
                .build();

        final GitLabResourceAccessToken copy = TokenValues.withoutSecret(answer);

        assertSoftly(softly -> {
            softly.assertThat(copy.accessLevel()).isNull();
            softly.assertThat(copy.resourceType()).isNull();
        });
    }

    @Test
    void describe_shouldNameTheTypeIdAndNameButNotTheSecret_whenCalled() {
        final String text = TokenValues.describe("CreatedAccessToken", created(8L, "ci"));

        assertThat(text).isEqualTo("CreatedAccessToken[id=8, name=ci, token=<redacted>]");
    }
}
