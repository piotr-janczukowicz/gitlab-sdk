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
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesUpdateVariableType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class VariableUpdateTest {

    private static final String ID = "42";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final Variable scopedVariable = new Variable(sdk.project(ID), scoped("K", "v", "prod"));
    private final Variable plainVariable = new Variable(sdk.project(ID), variable("K", "v"));

    private GitLabProjectProjectLevelVariablesUpdatePayload payload() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectLevelVariablesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectLevelVariablesUpdatePayload.class);
        verify(gitLab.projectlevelvariables()).update(eq(ID), eq("K"), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate(final io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable answer)
            throws IOException {
        when(gitLab.projectlevelvariables()
                        .update(eq(ID), eq("K"), any(GitLabProjectProjectLevelVariablesUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() {
        final VariableUpdate sut = scopedVariable.update();

        final Throwable thrown = catchThrowable(sut::send);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(gitLab.projectlevelvariables());
        });
    }

    @Test
    void update_shouldSendOnlySetFields_whenValueSet() throws IOException {
        stubUpdate(variable("K", "n"));

        final Variable result = plainVariable.update().value("n").send();

        final GitLabProjectProjectLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.value()).isEqualTo("n");
            softly.assertThat(sent.protectedValue()).isNull();
            softly.assertThat(sent.masked()).isNull();
            softly.assertThat(sent.raw()).isNull();
            softly.assertThat(sent.variableType()).isNull();
            softly.assertThat(sent.environmentScope()).isNull();
            softly.assertThat(sent.description()).isNull();
            softly.assertThat(sent.filter()).isNull();
            softly.assertThat(result.value()).contains("n");
        });
    }

    @Test
    void update_shouldTargetByOwnScope_andSendNewScope() throws IOException {
        stubUpdate(scoped("K", "v", "staging"));

        final Variable result =
                scopedVariable.update().environmentScope("staging").send();

        final GitLabProjectProjectLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.filter().environmentScope()).isEqualTo("prod");
            softly.assertThat(sent.environmentScope()).isEqualTo("staging");
            softly.assertThat(result.environmentScope()).contains("staging");
        });
    }

    @Test
    void update_shouldSendNoFilter_whenOwnScopeIsBlank() throws IOException {
        stubUpdate(variable("K", "n"));
        final Variable blankScoped = new Variable(sdk.project(ID), scoped("K", "v", " "));

        blankScoped.update().value("n").send();

        assertThat(payload().filter()).isNull();
    }

    @Test
    void update_shouldNotSendScope_whenOnlyValueSetOnScopedVariable() throws IOException {
        stubUpdate(scoped("K", "n", "prod"));

        scopedVariable.update().value("n").send();

        final GitLabProjectProjectLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.filter().environmentScope()).isEqualTo("prod");
            softly.assertThat(sent.environmentScope()).isNull();
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

        final GitLabProjectProjectLevelVariablesUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.protectedValue()).isTrue();
            softly.assertThat(sent.masked()).isTrue();
            softly.assertThat(sent.raw()).isTrue();
            softly.assertThat(sent.variableType()).isEqualTo(GitLabProjectProjectLevelVariablesUpdateVariableType.FILE);
            softly.assertThat(sent.description()).isEqualTo("d");
        });
    }

    @Test
    void update_shouldThrowNullPointerException_whenArgumentIsNull() {
        final VariableUpdate sut = plainVariable.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> sut.value(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.type(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.description(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> sut.environmentScope(null)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new VariableUpdate(null))).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void environmentScope_shouldThrowIllegalArgumentException_whenBlank() {
        final VariableUpdate sut = plainVariable.update();

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
