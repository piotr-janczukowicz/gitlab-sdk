package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.member;
import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.requester;
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
import io.github.piotr_janczukowicz.gitlab.api.members.GitLabProjectMembersCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsShareGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsSharePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ProjectGroupLink;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectMembersTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void members_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.members().project().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return member(id, "u" + id, 30);
                }));

        final Stream<Member> members = sut.members();

        assertThat(consumed).hasValue(0);
        assertThat(members.limit(1).toList())
                .singleElement()
                .satisfies(m -> assertThat(m.userId()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.members().project(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void allMembers_shouldListInheritedMembers_whenCalled() throws IOException {
        when(gitLab.members().project().listAll(ID).stream()).thenReturn(Stream.of(member(9L, "inherited", 50)));

        final Stream<Member> members = sut.allMembers();

        assertThat(members.toList())
                .singleElement()
                .satisfies(m -> assertThat(m.username()).isEqualTo("inherited"));
    }

    @Test
    void member_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.members().project().get(ID, 5L)).thenReturn(Optional.empty());

        assertThat(sut.member(5L)).isEmpty();
    }

    @Test
    void member_shouldReturnMember_whenPresent() throws IOException {
        when(gitLab.members().project().get(ID, 5L)).thenReturn(Optional.of(member(5L, "ann", 30)));

        assertThat(sut.member(5L)).get().satisfies(m -> assertThat(m.username()).isEqualTo("ann"));
    }

    @Test
    void addMember_shouldSendUserIdAsString_andLevelValue() throws IOException {
        when(gitLab.members().project().create(eq(ID), any(GitLabProjectMembersCreatePayload.class)))
                .thenReturn(member(5L, "ann", 30));

        final Member result = sut.addMember(5L, AccessLevel.DEVELOPER);

        final ArgumentCaptor<GitLabProjectMembersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMembersCreatePayload.class);
        verify(gitLab.members().project()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().userId()).isEqualTo("5");
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(30);
            softly.assertThat(payload.getValue().username()).isNull();
            softly.assertThat(result.userId()).isEqualTo(5L);
        });
    }

    @Test
    void addMember_shouldSendUsername_whenGivenUsername() throws IOException {
        when(gitLab.members().project().create(eq(ID), any(GitLabProjectMembersCreatePayload.class)))
                .thenReturn(member(5L, "ann", 20));

        sut.addMember("ann", AccessLevel.REPORTER);

        final ArgumentCaptor<GitLabProjectMembersCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectMembersCreatePayload.class);
        verify(gitLab.members().project()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().username()).isEqualTo("ann");
            softly.assertThat(payload.getValue().userId()).isNull();
            softly.assertThat(payload.getValue().accessLevel()).isEqualTo(20);
        });
    }

    @Test
    void addMember_shouldThrowIllegalArgumentException_whenUsernameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.addMember(" ", AccessLevel.GUEST));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addMember_shouldThrowNullPointerException_whenLevelIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.addMember(5L, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void requestAccess_shouldReturnAccessRequest_whenCalled() throws IOException {
        when(gitLab.accessrequests().project().create(ID)).thenReturn(requester(5L, "me"));

        final AccessRequest result = sut.requestAccess();

        assertThat(result.username()).isEqualTo("me");
    }

    @Test
    void accessRequests_shouldListRequesters_whenCalled() throws IOException {
        when(gitLab.accessrequests().project().list(ID).stream()).thenReturn(Stream.of(requester(5L, "ann")));

        assertThat(sut.accessRequests().toList())
                .singleElement()
                .satisfies(r -> assertThat(r.userId()).isEqualTo(5L));
    }

    @Test
    void share_shouldSendGroupAccess_whenCalled() throws IOException {
        when(gitLab.projects().project().share(eq(ID), any(GitLabProjectProjectsSharePayload.class)))
                .thenReturn(GitLabProjectGroupLinkBuilder.builder()
                        .groupId(9L)
                        .groupAccess(30)
                        .build());

        final ProjectGroupLink link = sut.share(9L, AccessLevel.DEVELOPER);

        final ArgumentCaptor<GitLabProjectProjectsSharePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsSharePayload.class);
        verify(gitLab.projects().project()).share(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().groupId()).isEqualTo(9L);
            softly.assertThat(payload.getValue().groupAccess()).isEqualTo(GitLabProjectProjectsShareGroupAccess.N_30);
            softly.assertThat(link.groupId()).isEqualTo(9L);
            softly.assertThat(link.project()).isSameAs(sut);
        });
    }

    @Test
    void share_shouldThrowIllegalArgumentException_whenLevelIsMinimalAccess() {
        final Throwable thrown = catchThrowable(() -> sut.share(9L, AccessLevel.MINIMAL_ACCESS));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MINIMAL_ACCESS");
        verifyNoInteractions(gitLab);
    }
}
