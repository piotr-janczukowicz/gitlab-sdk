package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ManagementFormsTest.fieldOf;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
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
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessAndCatalog;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithCatalogBuilder;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectUpdateTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    private void coreAnswers() throws IOException {
        when(gitLab.projects().project().update(eq(ID), any()))
                .thenReturn(GitLabProjectWithCatalogBuilder.builder().id(42L).build());
        when(gitLab.projects().project().get("42")).thenReturn(Optional.of(loadedProject(42L, "demo")));
    }

    private String sentBody() throws IOException {
        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.projects().project()).update(eq(ID), form.capture());
        return ManagementFormsTest.textOf(form.getValue());
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final ProjectUpdate update = sut.update();

        final Throwable thrown = catchThrowable(update::send);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(gitLab);
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenSomeAreSet() throws IOException {
        coreAnswers();

        sut.update().name("zażółć").visibility(Visibility.PUBLIC).send();

        final String body = sentBody();
        assertSoftly(softly -> {
            softly.assertThat(fieldOf(body, "name")).contains("zażółć");
            softly.assertThat(fieldOf(body, "visibility")).contains("public");
        });
        assertThat(body)
                .doesNotContain("name=\"path\"")
                .doesNotContain("name=\"description\"")
                .doesNotContain("name=\"default_branch\"");
    }

    @Test
    void send_shouldSendEmptyDescription_whenDescriptionIsCleared() throws IOException {
        coreAnswers();

        sut.update().clearDescription().send();

        assertThat(fieldOf(sentBody(), "description")).contains("");
    }

    @Test
    void send_shouldSendPathAndDefaultBranch_whenSet() throws IOException {
        coreAnswers();

        sut.update().path("new-path").defaultBranch("trunk").description("d").send();

        assertThat(sentBody())
                .contains("name=\"path\"")
                .contains("new-path")
                .contains("name=\"default_branch\"")
                .contains("trunk")
                .contains("name=\"description\"");
    }

    @Test
    void send_shouldReloadViaGet_whenUpdated() throws IOException {
        coreAnswers();
        final GitLabProjectWithAccessAndCatalog loaded = loadedProject(42L, "demo");
        when(gitLab.projects().project().get("42")).thenReturn(Optional.of(loaded));

        final Project updated = sut.update().name("x").send();

        assertSoftly(softly -> {
            softly.assertThat(updated.id()).isEqualTo("42");
            softly.assertThat(updated.response()).isSameAs(loaded);
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenGoneAfterUpdate() throws IOException {
        coreAnswers();
        when(gitLab.projects().project().get("42")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.update().name("x").send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenBlank() {
        final Throwable thrown = catchThrowable(() -> sut.update().name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void description_shouldThrowNullPointerException_whenNull() {
        final Throwable thrown = catchThrowable(() -> sut.update().description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
