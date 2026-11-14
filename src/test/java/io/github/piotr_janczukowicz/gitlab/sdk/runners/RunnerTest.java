package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.TOKEN;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.fullRunner;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.job;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.manager;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.resetResult;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.runner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerManager;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiJobBasicWithProject;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetails;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetailsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetailsRunnerType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class RunnerTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    private Runner wrap(final GitLabCiRunnerDetails response) {
        return new Runner(sdk, response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabCiRunnerDetails response = fullRunner(7);

        final Runner sut = wrap(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(7L);
            softly.assertThat(sut.description()).contains("build box");
            softly.assertThat(sut.name()).contains("gitlab-runner");
            softly.assertThat(sut.type()).contains(RunnerType.PROJECT_TYPE);
            softly.assertThat(sut.typeName()).contains("project_type");
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.isPaused()).isFalse();
            softly.assertThat(sut.isOnline()).isTrue();
            softly.assertThat(sut.status()).isEqualTo("online");
            softly.assertThat(sut.version()).contains("18.0.0");
            softly.assertThat(sut.contactedAt()).contains(Instant.parse("2026-01-02T03:04:05Z"));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenAbsent() {
        final Runner sut = wrap(
                GitLabCiRunnerDetailsBuilder.builder().id(7L).status("offline").build());

        assertSoftly(softly -> {
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.type()).isEmpty();
            softly.assertThat(sut.typeName()).isEmpty();
            softly.assertThat(sut.isActive()).isFalse();
            softly.assertThat(sut.isPaused()).isFalse();
            softly.assertThat(sut.isOnline()).isFalse();
            softly.assertThat(sut.version()).isEmpty();
            softly.assertThat(sut.contactedAt()).isEmpty();
            softly.assertThat(sut.tagList()).isEmpty();
        });
    }

    @Test
    void tagList_shouldSplitAndTrim_whenCommaSeparated() {
        assertThat(wrap(fullRunner(7)).tagList()).containsExactly("docker", "linux");
    }

    @Test
    void tagList_shouldBeEmpty_whenBlank() {
        final Runner sut = wrap(
                GitLabCiRunnerDetailsBuilder.builder(runner(7)).tagList(" ").build());

        assertThat(sut.tagList()).isEmpty();
    }

    @Test
    void tagList_shouldBeImmutable_whenRead() {
        final List<String> tags = wrap(fullRunner(7)).tagList();

        final Throwable thrown = catchThrowable(() -> tags.add("x"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void type_shouldThrowIllegalArgumentException_whenUnknown() {
        final Runner sut = wrap(GitLabCiRunnerDetailsBuilder.builder(runner(7))
                .runnerType(GitLabCiRunnerDetailsRunnerType.UNRECOGNIZED)
                .build());

        final Throwable thrown = catchThrowable(sut::type);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.typeName()).contains("unrecognized");
        });
    }

    @Test
    void refresh_shouldLoadThroughInstanceGet_whenPresent() throws IOException {
        when(gitLab.runners().instance().get(7L)).thenReturn(Optional.of(fullRunner(7)));

        final Runner result = wrap(runner(7)).refresh();

        assertThat(result.name()).contains("gitlab-runner");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.runners().instance().get(7L)).thenReturn(Optional.empty());
        final Runner sut = wrap(runner(7));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void update_shouldReturnBuilder_whenCalled() {
        assertThat(wrap(runner(7)).update()).isNotNull();
    }

    @Test
    void delete_shouldUseGlobalId_whenCalled() throws IOException {
        wrap(runner(7)).delete();

        verify(gitLab.runners().instance()).deleteById(7L);
        verifyNoInteractions(gitLab.runners().project());
    }

    @Test
    void disable_shouldCallProjectDelete_whenProjectGiven() throws IOException {
        final Project project = sdk.project("my-group/demo");

        wrap(runner(7)).disable(project);

        verify(gitLab.runners().project()).delete("my-group/demo", 7L);
        verifyNoInteractions(gitLab.runners().instance());
    }

    @Test
    void disable_shouldThrowNullPointerException_whenProjectIsNull() {
        final Runner sut = wrap(runner(7));

        final Throwable thrown = catchThrowable(() -> sut.disable(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void managers_shouldPassThrough_whenListed() throws IOException {
        final List<GitLabCiRunnerManager> managers = List.of(manager(1), manager(2));
        when(gitLab.runners().instance().listManagers(7L)).thenReturn(managers);

        final List<GitLabCiRunnerManager> result = wrap(runner(7)).managers();

        final Throwable thrown = catchThrowable(() -> result.add(manager(3)));
        assertSoftly(softly -> {
            softly.assertThat(result).containsExactlyElementsOf(managers);
            softly.assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void jobs_shouldPassThrough_whenListed() throws IOException {
        when(gitLab.runners().instance().listJobs(7L).stream()).thenReturn(Stream.of(job(1), job(2)));

        final List<GitLabCiJobBasicWithProject> result = wrap(runner(7)).jobs().toList();

        assertThat(result).extracting(GitLabCiJobBasicWithProject::id).containsExactly(1L, 2L);
    }

    @Test
    void jobs_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.runners().instance().listJobs(7L).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return job(id);
                }));

        final Stream<GitLabCiJobBasicWithProject> stream = wrap(runner(7)).jobs();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void resetAuthenticationToken_shouldUseRunnerId_whenCalled() throws IOException {
        when(gitLab.runners().instance().resetAuthenticationTokenById(7L)).thenReturn(resetResult());

        final RegistrationToken result = wrap(runner(7)).resetAuthenticationToken();

        assertSoftly(softly -> {
            softly.assertThat(result.token()).isEqualTo(TOKEN);
            softly.assertThat(result.toString()).doesNotContain(TOKEN);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        final Throwable noSdk = catchThrowable(() -> new Runner(null, runner(7)));
        final Throwable noResponse = catchThrowable(() -> new Runner(sdk, null));

        assertSoftly(softly -> {
            softly.assertThat(noSdk).isInstanceOf(NullPointerException.class);
            softly.assertThat(noResponse).isInstanceOf(NullPointerException.class);
        });
    }
}
