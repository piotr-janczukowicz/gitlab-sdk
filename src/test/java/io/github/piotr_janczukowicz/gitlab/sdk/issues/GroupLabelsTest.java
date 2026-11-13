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
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.grouplabels.GitLabGroupGroupLabelsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupLabelsTest {

    private static final String ID = "my-group/sub";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    @Test
    void labels_shouldListLabels_whenGitLabReturnsThem() throws IOException {
        when(gitLab.grouplabels().list(ID).stream())
                .thenReturn(Stream.of(groupLabel("a", "#111"), groupLabel("b", "#222")));

        final List<GroupLabel> result = sut.labels().toList();

        assertThat(result).extracting(GroupLabel::name).containsExactly("a", "b");
    }

    @Test
    void labels_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.grouplabels().list(ID).stream())
                .thenReturn(Stream.of(1, 2, 3).map(i -> {
                    consumed.incrementAndGet();
                    return groupLabel("l" + i, "#111");
                }));

        final Stream<GroupLabel> stream = sut.labels();

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void labels_shouldPassFilter_whenGiven() throws IOException {
        final GitLabGroupGroupLabelsListFilter filter =
                GitLabGroupGroupLabelsListFilterBuilder.builder().search("bug").build();
        when(gitLab.grouplabels().list(ID, filter).stream()).thenReturn(Stream.of(groupLabel("bug", "#111")));

        final List<GroupLabel> result = sut.labels(filter).toList();

        assertThat(result).extracting(GroupLabel::name).containsExactly("bug");
    }

    @Test
    void labels_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.labels(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.grouplabels());
    }

    @Test
    void label_shouldReturnLabel_whenGitLabHasIt() throws IOException {
        when(gitLab.grouplabels().get(ID, "bug")).thenReturn(Optional.of(groupLabel("bug", "#111")));

        final Optional<GroupLabel> result = sut.label("bug");

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().group()).isSameAs(sut);
        });
    }

    @Test
    void label_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.grouplabels().get(ID, "bug")).thenReturn(Optional.empty());

        final Optional<GroupLabel> result = sut.label("bug");

        assertThat(result).isEmpty();
    }

    @Test
    void label_shouldPassNameWithSlashUnchanged_whenNameContainsSlashAndSpace() throws IOException {
        when(gitLab.grouplabels().get(ID, "team/a b")).thenReturn(Optional.of(groupLabel("team/a b", "#111")));

        final Optional<GroupLabel> result = sut.label("team/a b");

        assertThat(result).map(GroupLabel::name).contains("team/a b");
    }

    @Test
    void label_shouldThrowBeforeCoreCall_whenNameIsBlankOrNull() {
        final Throwable blank = catchThrowable(() -> sut.label(" "));
        final Throwable absent = catchThrowable(() -> sut.label(null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(absent).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.grouplabels());
    }

    @Test
    void createLabel_shouldSendNameAndColor_whenCalled() throws IOException {
        when(gitLab.grouplabels().create(any(), any(GitLabGroupGroupLabelsCreatePayload.class)))
                .thenReturn(groupLabel("bug", "#FF0000"));

        final GroupLabel result = sut.createLabel("bug", "#FF0000");

        final ArgumentCaptor<GitLabGroupGroupLabelsCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLabelsCreatePayload.class);
        verify(gitLab.grouplabels()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("bug");
            softly.assertThat(payload.getValue().color()).isEqualTo("#FF0000");
            softly.assertThat(result.name()).isEqualTo("bug");
            softly.assertThat(result.group()).isSameAs(sut);
        });
    }

    @Test
    void createLabel_shouldThrowBeforeCoreCall_whenNameOrColorIsBlankOrNull() {
        final Throwable blankName = catchThrowable(() -> sut.createLabel(" ", "#111"));
        final Throwable nullColor = catchThrowable(() -> sut.createLabel("bug", null));

        assertSoftly(softly -> {
            softly.assertThat(blankName).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullColor).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.grouplabels());
    }
}
