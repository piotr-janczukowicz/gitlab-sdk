package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroup;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabInstanceProjectsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccess;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccessBuilder;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUsersListFilter;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUsersListFilterBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class GitLabSdkListsTest {

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sut = GitLabSdk.of(gitLab);

    private static GitLabProjectWithAccess listedProject(final long id) {
        return GitLabProjectWithAccessBuilder.builder().id(id).build();
    }

    private static GitLabGroup listedGroup(final long id) {
        return GitLabGroupBuilder.builder().id(id).build();
    }

    private static GitLabUserBasic listedUser(final long id) {
        return GitLabUserBasicBuilder.builder().id(id).build();
    }

    @Test
    void projects_shouldLoadEachListedProject_whenListed() throws IOException {
        when(gitLab.projects().project().list(any(GitLabProjectProjectsListFilter.class)).stream())
                .thenReturn(Stream.of(listedProject(1L), listedProject(2L)));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.of(loadedProject(1L, "one")));
        when(gitLab.projects().project().get("2")).thenReturn(Optional.of(loadedProject(2L, "two")));

        final List<Project> projects = sut.projects().toList();

        assertThat(projects).extracting(Project::id).containsExactly("1", "2");
    }

    @Test
    void projects_shouldSkipProjectGone_whenListedButNotLoadable() throws IOException {
        when(gitLab.projects().project().list(any(GitLabProjectProjectsListFilter.class)).stream())
                .thenReturn(Stream.of(listedProject(1L), listedProject(2L)));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.empty());
        when(gitLab.projects().project().get("2")).thenReturn(Optional.of(loadedProject(2L, "two")));

        final List<Project> projects = sut.projects().toList();

        assertThat(projects).extracting(Project::id).containsExactly("2");
    }

    @Test
    void projects_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projects().project().list(any(GitLabProjectProjectsListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return listedProject(id);
                }));
        when(gitLab.projects().project().get("1")).thenReturn(Optional.of(loadedProject(1L, "one")));

        final Stream<Project> projects = sut.projects();

        assertSoftly(softly -> {
            softly.assertThat(consumed).hasValue(0);
            softly.assertThat(projects.limit(1).toList()).hasSize(1);
            softly.assertThat(consumed).hasValue(1);
        });
        verify(gitLab.projects().project(), never()).get("2");
    }

    @Test
    void projects_shouldThrowIllegalStateException_whenStreamIsConsumedTwice() throws IOException {
        when(gitLab.projects().project().list(any(GitLabProjectProjectsListFilter.class)).stream())
                .thenReturn(Stream.empty());
        final Stream<Project> projects = sut.projects();
        projects.toList();

        final Throwable thrown = catchThrowable(projects::toList);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void projects_shouldPassFilterToCore_whenOwnedAndStarredAreSet() throws IOException {
        final GitLabProjectProjectsListFilter filter = GitLabProjectProjectsListFilterBuilder.builder()
                .owned(true)
                .starred(true)
                .membership(true)
                .build();
        when(gitLab.projects().project().list(filter).stream()).thenReturn(Stream.empty());

        final long count = sut.projects(filter).count();

        assertThat(count).isZero();
    }

    @Test
    void projects_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.projects(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void userProjects_shouldListByUserIdAndLoadEach_whenListed() throws IOException {
        final GitLabInstanceProjectsListFilter filter = GitLabInstanceProjectsListFilter.empty();
        when(gitLab.projects().instance().list("7", filter).stream()).thenReturn(Stream.of(listedProject(3L)));
        when(gitLab.projects().project().get("3")).thenReturn(Optional.of(loadedProject(3L, "three")));

        final List<Project> projects = sut.userProjects(7L, filter).toList();

        assertThat(projects).extracting(Project::id).containsExactly("3");
    }

    @Test
    void userProjects_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.userProjects(7L, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void starredProjects_shouldListByUserIdAndLoadEach_whenListed() throws IOException {
        when(gitLab.projects().instance().listUsersStarredProjects("7").stream())
                .thenReturn(Stream.of(listedProject(4L)));
        when(gitLab.projects().project().get("4")).thenReturn(Optional.of(loadedProject(4L, "four")));

        final List<Project> projects = sut.starredProjects(7L).toList();

        assertThat(projects).extracting(Project::id).containsExactly("4");
    }

    @Test
    void groups_shouldLoadEachListedGroupAndSkipGoneOnes_whenListed() throws IOException {
        when(gitLab.groups().list(any(GitLabGroupGroupsListFilter.class)).stream())
                .thenReturn(Stream.of(listedGroup(1L), listedGroup(2L)));
        when(gitLab.groups().get("1")).thenReturn(Optional.empty());
        when(gitLab.groups().get("2")).thenReturn(Optional.of(loadedGroup(2L, "two")));

        final List<Group> groups = sut.groups().toList();

        assertThat(groups).extracting(Group::id).containsExactly("2");
    }

    @Test
    void groups_shouldPassFilterToCore_whenFilterIsGiven() throws IOException {
        final GitLabGroupGroupsListFilter filter = GitLabGroupGroupsListFilter.empty();
        when(gitLab.groups().list(filter).stream()).thenReturn(Stream.empty());

        final List<Group> groups = sut.groups(filter).toList();

        assertThat(groups).isEmpty();
    }

    @Test
    void groups_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.groups(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void users_shouldLoadEachListedUserAndSkipGoneOnes_whenListed() throws IOException {
        when(gitLab.users().list(any(GitLabUsersListFilter.class)).stream())
                .thenReturn(Stream.of(listedUser(1L), listedUser(2L)));
        when(gitLab.users().get(1L)).thenReturn(Optional.empty());
        when(gitLab.users().get(2L)).thenReturn(Optional.of(profile(2L, "two")));

        final List<User> users = sut.users().toList();

        assertThat(users).extracting(User::username).containsExactly("two");
    }

    @Test
    void users_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.users().list(any(GitLabUsersListFilter.class)).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return listedUser(id);
                }));
        when(gitLab.users().get(1L)).thenReturn(Optional.of(profile(1L, "one")));

        final Stream<User> users = sut.users();

        assertSoftly(softly -> {
            softly.assertThat(consumed).hasValue(0);
            softly.assertThat(users.limit(1).toList()).hasSize(1);
        });
        verify(gitLab.users(), never()).get(2L);
    }

    @Test
    void users_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.users(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void userByUsername_shouldFilterByUsernameAndLoadTheFirstMatch_whenFound() throws IOException {
        final GitLabUsersListFilter filter =
                GitLabUsersListFilterBuilder.builder().username("tanuki").build();
        when(gitLab.users().list(filter).stream()).thenReturn(Stream.of(listedUser(5L)));
        when(gitLab.users().get(5L)).thenReturn(Optional.of(profile(5L, "tanuki")));

        final Optional<User> found = sut.userByUsername("tanuki");

        assertThat(found).map(User::id).contains(5L);
    }

    @Test
    void userByUsername_shouldReturnEmpty_whenNoUserMatches() throws IOException {
        when(gitLab.users().list(any(GitLabUsersListFilter.class)).stream()).thenReturn(Stream.empty());

        assertThat(sut.userByUsername("nobody")).isEmpty();
    }

    @Test
    void userByUsername_shouldThrowIllegalArgumentException_whenUsernameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.userByUsername(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userByUsername_shouldThrowNullPointerException_whenUsernameIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.userByUsername(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void user_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.users().get(9L)).thenReturn(Optional.empty());

        assertThat(sut.user(9L)).isEmpty();
    }
}
