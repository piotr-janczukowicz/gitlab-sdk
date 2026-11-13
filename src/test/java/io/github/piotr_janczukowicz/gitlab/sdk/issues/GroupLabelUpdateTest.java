package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.groupLabel;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsUpdateByNamePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupLabelUpdateTest {

    private static final String ID = "my-group";
    private static final String NAME = "team/a b";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GroupLabel label = new GroupLabel(GitLabSdk.of(gitLab).group(ID), groupLabel(NAME, "#FF0000"));

    private void stubUpdate(final GitLabGroupLabel answer) throws IOException {
        when(gitLab.grouplabels().updateByName(eq(ID), eq(NAME), any(GitLabGroupGroupLabelsUpdateByNamePayload.class)))
                .thenReturn(answer);
    }

    private GitLabGroupGroupLabelsUpdateByNamePayload sent() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupLabelsUpdateByNamePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLabelsUpdateByNamePayload.class);
        verify(gitLab.grouplabels()).updateByName(eq(ID), eq(NAME), payload.capture());
        return payload.getValue();
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GroupLabelUpdate update = label.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab.grouplabels());
    }

    @Test
    void send_shouldSendOnlySetFields_whenOnlyNameIsSet() throws IOException {
        final GitLabGroupLabel answer = groupLabel("renamed", "#FF0000");
        stubUpdate(answer);

        final GroupLabel result = label.update().name("renamed").send();

        final GitLabGroupGroupLabelsUpdateByNamePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.newName()).isEqualTo("renamed");
            softly.assertThat(payload.color()).isNull();
            softly.assertThat(payload.description()).isNull();
            softly.assertThat(payload.archived()).isNull();
            softly.assertThat(result.response()).isSameAs(answer);
            softly.assertThat(result.name()).isEqualTo("renamed");
        });
    }

    @Test
    void send_shouldSendColorDescriptionAndArchived_whenSet() throws IOException {
        stubUpdate(groupLabel("x", "#1"));

        label.update().color("#00FF00").description("d").archived(true).send();

        final GitLabGroupGroupLabelsUpdateByNamePayload payload = sent();
        assertSoftly(softly -> {
            softly.assertThat(payload.color()).isEqualTo("#00FF00");
            softly.assertThat(payload.description()).isEqualTo("d");
            softly.assertThat(payload.archived()).isTrue();
            softly.assertThat(payload.newName()).isNull();
        });
    }

    @Test
    void setters_shouldRejectInvalidValues_whenBlankOrNull() {
        final GroupLabelUpdate update = label.update();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> update.name(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> update.name(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> update.color(" "))).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(catchThrowable(() -> update.description(null))).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.grouplabels());
    }
}
