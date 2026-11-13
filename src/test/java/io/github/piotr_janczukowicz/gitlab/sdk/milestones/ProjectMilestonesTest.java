package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.milestone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListState;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectMilestonesTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void milestones_shouldListMilestones_whenGitLabReturnsThem() throws IOException {
        when(gitLab.projectmilestones().list(ID).stream()).thenReturn(Stream.of(milestone(1, "a"), milestone(2, "b")));

        final List<Milestone> result = sut.milestones().toList();

        assertThat(result).extracting(Milestone::title).containsExactly("a", "b");
    }

    @Test
    void milestones_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectmilestones().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return milestone(id, "m");
                }));

        final Stream<Milestone> stream = sut.milestones();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void milestones_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectProjectMilestonesListFilter filter =
                GitLabProjectProjectMilestonesListFilterBuilder.builder()
                        .state(GitLabProjectProjectMilestonesListState.CLOSED)
                        .build();
        when(gitLab.projectmilestones().list(ID, filter).stream()).thenReturn(Stream.of(milestone(1, "a")));

        final List<Milestone> result = sut.milestones(filter).toList();

        assertThat(result).extracting(Milestone::title).containsExactly("a");
    }

    @Test
    void milestones_shouldThrowNullPointerException_whenFilterIsNull() {
        assertThatThrownBy(() -> sut.milestones(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.projectmilestones());
    }

    @Test
    void milestone_shouldReturnLoadedMilestone_whenGitLabHasIt() throws IOException {
        final GitLabMilestone response = milestone(7, "v1");
        when(gitLab.projectmilestones().get(ID, 7L)).thenReturn(Optional.of(response));

        final Optional<Milestone> found = sut.milestone(7);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void milestone_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectmilestones().get(ID, 7L)).thenReturn(Optional.empty());

        final Optional<Milestone> found = sut.milestone(7);

        assertThat(found).isEmpty();
    }

    @Test
    void createMilestone_shouldSendTitle_whenTitleGiven() throws IOException {
        final GitLabMilestone created = milestone(7, "v1");
        final GitLabProjectProjectMilestonesCreatePayload payload =
                GitLabProjectProjectMilestonesCreatePayloadBuilder.builder()
                        .title("v1")
                        .build();
        when(gitLab.projectmilestones().create(ID, payload)).thenReturn(created);

        final Milestone result = sut.createMilestone("v1");

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.project()).isSameAs(sut);
        });
    }

    @Test
    void createMilestone_shouldSendPayloadAsGiven_whenPayloadGiven() throws IOException {
        final GitLabMilestone created = milestone(7, "v1");
        final GitLabProjectProjectMilestonesCreatePayload payload =
                GitLabProjectProjectMilestonesCreatePayloadBuilder.builder()
                        .title("v1")
                        .dueDate("2026-12-31")
                        .build();
        when(gitLab.projectmilestones().create(ID, payload)).thenReturn(created);

        final Milestone result = sut.createMilestone(payload);

        assertThat(result.response()).isSameAs(created);
    }

    @Test
    void createMilestone_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        assertThatThrownBy(() -> sut.createMilestone(" ")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.projectmilestones());
    }

    @Test
    void createMilestone_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> sut.createMilestone((String) null))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> sut.createMilestone((GitLabProjectProjectMilestonesCreatePayload) null))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.projectmilestones());
    }
}
