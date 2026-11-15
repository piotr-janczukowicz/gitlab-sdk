package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedGroup;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GroupListsTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    @Test
    void descendantGroups_shouldLoadEachListedGroupAndSkipGoneOnes_whenListed() throws IOException {
        when(gitLab.groups().listDescendantGroups(ID).stream()).thenReturn(Stream.of(listedGroup(1L), listedGroup(2L)));
        when(gitLab.groups().get("1")).thenReturn(Optional.empty());
        when(gitLab.groups().get("2")).thenReturn(Optional.of(loadedGroup(2L, "two")));

        final List<Group> groups = sut.descendantGroups().toList();

        assertThat(groups).extracting(Group::id).containsExactly("2");
    }

    @Test
    void descendantGroups_shouldBeLazy_whenStreamIsLimited() throws IOException {
        when(gitLab.groups().listDescendantGroups(ID).stream()).thenReturn(Stream.of(listedGroup(1L), listedGroup(2L)));
        when(gitLab.groups().get("1")).thenReturn(Optional.of(loadedGroup(1L, "one")));

        final List<Group> groups = sut.descendantGroups().limit(1).toList();

        assertThat(groups).hasSize(1);
        verify(gitLab.groups(), never()).get("2");
    }

    @Test
    void invitedToGroups_shouldLoadEachListedGroup_whenListed() throws IOException {
        when(gitLab.groups().listShared(ID).stream()).thenReturn(Stream.of(listedGroup(3L)));
        when(gitLab.groups().get("3")).thenReturn(Optional.of(loadedGroup(3L, "three")));

        final List<Group> groups = sut.invitedToGroups().toList();

        assertThat(groups)
                .singleElement()
                .satisfies(group -> assertThat(group.response().path()).isEqualTo("three"));
        verify(gitLab.groups(), never()).listInvitedGroups(ID);
    }

    @Test
    void invitedGroups_shouldLoadEachListedGroup_whenListed() throws IOException {
        when(gitLab.groups().listInvitedGroups(ID).stream()).thenReturn(Stream.of(listedGroup(4L)));
        when(gitLab.groups().get("4")).thenReturn(Optional.of(loadedGroup(4L, "four")));

        final List<Group> groups = sut.invitedGroups().toList();

        assertThat(groups)
                .singleElement()
                .satisfies(group -> assertThat(group.response().path()).isEqualTo("four"));
        verify(gitLab.groups(), never()).listShared(ID);
    }

    @Test
    void invitedGroups_shouldSkipAGroupThatIsGone_whenListed() throws IOException {
        when(gitLab.groups().listInvitedGroups(ID).stream()).thenReturn(Stream.of(listedGroup(4L), listedGroup(5L)));
        when(gitLab.groups().get("4")).thenReturn(Optional.empty());
        when(gitLab.groups().get("5")).thenReturn(Optional.of(loadedGroup(5L, "five")));

        final List<Group> groups = sut.invitedGroups().toList();

        assertThat(groups).extracting(group -> group.response().path()).containsExactly("five");
    }
}
