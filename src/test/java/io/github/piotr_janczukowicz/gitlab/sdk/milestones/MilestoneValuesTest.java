package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.gitlab.sdk.milestones.MilestonesFixtures.milestone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestoneBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groupmilestones.GitLabGroupGroupMilestonesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesListState;
import io.github.piotr_janczukowicz.gitlab.api.projectmilestones.GitLabProjectProjectMilestonesUpdateStateEvent;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@UnitTest
class MilestoneValuesTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<MilestoneValues> constructor = MilestoneValues.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void date_shouldParseIsoDate_whenValueHasSurroundingSpaces() {
        assertThat(MilestoneValues.date(" 2026-02-03 ", "due date")).contains(LocalDate.of(2026, 2, 3));
    }

    @Test
    void isoDate_shouldThrowNullPointerException_whenDateIsNull() {
        assertThat(catchThrowable(() -> MilestoneValues.isoDate(null, "dueDate")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void id_shouldThrowNullPointerException_whenAbsent() {
        final var response = GitLabMilestoneBuilder.builder().build();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> MilestoneValues.id(response)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> MilestoneValues.iid(response)).isInstanceOf(NullPointerException.class);
            softly.assertThatThrownBy(() -> MilestoneValues.stateName(response))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void state_shouldMapActive_whenResponseIsActive() {
        assertThat(MilestoneValues.state(milestone(1, "a"))).isEqualTo(MilestoneState.ACTIVE);
    }

    @Test
    void stateEvents_shouldMatchCoreWireValues_whenListedForBothOwners() {
        assertSoftly(softly -> {
            softly.assertThat(GitLabProjectProjectMilestonesUpdateStateEvent.CLOSE.wireValue())
                    .isEqualTo("close");
            softly.assertThat(GitLabProjectProjectMilestonesUpdateStateEvent.ACTIVATE.wireValue())
                    .isEqualTo("activate");
            softly.assertThat(GitLabGroupGroupMilestonesUpdateStateEvent.CLOSE.wireValue())
                    .isEqualTo("close");
            softly.assertThat(GitLabGroupGroupMilestonesUpdateStateEvent.ACTIVATE.wireValue())
                    .isEqualTo("activate");
            softly.assertThat(GitLabProjectProjectMilestonesListState.ACTIVE.wireValue())
                    .isEqualTo("active");
        });
    }
}
