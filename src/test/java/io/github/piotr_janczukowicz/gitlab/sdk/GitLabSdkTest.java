package io.github.piotr_janczukowicz.gitlab.sdk;

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
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabInstanceIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabInstanceIssuesListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabInstanceIssuesListScope;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabInstanceMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabInstanceMergeRequestsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabInstanceMergeRequestsListScope;
import io.github.piotr_janczukowicz.gitlab.client.GitLabAuthenticationProviders;
import io.github.piotr_janczukowicz.gitlab.client.GitLabCaller;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClients;
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
class GitLabSdkTest {

    @Test
    void of_shouldWrapClient_whenGivenGitLab() {
        final GitLab gitlab = mock(GitLab.class);

        final GitLabSdk sut = GitLabSdk.of(gitlab);

        assertThat(sut.gitLab()).isSameAs(gitlab);
        verifyNoInteractions(gitlab);
    }

    @Test
    void of_shouldThrowNullPointerException_whenGitLabIsNull() {
        final Throwable thrown = catchThrowable(() -> GitLabSdk.of((GitLab) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("gitlab");
    }

    @Test
    void of_shouldThrowNullPointerException_whenCallerIsNull() {
        final Throwable thrown = catchThrowable(() -> GitLabSdk.of((GitLabCaller) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("caller");
    }

    @Test
    void of_shouldThrowNullPointerException_whenClientIsNull() {
        final Throwable thrown = catchThrowable(() -> GitLabSdk.of((GitLabClient) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("client");
    }

    @Test
    void of_shouldBuildFromCaller() {
        try (GitLabClient client =
                GitLabClients.create(GitLabAuthenticationProviders.personalToken("token-for-a-test"), "my-app/1.0")) {

            final GitLabSdk sut = GitLabSdk.of(GitLabCaller.of(client));

            assertThat(sut.gitLab()).isNotNull();
        }
    }

    @Test
    void of_shouldBuildFromClient() {
        try (GitLabClient client =
                GitLabClients.create(GitLabAuthenticationProviders.personalToken("token-for-a-test"), "my-app/1.0")) {

            final GitLabSdk sut = GitLabSdk.of(client);

            assertThat(sut.gitLab()).isNotNull();
        }
    }

    @Test
    void issues_shouldWrapEachIssueWithItsProject_whenInstanceHasIssues() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitlab.issues().instance().list(any(GitLabInstanceIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(listedIssue(1L, 5L), listedIssue(2L, 6L)));
        final GitLabSdk sut = GitLabSdk.of(gitlab);

        final List<Issue> issues = sut.issues().toList();

        verify(gitlab.issues().instance()).list(GitLabInstanceIssuesListFilter.empty());

        assertSoftly(softly -> {
            softly.assertThat(issues).extracting(Issue::iid).containsExactly(1L, 2L);
            softly.assertThat(issues).extracting(issue -> issue.project().id()).containsExactly("5", "6");
        });
    }

    @Test
    void issues_shouldPassTheScope_whenFilterGivesOne() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        final GitLabInstanceIssuesListFilter filter = GitLabInstanceIssuesListFilterBuilder.builder()
                .scope(GitLabInstanceIssuesListScope.ALL)
                .build();
        when(gitlab.issues().instance().list(filter).stream()).thenReturn(Stream.of(listedIssue(1L, 5L)));
        final GitLabSdk sut = GitLabSdk.of(gitlab);

        final List<Issue> issues = sut.issues(filter).toList();

        assertThat(issues).hasSize(1);
    }

    @Test
    void issues_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        final AtomicInteger consumed = new AtomicInteger();
        when(gitlab.issues().instance().list(any(GitLabInstanceIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return listedIssue(iid, 5L);
                }));
        final GitLabSdk sut = GitLabSdk.of(gitlab);

        final Stream<Issue> issues = sut.issues();

        assertThat(consumed).hasValue(0);
        assertThat(issues.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void issues_shouldThrowNullPointerException_whenFilterIsNull() {
        final GitLabSdk sut = GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS));

        final Throwable thrown = catchThrowable(() -> sut.issues(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    @Test
    void issues_shouldThrowNullPointerException_whenListedIssueNamesNoProject() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitlab.issues().instance().list(any(GitLabInstanceIssuesListFilter.class)).stream())
                .thenReturn(Stream.of(GitLabIssueBuilder.builder().iid(1L).build()));
        final Stream<Issue> issues = GitLabSdk.of(gitlab).issues();

        final Throwable thrown = catchThrowable(issues::toList);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("projectId");
    }

    @Test
    void mergeRequests_shouldLoadEachMergeRequestThroughItsProject_andSkipOneThatIsGone() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitlab.mergerequests().instance().list(any(GitLabInstanceMergeRequestsListFilter.class)).stream())
                .thenReturn(
                        Stream.of(listedMergeRequest(1L, 5L), listedMergeRequest(2L, 5L), listedMergeRequest(3L, 6L)));
        when(gitlab.mergerequests().project().get(eq("5"), eq(1L), any()))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(1L).build()));
        when(gitlab.mergerequests().project().get(eq("5"), eq(2L), any())).thenReturn(Optional.empty());
        when(gitlab.mergerequests().project().get(eq("6"), eq(3L), any()))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(3L).build()));
        final GitLabSdk sut = GitLabSdk.of(gitlab);

        final List<MergeRequest> result = sut.mergeRequests().toList();

        verify(gitlab.mergerequests().instance()).list(GitLabInstanceMergeRequestsListFilter.empty());

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 3L);
            softly.assertThat(result).extracting(found -> found.project().id()).containsExactly("5", "6");
        });
    }

    @Test
    void mergeRequests_shouldPassTheScope_whenFilterGivesOne() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        final GitLabInstanceMergeRequestsListFilter filter = GitLabInstanceMergeRequestsListFilterBuilder.builder()
                .scope(GitLabInstanceMergeRequestsListScope.ALL)
                .build();
        when(gitlab.mergerequests().instance().list(filter).stream()).thenReturn(Stream.of(listedMergeRequest(1L, 5L)));
        when(gitlab.mergerequests().project().get(eq("5"), eq(1L), any()))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(1L).build()));
        final GitLabSdk sut = GitLabSdk.of(gitlab);

        final List<MergeRequest> result = sut.mergeRequests(filter).toList();

        assertThat(result).hasSize(1);
    }

    @Test
    void mergeRequests_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final GitLab gitlab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        final AtomicInteger consumed = new AtomicInteger();
        when(gitlab.mergerequests().instance().list(any(GitLabInstanceMergeRequestsListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return listedMergeRequest(iid, 5L);
                }));
        when(gitlab.mergerequests().project().get(eq("5"), eq(1L), any()))
                .thenReturn(
                        Optional.of(GitLabMergeRequestBuilder.builder().iid(1L).build()));
        final GitLabSdk sut = GitLabSdk.of(gitlab);

        final Stream<MergeRequest> result = sut.mergeRequests();

        assertThat(consumed).hasValue(0);
        assertThat(result.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void mergeRequests_shouldThrowNullPointerException_whenFilterIsNull() {
        final GitLabSdk sut = GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS));

        final Throwable thrown = catchThrowable(() -> sut.mergeRequests(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("filter");
    }

    private static GitLabMergeRequestBasic listedMergeRequest(final long iid, final long projectId) {
        return GitLabMergeRequestBasicBuilder.builder()
                .iid(iid)
                .projectId(projectId)
                .title("t")
                .build();
    }

    private static GitLabIssue listedIssue(final long iid, final long projectId) {
        return GitLabIssueBuilder.builder()
                .iid(iid)
                .projectId(projectId)
                .title("t")
                .state("opened")
                .build();
    }
}
