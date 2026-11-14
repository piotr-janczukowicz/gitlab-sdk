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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesGetFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectVariablesTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void variables_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectlevelvariables().list(ID).stream())
                .thenReturn(Stream.of("A", "B", "C").map(key -> {
                    consumed.incrementAndGet();
                    return variable(key, "v");
                }));

        final Stream<Variable> variables = project.variables();

        assertThat(consumed).hasValue(0);
        assertThat(variables.limit(1).toList())
                .singleElement()
                .satisfies(v -> assertThat(v.key()).isEqualTo("A"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.projectlevelvariables(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void variable_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectlevelvariables().get(ID, "K")).thenReturn(Optional.empty());

        assertThat(project.variable("K")).isEmpty();
    }

    @Test
    void variable_shouldReturnVariable_whenPresent() throws IOException {
        when(gitLab.projectlevelvariables().get(ID, "K")).thenReturn(Optional.of(variable("K", "v")));

        assertThat(project.variable("K"))
                .get()
                .satisfies(v -> assertThat(v.key()).isEqualTo("K"));
    }

    @Test
    void variable_shouldSendScopeFilter_whenScopeGiven() throws IOException {
        when(gitLab.projectlevelvariables()
                        .get(
                                ID,
                                "K",
                                GitLabProjectProjectLevelVariablesGetFilterBuilder.builder()
                                        .filterEnvironmentScope("prod")
                                        .build()))
                .thenReturn(Optional.of(scoped("K", "v", "prod")));

        final Optional<Variable> result = project.variable("K", "prod");

        assertThat(result).get().satisfies(v -> assertThat(v.environmentScope()).contains("prod"));
    }

    @Test
    void variable_shouldReturnEmpty_whenScopedGitLabAnswers404() throws IOException {
        when(gitLab.projectlevelvariables()
                        .get(
                                any(String.class),
                                any(String.class),
                                any(
                                        io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables
                                                .GitLabProjectProjectLevelVariablesGetFilter.class)))
                .thenReturn(Optional.empty());

        assertThat(project.variable("K", "prod")).isEmpty();
    }

    @Test
    void variable_shouldPassKeyUnchanged_whenKeyHasSpecialCharacters() throws IOException {
        when(gitLab.projectlevelvariables().get(ID, "a/b c")).thenReturn(Optional.empty());

        project.variable("a/b c");

        verify(gitLab.projectlevelvariables()).get(ID, "a/b c");
    }

    @Test
    void variable_shouldThrowIllegalArgumentException_whenKeyOrScopeIsBlank() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> project.variable(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> project.variable(" ", "prod")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> project.variable("K", " ")))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(gitLab.projectlevelvariables());
        });
    }

    @Test
    void createVariable_shouldSendKeyAndValue_whenGiven() throws IOException {
        when(gitLab.projectlevelvariables().create(eq(ID), any(GitLabProjectProjectLevelVariablesCreatePayload.class)))
                .thenReturn(variable("K", SECRET));

        final Variable result = project.createVariable("K", SECRET);

        final ArgumentCaptor<GitLabProjectProjectLevelVariablesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectLevelVariablesCreatePayload.class);
        verify(gitLab.projectlevelvariables()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().key()).isEqualTo("K");
            softly.assertThat(payload.getValue().value()).isEqualTo(SECRET);
            softly.assertThat(payload.getValue().masked()).isNull();
            softly.assertThat(payload.getValue().variableType()).isNull();
            softly.assertThat(result.key()).isEqualTo("K");
        });
    }

    @Test
    void createVariable_shouldSendEmptyValue_whenValueIsEmpty() throws IOException {
        when(gitLab.projectlevelvariables().create(eq(ID), any(GitLabProjectProjectLevelVariablesCreatePayload.class)))
                .thenReturn(variable("K", ""));

        project.createVariable("K", "");

        final ArgumentCaptor<GitLabProjectProjectLevelVariablesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectLevelVariablesCreatePayload.class);
        verify(gitLab.projectlevelvariables()).create(eq(ID), payload.capture());
        assertThat(payload.getValue().value()).isEmpty();
    }

    @Test
    void createVariable_shouldThrowWithoutCoreCall_whenKeyOrValueIsInvalid() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> project.createVariable(" ", "v")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> project.createVariable(null, "v")))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> project.createVariable("K", null)))
                    .isInstanceOf(NullPointerException.class);
            verifyNoInteractions(gitLab.projectlevelvariables());
        });
    }

    @Test
    void createVariable_shouldSendPayload_whenPayloadGiven() throws IOException {
        final GitLabProjectProjectLevelVariablesCreatePayload payload =
                GitLabProjectProjectLevelVariablesCreatePayloadBuilder.builder()
                        .key("K")
                        .value("v")
                        .maskedAndHidden(true)
                        .build();
        when(gitLab.projectlevelvariables().create(ID, payload)).thenReturn(variable("K", null));

        final Variable result = project.createVariable(payload);

        assertThat(result.key()).isEqualTo("K");
    }

    @Test
    void createVariable_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown =
                catchThrowable(() -> project.createVariable((GitLabProjectProjectLevelVariablesCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
