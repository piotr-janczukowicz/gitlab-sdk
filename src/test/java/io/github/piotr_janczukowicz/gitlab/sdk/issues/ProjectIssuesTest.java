package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issue;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.label;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectIssuesTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void issue_shouldReturnLoadedIssue_whenGitLabHasIt() throws IOException {
        final GitLabIssue response = issue(7L, "Broken", "opened");
        when(gitLab.issues().project().get(ID, 7L)).thenReturn(Optional.of(response));

        final Optional<Issue> found = sut.issue(7L);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void issue_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.issues().project().get(ID, 7L)).thenReturn(Optional.empty());

        final Optional<Issue> found = sut.issue(7L);

        assertThat(found).isEmpty();
    }

    @Test
    void issues_shouldBeLazy_andStopAtLimit() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.issues().project().list(eq(ID), any(GitLabProjectIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return issue(iid, "t", "opened");
                }));

        final Stream<Issue> issues = sut.issues();

        assertThat(consumed).hasValue(0);
        assertThat(issues.limit(1).toList())
                .singleElement()
                .satisfies(i -> assertThat(i.iid()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.issues().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void issues_shouldReturnEmptyStream_whenProjectHasNoIssues() throws IOException {
        when(gitLab.issues().project().list(eq(ID), any(GitLabProjectIssuesListFilter.class)).stream())
                .thenReturn(Stream.empty());

        final List<Issue> issues = sut.issues().toList();

        assertThat(issues).isEmpty();
    }

    @Test
    void issues_shouldPassFilterToCore_whenFilterIsGiven() throws IOException {
        final GitLabProjectIssuesListFilter filter = GitLabProjectIssuesListFilter.empty();
        when(gitLab.issues().project().list(ID, filter).stream()).thenReturn(Stream.of(issue(1L, "t", "closed")));

        final List<Issue> issues = sut.issues(filter).toList();

        assertThat(issues).hasSize(1);
    }

    @Test
    void createIssue_shouldSendTitle_whenTitleIsGiven() throws IOException {
        final GitLabIssue created = issue(8L, "New", "opened");
        when(gitLab.issues().project().create(eq(ID), any(GitLabProjectIssuesCreatePayload.class)))
                .thenReturn(created);

        final Issue result = sut.createIssue("New");

        final ArgumentCaptor<GitLabProjectIssuesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesCreatePayload.class);
        verify(gitLab.issues().project()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().title()).isEqualTo("New");
            softly.assertThat(result.response()).isSameAs(created);
        });
    }

    @Test
    void createIssue_shouldSendPayload_whenPayloadIsGiven() throws IOException {
        final GitLabProjectIssuesCreatePayload payload = GitLabProjectIssuesCreatePayloadBuilder.builder()
                .title("New")
                .description("Text")
                .build();
        when(gitLab.issues().project().create(ID, payload)).thenReturn(issue(8L, "New", "opened"));

        final Issue result = sut.createIssue(payload);

        assertThat(result.iid()).isEqualTo(8L);
    }

    @Test
    void createIssue_shouldThrowIllegalArgumentException_whenTitleIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createIssue(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void labels_shouldMapEachLabelToProject() throws IOException {
        when(gitLab.labels().list(ID).stream()).thenReturn(Stream.of(label("bug", "#FF0000"), label("ux", "#00FF00")));

        final List<Label> labels = sut.labels().toList();

        assertThat(labels).hasSize(2).allSatisfy(l -> assertThat(l.project()).isSameAs(sut));
    }

    @Test
    void createLabel_shouldSendNameAndColor_whenGiven() throws IOException {
        final var created = label("bug", "#FF0000");
        when(gitLab.labels().create(eq(ID), any(GitLabProjectLabelsCreatePayload.class)))
                .thenReturn(created);

        final Label result = sut.createLabel("bug", "#FF0000");

        final ArgumentCaptor<GitLabProjectLabelsCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectLabelsCreatePayload.class);
        verify(gitLab.labels()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("bug");
            softly.assertThat(payload.getValue().color()).isEqualTo("#FF0000");
            softly.assertThat(result.response()).isSameAs(created);
        });
    }

    @Test
    void createLabel_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.createLabel(" ", "#FF0000"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
