package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateCommentLineType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class CommitLineTypeTest {

    @Test
    void value_shouldBeKnownToCoreEnum_whenEveryConstantIsMapped() {
        for (final CommitLineType each : CommitLineType.values()) {
            assertThat(GitLabProjectCommitsCreateCommentLineType.fromWire(each.value())
                            .wireValue())
                    .isEqualTo(each.value());
        }
    }

    @Test
    void values_shouldCoverEveryCoreLineType_whenCoreEnumIsListed() {
        final String[] core = Arrays.stream(GitLabProjectCommitsCreateCommentLineType.values())
                .map(GitLabProjectCommitsCreateCommentLineType::wireValue)
                .toArray(String[]::new);

        final String[] sdk = Arrays.stream(CommitLineType.values())
                .map(CommitLineType::value)
                .toArray(String[]::new);

        assertThat(sdk).containsExactlyInAnyOrder(core);
    }

    @Test
    void toCore_shouldReturnMatchingCoreConstant_whenEveryConstantIsMapped() {
        for (final CommitLineType each : CommitLineType.values()) {
            assertThat(each.toCore().wireValue()).isEqualTo(each.value());
        }
    }
}
