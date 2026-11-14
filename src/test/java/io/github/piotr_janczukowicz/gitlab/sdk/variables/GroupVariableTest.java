package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.scoped;
import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.variable;
import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.withType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariableBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupVariableTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private GroupVariable sut(final GitLabCiVariable response) {
        return new GroupVariable(GitLabSdk.of(gitLab).group(ID), response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GroupVariable sut = sut(GitLabCiVariableBuilder.builder()
                .key("K")
                .value(SECRET)
                .variableType("file")
                .protectedValue(true)
                .masked(true)
                .raw(true)
                .hidden(true)
                .environmentScope("prod")
                .description("d")
                .build());

        assertSoftly(softly -> {
            softly.assertThat(sut.key()).isEqualTo("K");
            softly.assertThat(sut.value()).contains(SECRET);
            softly.assertThat(sut.type()).isEqualTo(VariableType.FILE);
            softly.assertThat(sut.typeName()).isEqualTo("file");
            softly.assertThat(sut.isProtected()).isTrue();
            softly.assertThat(sut.isMasked()).isTrue();
            softly.assertThat(sut.isRaw()).isTrue();
            softly.assertThat(sut.isHidden()).isTrue();
            softly.assertThat(sut.environmentScope()).contains("prod");
            softly.assertThat(sut.description()).contains("d");
            softly.assertThat(sut.group().id()).isEqualTo(ID);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenAbsent() {
        final GroupVariable sut = sut(variable("K", null));

        assertSoftly(softly -> {
            softly.assertThat(sut.value()).isEmpty();
            softly.assertThat(sut.isProtected()).isFalse();
            softly.assertThat(sut.isMasked()).isFalse();
            softly.assertThat(sut.isRaw()).isFalse();
            softly.assertThat(sut.isHidden()).isFalse();
            softly.assertThat(sut.environmentScope()).isEmpty();
            softly.assertThat(sut.description()).isEmpty();
        });
    }

    @Test
    void value_shouldBeEmpty_whenHidden() {
        final GroupVariable sut = sut(GitLabCiVariableBuilder.builder()
                .key("K")
                .variableType("env_var")
                .hidden(true)
                .build());

        assertThat(sut.value()).isEmpty();
    }

    @Test
    void type_shouldMapEnvVar_whenEnvVar() {
        assertThat(sut(variable("K", "v")).type()).isEqualTo(VariableType.ENV_VAR);
    }

    @Test
    void type_shouldThrowIllegalArgumentException_whenUnknown() {
        final GroupVariable sut = sut(withType("K", "other"));

        final Throwable thrown = catchThrowable(sut::type);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.typeName()).isEqualTo("other");
        });
    }

    @Test
    void objects_shouldNotExposeValueInToString_whenValueIsSet() {
        final GroupVariable sut = sut(variable("K", SECRET));

        assertSoftly(softly -> {
            softly.assertThat(sut.toString()).doesNotContain(SECRET);
            softly.assertThat(sut.update().value(SECRET).toString()).doesNotContain(SECRET);
            softly.assertThat(sut.response().toString()).doesNotContain(SECRET).contains("<redacted>");
        });
    }

    @Test
    void delete_shouldIgnoreScope_whenScopeIsSet() throws IOException {
        sut(scoped("K", "v", "prod")).delete();

        verify(gitLab.grouplevelvariables()).delete(ID, "K");
    }

    @Test
    void delete_shouldReturnVoid_whenCoreReturnsOptional() throws IOException {
        when(gitLab.grouplevelvariables().delete(ID, "K")).thenReturn(Optional.of(variable("K", "v")));

        sut(variable("K", "v")).delete();

        verify(gitLab.grouplevelvariables()).delete(ID, "K");
    }

    @Test
    void refresh_shouldLoadByKey_whenScopeIsSet() throws IOException {
        when(gitLab.grouplevelvariables().get(ID, "K")).thenReturn(Optional.of(scoped("K", "new", "prod")));

        final GroupVariable refreshed = sut(scoped("K", "old", "prod")).refresh();

        assertThat(refreshed.value()).contains("new");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.grouplevelvariables().get(ID, "K")).thenReturn(Optional.empty());
        final GroupVariable sut = sut(variable("K", "v"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("K");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabCiVariable record = variable("K", "v");

        assertThat(sut(record).response()).isSameAs(record);
    }
}
