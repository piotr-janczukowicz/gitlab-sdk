package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullProjectRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.projectRule;
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
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectApprovalRule;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsUpdateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectApprovalRuleTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private ProjectApprovalRule sut(final GitLabProjectApprovalRule response) {
        return new ProjectApprovalRule(GitLabSdk.of(gitLab).project(ID), response);
    }

    private GitLabProjectProjectsUpdateApprovalRulePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectsUpdateApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsUpdateApprovalRulePayload.class);
        verify(gitLab.projects().project()).updateApprovalRule(eq(ID), eq(3L), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.projects()
                        .project()
                        .updateApprovalRule(eq(ID), eq(3L), any(GitLabProjectProjectsUpdateApprovalRulePayload.class)))
                .thenReturn(fullProjectRule(3L));
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final ProjectApprovalRule sut = sut(fullProjectRule(3L));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(3L);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
            softly.assertThat(sut.name()).contains("security");
            softly.assertThat(sut.ruleType()).contains("regular");
            softly.assertThat(sut.approvalsRequired()).contains(2);
            softly.assertThat(sut.containsHiddenGroups()).isTrue();
            softly.assertThat(sut.appliesToAllProtectedBranches()).isTrue();
            softly.assertThat(sut.reportType()).contains("code_coverage");
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenResponseLeavesFieldsOut() {
        final ProjectApprovalRule sut = sut(projectRule(3L));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.ruleType()).isEmpty();
            softly.assertThat(sut.approvalsRequired()).isEmpty();
            softly.assertThat(sut.containsHiddenGroups()).isFalse();
            softly.assertThat(sut.appliesToAllProtectedBranches()).isFalse();
            softly.assertThat(sut.reportType()).isEmpty();
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenNameAndCountAreSet() throws IOException {
        stubUpdate();

        final ProjectApprovalRule result =
                sut(projectRule(3L)).update().name("sec").approvalsRequired(1).send();

        final GitLabProjectProjectsUpdateApprovalRulePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("sec");
            softly.assertThat(payload.approvalsRequired()).isEqualTo(1);
            softly.assertThat(payload.userIds()).isNull();
            softly.assertThat(payload.groupIds()).isNull();
            softly.assertThat(payload.usernames()).isNull();
            softly.assertThat(payload.protectedBranchIds()).isNull();
            softly.assertThat(payload.appliesToAllProtectedBranches()).isNull();
            softly.assertThat(payload.removeHiddenGroups()).isNull();
            softly.assertThat(result.id()).isEqualTo(3L);
        });
    }

    @Test
    void send_shouldSendTheCurrentFieldsOnlyAndNeverTheDeprecatedOnes_whenAllAreSet() throws IOException {
        stubUpdate();

        sut(projectRule(3L))
                .update()
                .userIds(List.of(1L, 2L))
                .groupIds(List.of(5L))
                .usernames(List.of("anna"))
                .protectedBranchIds(List.of(8L))
                .appliesToAllProtectedBranches(true)
                .removeHiddenGroups(true)
                .send();

        final GitLabProjectProjectsUpdateApprovalRulePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.userIds()).containsExactly(1L, 2L);
            softly.assertThat(payload.groupIds()).containsExactly(5L);
            softly.assertThat(payload.usernames()).containsExactly("anna");
            softly.assertThat(payload.protectedBranchIds()).containsExactly(8L);
            softly.assertThat(payload.appliesToAllProtectedBranches()).isTrue();
            softly.assertThat(payload.removeHiddenGroups()).isTrue();
            softly.assertThat(payload.users()).isNull();
            softly.assertThat(payload.groups()).isNull();
        });
    }

    @Test
    void send_shouldSendEmptyLists_whenListsAreEmpty() throws IOException {
        stubUpdate();

        sut(projectRule(3L)).update().userIds(List.of()).groupIds(List.of()).send();

        final GitLabProjectProjectsUpdateApprovalRulePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.userIds()).isEmpty();
            softly.assertThat(payload.groupIds()).isEmpty();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final ProjectApprovalRuleUpdate update = sut(projectRule(3L)).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void setters_shouldThrowOnBadArguments_whenNullBlankOrNegative() {
        final ProjectApprovalRuleUpdate update = sut(projectRule(3L)).update();

        final Throwable blank = catchThrowable(() -> update.name(" "));
        final Throwable negative = catchThrowable(() -> update.approvalsRequired(-1));
        final Throwable nullIds = catchThrowable(() -> update.userIds(null));
        final Throwable blankName = catchThrowable(() -> update.usernames(List.of(" ")));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(negative).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullIds).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankName).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void delete_shouldCallCoreDelete_whenCalled() throws IOException {
        sut(projectRule(3L)).delete();

        verify(gitLab.projects().project()).deleteApprovalRule(ID, 3L);
    }

    @Test
    void refresh_shouldReturnLoadedRule_whenPresent() throws IOException {
        when(gitLab.projects().project().getApprovalRule(ID, 3L)).thenReturn(Optional.of(fullProjectRule(3L)));

        final ProjectApprovalRule refreshed = sut(projectRule(3L)).refresh();

        assertThat(refreshed.name()).contains("security");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projects().project().getApprovalRule(ID, 3L)).thenReturn(Optional.empty());
        final ProjectApprovalRule sut = sut(projectRule(3L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("3");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabProjectApprovalRule record = projectRule(3L);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
