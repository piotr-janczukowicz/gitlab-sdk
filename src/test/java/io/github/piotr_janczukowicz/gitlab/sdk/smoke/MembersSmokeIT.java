package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.members.AccessLevel;
import io.github.piotr_janczukowicz.gitlab.sdk.members.Member;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads the members of the sandbox. Adding, changing and removing a member needs a second user, named by the optional
 * {@code SMOKE_GITLAB_USER_ID}; that part is skipped without it. Requesting access and approving or denying a request
 * are not run live.
 */
@Tag("smoke")
class MembersSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void members_shouldListAndLoadAMember_whenTheSandboxHasMembers() throws IOException {
        final Project project = ENV.sandbox();
        final long signedIn = ENV.sdk().me().profile().id();

        final boolean signedInListed = project.allMembers().limit(200).anyMatch(member -> member.userId() == signedIn);
        final Optional<Member> first = project.members().limit(50).findFirst();
        assumeTrue(first.isPresent(), "the sandbox has no direct members");
        final Optional<Member> loaded = project.member(first.orElseThrow().userId());
        final Member refreshed = loaded.orElseThrow().refresh();

        assertSoftly(softly -> {
            softly.assertThat(signedInListed).isTrue();
            softly.assertThat(loaded)
                    .map(Member::userId)
                    .contains(first.orElseThrow().userId());
            softly.assertThat(refreshed.accessLevelValue()).isPositive();
            softly.assertThat(refreshed.username()).isNotBlank();
        });
    }

    @Test
    void member_shouldReturnEmpty_whenUserIsNotAMember() throws IOException {
        final Optional<Member> missing = ENV.sandbox().member(Long.MAX_VALUE);

        assertThat(missing).isEmpty();
    }

    @Test
    void member_shouldRoundTrip_whenAddedChangedAndRemoved() throws IOException {
        final long userId = ENV.secondUserId().orElse(0L);
        assumeTrue(userId > 0, SmokeEnvironment.USER_ID_VARIABLE + " is not set");
        final Project project = ENV.sandbox();
        assumeTrue(project.member(userId).isEmpty(), "the second user is already a direct member of the sandbox");
        final Member added = project.addMember(userId, AccessLevel.DEVELOPER);
        SmokeCleanup.run(
                () -> {
                    final Member changed =
                            added.update().accessLevel(AccessLevel.REPORTER).send();
                    final Optional<Member> reloaded = project.member(userId);
                    changed.remove();
                    final Optional<Member> afterRemove = project.member(userId);

                    assertSoftly(softly -> {
                        softly.assertThat(added.userId()).isEqualTo(userId);
                        softly.assertThat(added.accessLevel()).isEqualTo(AccessLevel.DEVELOPER);
                        softly.assertThat(changed.accessLevel()).isEqualTo(AccessLevel.REPORTER);
                        softly.assertThat(reloaded).map(Member::accessLevel).contains(AccessLevel.REPORTER);
                        softly.assertThat(afterRemove).isEmpty();
                    });
                },
                () -> {
                    final Optional<Member> member = project.member(userId);
                    if (member.isPresent()) {
                        member.get().remove();
                    }
                });
    }
}
