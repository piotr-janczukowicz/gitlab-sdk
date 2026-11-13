package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.groupLabel;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.label;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.projectIn;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.projectWithoutNamespace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsUpdateByNamePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

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

    @Test
    void promote_shouldCallPromoteByName_whenProjectIsInGroup() throws IOException {
        final GitLabGroupLabel promoted = groupLabel("bug", "#FF0000");
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(promoted);
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn("group", 77L, "my-group")));

        final GroupLabel result = sut.promote();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(promoted);
            softly.assertThat(result.group().id()).isEqualTo("77");
        });
    }

    @Test
    void promote_shouldPromoteAfterLoadingProject_whenCalled() throws IOException {
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(groupLabel("bug", "#FF0000"));
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn("group", 77L, "my-group")));

        sut.promote();

        final InOrder order = inOrder(gitLab.projects().project(), gitLab.labels());
        order.verify(gitLab.projects().project()).get(ID);
        order.verify(gitLab.labels()).promoteByName(ID, "bug");
    }

    @Test
    void promote_shouldNotPromote_whenProjectCannotBeLoaded() throws IOException {
        when(gitLab.projects().project().get(ID)).thenThrow(new IOException("down"));

        final Throwable thrown = catchThrowable(sut::promote);

        assertThat(thrown).isInstanceOf(IOException.class);
        verify(gitLab.labels(), never()).promoteByName(any(), any());
    }

    @Test
    void promote_shouldNotPromote_whenProjectIsGone() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::promote);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.labels(), never()).promoteByName(any(), any());
    }

    @Test
    void promote_shouldPassCoreException_whenProjectIsInUserNamespace() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn("user", 5L, "jane")));
        when(gitLab.labels().promoteByName(ID, "bug")).thenThrow(new IOException("rejected"));

        final Throwable thrown = catchThrowable(sut::promote);

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("rejected");
        verify(gitLab.labels()).promoteByName(ID, "bug");
    }

    @Test
    void promote_shouldAddressGroupByFullPath_whenNamespaceHasNoId() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn("group", null, "my-group")));
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(groupLabel("bug", "#FF0000"));

        final GroupLabel result = sut.promote();

        assertThat(result.group().id()).isEqualTo("my-group");
    }

    @Test
    void promote_shouldTreatMissingKindAsGroup_whenNamespaceHasId() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn(null, 9L, null)));
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(groupLabel("bug", "#FF0000"));

        final GroupLabel result = sut.promote();

        assertThat(result.group().id()).isEqualTo("9");
    }

    @Test
    void promote_shouldThrowIllegalStateException_whenPromotedButNoGroupDerivable() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn("user", 5L, "jane")));
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(groupLabel("bug", "#FF0000"));

        final Throwable thrown = catchThrowable(sut::promote);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("was promoted");
    }

    @Test
    void promote_shouldThrowIllegalStateException_whenNamespaceNamesNoGroup() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectIn(null, null, " ")));
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(groupLabel("bug", "#FF0000"));

        final Throwable thrown = catchThrowable(sut::promote);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("was promoted");
    }

    @Test
    void promote_shouldThrowIllegalStateException_whenProjectHasNoNamespaceAndPromotionSucceeds() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(projectWithoutNamespace()));
        when(gitLab.labels().promoteByName(ID, "bug")).thenReturn(groupLabel("bug", "#FF0000"));

        final Throwable thrown = catchThrowable(sut::promote);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("was promoted");
    }
}
