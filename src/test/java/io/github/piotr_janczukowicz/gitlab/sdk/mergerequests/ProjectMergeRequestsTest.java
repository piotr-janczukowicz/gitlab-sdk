package io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;

import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.basic;
import static io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestsFixtures.mergeRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsListFilter;
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
class ProjectMergeRequestsTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void mergeRequest_shouldReturnLoadedMergeRequest_whenGitLabHasIt() throws IOException {
        final GitLabMergeRequest response = mergeRequest(7L, "Fix", "opened");
        when(gitLab.mergerequests().project().get(ID, 7L)).thenReturn(Optional.of(response));

        final Optional<MergeRequest> found = sut.mergeRequest(7L);

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.orElseThrow().response()).isSameAs(response);
            softly.assertThat(found.orElseThrow().project()).isSameAs(sut);
        });
    }

    @Test
    void mergeRequest_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.mergerequests().project().get(ID, 7L)).thenReturn(Optional.empty());

        final Optional<MergeRequest> found = sut.mergeRequest(7L);

        assertThat(found).isEmpty();
    }

    @Test
    void mergeRequest_shouldPassNumericProjectIdUnchanged_whenProjectIsNumeric() throws IOException {
        final GitLab other = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(other.mergerequests().project().get("42", 3L)).thenReturn(Optional.of(mergeRequest(3L, "Fix", "opened")));

        final Optional<MergeRequest> found = GitLabSdk.of(other).project("42").mergeRequest(3L);

        assertThat(found).isPresent();
    }

    @Test
    void mergeRequests_shouldLoadEachItem_whenListReturnsBasicRecords() throws IOException {
        when(gitLab.mergerequests().project().list(eq(ID), any(GitLabProjectMergeRequestsListFilter.class)).stream())
                .thenReturn(Stream.of(basic(1L), basic(2L)));
        when(gitLab.mergerequests().project().get(ID, 1L)).thenReturn(Optional.of(mergeRequest(1L, "One", "opened")));
        when(gitLab.mergerequests().project().get(ID, 2L)).thenReturn(Optional.of(mergeRequest(2L, "Two", "merged")));

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(MergeRequest::title).containsExactly("One", "Two");
            softly.assertThat(result)
                    .extracting(MergeRequest::state)
                    .containsExactly(MergeRequestState.OPENED, MergeRequestState.MERGED);
            softly.assertThat(result)
                    .allSatisfy(m -> softly.assertThat(m.project()).isSameAs(sut));
        });
    }

    @Test
    void mergeRequests_shouldSkipItem_whenGoneBeforeLoaded() throws IOException {
        final GitLabProjectMergeRequestsListFilter filter = GitLabProjectMergeRequestsListFilter.empty();
        when(gitLab.mergerequests().project().list(ID, filter).stream())
                .thenReturn(Stream.of(basic(1L), basic(2L), basic(3L)));
        when(gitLab.mergerequests().project().get(ID, 1L)).thenReturn(Optional.of(mergeRequest(1L, "One", "opened")));
        when(gitLab.mergerequests().project().get(ID, 2L)).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get(ID, 3L)).thenReturn(Optional.of(mergeRequest(3L, "Three", "closed")));

        final List<MergeRequest> result = sut.mergeRequests(filter).toList();

        assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 3L);
    }

    @Test
    void mergeRequests_shouldBeLazy_andLoadOnlyConsumedItems() throws IOException {
        final GitLabProjectMergeRequestsListFilter filter = GitLabProjectMergeRequestsListFilter.empty();
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.mergerequests().project().list(ID, filter).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(iid -> {
                    consumed.incrementAndGet();
                    return basic(iid);
                }));
        when(gitLab.mergerequests().project().get(ID, 1L)).thenReturn(Optional.of(mergeRequest(1L, "One", "opened")));

        final Stream<MergeRequest> stream = sut.mergeRequests(filter);

        assertThat(consumed).hasValue(0);
        assertThat(stream.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
        verify(gitLab.mergerequests().project(), never()).get(ID, 2L);
    }

    @Test
    void mergeRequests_shouldReturnEmptyStream_whenProjectHasNoMergeRequests() throws IOException {
        when(gitLab.mergerequests().project().list(eq(ID), any(GitLabProjectMergeRequestsListFilter.class)).stream())
                .thenReturn(Stream.empty());

        final List<MergeRequest> result = sut.mergeRequests().toList();

        assertThat(result).isEmpty();
        verify(gitLab.mergerequests().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void createMergeRequest_shouldSendPayload_whenPayloadIsGiven() throws IOException {
        final GitLabProjectMergeRequestsCreatePayload payload = GitLabProjectMergeRequestsCreatePayloadBuilder.builder()
                .title("New")
                .sourceBranch("feature")
                .targetBranch("main")
                .build();
        final GitLabMergeRequest created = mergeRequest(9L, "New", "opened");
        when(gitLab.mergerequests().project().create(ID, payload)).thenReturn(created);

        final MergeRequest result = sut.createMergeRequest(payload);

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.project()).isSameAs(sut);
        });
    }
}
