package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProjectBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectDraftTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);

    private void coreAnswers() throws IOException {
        when(gitLab.projects().project().create(any()))
                .thenReturn(GitLabProjectBuilder.builder().id(42L).build());
        when(gitLab.projects().project().get("42")).thenReturn(Optional.of(loadedProject(42L, "demo")));
    }

    private String sentBody() throws IOException {
        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.projects().project()).create(form.capture());
        return ProjectFormsTest.textOf(form.getValue());
    }

    @Test
    void send_shouldCreateThenReloadViaGet_whenCreated() throws IOException {
        coreAnswers();
        final GitLabProjectWithAccessAndCatalog loaded = loadedProject(42L, "demo");
        when(gitLab.projects().project().get("42")).thenReturn(Optional.of(loaded));

        final Project created = sdk.createProject("demo").send();

        assertSoftly(softly -> {
            softly.assertThat(created.id()).isEqualTo("42");
            softly.assertThat(created.response()).isSameAs(loaded);
        });
    }

    @Test
    void send_shouldSendEverySetField_whenAllAreSet() throws IOException {
        coreAnswers();

        sdk.createProject("zażółć")
                .path("demo")
                .namespaceId(7L)
                .description("opis")
                .visibility(Visibility.INTERNAL)
                .defaultBranch("trunk")
                .initializeWithReadme(true)
                .send();

        assertThat(sentBody())
                .contains("name=\"name\"")
                .contains("zażółć")
                .contains("name=\"path\"")
                .contains("name=\"namespace_id\"")
                .contains("name=\"description\"")
                .contains("name=\"visibility\"")
                .contains("internal")
                .contains("name=\"default_branch\"")
                .contains("trunk")
                .contains("name=\"initialize_with_readme\"")
                .contains("true");
    }

    @Test
    void send_shouldSendOnlyName_whenNothingElseIsSet() throws IOException {
        coreAnswers();

        sdk.createProject("demo").send();

        assertThat(sentBody())
                .contains("name=\"name\"")
                .doesNotContain("name=\"path\"")
                .doesNotContain("name=\"visibility\"")
                .doesNotContain("initialize_with_readme");
    }

    @Test
    void send_shouldThrowIllegalStateException_whenGoneAfterCreate() throws IOException {
        coreAnswers();
        when(gitLab.projects().project().get("42")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sdk.createProject("demo").send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("42");
    }

    @Test
    void createProject_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sdk.createProject(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createProject_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> sdk.createProject(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void path_shouldThrowIllegalArgumentException_whenBlank() {
        final Throwable thrown = catchThrowable(() -> sdk.createProject("demo").path(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void visibility_shouldThrowNullPointerException_whenNull() {
        final Throwable thrown = catchThrowable(() -> sdk.createProject("demo").visibility(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
