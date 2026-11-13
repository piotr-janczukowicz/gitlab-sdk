package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedAccess;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectedTag;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Protected branches and tags of the sandbox, on a throwaway branch name and a throwaway tag pattern. Needs the
 * Maintainer role. Group-level protected branches (Premium) are not run live.
 */
@Tag("smoke")
class ProtectionSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void protectedBranch_shouldRoundTrip_whenProtectedUpdatedAndUnprotected() throws IOException {
        final Project project = ENV.sandbox();
        final String name = "smoke-" + ENV.runId() + "/protected";
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        project.createBranch(name, defaultBranch);
        SmokeCleanup.run(
                () -> {
                    final ProtectedBranch protectedBranch =
                            project.protectBranch(name, ProtectedAccess.MAINTAINER, ProtectedAccess.MAINTAINER);
                    final ProtectedBranch forcePush = protectedBranch.allowForcePush(true);
                    final Optional<ProtectedBranch> loaded = project.protectedBranch(name);
                    final boolean listed =
                            project.protectedBranches().limit(100).anyMatch(branch -> name.equals(branch.name()));
                    forcePush.unprotect();
                    final Optional<ProtectedBranch> afterUnprotect = project.protectedBranch(name);

                    assertSoftly(softly -> {
                        softly.assertThat(protectedBranch.name()).isEqualTo(name);
                        softly.assertThat(protectedBranch.pushAccess()).containsExactly(ProtectedAccess.MAINTAINER);
                        softly.assertThat(protectedBranch.mergeAccess()).containsExactly(ProtectedAccess.MAINTAINER);
                        softly.assertThat(protectedBranch.allowsForcePush()).isFalse();
                        softly.assertThat(forcePush.allowsForcePush()).isTrue();
                        softly.assertThat(loaded)
                                .map(ProtectedBranch::allowsForcePush)
                                .contains(true);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(afterUnprotect).isEmpty();
                    });
                },
                () -> {
                    final Optional<ProtectedBranch> remaining = project.protectedBranch(name);
                    if (remaining.isPresent()) {
                        remaining.get().unprotect();
                    }
                },
                () -> {
                    final Optional<Branch> branch = project.branch(name);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void protectedTag_shouldRoundTrip_whenProtectedAndUnprotected() throws IOException {
        final Project project = ENV.sandbox();
        final String pattern = "smoke-" + ENV.runId() + "-*";
        final ProtectedTag created = project.protectTag(pattern, ProtectedAccess.MAINTAINER);
        SmokeCleanup.run(
                () -> {
                    final Optional<ProtectedTag> loaded = project.protectedTag(pattern);
                    final boolean listed =
                            project.protectedTags().limit(100).anyMatch(tag -> pattern.equals(tag.name()));
                    created.unprotect();
                    final Optional<ProtectedTag> afterUnprotect = project.protectedTag(pattern);

                    assertSoftly(softly -> {
                        softly.assertThat(created.name()).isEqualTo(pattern);
                        softly.assertThat(created.createAccess()).containsExactly(ProtectedAccess.MAINTAINER);
                        softly.assertThat(loaded).map(ProtectedTag::name).contains(pattern);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(afterUnprotect).isEmpty();
                    });
                },
                () -> {
                    final Optional<ProtectedTag> remaining = project.protectedTag(pattern);
                    if (remaining.isPresent()) {
                        remaining.get().unprotect();
                    }
                });
    }

    @Test
    void protection_shouldReturnEmpty_whenBranchOrTagIsNotProtected() throws IOException {
        final Project project = ENV.sandbox();

        final Optional<ProtectedBranch> branch = project.protectedBranch(ENV.name("missing"));
        final Optional<ProtectedTag> tag = project.protectedTag(ENV.name("missing"));

        assertThat(branch).isEmpty();
        assertThat(tag).isEmpty();
    }
}
