package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.SECRET;
import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.scoped;
import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.variable;
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
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupVariableUpdateTest {

    private static final String ID = "42";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final GroupVariable scopedVariable = new GroupVariable(sdk.group(ID), scoped("K", "v", "prod"));
    private final GroupVariable plainVariable = new GroupVariable(sdk.group(ID), variable("K", "v"));

    private GitLabGroupGroupLevelVariablesUpdatePayload payload() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupLevelVariablesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelVariablesUpdatePayload.class);
        verify(gitLab.grouplevelvariables()).update(eq(ID), eq("K"), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate(final io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable answer)
            throws IOException {
        when(gitLab.grouplevelvariables()
                        .update(eq(ID), eq("K"), any(GitLabGroupGroupLevelVariablesUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() {
        final GroupVariableUpdate sut = scopedVariable.update();

        final Throwable thrown = catchThrowable(sut::send);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(gitLab.grouplevelvariables());
        });
    }

    @Test
    void update_shouldSendOnlySetFields_whenValueSet() throws IOException {
        stubUpdate(variable("K", "n"));

        final GroupVariable result = plainVariable.update().value("n").send();

        final GitLabGroupGroupLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.value()).isEqualTo("n");
            softly.assertThat(sent.protectedValue()).isNull();
            softly.assertThat(sent.masked()).isNull();
            softly.assertThat(sent.raw()).isNull();
            softly.assertThat(sent.variableType()).isNull();
            softly.assertThat(sent.environmentScope()).isNull();
            softly.assertThat(sent.description()).isNull();
            softly.assertThat(result.value()).contains("n");
        });
    }

    @Test
    void update_shouldSendNewScope_whenScopeSet() throws IOException {
        stubUpdate(scoped("K", "v", "staging"));

        final GroupVariable result =
                scopedVariable.update().environmentScope("staging").send();

        final GitLabGroupGroupLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.environmentScope()).isEqualTo("staging");
            softly.assertThat(result.environmentScope()).contains("staging");
        });
    }

    @Test
    void update_shouldSendAllFields_whenAllSet() throws IOException {
        stubUpdate(variable("K", "n"));

        plainVariable
                .update()
                .value("n")
                .protectedValue(true)
                .masked(true)
                .raw(true)
                .type(VariableType.FILE)
                .description("d")
                .send();

        final GitLabGroupGroupLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.protectedValue()).isTrue();
            softly.assertThat(sent.masked()).isTrue();
            softly.assertThat(sent.raw()).isTrue();
            softly.assertThat(sent.variableType()).isEqualTo(GitLabGroupGroupLevelVariablesUpdateVariableType.FILE);
            softly.assertThat(sent.description()).isEqualTo("d");
        });
    }

    @Test
    void update_shouldThrowNullPointerException_whenArgumentIsNull() {
        final GroupVariableUpdate sut = plainVariable.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.value(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.type(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.environmentScope(null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new GroupVariableUpdate(null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void environmentScope_shouldThrowIllegalArgumentException_whenBlank() {
        final GroupVariableUpdate sut = plainVariable.update();

        final Throwable thrown = catchThrowable(() -> sut.environmentScope(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void value_shouldSendEmptyValue_whenEmptyGiven() throws IOException {
        stubUpdate(variable("K", ""));

        plainVariable.update().value("").send();

        assertThat(payload().value()).isEmpty();
    }

    @Test
    void send_shouldNotExposeValue_whenPayloadIsPrinted() throws IOException {
        stubUpdate(variable("K", SECRET));

        plainVariable.update().value(SECRET).send();

        assertThat(payload().toString()).doesNotContain(SECRET);
    }
}
