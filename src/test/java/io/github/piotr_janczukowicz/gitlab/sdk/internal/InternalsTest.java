package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMember;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMemberBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupMember;
import io.github.piotr_janczukowicz.gitlab.sdk.members.Member;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

/**
 * Every first-registration test builds the real object before it registers a fake, so the class of that object has
 * already registered its own access and the fake can never win for the rest of the test run.
 */
@UnitTest
class InternalsTest {

    private static final GitLabMember MEMBER =
            GitLabMemberBuilder.builder().id(7L).username("ann").accessLevel(30).build();

    @Test
    void gitLab_shouldReturnTheClientOfTheSdk_whenTheSdkIsCreated() {
        final GitLab gitlab = mock(GitLab.class);
        final GitLabSdk sdk = GitLabSdk.of(gitlab);

        final GitLab result = Internals.gitLab(sdk);

        assertThat(result).isSameAs(gitlab);
    }

    @Test
    void registerSdkAccess_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater() {
        final GitLab gitlab = mock(GitLab.class);
        final GitLab other = mock(GitLab.class);
        final GitLabSdk sdk = GitLabSdk.of(gitlab);

        Internals.registerSdkAccess(ignored -> other);

        assertThat(Internals.gitLab(sdk)).isSameAs(gitlab);
    }

    @Test
    void sdk_shouldReturnTheSdkOfTheProject_whenProjectIsGiven() {
        final GitLab gitlab = mock(GitLab.class);
        final GitLabSdk sdk = GitLabSdk.of(gitlab);
        final Project project = sdk.project("my-group/my-project");

        assertSoftly(softly -> {
            softly.assertThat(Internals.sdk(project)).isSameAs(sdk);
            softly.assertThat(Internals.gitLab(project)).isSameAs(gitlab);
        });
    }

    @Test
    void registerProjectAccess_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater() {
        final GitLabSdk sdk = GitLabSdk.of(mock(GitLab.class));
        final GitLabSdk other = GitLabSdk.of(mock(GitLab.class));
        final Project project = sdk.project("my-group/my-project");

        Internals.registerProjectAccess(ignored -> other);

        assertThat(Internals.sdk(project)).isSameAs(sdk);
    }

    @Test
    void sdk_shouldReturnTheSdkOfTheGroup_whenGroupIsGiven() {
        final GitLab gitlab = mock(GitLab.class);
        final GitLabSdk sdk = GitLabSdk.of(gitlab);
        final Group group = sdk.group("my-group");

        assertSoftly(softly -> {
            softly.assertThat(Internals.sdk(group)).isSameAs(sdk);
            softly.assertThat(Internals.gitLab(group)).isSameAs(gitlab);
        });
    }

    @Test
    void registerGroupAccess_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater() {
        final GitLabSdk sdk = GitLabSdk.of(mock(GitLab.class));
        final GitLabSdk other = GitLabSdk.of(mock(GitLab.class));
        final Group group = sdk.group("my-group");

        Internals.registerGroupAccess(ignored -> other);

        assertThat(Internals.sdk(group)).isSameAs(sdk);
    }

    @Test
    void registerAllMembersAccess_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater() {
        final Project project = GitLabSdk.of(mock(GitLab.class)).project("my-group/my-project");
        final Member member = new Member(project, MEMBER);
        final Member sentinel = new Member(project, MEMBER);

        Internals.registerAllMembersAccess(ignored -> sentinel);
        final Member result = Internals.fromAllMembers(member);

        assertSoftly(softly -> {
            softly.assertThat(result).isNotSameAs(sentinel);
            softly.assertThat(result).isNotSameAs(member);
            softly.assertThat(result.response()).isSameAs(MEMBER);
        });
    }

    @Test
    void registerAllGroupMembersAccess_shouldKeepTheFirstAccess_whenAnotherOneIsRegisteredLater() {
        final Group group = GitLabSdk.of(mock(GitLab.class)).group("my-group");
        final GroupMember member = new GroupMember(group, MEMBER);
        final GroupMember sentinel = new GroupMember(group, MEMBER);

        Internals.registerAllGroupMembersAccess(ignored -> sentinel);
        final GroupMember result = Internals.fromAllGroupMembers(member);

        assertSoftly(softly -> {
            softly.assertThat(result).isNotSameAs(sentinel);
            softly.assertThat(result).isNotSameAs(member);
            softly.assertThat(result.response()).isSameAs(MEMBER);
        });
    }

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenCalled() throws ReflectiveOperationException {
        final Constructor<Internals> constructor = Internals.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
