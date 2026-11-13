package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

@UnitTest
class InternalsTest {

    @Test
    void gitLab_shouldReturnTheClientOfTheSdk_whenTheSdkIsCreated() {
        final GitLab gitlab = mock(GitLab.class);
        final GitLabSdk sdk = GitLabSdk.of(gitlab);

        final GitLab result = Internals.gitLab(sdk);

        assertThat(result).isSameAs(gitlab);
    }

    @Test
    void registerSdkAccess_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater() {
        final GitLab gitlab = mock(GitLab.class);
        final GitLab other = mock(GitLab.class);
        final GitLabSdk sdk = GitLabSdk.of(gitlab);

        Internals.registerSdkAccess(ignored -> other);

        assertThat(Internals.gitLab(sdk)).isSameAs(gitlab);
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Internals> constructor = Internals.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void project_shouldReturnHandleOfTheSdkOfTheGroup_whenGroupIsGiven() {
        final GitLabSdk sdk = GitLabSdk.of(mock(GitLab.class));

        final Project result = Internals.project(sdk.group("my-group"), "42");

        assertThat(result.id()).isEqualTo("42");
    }
}
