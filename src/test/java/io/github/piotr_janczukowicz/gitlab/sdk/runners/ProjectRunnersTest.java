package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.TOKEN;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.fullRunner;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.listed;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.resetResult;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.runner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersListFilter;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabProjectRunnersListType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@UnitTest
class ProjectRunnersTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void runners_shouldLoadEachItem_whenListReturnsSmallerRecord() throws IOException {
        when(gitLab.runners().project().list(ID).stream()).thenReturn(Stream.of(listed(1), listed(2)));
        when(gitLab.runners().instance().get(1L)).thenReturn(Optional.of(runner(1)));
        when(gitLab.runners().instance().get(2L)).thenReturn(Optional.of(fullRunner(2)));

        final List<Runner> result = project.runners().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Runner::id).containsExactly(1L, 2L);
            softly.assertThat(result.get(1).response()).isEqualTo(fullRunner(2));
        });
    }

    @Test
    void runners_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.runners().project().list(ID).stream()).thenReturn(Stream.of(listed(1), listed(2), listed(3)));
        when(gitLab.runners().instance().get(1L)).thenReturn(Optional.of(runner(1)));
        when(gitLab.runners().instance().get(2L)).thenReturn(Optional.empty());
        when(gitLab.runners().instance().get(3L)).thenReturn(Optional.of(runner(3)));

        final List<Runner> result = project.runners().toList();

        assertThat(result).extracting(Runner::id).containsExactly(1L, 3L);
    }

    @Test
    void runners_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectRunnersListFilter filter = GitLabProjectRunnersListFilterBuilder.builder()
                .type(GitLabProjectRunnersListType.PROJECT_TYPE)
                .build();
        when(gitLab.runners().project().list(ID, filter).stream()).thenReturn(Stream.of(listed(1)));
        when(gitLab.runners().instance().get(1L)).thenReturn(Optional.of(runner(1)));

        final List<Runner> result = project.runners(filter).toList();

        assertThat(result).extracting(Runner::id).containsExactly(1L);
    }

    @Test
    void runners_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> project.runners(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void runners_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.runners().project().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return listed(id);
                }));
        when(gitLab.runners().instance().get(any(Long.class))).thenReturn(Optional.of(runner(1)));

        final Stream<Runner> stream = project.runners();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void enableRunner_shouldCreateThenLoad_whenCalled() throws IOException {
        when(gitLab.runners().project().create(any(String.class), any(GitLabProjectRunnersCreatePayload.class)))
                .thenReturn(listed(5));
        when(gitLab.runners().instance().get(5L)).thenReturn(Optional.of(fullRunner(5)));

        final Runner result = project.enableRunner(5L);

        final ArgumentCaptor<GitLabProjectRunnersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectRunnersCreatePayload.class);
        final InOrder order =
                inOrder(gitLab.runners().project(), gitLab.runners().instance());
        order.verify(gitLab.runners().project()).create(eq(ID), payload.capture());
        order.verify(gitLab.runners().instance()).get(5L);
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().runnerId()).isEqualTo(5L);
            softly.assertThat(result.name()).contains("gitlab-runner");
        });
    }

    @Test
    void enableRunner_shouldThrowIllegalStateException_whenGoneAfterCreate() throws IOException {
        when(gitLab.runners().project().create(any(String.class), any(GitLabProjectRunnersCreatePayload.class)))
                .thenReturn(listed(5));
        when(gitLab.runners().instance().get(5L)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> project.enableRunner(5L));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Runner 5 was enabled but could not be loaded");
    }

    @Test
    void enableRunner_shouldLoadByArgumentId_whenAnswerNamesAnotherId() throws IOException {
        when(gitLab.runners().project().create(any(String.class), any(GitLabProjectRunnersCreatePayload.class)))
                .thenReturn(listed(9));
        when(gitLab.runners().instance().get(5L)).thenReturn(Optional.of(fullRunner(5)));

        final Runner result = project.enableRunner(5L);

        assertThat(result.id()).isEqualTo(5L);
        verify(gitLab.runners().instance(), never()).get(9L);
    }

    @Test
    void resetRunnerRegistrationToken_shouldReturnRedactedToken_whenCalled() throws IOException {
        when(gitLab.runners().project().resetRegistrationToken(ID)).thenReturn(resetResult());

        final RegistrationToken result = project.resetRunnerRegistrationToken();

        assertSoftly(softly -> {
            softly.assertThat(result.token()).isEqualTo(TOKEN);
            softly.assertThat(result.toString()).doesNotContain(TOKEN);
        });
    }
}
