package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.GroupLabel;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Label;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Throwaway labels of the sandbox group, named by the optional {@code SMOKE_GITLAB_GROUP}; skipped when it is not set.
 * The promotion test needs the sandbox project to live in that group and is skipped otherwise.
 */
@Tag("smoke")
class GroupLabelsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireGroupConfigured();
    }

    @Test
    void groupLabel_shouldRoundTrip_whenCreatedUpdatedSubscribedAndDeleted() throws IOException {
        final Group group = ENV.sandboxGroup();
        final String name = ENV.name("group-label");
        final GroupLabel created = group.createLabel(name, "#428bca");
        SmokeCleanup.run(
                () -> {
                    final GroupLabel updated = created.update()
                            .description("smoke")
                            .color("#dc143c")
                            .send();
                    final GroupLabel subscribed = updated.subscribe();
                    final GroupLabel unsubscribed = subscribed.unsubscribe();
                    final boolean listed = group.labels()
                            .limit(100)
                            .anyMatch(each -> each.name().equals(name));
                    final Optional<GroupLabel> loaded = group.label(name);
                    unsubscribed.delete();
                    final Optional<GroupLabel> afterDelete = group.label(name);

                    assertSoftly(softly -> {
                        softly.assertThat(created.name()).isEqualTo(name);
                        softly.assertThat(updated.description()).contains("smoke");
                        softly.assertThat(subscribed.isSubscribed()).isTrue();
                        softly.assertThat(unsubscribed.isSubscribed()).isFalse();
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(loaded).map(GroupLabel::name).contains(name);
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<GroupLabel> remaining = group.label(name);
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }

    @Test
    void label_shouldBecomeAGroupLabel_whenPromoted() throws IOException {
        final Group group = ENV.sandboxGroup();
        final Project project = ENV.sandbox();
        assumeTrue(
                ENV.namespace().equals(ENV.sandboxGroupPath()),
                "the sandbox project does not live directly in the sandbox group");
        final String name = ENV.name("promoted-label");
        final Label label = project.createLabel(name, "#428bca");
        SmokeCleanup.run(
                () -> {
                    final GroupLabel promoted = label.promote();
                    final Optional<GroupLabel> loaded = group.label(name);

                    assertSoftly(softly -> {
                        softly.assertThat(promoted.name()).isEqualTo(name);
                        softly.assertThat(loaded).map(GroupLabel::name).contains(name);
                    });
                },
                () -> {
                    final Optional<GroupLabel> remaining = group.label(name);
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                },
                () -> {
                    final Optional<Label> leftover = project.label(name);
                    if (leftover.isPresent()) {
                        leftover.get().delete();
                    }
                });
    }
}
