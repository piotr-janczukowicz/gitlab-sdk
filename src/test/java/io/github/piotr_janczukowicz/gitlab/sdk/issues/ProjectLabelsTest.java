package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.label;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectLabelsTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void labels_shouldPassFilter_whenGiven() throws IOException {
        final GitLabProjectLabelsListFilter filter =
                GitLabProjectLabelsListFilterBuilder.builder().search("bug").build();
        when(gitLab.labels().list(ID, filter).stream()).thenReturn(Stream.of(label("bug", "#111")));

        final List<Label> result = sut.labels(filter).toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Label::name).containsExactly("bug");
            softly.assertThat(result).allSatisfy(l -> assertThat(l.project()).isSameAs(sut));
        });
    }

    @Test
    void labels_shouldBeLazy_whenFilterGivenAndOnlyOneIsTaken() throws IOException {
        final GitLabProjectLabelsListFilter filter =
                GitLabProjectLabelsListFilterBuilder.builder().search("l").build();
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.labels().list(ID, filter).stream())
                .thenReturn(Stream.of(1, 2, 3).map(i -> {
                    consumed.incrementAndGet();
                    return label("l" + i, "#111");
                }));

        final Stream<Label> stream = sut.labels(filter);

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void labels_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.labels(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.labels());
    }

    @Test
    void label_shouldReturnLabel_whenGitLabHasIt() throws IOException {
        when(gitLab.labels().get(ID, "bug")).thenReturn(Optional.of(label("bug", "#111")));

        final Optional<Label> result = sut.label("bug");

        assertSoftly(softly -> {
            softly.assertThat(result).map(Label::name).contains("bug");
            softly.assertThat(result.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void label_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.labels().get(ID, "bug")).thenReturn(Optional.empty());

        final Optional<Label> result = sut.label("bug");

        assertThat(result).isEmpty();
    }

    @Test
    void label_shouldPassNameWithSlashUnchanged_whenNameContainsSlashAndSpace() throws IOException {
        when(gitLab.labels().get(ID, "team/a b")).thenReturn(Optional.of(label("team/a b", "#111")));

        final Optional<Label> result = sut.label("team/a b");

        assertThat(result).map(Label::name).contains("team/a b");
    }

    @Test
    void label_shouldThrowBeforeCoreCall_whenNameIsBlankOrNull() {
        final Throwable blank = catchThrowable(() -> sut.label(" "));
        final Throwable absent = catchThrowable(() -> sut.label(null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(absent).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.labels());
    }
}
