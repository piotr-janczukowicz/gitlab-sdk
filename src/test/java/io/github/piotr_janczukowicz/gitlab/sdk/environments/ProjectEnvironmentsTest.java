package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static io.github.piotr_janczukowicz.gitlab.sdk.environments.EnvironmentsFixtures.environment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabProjectEnvironmentsListFilterBuilder;
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
class ProjectEnvironmentsTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void environments_shouldBeLazyAndNeedNoReload_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.environments().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return environment(id, "e" + id, "other");
                }));

        final Stream<Environment> environments = project.environments();

        assertThat(consumed).hasValue(0);
        assertThat(environments.limit(1).toList())
                .singleElement()
                .satisfies(e -> assertThat(e.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.environments(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void environments_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectEnvironmentsListFilter filter = GitLabProjectEnvironmentsListFilterBuilder.builder()
                .name("prod")
                .build();
        when(gitLab.environments().list(ID, filter).stream())
                .thenReturn(Stream.of(environment(1, "prod", "production")));

        final Stream<Environment> result = project.environments(filter);

        assertThat(result.toList())
                .singleElement()
                .satisfies(e -> assertThat(e.name()).isEqualTo("prod"));
    }

    @Test
    void environments_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> project.environments(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.environments());
    }

    @Test
    void environment_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.environments().get(ID, 7L)).thenReturn(Optional.empty());

        assertThat(project.environment(7L)).isEmpty();
    }

    @Test
    void environment_shouldReturnEnvironment_whenPresent() throws IOException {
        when(gitLab.environments().get(ID, 7L)).thenReturn(Optional.of(environment(7, "prod", "production")));

        assertThat(project.environment(7L)).get().satisfies(e -> {
            assertThat(e.name()).isEqualTo("prod");
            assertThat(e.project()).isSameAs(project);
        });
    }

    @Test
    void createEnvironment_shouldSendName_whenNameGiven() throws IOException {
        when(gitLab.environments().create(any(String.class), any(GitLabProjectEnvironmentsCreatePayload.class)))
                .thenReturn(environment(8, "review/x", null));

        final Environment result = project.createEnvironment("review/x");

        final ArgumentCaptor<GitLabProjectEnvironmentsCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectEnvironmentsCreatePayload.class);
        verify(gitLab.environments()).create(org.mockito.ArgumentMatchers.eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("review/x");
            softly.assertThat(payload.getValue().tier()).isNull();
            softly.assertThat(result.id()).isEqualTo(8L);
        });
    }

    @Test
    void createEnvironment_shouldThrowBeforeCoreCall_whenNameIsBlankOrNull() {
        final Throwable blank = catchThrowable(() -> project.createEnvironment(" "));
        final Throwable nothing = catchThrowable(() -> project.createEnvironment((String) null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nothing).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void createEnvironment_shouldSendPayloadAsGiven_whenPayloadGiven() throws IOException {
        final GitLabProjectEnvironmentsCreatePayload payload = GitLabProjectEnvironmentsCreatePayloadBuilder.builder()
                .name("n")
                .externalUrl("https://e")
                .build();
        when(gitLab.environments().create(ID, payload)).thenReturn(environment(9, "n", null));

        final Environment result = project.createEnvironment(payload);

        assertThat(result.id()).isEqualTo(9L);
    }

    @Test
    void createEnvironment_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown =
                catchThrowable(() -> project.createEnvironment((GitLabProjectEnvironmentsCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.environments());
    }
}
