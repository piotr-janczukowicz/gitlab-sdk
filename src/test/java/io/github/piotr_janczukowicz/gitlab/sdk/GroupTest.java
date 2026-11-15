package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedGroup;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedProject;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetail;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabGroupIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabGroupMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    private static GitLabIssue listedIssue(final long iid, final long projectId) {
        return GitLabIssueBuilder.builder()
                .iid(iid)
                .projectId(projectId)
                .title("t")
                .state("opened")
                .build();
    }

    @Test
    void fetch_shouldReturnLoadedGroup_whenGitLabHasIt() throws IOException {
        final GitLabGroupDetail response = loadedGroup(7L, "my-group");
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(response));

        final Optional<Group> loaded = sut.fetch();

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(loaded.orElseThrow().id()).isEqualTo(ID);
            softly.assertThat(loaded.orElseThrow().response()).isSameAs(response);
        });
    }

    @Test
    void fetch_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.empty());

        assertThat(sut.fetch()).isEmpty();
    }

    @Test
    void response_shouldThrowIllegalStateException_whenGroupIsAHandle() {
        final Throwable thrown = catchThrowable(sut::response);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID + " is gone");
    }

    @Test
    void archive_shouldReturnReloadedGroup_whenCalled() throws IOException {
        final GitLabGroupDetail after = loadedGroup(7L, "my-group");
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(after));

        final Group archived = sut.archive();

        assertThat(archived.response()).isSameAs(after);
        verify(gitLab.groups()).archive(ID);
    }

    @Test
    void unarchive_shouldReturnReloadedGroup_whenCalled() throws IOException {
        final GitLabGroupDetail after = loadedGroup(7L, "my-group");
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(after));

        final Group unarchived = sut.unarchive();

        assertThat(unarchived.response()).isSameAs(after);
        verify(gitLab.groups()).unarchive(ID);
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.groups()).delete(ID);
    }

    @Test
    void projects_shouldMapGroupProjectsLazily() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listProjects(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return listedProject(id);
                }));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.of(loadedProject(1L, "one")));

        final Stream<Project> projects = sut.projects();

        assertThat(consumed).hasValue(0);
        verify(gitLab.projects().project(), never()).get("1");
        final List<Project> first = projects.limit(1).toList();
        assertThat(first).singleElement().satisfies(p -> assertThat(p.id()).isEqualTo("1"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.projects().project(), never()).get("2");
    }

    @Test
    void projects_shouldSkipProject_whenGoneBeforeLoading() throws IOException {
        when(gitLab.groups().listProjects(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(listedProject(1L), listedProject(2L)));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.empty());
        when(gitLab.projects().project().get("2")).thenReturn(Optional.of(loadedProject(2L, "two")));

        final List<Project> projects = sut.projects().toList();

        assertThat(projects).singleElement().satisfies(p -> assertThat(p.id()).isEqualTo("2"));
    }

    @Test
    void subgroups_shouldLoadDirectChildrenAndSkipGone_whenListed() throws IOException {
        final GitLabGroupDetail child = loadedGroup(8L, "child");
        when(gitLab.groups().listGroup(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(listedGroup(8L), listedGroup(9L)));
        when(gitLab.groups().get("8")).thenReturn(Optional.of(child));
        when(gitLab.groups().get("9")).thenReturn(Optional.empty());

        final List<Group> groups = sut.subgroups().toList();

        assertThat(groups)
                .singleElement()
                .satisfies(g -> assertSoftly(softly -> {
                    softly.assertThat(g.id()).isEqualTo("8");
                    softly.assertThat(g.response()).isSameAs(child);
                }));
        verify(gitLab.groups()).listGroup(eq(ID), any(GitLabPageRequest.class));
        verify(gitLab.groups(), never()).listDescendantGroups(any(String.class), any(GitLabPageRequest.class));
    }

    @Test
    void subgroups_shouldLoadLazily_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groups().listGroup(any(String.class), any(GitLabPageRequest.class)).stream())
                .thenReturn(Stream.of(8L, 9L).map(id -> {
                    consumed.incrementAndGet();
                    return listedGroup(id);
                }));
        when(gitLab.groups().get("8")).thenReturn(Optional.of(loadedGroup(8L, "child")));

        final Stream<Group> groups = sut.subgroups();

        assertThat(consumed).hasValue(0);
        assertThat(groups.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
        verify(gitLab.groups(), never()).get("9");
    }

    @Test
    void issues_shouldWrapEachIssueWithItsProject_whenGroupHasIssues() throws IOException {
        when(gitLab.issues().group().list(eq(ID), any(GitLabGroupIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(listedIssue(1L, 5L), listedIssue(2L, 6L)));

        final List<Issue> issues = sut.issues().toList();

        assertSoftly(softly -> {
            softly.assertThat(issues).extracting(Issue::iid).containsExactly(1L, 2L);
            softly.assertThat(issues).extracting(issue -> issue.project().id()).containsExactly("5", "6");
        });
    }

    @Test
    void issues_shouldPassFilter_whenFilterIsGiven() throws IOException {
        final GitLabGroupIssuesListFilter filter = GitLabGroupIssuesListFilter.empty();
        when(gitLab.issues().group().list(ID, filter).stream()).thenReturn(Stream.of(listedIssue(1L, 5L)));

        final List<Issue> issues = sut.issues(filter).toList();

        assertThat(issues).hasSize(1);
    }

    @Test
    void issues_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.issues().group().list(eq(ID), any(GitLabGroupIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return listedIssue(iid, 5L);
                }));

        final Stream<Issue> issues = sut.issues();

        assertThat(consumed).hasValue(0);
        assertThat(issues.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void issues_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.issues(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void issues_shouldThrowNullPointerException_whenListedIssueNamesNoProject() throws IOException {
        when(gitLab.issues().group().list(eq(ID), any(GitLabGroupIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(GitLabIssueBuilder.builder().iid(1L).build()));
        final Stream<Issue> issues = sut.issues();

        final Throwable thrown = catchThrowable(issues::toList);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("projectId");
    }

    @Test
    void mergeRequests_shouldLoadEachMergeRequestThroughItsProject_andSkipOneThatIsGone() throws IOException {
        when(gitLab.mergerequests().group().list(eq(ID), any(GitLabGroupMergeRequestsListFilter.class)).stream())
                .thenReturn(
                        Stream.of(listedMergeRequest(1L, 5L), listedMergeRequest(2L, 5L), listedMergeRequest(3L, 6L)));
        when(gitLab.mergerequests().project().get("5", 1L))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(1L).build()));
        when(gitLab.mergerequests().project().get("5", 2L)).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get("6", 3L))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(3L).build()));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 3L);
            softly.assertThat(result).extracting(found -> found.project().id()).containsExactly("5", "6");
        });
    }

    @Test
    void mergeRequests_shouldPassFilter_whenFilterIsGiven() throws IOException {
        final GitLabGroupMergeRequestsListFilter filter = GitLabGroupMergeRequestsListFilter.empty();
        when(gitLab.mergerequests().group().list(ID, filter).stream())
                .thenReturn(Stream.of(listedMergeRequest(1L, 5L)));
        when(gitLab.mergerequests().project().get("5", 1L))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(1L).build()));

        final List<MergeRequest> result = sut.mergeRequests(filter).toList();

        assertThat(result).hasSize(1);
    }

    @Test
    void mergeRequests_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.mergerequests().group().list(eq(ID), any(GitLabGroupMergeRequestsListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return listedMergeRequest(iid, 5L);
                }));
        when(gitLab.mergerequests().project().get("5", 1L))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(1L).build()));

        final Stream<MergeRequest> result = sut.mergeRequests();

        assertThat(consumed).hasValue(0);
        assertThat(result.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void mergeRequests_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.mergeRequests(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void mergeRequests_shouldThrowNullPointerException_whenListedMergeRequestNamesNoProject() throws IOException {
        when(gitLab.mergerequests().group().list(eq(ID), any(GitLabGroupMergeRequestsListFilter.class)).stream())
                .thenReturn(Stream.of(
                        GitLabMergeRequestBasicBuilder.builder().iid(1L).build()));
        final Stream<MergeRequest> result = sut.mergeRequests();

        final Throwable thrown = catchThrowable(result::toList);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("projectId");
    }

    @Test
    void mergeRequests_shouldThrowNullPointerException_whenListedMergeRequestHasNoIid() throws IOException {
        when(gitLab.mergerequests().group().list(eq(ID), any(GitLabGroupMergeRequestsListFilter.class)).stream())
                .thenReturn(Stream.of(
                        GitLabMergeRequestBasicBuilder.builder().projectId(5L).build()));
        final Stream<MergeRequest> result = sut.mergeRequests();

        final Throwable thrown = catchThrowable(result::toList);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("iid");
    }

    private static GitLabMergeRequestBasic listedMergeRequest(final long iid, final long projectId) {
        return GitLabMergeRequestBasicBuilder.builder()
                .iid(iid)
                .projectId(projectId)
                .title("t")
                .build();
    }

    @Test
    void internals_shouldReachTheClient_whenGivenAGroup() {
        assertThat(Internals.gitLab(sut)).isSameAs(gitLab);
    }
}
