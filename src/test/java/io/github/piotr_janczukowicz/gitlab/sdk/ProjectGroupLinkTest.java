package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLink;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLinkBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectGroupLinkTest {

    private final Project project = GitLabSdk.of(mock(GitLab.class)).project("my-group/demo");

    @Test
    void expiresAt_shouldReturnDate_whenPresent() {
        final GitLabProjectGroupLink response = GitLabProjectGroupLinkBuilder.builder()
                .groupId(9L)
                .expiresAt(LocalDate.of(2027, 1, 2))
                .build();

        final ProjectGroupLink sut = new ProjectGroupLink(project, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.groupId()).isEqualTo(9L);
            softly.assertThat(sut.expiresAt()).contains(LocalDate.of(2027, 1, 2));
            softly.assertThat(sut.response()).isSameAs(response);
            softly.assertThat(sut.project()).isSameAs(project);
        });
    }

    @Test
    void expiresAt_shouldBeEmpty_whenAbsent() {
        final ProjectGroupLink sut = new ProjectGroupLink(
                project, GitLabProjectGroupLinkBuilder.builder().groupId(9L).build());

        assertThat(sut.expiresAt()).isEmpty();
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenProjectIsNull() {
        final GitLabProjectGroupLink response =
                GitLabProjectGroupLinkBuilder.builder().groupId(9L).build();

        final Throwable thrown = catchThrowable(() -> new ProjectGroupLink(null, response));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
