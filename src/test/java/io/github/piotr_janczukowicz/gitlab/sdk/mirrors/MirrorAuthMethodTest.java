package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsCreateAuthMethod;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class MirrorAuthMethodTest {

    @ParameterizedTest
    @EnumSource(MirrorAuthMethod.class)
    void value_shouldMatchTheCoreEnum_whenMethodIsKnown(final MirrorAuthMethod method) {
        assertThat(method.value())
                .isEqualTo(GitLabProjectRemoteMirrorsCreateAuthMethod.valueOf(method.name())
                        .wireValue());
    }

    @Test
    void values_shouldCoverEveryMethodOfTheCoreEnum_whenCompared() {
        assertThat(Arrays.stream(MirrorAuthMethod.values()).map(MirrorAuthMethod::name))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(GitLabProjectRemoteMirrorsCreateAuthMethod.values())
                        .map(Enum::name)
                        .toList());
    }

    @Test
    void from_shouldReturnMethod_whenNameIsKnown() {
        assertThat(MirrorAuthMethod.from("ssh_public_key")).isEqualTo(MirrorAuthMethod.SSH_PUBLIC_KEY);
    }

    @Test
    void from_shouldThrowIllegalArgumentException_whenNameIsUnknown() {
        final Throwable thrown = catchThrowable(() -> MirrorAuthMethod.from("token"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("token");
    }

    @Test
    void from_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> MirrorAuthMethod.from(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
