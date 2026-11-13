package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.label;
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
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsUpdateByNamePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class LabelTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabProjectLabel response = label("bug", "#FF0000");
    private final Label sut = new Label(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenLabelIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("bug");
            softly.assertThat(sut.color()).isEqualTo("#FF0000");
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenOnlyNameIsSet() throws IOException {
        final GitLabProjectLabel changed = label("defect", "#FF0000");
        when(gitLab.labels().updateByName(eq(ID), eq("bug"), any(GitLabProjectLabelsUpdateByNamePayload.class)))
                .thenReturn(changed);

        final Label result = sut.update().name("defect").send();

        final ArgumentCaptor<GitLabProjectLabelsUpdateByNamePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectLabelsUpdateByNamePayload.class);
        verify(gitLab.labels()).updateByName(eq(ID), eq("bug"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().newName()).isEqualTo("defect");
            softly.assertThat(payload.getValue().color()).isNull();
            softly.assertThat(payload.getValue().description()).isNull();
            softly.assertThat(result.response()).isSameAs(changed);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void send_shouldSendColorAndDescription_whenSet() throws IOException {
        when(gitLab.labels().updateByName(eq(ID), eq("bug"), any(GitLabProjectLabelsUpdateByNamePayload.class)))
                .thenReturn(label("bug", "#00FF00"));

        sut.update().color("#00FF00").description("Broken things").send();

        final ArgumentCaptor<GitLabProjectLabelsUpdateByNamePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectLabelsUpdateByNamePayload.class);
        verify(gitLab.labels()).updateByName(eq(ID), eq("bug"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().color()).isEqualTo("#00FF00");
            softly.assertThat(payload.getValue().description()).isEqualTo("Broken things");
            softly.assertThat(payload.getValue().newName()).isNull();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GitLab untouched = mock(GitLab.class);
        final Label other = new Label(GitLabSdk.of(untouched).project(ID), response);

        final Throwable thrown = catchThrowable(() -> other.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(untouched);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.labels()).deleteByName(ID, "bug");
    }

    @Test
    void refresh_shouldReturnNewLabel_whenGitLabHasIt() throws IOException {
        final GitLabProjectLabel reloaded = label("bug", "#0000FF");
        when(gitLab.labels().get(ID, "bug")).thenReturn(Optional.of(reloaded));

        final Label refreshed = sut.refresh();

        assertThat(refreshed.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.labels().get(ID, "bug")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Label bug is gone");
    }
}
