package io.github.piotr_janczukowicz.gitlab.sdk.pushrules;

import static io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRulesFixtures.fullGroupRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupGroupPushRulesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupGroupPushRulesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupPushRulesAccessTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    @Test
    void pushRules_shouldReturnRules_whenPresent() throws IOException {
        when(gitLab.grouppushrules().get(ID)).thenReturn(fullGroupRule(3L));

        final Optional<GroupPushRules> result = group.pushRules();

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().id()).isEqualTo(3L);
            softly.assertThat(result.orElseThrow().group().id()).isEqualTo(ID);
        });
    }

    @Test
    void pushRules_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.grouppushrules().get(ID)).thenThrow(mock(GitLabNotFoundException.class));

        assertThat(group.pushRules()).isEmpty();
    }

    @Test
    void pushRules_shouldPassOtherExceptionsThrough_whenGitLabRefuses() throws IOException {
        final GitLabForbiddenException refused = mock(GitLabForbiddenException.class);
        when(gitLab.grouppushrules().get(ID)).thenThrow(refused);

        final Throwable thrown = catchThrowable(group::pushRules);

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void pushRules_shouldPassTheTierErrorThrough_whenTierIsTooLow() throws IOException {
        final GitLabTierException tier = new GitLabTierException("op", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.grouppushrules().get(ID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(group::pushRules);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void createPushRules_shouldSendPayloadAndWrapAnswer_whenCalled() throws IOException {
        final GitLabGroupGroupPushRulesCreatePayload payload = GitLabGroupGroupPushRulesCreatePayloadBuilder.builder()
                .commitMessageRegex("^JIRA-")
                .build();
        when(gitLab.grouppushrules().create(ID, payload)).thenReturn(fullGroupRule(3L));

        final GroupPushRules result = group.createPushRules(payload);

        assertThat(result.id()).isEqualTo(3L);
    }

    @Test
    void createPushRules_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> group.createPushRules(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
