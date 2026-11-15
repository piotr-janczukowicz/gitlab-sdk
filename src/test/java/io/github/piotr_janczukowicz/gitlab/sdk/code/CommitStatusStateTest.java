package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateState;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitStatusStateTest {

    @Test
    void value_shouldBeKnownToCoreEnum_whenEveryConstantIsMapped() {
        for (final CommitStatusState each : CommitStatusState.values()) {
            assertThat(GitLabProjectCommitsCreateState.fromWire(each.value()).wireValue())
                    .isEqualTo(each.value());
        }
    }

    @Test
    void values_shouldCoverEveryCoreState_whenCoreEnumIsListed() {
        final String[] core = Arrays.stream(GitLabProjectCommitsCreateState.values())
                .map(GitLabProjectCommitsCreateState::wireValue)
                .toArray(String[]::new);

        final String[] sdk = Arrays.stream(CommitStatusState.values())
                .map(CommitStatusState::value)
                .toArray(String[]::new);

        assertThat(sdk).containsExactlyInAnyOrder(core);
    }

    @Test
    void toCore_shouldReturnMatchingCoreConstant_whenEveryConstantIsMapped() {
        for (final CommitStatusState each : CommitStatusState.values()) {
            assertThat(each.toCore().wireValue()).isEqualTo(each.value());
        }
    }
}
