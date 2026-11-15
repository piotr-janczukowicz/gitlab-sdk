package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.AccessTokenScope;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class TokenRequestsTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<TokenRequests> constructor = TokenRequests.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccessLevel.class,
            names = {"GUEST", "REPORTER", "DEVELOPER", "MAINTAINER", "OWNER"})
    void requireTokenLevel_shouldReturnTheLevel_whenTheCoreCanDecodeIt(final AccessLevel level) {
        assertThat(TokenRequests.requireTokenLevel(level)).isSameAs(level);
    }

    @ParameterizedTest
    @EnumSource(
            value = AccessLevel.class,
            names = {"MINIMAL_ACCESS", "PLANNER", "SECURITY_MANAGER"})
    void requireTokenLevel_shouldThrowIllegalArgumentExceptionWithTheCause_whenTheCoreCannotDecodeIt(
            final AccessLevel level) {
        final Throwable thrown = catchThrowable(() -> TokenRequests.requireTokenLevel(level));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(level.name())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireTokenLevel_shouldAcceptExactlyTheLevelsOfTheCoreResponse_whenAllLevelsAreTried() {
        final EnumSet<AccessLevel> accepted = EnumSet.noneOf(AccessLevel.class);
        for (final AccessLevel level : AccessLevel.values()) {
            if (catchThrowable(() -> TokenRequests.requireTokenLevel(level)) == null) {
                accepted.add(level);
            }
        }

        assertThat(accepted)
                .containsExactlyInAnyOrder(
                        AccessLevel.GUEST,
                        AccessLevel.REPORTER,
                        AccessLevel.DEVELOPER,
                        AccessLevel.MAINTAINER,
                        AccessLevel.OWNER);
    }

    @Test
    void requireTokenLevel_shouldThrowNullPointerException_whenLevelIsNull() {
        final Throwable thrown = catchThrowable(() -> TokenRequests.requireTokenLevel(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void requireScopes_shouldReturnAnImmutableCopy_whenScopesAreGiven() {
        final List<AccessTokenScope> source = new ArrayList<>(Arrays.asList(AccessTokenScope.API));

        final List<AccessTokenScope> result = TokenRequests.requireScopes(source);
        source.add(AccessTokenScope.READ_API);

        assertSoftly(softly -> {
            softly.assertThat(result).containsExactly(AccessTokenScope.API);
            softly.assertThat(catchThrowable(() -> result.add(AccessTokenScope.READ_API)))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void requireScopes_shouldThrowIllegalArgumentException_whenNoScopeIsGiven() {
        final Throwable thrown = catchThrowable(() -> TokenRequests.requireScopes(List.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireScopes_shouldThrowNullPointerException_whenScopesOrAScopeIsNull() {
        final Throwable nullScopes = catchThrowable(() -> TokenRequests.requireScopes(null));
        final Throwable nullScope =
                catchThrowable(() -> TokenRequests.requireScopes(Arrays.asList((AccessTokenScope) null)));

        assertSoftly(softly -> {
            softly.assertThat(nullScopes).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullScope).isInstanceOf(NullPointerException.class);
        });
    }
}
