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
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsShareGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsSharePayload;
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabGroupMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupMembersTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    @Test
    void members_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.members().group().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return member(id, "u" + id, 30);
                }));

        final Stream<GroupMember> members = sut.members();

        assertThat(consumed).hasValue(0);
        assertThat(members.limit(1).toList())
                .singleElement()
                .satisfies(m -> assertThat(m.userId()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.members().group(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void allMembers_shouldListInheritedMembers_whenCalled() throws IOException {
        when(gitLab.members().group().listAll(ID).stream()).thenReturn(Stream.of(member(9L, "inherited", 50)));

        assertThat(sut.allMembers().toList())
                .singleElement()
                .satisfies(m -> assertThat(m.username()).isEqualTo("inherited"));
    }

    @Test
    void member_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.members().group().get(ID, 5L)).thenReturn(Optional.empty());

        assertThat(sut.member(5L)).isEmpty();
    }

    @Test
    void addMember_shouldSendUserIdAsString_andLevelValue() throws IOException {
        when(gitLab.members().group().create(eq(ID), any(GitLabGroupMembersCreatePayload.class)))
                .thenReturn(member(5L, "ann", 30));

        final GroupMember result = sut.addMember(5L, AccessLevel.DEVELOPER);

        final ArgumentCaptor<GitLabGroupMembersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupMembersCreatePayload.class);
        verify(gitLab.members().group()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().userId()).isEqualTo("5");
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(30);
            softly.assertThat(payload.getValue().username()).isNull();
            softly.assertThat(result.userId()).isEqualTo(5L);
        });
    }

    @Test
    void addMember_shouldSendUsername_whenGivenUsername() throws IOException {
        when(gitLab.members().group().create(eq(ID), any(GitLabGroupMembersCreatePayload.class)))
                .thenReturn(member(5L, "ann", 20));

        sut.addMember("ann", AccessLevel.REPORTER);

        final ArgumentCaptor<GitLabGroupMembersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupMembersCreatePayload.class);
        verify(gitLab.members().group()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().username()).isEqualTo("ann");
            softly.assertThat(payload.getValue().userId()).isNull();
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(20);
        });
    }

    @Test
    void requestAccess_shouldReturnAccessRequest_whenCalled() throws IOException {
        when(gitLab.accessrequests().group().create(ID)).thenReturn(requester(5L, "me"));

        assertThat(sut.requestAccess().username()).isEqualTo("me");
    }

    @Test
    void accessRequests_shouldListRequesters_whenCalled() throws IOException {
        when(gitLab.accessrequests().group().list(ID).stream()).thenReturn(Stream.of(requester(5L, "ann")));

        assertThat(sut.accessRequests().toList())
                .singleElement()
                .satisfies(r -> assertThat(r.userId()).isEqualTo(5L));
    }

    @Test
    void share_shouldSendGroupAccess_andBuildGroupFromAnswer_whenCalled() throws IOException {
        when(gitLab.groups().share(eq(ID), any(GitLabGroupGroupsSharePayload.class)))
                .thenReturn(GitLabGroupDetailBuilder.builder()
                        .id(7L)
                        .path("my-group")
                        .build());

        final Group result = sut.share(9L, AccessLevel.MAINTAINER);

        final ArgumentCaptor<GitLabGroupGroupsSharePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupsSharePayload.class);
        verify(gitLab.groups()).share(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().groupId()).isEqualTo(9L);
            softly.assertThat(payload.getValue().groupAccess()).isEqualTo(GitLabGroupGroupsShareGroupAccess.N_40);
            softly.assertThat(result.id()).isEqualTo(ID);
            softly.assertThat(result.response().id()).isEqualTo(7L);
        });
        verify(gitLab.groups(), never()).get(any(String.class));
    }
}
