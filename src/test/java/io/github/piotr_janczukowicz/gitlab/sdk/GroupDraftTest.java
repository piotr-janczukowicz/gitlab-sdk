package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupDraftTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    private void coreAnswers() throws IOException {
        when(gitLab.groups().create(any()))
                .thenReturn(GitLabGroupBuilder.builder().id(9L).build());
        when(gitLab.groups().get("9")).thenReturn(Optional.of(loadedGroup(9L, "team")));
    }

    private String sentBody() throws IOException {
        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.groups()).create(form.capture());
        return ProjectFormsTest.textOf(form.getValue());
    }

    @Test
    void send_shouldCreateThenReloadViaGet_whenCreated() throws IOException {
        coreAnswers();
        final GitLabGroupDetail loaded = loadedGroup(9L, "team");
        when(gitLab.groups().get("9")).thenReturn(Optional.of(loaded));

        final Group created = sdk.createGroup("Team", "team").send();

        assertSoftly(softly -> {
            softly.assertThat(created.id()).isEqualTo("9");
            softly.assertThat(created.response()).isSameAs(loaded);
        });
    }

    @Test
    void send_shouldSendNameAndPathOnly_whenNothingElseIsSet() throws IOException {
        coreAnswers();

        sdk.createGroup("Zespół", "team").send();

        assertThat(sentBody())
                .contains("name=\"name\"")
                .contains("Zespół")
                .contains("name=\"path\"")
                .contains("team")
                .doesNotContain("name=\"parent_id\"")
                .doesNotContain("name=\"visibility\"")
                .doesNotContain("name=\"description\"");
    }

    @Test
    void send_shouldSendParentId_whenSet() throws IOException {
        coreAnswers();

        sdk.createGroup("Team", "team")
                .parentId(5L)
                .description("d")
                .visibility(Visibility.PRIVATE)
                .send();

        assertThat(sentBody())
                .contains("name=\"parent_id\"")
                .contains("name=\"description\"")
                .contains("name=\"visibility\"")
                .contains("private");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenGoneAfterCreate() throws IOException {
        coreAnswers();
        when(gitLab.groups().get("9")).thenReturn(Optional.empty());

        final Throwable thrown =
                catchThrowable(() -> sdk.createGroup("Team", "team").send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("9");
    }

    @Test
    void createGroup_shouldThrowIllegalArgumentException_whenPathIsBlank() {
        final Throwable thrown = catchThrowable(() -> sdk.createGroup("Team", " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createGroup_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> sdk.createGroup(null, "team"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
