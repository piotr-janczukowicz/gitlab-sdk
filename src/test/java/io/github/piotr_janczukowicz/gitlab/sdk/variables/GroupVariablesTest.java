package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static io.github.piotr_janczukowicz.gitlab.sdk.variables.VariablesFixtures.SECRET;
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
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelvariables.GitLabGroupGroupLevelVariablesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupVariablesTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    @Test
    void variables_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.grouplevelvariables().list(ID).stream())
                .thenReturn(Stream.of("A", "B", "C").map(key -> {
                    consumed.incrementAndGet();
                    return variable(key, "v");
                }));

        final Stream<GroupVariable> variables = group.variables();

        assertThat(consumed).hasValue(0);
        assertThat(variables.limit(1).toList())
                .singleElement()
                .satisfies(v -> assertThat(v.key()).isEqualTo("A"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.grouplevelvariables(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void variable_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.grouplevelvariables().get(ID, "K")).thenReturn(Optional.empty());

        assertThat(group.variable("K")).isEmpty();
    }

    @Test
    void variable_shouldReturnVariable_whenPresent() throws IOException {
        when(gitLab.grouplevelvariables().get(ID, "K")).thenReturn(Optional.of(variable("K", "v")));

        assertThat(group.variable("K")).get().satisfies(v -> assertThat(v.key()).isEqualTo("K"));
    }

    @Test
    void variable_shouldPassKeyUnchanged_whenKeyHasSpecialCharacters() throws IOException {
        when(gitLab.grouplevelvariables().get(ID, "a/b c")).thenReturn(Optional.empty());

        group.variable("a/b c");

        verify(gitLab.grouplevelvariables()).get(ID, "a/b c");
    }

    @Test
    void variable_shouldThrowIllegalArgumentException_whenKeyIsBlank() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> group.variable(" "))).isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(gitLab.grouplevelvariables());
        });
    }

    @Test
    void createVariable_shouldSendKeyAndValue_whenGiven() throws IOException {
        when(gitLab.grouplevelvariables().create(eq(ID), any(GitLabGroupGroupLevelVariablesCreatePayload.class)))
                .thenReturn(variable("K", SECRET));

        final GroupVariable result = group.createVariable("K", SECRET);

        final ArgumentCaptor<GitLabGroupGroupLevelVariablesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelVariablesCreatePayload.class);
        verify(gitLab.grouplevelvariables()).create(eq(ID), payload.capture());
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
        when(gitLab.grouplevelvariables().create(eq(ID), any(GitLabGroupGroupLevelVariablesCreatePayload.class)))
                .thenReturn(variable("K", ""));

        group.createVariable("K", "");

        final ArgumentCaptor<GitLabGroupGroupLevelVariablesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelVariablesCreatePayload.class);
        verify(gitLab.grouplevelvariables()).create(eq(ID), payload.capture());
        assertThat(payload.getValue().value()).isEmpty();
    }

    @Test
    void createVariable_shouldThrowWithoutCoreCall_whenKeyOrValueIsInvalid() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> group.createVariable(" ", "v")))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> group.createVariable(null, "v")))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> group.createVariable("K", null)))
                    .isInstanceOf(NullPointerException.class);
            verifyNoInteractions(gitLab.grouplevelvariables());
        });
    }

    @Test
    void createVariable_shouldSendPayload_whenPayloadGiven() throws IOException {
        final GitLabGroupGroupLevelVariablesCreatePayload payload =
                GitLabGroupGroupLevelVariablesCreatePayloadBuilder.builder()
                        .key("K")
                        .value("v")
                        .maskedAndHidden(true)
                        .build();
        when(gitLab.grouplevelvariables().create(ID, payload)).thenReturn(variable("K", null));

        final GroupVariable result = group.createVariable(payload);

        assertThat(result.key()).isEqualTo("K");
    }

    @Test
    void createVariable_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown =
                catchThrowable(() -> group.createVariable((GitLabGroupGroupLevelVariablesCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
