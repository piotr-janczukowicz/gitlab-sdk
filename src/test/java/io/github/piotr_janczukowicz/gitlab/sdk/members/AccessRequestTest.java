package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.member;
import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.requester;
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
import io.github.piotr_janczukowicz.gitlab.api.accessrequests.GitLabGroupAccessRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.api.accessrequests.GitLabProjectAccessRequestsApprovePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class AccessRequestTest {

    private static final String PROJECT_ID = "42";
    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final AccessRequest projectRequest = new AccessRequest(sdk.project(PROJECT_ID), requester(5L, "ann"));
    private final GroupAccessRequest groupRequest = new GroupAccessRequest(sdk.group(GROUP_ID), requester(6L, "bob"));

    @Test
    void accessors_shouldReadResponse_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(projectRequest.userId()).isEqualTo(5L);
            softly.assertThat(projectRequest.username()).isEqualTo("ann");
            softly.assertThat(projectRequest.project().id()).isEqualTo(PROJECT_ID);
            softly.assertThat(groupRequest.userId()).isEqualTo(6L);
            softly.assertThat(groupRequest.username()).isEqualTo("bob");
            softly.assertThat(groupRequest.group().id()).isEqualTo(GROUP_ID);
        });
    }

    @Test
    void approve_shouldReturnMember_whenProjectRequestIsApproved() throws IOException {
        when(gitLab.accessrequests()
                        .project()
                        .approve(eq(PROJECT_ID), eq(5L), any(GitLabProjectAccessRequestsApprovePayload.class)))
                .thenReturn(member(5L, "ann", 30));

        final Member result = projectRequest.approve(AccessLevel.DEVELOPER);

        final ArgumentCaptor<GitLabProjectAccessRequestsApprovePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectAccessRequestsApprovePayload.class);
        verify(gitLab.accessrequests().project()).approve(eq(PROJECT_ID), eq(5L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(30);
            softly.assertThat(result.accessLevel()).isEqualTo(AccessLevel.DEVELOPER);
            softly.assertThat(result.userId()).isEqualTo(5L);
        });
        verify(gitLab.members().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void approve_shouldReturnGroupMember_whenGroupRequestIsApproved() throws IOException {
        when(gitLab.accessrequests()
                        .group()
                        .approve(eq(GROUP_ID), eq(6L), any(GitLabGroupAccessRequestsApprovePayload.class)))
                .thenReturn(member(6L, "bob", 20));

        final GroupMember result = groupRequest.approve(AccessLevel.REPORTER);

        final ArgumentCaptor<GitLabGroupAccessRequestsApprovePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupAccessRequestsApprovePayload.class);
        verify(gitLab.accessrequests().group()).approve(eq(GROUP_ID), eq(6L), payload.capture());
        assertThat(payload.getValue().accessLevel()).isEqualTo(20);
        assertThat(result.accessLevel()).isEqualTo(AccessLevel.REPORTER);
    }

    @Test
    void deny_shouldCallDelete_whenProjectRequestIsDenied() throws IOException {
        projectRequest.deny();

        verify(gitLab.accessrequests().project()).delete(PROJECT_ID, 5L);
    }

    @Test
    void deny_shouldCallDelete_whenGroupRequestIsDenied() throws IOException {
        groupRequest.deny();

        verify(gitLab.accessrequests().group()).delete(GROUP_ID, 6L);
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        assertSoftly(softly -> {
            softly.assertThat(projectRequest.response().username()).isEqualTo("ann");
            softly.assertThat(groupRequest.response().username()).isEqualTo("bob");
        });
    }
}
