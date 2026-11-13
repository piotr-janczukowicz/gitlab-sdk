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
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MemberUpdateTest {

    private static final String PROJECT_ID = "42";
    private static final String GROUP_ID = "my-group";
    private static final Instant START_OF_DAY = Instant.parse("2027-03-01T00:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2027, 3, 1);

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
            softly.assertThat(payload.getValue().expiresAt()).isEqualTo(OptionalNull.of(START_OF_DAY));
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
            softly.assertThat(payload.getValue().expiresAt()).isEqualTo(OptionalNull.of(START_OF_DAY));
            softly.assertThat(payload.getValue().memberRoleId()).isNull();
            softly.assertThat(result.accessLevel()).isEqualTo(AccessLevel.REPORTER);
        });
        verify(gitLab.members().group(), never()).get(any(String.class), any(Long.class));
    }

    private GitLabProjectMembersUpdatePayload sendProject(final java.util.function.UnaryOperator<MemberUpdate> change)
            throws IOException {
        when(gitLab.members().project().update(eq(PROJECT_ID), eq(5L), any(GitLabProjectMembersUpdatePayload.class)))
                .thenReturn(member(5L, "ann", 30));
        change.apply(projectMember.update()).send();
        final ArgumentCaptor<GitLabProjectMembersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMembersUpdatePayload.class);
        verify(gitLab.members().project()).update(eq(PROJECT_ID), eq(5L), payload.capture());
        return payload.getValue();
    }

    private GitLabGroupMembersUpdatePayload sendGroup(final java.util.function.UnaryOperator<GroupMemberUpdate> change)
            throws IOException {
        when(gitLab.members().group().update(eq(GROUP_ID), eq(5L), any(GitLabGroupMembersUpdatePayload.class)))
                .thenReturn(member(5L, "ann", 30));
        change.apply(groupMember.update()).send();
        final ArgumentCaptor<GitLabGroupMembersUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupMembersUpdatePayload.class);
        verify(gitLab.members().group()).update(eq(GROUP_ID), eq(5L), payload.capture());
        return payload.getValue();
    }

    private static String json(final Object payload) {
        return new String(GitLabJsonCodecs.defaults().encode(payload), StandardCharsets.UTF_8);
    }

    @Test
    void clearExpiresAt_shouldSendJsonNull_whenCalled() throws IOException {
        final GitLabProjectMembersUpdatePayload payload = sendProject(MemberUpdate::clearExpiresAt);

        assertThat(payload.expiresAt()).isEqualTo(OptionalNull.ofNull());
    }

    @Test
    void clearExpiresAt_shouldSendJsonNull_whenCalledOnGroupMember() throws IOException {
        final GitLabGroupMembersUpdatePayload payload = sendGroup(GroupMemberUpdate::clearExpiresAt);

        assertThat(payload.expiresAt()).isEqualTo(OptionalNull.ofNull());
    }

    @Test
    void expiresAt_shouldSendValue_whenDateIsSet() throws IOException {
        final GitLabProjectMembersUpdatePayload project = sendProject(u -> u.expiresAt(DATE));

        assertThat(project.expiresAt()).isEqualTo(OptionalNull.of(START_OF_DAY));
    }

    @Test
    void expiresAt_shouldSendValue_whenDateIsSetOnGroupMember() throws IOException {
        final GitLabGroupMembersUpdatePayload group = sendGroup(u -> u.expiresAt(DATE));

        assertThat(group.expiresAt()).isEqualTo(OptionalNull.of(START_OF_DAY));
    }

    @Test
    void send_shouldNotSendExpiry_whenOnlyAccessLevelIsSet() throws IOException {
        final GitLabProjectMembersUpdatePayload payload = sendProject(u -> u.accessLevel(AccessLevel.REPORTER));

        assertThat(payload.expiresAt()).isNull();
    }

    @Test
    void send_shouldNotSendExpiry_whenOnlyAccessLevelIsSetOnGroupMember() throws IOException {
        final GitLabGroupMembersUpdatePayload payload = sendGroup(u -> u.accessLevel(AccessLevel.REPORTER));

        assertThat(payload.expiresAt()).isNull();
    }

    @Test
    void clearExpiresAt_shouldMarkUpdate_soSendIsNotEmpty() throws IOException {
        final GitLabProjectMembersUpdatePayload project = sendProject(MemberUpdate::clearExpiresAt);
        final GitLabGroupMembersUpdatePayload group = sendGroup(GroupMemberUpdate::clearExpiresAt);

        assertSoftly(softly -> {
            softly.assertThat(project).isNotNull();
            softly.assertThat(group).isNotNull();
        });
    }

    @Test
    void expiresAt_shouldWin_whenCalledAfterClearExpiresAt() throws IOException {
        final GitLabProjectMembersUpdatePayload project =
                sendProject(u -> u.clearExpiresAt().expiresAt(DATE));
        final GitLabGroupMembersUpdatePayload group =
                sendGroup(u -> u.clearExpiresAt().expiresAt(DATE));

        assertSoftly(softly -> {
            softly.assertThat(project.expiresAt()).isEqualTo(OptionalNull.of(START_OF_DAY));
            softly.assertThat(group.expiresAt()).isEqualTo(OptionalNull.of(START_OF_DAY));
        });
    }

    @Test
    void clearExpiresAt_shouldWin_whenCalledAfterExpiresAt() throws IOException {
        final GitLabProjectMembersUpdatePayload project =
                sendProject(u -> u.expiresAt(DATE).clearExpiresAt());
        final GitLabGroupMembersUpdatePayload group =
                sendGroup(u -> u.expiresAt(DATE).clearExpiresAt());

        assertSoftly(softly -> {
            softly.assertThat(project.expiresAt()).isEqualTo(OptionalNull.ofNull());
            softly.assertThat(group.expiresAt()).isEqualTo(OptionalNull.ofNull());
        });
    }

    @Test
    void wire_shouldContainJsonNull_whenExpiryIsCleared() throws IOException {
        final String project = json(sendProject(MemberUpdate::clearExpiresAt));
        final String group = json(sendGroup(GroupMemberUpdate::clearExpiresAt));

        assertSoftly(softly -> {
            softly.assertThat(project).contains("\"expires_at\":null");
            softly.assertThat(group).contains("\"expires_at\":null");
        });
    }

    @Test
    void wire_shouldOmitExpiry_whenOnlyAccessLevelIsSet() throws IOException {
        final String project = json(sendProject(u -> u.accessLevel(AccessLevel.REPORTER)));
        final String group = json(sendGroup(u -> u.accessLevel(AccessLevel.REPORTER)));

        assertSoftly(softly -> {
            softly.assertThat(project).doesNotContain("expires_at").contains("\"access_level\":20");
            softly.assertThat(group).doesNotContain("expires_at").contains("\"access_level\":20");
        });
    }

    @Test
    void wire_shouldContainIsoValue_whenExpiryIsSet() throws IOException {
        final String project = json(sendProject(u -> u.expiresAt(DATE)));
        final String group = json(sendGroup(u -> u.expiresAt(DATE)));

        assertSoftly(softly -> {
            softly.assertThat(project).contains("\"expires_at\":\"2027-03-01T00:00:00Z\"");
            softly.assertThat(group).contains("\"expires_at\":\"2027-03-01T00:00:00Z\"");
        });
    }
}
