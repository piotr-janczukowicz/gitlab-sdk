package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullProjectRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullSettings;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.projectRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.settings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsCreateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectApprovalRulesTest {

    private static final String PROJECT_ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(PROJECT_ID);

    @Test
    void approvalRules_shouldBeLazy_whenProjectStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projects().project().listApprovalRules(PROJECT_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return projectRule(id);
                }));

        final Stream<ProjectApprovalRule> rules = project.approvalRules();

        assertThat(consumed).hasValue(0);
        assertThat(rules.limit(1).toList())
                .singleElement()
                .satisfies(r -> assertThat(r.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.projects().project(), never()).getApprovalRule(any(String.class), any(Long.class));
    }

    @Test
    void approvalRule_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projects().project().getApprovalRule(PROJECT_ID, 4L)).thenReturn(Optional.empty());

        assertThat(project.approvalRule(4L)).isEmpty();
        verify(gitLab.projects().project()).getApprovalRule(PROJECT_ID, 4L);
    }

    @Test
    void approvalRule_shouldReturnRule_whenPresent() throws IOException {
        when(gitLab.projects().project().getApprovalRule(PROJECT_ID, 4L)).thenReturn(Optional.of(fullProjectRule(4L)));

        assertThat(project.approvalRule(4L))
                .get()
                .satisfies(r -> assertThat(r.name()).contains("security"));
    }

    @Test
    void createApprovalRule_shouldSendNameAndCountOnly_whenProjectRuleIsCreated() throws IOException {
        when(gitLab.projects()
                        .project()
                        .createApprovalRule(eq(PROJECT_ID), any(GitLabProjectProjectsCreateApprovalRulePayload.class)))
                .thenReturn(fullProjectRule(4L));

        final ProjectApprovalRule result = project.createApprovalRule("security", 2);

        final ArgumentCaptor<GitLabProjectProjectsCreateApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsCreateApprovalRulePayload.class);
        verify(gitLab.projects().project()).createApprovalRule(eq(PROJECT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("security");
            softly.assertThat(payload.getValue().approvalsRequired()).isEqualTo(2);
            softly.assertThat(payload.getValue().ruleType()).isNull();
            softly.assertThat(payload.getValue().users()).isNull();
            softly.assertThat(payload.getValue().groups()).isNull();
            softly.assertThat(payload.getValue().userIds()).isNull();
            softly.assertThat(result.id()).isEqualTo(4L);
        });
    }

    @Test
    void createApprovalRule_shouldSendTheApproversAndNoDeprecatedField_whenApproversAreGiven() throws IOException {
        when(gitLab.projects()
                        .project()
                        .createApprovalRule(eq(PROJECT_ID), any(GitLabProjectProjectsCreateApprovalRulePayload.class)))
                .thenReturn(fullProjectRule(4L));

        project.createApprovalRule("owners", 1, List.of(7L, 8L), List.of(9L), List.of("anna"));

        final ArgumentCaptor<GitLabProjectProjectsCreateApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsCreateApprovalRulePayload.class);
        verify(gitLab.projects().project()).createApprovalRule(eq(PROJECT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().userIds()).containsExactly(7L, 8L);
            softly.assertThat(payload.getValue().groupIds()).containsExactly(9L);
            softly.assertThat(payload.getValue().usernames()).containsExactly("anna");
            softly.assertThat(payload.getValue().users()).isNull();
            softly.assertThat(payload.getValue().groups()).isNull();
            softly.assertThat(payload.getValue().ruleType()).isNull();
        });
    }

    @Test
    void createApprovalRule_shouldThrowOnBadArguments_whenNullBlankOrNegative() {
        final Throwable blank = catchThrowable(() -> project.createApprovalRule(" ", 1));
        final Throwable negative = catchThrowable(() -> project.createApprovalRule("a", -1));
        final Throwable nullIds = catchThrowable(() -> project.createApprovalRule("a", 1, null, List.of(), List.of()));
        final Throwable nullNames =
                catchThrowable(() -> project.createApprovalRule("a", 1, List.of(), List.of(), null));
        final Throwable blankName =
                catchThrowable(() -> project.createApprovalRule("a", 1, List.of(), List.of(), List.of(" ")));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(negative).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullIds).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullNames).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankName).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void approvalConfiguration_shouldReturnConfiguration_whenPresent() throws IOException {
        when(gitLab.mergerequestapprovals().getApprovals(PROJECT_ID)).thenReturn(fullSettings());

        final Optional<ApprovalConfiguration> result = project.approvalConfiguration();

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().project().id()).isEqualTo(PROJECT_ID);
            softly.assertThat(result.orElseThrow().resetApprovalsOnPush()).isTrue();
        });
    }

    @Test
    void approvalConfiguration_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequestapprovals().getApprovals(PROJECT_ID)).thenThrow(mock(GitLabNotFoundException.class));

        assertThat(project.approvalConfiguration()).isEmpty();
    }

    @Test
    void approvalConfiguration_shouldPassTheTierErrorThrough_whenTierIsTooLow() throws IOException {
        final GitLabTierException tier = new GitLabTierException("op", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.mergerequestapprovals().getApprovals(PROJECT_ID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(project::approvalConfiguration);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void approvalConfiguration_shouldReadFlagsAsFalse_whenResponseLeavesThemOut() throws IOException {
        when(gitLab.mergerequestapprovals().getApprovals(PROJECT_ID)).thenReturn(settings());

        final ApprovalConfiguration configuration =
                project.approvalConfiguration().orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(configuration.resetApprovalsOnPush()).isFalse();
            softly.assertThat(configuration.selectiveCodeOwnerRemovals()).isFalse();
            softly.assertThat(configuration.disableOverridingApproversPerMergeRequest())
                    .isFalse();
            softly.assertThat(configuration.mergeRequestsAuthorApproval()).isFalse();
            softly.assertThat(configuration.mergeRequestsDisableCommittersApproval())
                    .isFalse();
            softly.assertThat(configuration.requireReauthenticationToApprove()).isFalse();
        });
    }
}
