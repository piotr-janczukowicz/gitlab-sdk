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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListRunnersFilter;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListRunnersFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListRunnersType;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupRunnersTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    @Test
    void runners_shouldLoadEachItem_whenListReturnsSmallerRecord() throws IOException {
        when(gitLab.groups().listRunners(ID).stream()).thenReturn(Stream.of(listed(1), listed(2)));
        when(gitLab.runners().instance().get(1L)).thenReturn(Optional.of(runner(1)));
        when(gitLab.runners().instance().get(2L)).thenReturn(Optional.of(fullRunner(2)));

        final List<Runner> result = group.runners().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Runner::id).containsExactly(1L, 2L);
            softly.assertThat(result.get(1).response()).isEqualTo(fullRunner(2));
        });
    }

    @Test
    void runners_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        when(gitLab.groups().listRunners(ID).stream()).thenReturn(Stream.of(listed(1), listed(2)));
        when(gitLab.runners().instance().get(1L)).thenReturn(Optional.empty());
        when(gitLab.runners().instance().get(2L)).thenReturn(Optional.of(runner(2)));

        final List<Runner> result = group.runners().toList();

        assertThat(result).extracting(Runner::id).containsExactly(2L);
    }

    @Test
    void runners_shouldPassFilter_whenGiven() throws IOException {
        final GitLabGroupGroupsListRunnersFilter filter = GitLabGroupGroupsListRunnersFilterBuilder.builder()
                .type(GitLabGroupGroupsListRunnersType.GROUP_TYPE)
                .build();
        when(gitLab.groups().listRunners(ID, filter).stream()).thenReturn(Stream.of(listed(1)));
        when(gitLab.runners().instance().get(1L)).thenReturn(Optional.of(runner(1)));

        final List<Runner> result = group.runners(filter).toList();

        assertThat(result).extracting(Runner::id).containsExactly(1L);
    }

    @Test
    void runners_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> group.runners(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void runners_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listRunners(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return listed(id);
                }));
        when(gitLab.runners().instance().get(any(Long.class))).thenReturn(Optional.of(runner(1)));

        final Stream<Runner> stream = group.runners();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void resetRunnerRegistrationToken_shouldReturnRedactedToken_whenCalled() throws IOException {
        when(gitLab.groups().resetRegistrationTokenRunner(ID)).thenReturn(resetResult());

        final RunnerToken result = group.resetRunnerRegistrationToken();

        assertSoftly(softly -> {
            softly.assertThat(result.token()).isEqualTo(TOKEN);
            softly.assertThat(result.toString()).doesNotContain(TOKEN);
        });
    }
}
