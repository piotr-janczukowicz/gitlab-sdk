package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ManagementFormsTest.fieldOf;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
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
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupUpdateTest {

    private static final String ID = "team";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    private void coreAnswers() throws IOException {
        when(gitLab.groups().update(eq(ID), any())).thenReturn(loadedGroup(9L, "team"));
        when(gitLab.groups().get("9")).thenReturn(Optional.of(loadedGroup(9L, "team")));
    }

    private String sentBody() throws IOException {
        final ArgumentCaptor<MultipartFormData> form = ArgumentCaptor.forClass(MultipartFormData.class);
        verify(gitLab.groups()).update(eq(ID), form.capture());
        return ManagementFormsTest.textOf(form.getValue());
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final GroupUpdate update = sut.update();

        final Throwable thrown = catchThrowable(update::send);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
            verifyNoInteractions(gitLab);
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenSomeAreSet() throws IOException {
        coreAnswers();

        sut.update().name("Zespół").visibility(Visibility.INTERNAL).send();

        final String body = sentBody();
        assertSoftly(softly -> {
            softly.assertThat(fieldOf(body, "name")).contains("Zespół");
            softly.assertThat(fieldOf(body, "visibility")).contains("internal");
        });
        assertThat(body).doesNotContain("name=\"path\"").doesNotContain("name=\"description\"");
    }

    @Test
    void send_shouldSendPathAndDescription_whenSet() throws IOException {
        coreAnswers();

        sut.update().path("new-team").description("d").send();

        assertThat(sentBody()).contains("name=\"path\"").contains("new-team").contains("name=\"description\"");
    }

    @Test
    void send_shouldReloadViaGet_whenUpdated() throws IOException {
        coreAnswers();
        final GitLabGroupDetail loaded = loadedGroup(9L, "team");
        when(gitLab.groups().get("9")).thenReturn(Optional.of(loaded));

        final Group updated = sut.update().name("x").send();

        assertSoftly(softly -> {
            softly.assertThat(updated.id()).isEqualTo("9");
            softly.assertThat(updated.response()).isSameAs(loaded);
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenGoneAfterUpdate() throws IOException {
        coreAnswers();
        when(gitLab.groups().get("9")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.update().name("x").send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenBlank() {
        final Throwable thrown = catchThrowable(() -> sut.update().name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
