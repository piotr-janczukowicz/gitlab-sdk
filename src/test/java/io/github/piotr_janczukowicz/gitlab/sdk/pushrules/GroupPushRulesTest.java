package io.github.piotr_janczukowicz.gitlab.sdk.pushrules;

import static io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRulesFixtures.CREATED;
import static io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRulesFixtures.fullGroupRule;
import static io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRulesFixtures.groupRule;
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
import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupGroupPushRulesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouppushrules.GitLabGroupPushRule;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupPushRulesTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private GroupPushRules sut(final GitLabGroupPushRule response) {
        return new GroupPushRules(GitLabSdk.of(gitLab).group(ID), response);
    }

    private GitLabGroupGroupPushRulesUpdatePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupPushRulesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupPushRulesUpdatePayload.class);
        verify(gitLab.grouppushrules()).update(eq(ID), payload.capture());
        return payload.getValue();
    }

    private void stubUpdate() throws IOException {
        when(gitLab.grouppushrules().update(eq(ID), any(GitLabGroupGroupPushRulesUpdatePayload.class)))
                .thenReturn(fullGroupRule(3L));
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final GroupPushRules sut = sut(fullGroupRule(3L));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(3L);
            softly.assertThat(sut.group().id()).isEqualTo(ID);
            softly.assertThat(sut.createdAt()).contains(CREATED);
            softly.assertThat(sut.commitMessageRegex()).contains("^JIRA-\\d+");
            softly.assertThat(sut.commitMessageNegativeRegex()).contains("WIP");
            softly.assertThat(sut.branchNameRegex()).contains("^feature/");
            softly.assertThat(sut.authorEmailRegex()).contains("@example\\.test$");
            softly.assertThat(sut.fileNameRegex()).contains("\\.exe$");
            softly.assertThat(sut.maxFileSize()).contains(5L);
            softly.assertThat(sut.denyDeleteTag()).isTrue();
            softly.assertThat(sut.memberCheck()).isTrue();
            softly.assertThat(sut.preventSecrets()).isTrue();
            softly.assertThat(sut.commitCommitterCheck()).isTrue();
            softly.assertThat(sut.commitCommitterNameCheck()).isTrue();
            softly.assertThat(sut.rejectUnsignedCommits()).isTrue();
            softly.assertThat(sut.rejectNonDcoCommits()).isTrue();
        });
    }

    @Test
    void accessors_shouldBeEmptyOrFalse_whenResponseLeavesFieldsOut() {
        final GroupPushRules sut = sut(groupRule(3L));

        assertSoftly(softly -> {
            softly.assertThat(sut.createdAt()).isEmpty();
            softly.assertThat(sut.commitMessageRegex()).isEmpty();
            softly.assertThat(sut.commitMessageNegativeRegex()).isEmpty();
            softly.assertThat(sut.branchNameRegex()).isEmpty();
            softly.assertThat(sut.authorEmailRegex()).isEmpty();
            softly.assertThat(sut.fileNameRegex()).isEmpty();
            softly.assertThat(sut.maxFileSize()).isEmpty();
            softly.assertThat(sut.denyDeleteTag()).isFalse();
            softly.assertThat(sut.memberCheck()).isFalse();
            softly.assertThat(sut.preventSecrets()).isFalse();
            softly.assertThat(sut.commitCommitterCheck()).isFalse();
            softly.assertThat(sut.commitCommitterNameCheck()).isFalse();
            softly.assertThat(sut.rejectUnsignedCommits()).isFalse();
            softly.assertThat(sut.rejectNonDcoCommits()).isFalse();
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenTwoFieldsAreSet() throws IOException {
        stubUpdate();

        final GroupPushRules result = sut(groupRule(3L))
                .update()
                .commitMessageRegex("^FIX-")
                .maxFileSize(2)
                .send();

        final GitLabGroupGroupPushRulesUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.commitMessageRegex()).isEqualTo("^FIX-");
            softly.assertThat(payload.maxFileSize()).isEqualTo(2L);
            softly.assertThat(payload.commitMessageNegativeRegex()).isNull();
            softly.assertThat(payload.branchNameRegex()).isNull();
            softly.assertThat(payload.authorEmailRegex()).isNull();
            softly.assertThat(payload.fileNameRegex()).isNull();
            softly.assertThat(payload.denyDeleteTag()).isNull();
            softly.assertThat(payload.memberCheck()).isNull();
            softly.assertThat(payload.preventSecrets()).isNull();
            softly.assertThat(payload.commitCommitterCheck()).isNull();
            softly.assertThat(payload.commitCommitterNameCheck()).isNull();
            softly.assertThat(payload.rejectUnsignedCommits()).isNull();
            softly.assertThat(payload.rejectNonDcoCommits()).isNull();
            softly.assertThat(result.id()).isEqualTo(3L);
        });
    }

    @Test
    void send_shouldSendEveryField_whenAllAreSet() throws IOException {
        stubUpdate();

        sut(groupRule(3L))
                .update()
                .commitMessageRegex("a")
                .commitMessageNegativeRegex("b")
                .branchNameRegex("c")
                .authorEmailRegex("d")
                .fileNameRegex("e")
                .maxFileSize(9)
                .denyDeleteTag(true)
                .memberCheck(false)
                .preventSecrets(true)
                .commitCommitterCheck(false)
                .commitCommitterNameCheck(true)
                .rejectUnsignedCommits(false)
                .rejectNonDcoCommits(true)
                .send();

        final GitLabGroupGroupPushRulesUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.commitMessageRegex()).isEqualTo("a");
            softly.assertThat(payload.commitMessageNegativeRegex()).isEqualTo("b");
            softly.assertThat(payload.branchNameRegex()).isEqualTo("c");
            softly.assertThat(payload.authorEmailRegex()).isEqualTo("d");
            softly.assertThat(payload.fileNameRegex()).isEqualTo("e");
            softly.assertThat(payload.maxFileSize()).isEqualTo(9L);
            softly.assertThat(payload.denyDeleteTag()).isTrue();
            softly.assertThat(payload.memberCheck()).isFalse();
            softly.assertThat(payload.preventSecrets()).isTrue();
            softly.assertThat(payload.commitCommitterCheck()).isFalse();
            softly.assertThat(payload.commitCommitterNameCheck()).isTrue();
            softly.assertThat(payload.rejectUnsignedCommits()).isFalse();
            softly.assertThat(payload.rejectNonDcoCommits()).isTrue();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GroupPushRulesUpdate update = sut(groupRule(3L)).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void setters_shouldThrowIllegalArgumentException_whenRegexIsBlankOrSizeIsNegative() {
        final GroupPushRulesUpdate update = sut(groupRule(3L)).update();

        final Throwable blank = catchThrowable(() -> update.branchNameRegex(" "));
        final Throwable negative = catchThrowable(() -> update.maxFileSize(-1));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(negative).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void setters_shouldThrowNullPointerException_whenRegexIsNull() {
        final GroupPushRulesUpdate update = sut(groupRule(3L)).update();

        final Throwable thrown = catchThrowable(() -> update.fileNameRegex(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void delete_shouldCallCoreDelete_whenCalled() throws IOException {
        sut(groupRule(3L)).delete();

        verify(gitLab.grouppushrules()).delete(ID);
    }

    @Test
    void refresh_shouldReturnLoadedRules_whenPresent() throws IOException {
        when(gitLab.grouppushrules().get(ID)).thenReturn(fullGroupRule(3L));

        final GroupPushRules refreshed = sut(groupRule(3L)).refresh();

        assertThat(refreshed.commitMessageRegex()).contains("^JIRA-\\d+");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.grouppushrules().get(ID)).thenThrow(mock(GitLabNotFoundException.class));
        final GroupPushRules sut = sut(groupRule(3L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabGroupPushRule record = groupRule(3L);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
