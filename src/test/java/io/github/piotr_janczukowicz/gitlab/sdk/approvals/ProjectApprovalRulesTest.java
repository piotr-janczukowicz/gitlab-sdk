package io.github.piotr_janczukowicz.gitlab.sdk.approvals;

import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullGroupRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullProjectRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.fullSettings;
import static io.github.piotr_janczukowicz.gitlab.sdk.approvals.RuleFixtures.groupRule;
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
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsCreateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsCreateApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsCreateApprovalRulePayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsCreateApprovalRulePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectApprovalRulesTest {

    private static final String PROJECT_ID = "my-group/demo";
    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(PROJECT_ID);
    private final Group group = GitLabSdk.of(gitLab).group(GROUP_ID);

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
    void approvalRules_shouldBeLazy_whenGroupStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listApprovalRules(GROUP_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return groupRule(id);
                }));

        final Stream<GroupApprovalRule> rules = group.approvalRules();

        assertThat(consumed).hasValue(0);
        assertThat(rules.limit(1).toList())
                .singleElement()
                .satisfies(r -> assertThat(r.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
    }

    @Test
    void approvalRule_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projects().project().getApprovalRule(PROJECT_ID, 4L)).thenReturn(Optional.empty());

        assertThat(project.approvalRule(4L)).isEmpty();
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
    void createApprovalRule_shouldPassPayloadThrough_whenProjectPayloadIsGiven() throws IOException {
        final GitLabProjectProjectsCreateApprovalRulePayload payload =
                GitLabProjectProjectsCreateApprovalRulePayloadBuilder.builder()
                        .name("owners")
                        .approvalsRequired(1)
                        .userIds(java.util.List.of(7L))
                        .build();
        when(gitLab.projects().project().createApprovalRule(PROJECT_ID, payload))
                .thenReturn(fullProjectRule(4L));

        assertThat(project.createApprovalRule(payload).id()).isEqualTo(4L);
    }

    @Test
    void createApprovalRule_shouldSendNameAndCountOnly_whenGroupRuleIsCreated() throws IOException {
        when(gitLab.groups().createApprovalRule(eq(GROUP_ID), any(GitLabGroupGroupsCreateApprovalRulePayload.class)))
                .thenReturn(fullGroupRule(4L));

        final GroupApprovalRule result = group.createApprovalRule("security", 2);

        final ArgumentCaptor<GitLabGroupGroupsCreateApprovalRulePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupsCreateApprovalRulePayload.class);
        verify(gitLab.groups()).createApprovalRule(eq(GROUP_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("security");
            softly.assertThat(payload.getValue().approvalsRequired()).isEqualTo(2);
            softly.assertThat(payload.getValue().ruleType()).isNull();
            softly.assertThat(payload.getValue().userIds()).isNull();
            softly.assertThat(payload.getValue().groupIds()).isNull();
            softly.assertThat(result.id()).isEqualTo(4L);
        });
    }

    @Test
    void createApprovalRule_shouldPassPayloadThrough_whenGroupPayloadIsGiven() throws IOException {
        final GitLabGroupGroupsCreateApprovalRulePayload payload =
                GitLabGroupGroupsCreateApprovalRulePayloadBuilder.builder()
                        .name("owners")
                        .approvalsRequired(1)
                        .build();
        when(gitLab.groups().createApprovalRule(GROUP_ID, payload)).thenReturn(fullGroupRule(4L));

        assertThat(group.createApprovalRule(payload).id()).isEqualTo(4L);
    }

    @Test
    void createApprovalRule_shouldThrowOnBadArguments_whenNullBlankOrNegative() {
        final Throwable blank = catchThrowable(() -> project.createApprovalRule(" ", 1));
        final Throwable negative = catchThrowable(() -> project.createApprovalRule("a", -1));
        final Throwable groupBlank = catchThrowable(() -> group.createApprovalRule(" ", 1));
        final Throwable groupNegative = catchThrowable(() -> group.createApprovalRule("a", -1));
        final Throwable nullPayload =
                catchThrowable(() -> project.createApprovalRule((GitLabProjectProjectsCreateApprovalRulePayload) null));
        final Throwable nullGroupPayload =
                catchThrowable(() -> group.createApprovalRule((GitLabGroupGroupsCreateApprovalRulePayload) null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(negative).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(groupBlank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(groupNegative).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullPayload).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullGroupPayload).isInstanceOf(NullPointerException.class);
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
