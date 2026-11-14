package io.github.piotr_janczukowicz.gitlab.sdk.schedules;

import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.TOKEN;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.owner;
import static io.github.piotr_janczukowicz.gitlab.sdk.schedules.SchedulesFixtures.trigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTrigger;
import io.github.piotr_janczukowicz.gitlab.api.pipelinetriggers.GitLabTriggerBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PipelineTriggerTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabTrigger response = GitLabTriggerBuilder.builder(trigger(4))
                .lastUsed(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2027-01-01T00:00:00Z"))
                .owner(owner())
                .build();

        final PipelineTrigger sut = new PipelineTrigger(project, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(4L);
            softly.assertThat(sut.description()).isEqualTo("deploy hook");
            softly.assertThat(sut.token()).isEqualTo(TOKEN);
            softly.assertThat(sut.lastUsed()).contains(Instant.parse("2026-01-01T00:00:00Z"));
            softly.assertThat(sut.expiresAt()).contains(Instant.parse("2027-01-01T00:00:00Z"));
            softly.assertThat(sut.owner()).contains(owner());
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenAbsent() {
        final PipelineTrigger sut = new PipelineTrigger(project, trigger(4));

        assertSoftly(softly -> {
            softly.assertThat(sut.lastUsed()).isEmpty();
            softly.assertThat(sut.expiresAt()).isEmpty();
            softly.assertThat(sut.owner()).isEmpty();
        });
    }

    @Test
    void toString_shouldNotContainToken_whenPrinted() {
        final PipelineTrigger sut = new PipelineTrigger(project, trigger(4));

        assertSoftly(softly -> {
            softly.assertThat(sut.toString()).doesNotContain(TOKEN);
            softly.assertThat(sut.response().toString()).doesNotContain(TOKEN);
        });
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        new PipelineTrigger(project, trigger(4)).delete();

        verify(gitLab.pipelinetriggers()).delete(ID, 4L);
    }

    @Test
    void refresh_shouldReloadTrigger_whenPresent() throws IOException {
        when(gitLab.pipelinetriggers().get(ID, 4L))
                .thenReturn(Optional.of(GitLabTriggerBuilder.builder(trigger(4))
                        .description("new")
                        .build()));

        final PipelineTrigger result = new PipelineTrigger(project, trigger(4)).refresh();

        assertThat(result.description()).isEqualTo("new");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.pipelinetriggers().get(ID, 4L)).thenReturn(Optional.empty());
        final PipelineTrigger sut = new PipelineTrigger(project, trigger(4));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("4");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new PipelineTrigger(null, trigger(1))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new PipelineTrigger(project, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }
}
