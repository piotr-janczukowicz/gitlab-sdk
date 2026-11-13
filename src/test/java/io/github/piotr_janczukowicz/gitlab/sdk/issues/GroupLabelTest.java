package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.groupLabel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabelBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupLabelTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);
    private final GitLabGroupLabel response = groupLabel("bug", "#FF0000");
    private final GroupLabel sut = new GroupLabel(group, response);

    @Test
    void accessors_shouldReturnResponseValues_whenLabelIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("bug");
            softly.assertThat(sut.color()).isEqualTo("#FF0000");
            softly.assertThat(sut.group()).isSameAs(group);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void description_shouldBeEmpty_whenAbsent() {
        assertThat(sut.description()).isEmpty();
    }

    @Test
    void description_shouldReturnValue_whenPresent() {
        final GroupLabel described = new GroupLabel(
                group,
                GitLabGroupLabelBuilder.builder()
                        .name("a")
                        .color("#1")
                        .description("d")
                        .build());

        assertThat(described.description()).contains("d");
    }

    @Test
    void isSubscribed_shouldBeFalse_whenFlagIsAbsent() {
        assertThat(sut.isSubscribed()).isFalse();
    }

    @Test
    void isSubscribed_shouldBeTrue_whenFlagIsSet() {
        final GroupLabel subscribed = new GroupLabel(
                group,
                GitLabGroupLabelBuilder.builder()
                        .name("a")
                        .color("#1")
                        .subscribed(true)
                        .build());

        assertThat(subscribed.isSubscribed()).isTrue();
    }

    @Test
    void delete_shouldCallDeleteByName_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.grouplabels()).deleteByName(ID, "bug");
    }

    @Test
    void subscribe_shouldReturnLabelFromAnswer_whenCalled() throws IOException {
        final GitLabGroupLabel answer = groupLabel("bug", "#FF0000");
        when(gitLab.grouplabels().subscribe(ID, "bug")).thenReturn(answer);

        final GroupLabel result = sut.subscribe();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(answer);
            softly.assertThat(result.group()).isSameAs(group);
        });
    }

    @Test
    void unsubscribe_shouldReturnLabelFromAnswer_whenCalled() throws IOException {
        final GitLabGroupLabel answer = groupLabel("bug", "#FF0000");
        when(gitLab.grouplabels().unsubscribe(ID, "bug")).thenReturn(answer);

        final GroupLabel result = sut.unsubscribe();

        assertThat(result.response()).isSameAs(answer);
    }

    @Test
    void refresh_shouldReturnNewLabel_whenGitLabHasIt() throws IOException {
        final GitLabGroupLabel reloaded = groupLabel("bug", "#0000FF");
        when(gitLab.grouplabels().get(ID, "bug")).thenReturn(Optional.of(reloaded));

        final GroupLabel refreshed = sut.refresh();

        assertThat(refreshed.response()).isSameAs(reloaded);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.grouplabels().get(ID, "bug")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Group label bug is gone");
    }
}
