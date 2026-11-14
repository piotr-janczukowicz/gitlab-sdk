package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.fullRunner;
import static io.github.piotr_janczukowicz.gitlab.sdk.runners.RunnersFixtures.runner;
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
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabInstanceRunnersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RunnerUpdateTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Runner runner = new Runner(GitLabSdk.of(gitLab), runner(7));

    private GitLabInstanceRunnersUpdatePayload payload() throws IOException {
        final ArgumentCaptor<GitLabInstanceRunnersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabInstanceRunnersUpdatePayload.class);
        verify(gitLab.runners().instance()).update(eq(7L), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.runners().instance().update(eq(7L), any(GitLabInstanceRunnersUpdatePayload.class)))
                .thenReturn(fullRunner(7));
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final RunnerUpdate sut = runner.update();

        final Throwable thrown = catchThrowable(sut::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.runners());
    }

    @Test
    void send_shouldSendOnlySetFields_whenOnlyPausedSet() throws IOException {
        stubUpdate();

        final Runner result = runner.update().paused(true).send();

        final GitLabInstanceRunnersUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.paused()).isTrue();
            softly.assertThat(sent.description()).isNull();
            softly.assertThat(sent.active()).isNull();
            softly.assertThat(sent.tagList()).isNull();
            softly.assertThat(sent.runUntagged()).isNull();
            softly.assertThat(sent.locked()).isNull();
            softly.assertThat(sent.accessLevel()).isNull();
            softly.assertThat(sent.maximumTimeout()).isNull();
            softly.assertThat(sent.maintenanceNote()).isNull();
            softly.assertThat(result.name()).contains("gitlab-runner");
        });
    }

    @Test
    void send_shouldSendEveryField_whenAllSet() throws IOException {
        stubUpdate();

        runner.update()
                .description("d")
                .paused(false)
                .active(true)
                .tags(List.of("a", "b"))
                .runUntagged(true)
                .locked(false)
                .maximumTimeout(3600)
                .maintenanceNote("n")
                .send();

        final GitLabInstanceRunnersUpdatePayload sent = payload();
        assertSoftly(softly -> {
            softly.assertThat(sent.description()).isEqualTo("d");
            softly.assertThat(sent.paused()).isFalse();
            softly.assertThat(sent.active()).isTrue();
            softly.assertThat(sent.tagList()).containsExactly("a", "b");
            softly.assertThat(sent.runUntagged()).isTrue();
            softly.assertThat(sent.locked()).isFalse();
            softly.assertThat(sent.maximumTimeout()).isEqualTo(3600);
            softly.assertThat(sent.maintenanceNote()).isEqualTo("n");
        });
    }

    @Test
    void tags_shouldPassListAsCoreExpects_whenTagsGiven() throws IOException {
        stubUpdate();

        runner.update().tags(List.of("docker", "linux")).send();

        assertThat(payload().tagList()).containsExactly("docker", "linux");
    }

    @Test
    void tags_shouldSendEmptyList_whenEmpty() throws IOException {
        stubUpdate();

        runner.update().tags(List.of()).send();

        assertThat(payload().tagList()).isEmpty();
    }

    @Test
    void tags_shouldSendCopy_whenCallerChangesListAfterwards() throws IOException {
        stubUpdate();
        final List<String> tags = new ArrayList<>(List.of("docker"));
        final RunnerUpdate sut = runner.update().tags(tags);
        tags.add("linux");

        sut.send();

        assertThat(payload().tagList()).containsExactly("docker");
    }

    @Test
    void tags_shouldThrowNullPointerException_whenElementIsNull() {
        final RunnerUpdate sut = runner.update();
        final List<String> tags = Arrays.asList("docker", null);

        final Throwable thrown = catchThrowable(() -> sut.tags(tags));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void tags_shouldThrowNullPointerException_whenNull() {
        final RunnerUpdate sut = runner.update();

        final Throwable thrown = catchThrowable(() -> sut.tags(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void textSetters_shouldThrowNullPointerException_whenNull() {
        final RunnerUpdate sut = runner.update();

        final Throwable description = catchThrowable(() -> sut.description(null));
        final Throwable note = catchThrowable(() -> sut.maintenanceNote(null));

        assertSoftly(softly -> {
            softly.assertThat(description).isInstanceOf(NullPointerException.class);
            softly.assertThat(note).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenRunnerIsNull() {
        assertThat(catchThrowable(() -> new RunnerUpdate(null))).isInstanceOf(NullPointerException.class);
    }
}
