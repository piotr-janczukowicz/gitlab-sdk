package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MemberUpdateTest {

    private static final String PROJECT_ID = "42";
    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final Member projectMember = new Member(sdk.project(PROJECT_ID), member(5L, "ann", 30));
    private final GroupMember groupMember = new GroupMember(sdk.group(GROUP_ID), member(5L, "ann", 30));

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final MemberUpdate update = projectMember.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void send_shouldSendOnlySetFields_whenOnlyLevelIsSet() throws IOException {
        when(gitLab.members().project().update(eq(PROJECT_ID), eq(5L), any(GitLabProjectMembersUpdatePayload.class)))
                .thenReturn(member(5L, "ann", 40));

        final Member result =
                projectMember.update().accessLevel(AccessLevel.MAINTAINER).send();

        final ArgumentCaptor<GitLabProjectMembersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMembersUpdatePayload.class);
        verify(gitLab.members().project()).update(eq(PROJECT_ID), eq(5L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(40);
            softly.assertThat(payload.getValue().expiresAt()).isNull();
            softly.assertThat(payload.getValue().memberRoleId()).isNull();
            softly.assertThat(result.accessLevel()).isEqualTo(AccessLevel.MAINTAINER);
        });
        verify(gitLab.members().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void send_shouldSendStartOfDayUtc_whenExpiresAtIsSet() throws IOException {
        when(gitLab.members().project().update(eq(PROJECT_ID), eq(5L), any(GitLabProjectMembersUpdatePayload.class)))
                .thenReturn(member(5L, "ann", 30));

        projectMember.update().expiresAt(LocalDate.of(2027, 3, 1)).send();

        final ArgumentCaptor<GitLabProjectMembersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMembersUpdatePayload.class);
        verify(gitLab.members().project()).update(eq(PROJECT_ID), eq(5L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().expiresAt()).isEqualTo(Instant.parse("2027-03-01T00:00:00Z"));
            softly.assertThat(payload.getValue().accessLevel()).isNull();
        });
    }

    @Test
    void accessLevel_shouldThrowNullPointerException_whenLevelIsNull() {
        final MemberUpdate update = projectMember.update();

        final Throwable thrown = catchThrowable(() -> update.accessLevel(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSetOnGroupMember() {
        final GroupMemberUpdate update = groupMember.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void send_shouldSendOnlySetFields_whenGroupMemberLevelAndExpiryAreSet() throws IOException {
        when(gitLab.members().group().update(eq(GROUP_ID), eq(5L), any(GitLabGroupMembersUpdatePayload.class)))
                .thenReturn(member(5L, "ann", 20));

        final GroupMember result = groupMember
                .update()
                .accessLevel(AccessLevel.REPORTER)
                .expiresAt(LocalDate.of(2027, 3, 1))
                .send();

        final ArgumentCaptor<GitLabGroupMembersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupMembersUpdatePayload.class);
        verify(gitLab.members().group()).update(eq(GROUP_ID), eq(5L), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(20);
            softly.assertThat(payload.getValue().expiresAt()).isEqualTo(Instant.parse("2027-03-01T00:00:00Z"));
            softly.assertThat(payload.getValue().memberRoleId()).isNull();
            softly.assertThat(result.accessLevel()).isEqualTo(AccessLevel.REPORTER);
        });
        verify(gitLab.members().group(), never()).get(any(String.class), any(Long.class));
    }
}
