package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.milestones.Milestone;
import io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestoneState;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A throwaway project milestone of the sandbox. Nothing is assigned to it, so its issues and merge requests are empty.
 * Promoting it to a group milestone and group milestones are not run live.
 */
@Tag("smoke")
class MilestonesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void milestone_shouldRoundTrip_whenCreatedUpdatedClosedReopenedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final Milestone created = project.createMilestone(ENV.name("milestone"));
        final LocalDate start = LocalDate.of(2030, 1, 1);
        final LocalDate due = LocalDate.of(2030, 12, 31);
        SmokeCleanup.run(
                () -> {
                    final Milestone updated = created.update()
                            .description("changed by a smoke test")
                            .startDate(start)
                            .dueDate(due)
                            .send();
                    final Milestone closed = updated.close();
                    final Milestone reopened = closed.reopen();
                    final Optional<Milestone> loaded = project.milestone(created.id());
                    final long issues = reopened.issues().limit(5).count();
                    final long mergeRequests = reopened.mergeRequests().limit(5).count();
                    reopened.delete();
                    final Optional<Milestone> afterDelete = project.milestone(created.id());

                    assertSoftly(softly -> {
                        softly.assertThat(created.title()).isEqualTo(ENV.name("milestone"));
                        softly.assertThat(updated.description()).contains("changed by a smoke test");
                        softly.assertThat(updated.startDate()).contains(start);
                        softly.assertThat(updated.dueDate()).contains(due);
                        softly.assertThat(closed.state()).isEqualTo(MilestoneState.CLOSED);
                        softly.assertThat(reopened.state()).isEqualTo(MilestoneState.ACTIVE);
                        softly.assertThat(loaded).map(Milestone::id).contains(created.id());
                        softly.assertThat(issues).isZero();
                        softly.assertThat(mergeRequests).isZero();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<Milestone> remaining = project.milestone(created.id());
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }
}
