package io.github.piotr_janczukowicz.gitlab.sdk.pushrules;

import static io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRulesFixtures.fullRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectProjectPushRulesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectpushrules.GitLabProjectProjectPushRulesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectPushRulesTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void pushRules_shouldReturnRules_whenPresent() throws IOException {
        when(gitLab.projectpushrules().get(ID)).thenReturn(fullRule(3L));

        final Optional<PushRules> result = project.pushRules();

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().id()).isEqualTo(3L);
            softly.assertThat(result.orElseThrow().project().id()).isEqualTo(ID);
        });
    }

    @Test
    void pushRules_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectpushrules().get(ID)).thenThrow(mock(GitLabNotFoundException.class));

        assertThat(project.pushRules()).isEmpty();
    }

    @Test
    void pushRules_shouldPassOtherExceptionsThrough_whenGitLabRefuses() throws IOException {
        final GitLabForbiddenException refused = mock(GitLabForbiddenException.class);
        when(gitLab.projectpushrules().get(ID)).thenThrow(refused);

        final Throwable thrown = catchThrowable(project::pushRules);

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void pushRules_shouldPassTheTierErrorThrough_whenTierIsTooLow() throws IOException {
        final GitLabTierException tier = new GitLabTierException("op", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.projectpushrules().get(ID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(project::pushRules);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void createPushRules_shouldSendPayloadAndWrapAnswer_whenCalled() throws IOException {
        final GitLabProjectProjectPushRulesCreatePayload payload =
                GitLabProjectProjectPushRulesCreatePayloadBuilder.builder()
                        .commitMessageRegex("^JIRA-")
                        .build();
        when(gitLab.projectpushrules().create(ID, payload)).thenReturn(fullRule(3L));

        final PushRules result = project.createPushRules(payload);

        assertThat(result.id()).isEqualTo(3L);
    }

    @Test
    void createPushRules_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown = catchThrowable(() -> project.createPushRules(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
