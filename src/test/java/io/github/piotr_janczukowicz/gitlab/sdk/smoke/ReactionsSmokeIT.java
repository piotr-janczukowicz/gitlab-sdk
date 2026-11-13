package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.reactions.Reaction;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** An emoji reaction on a throwaway issue. Reactions on notes and merge requests are not run live. */
@Tag("smoke")
class ReactionsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void reaction_shouldRoundTrip_whenCreatedListedAndDeleted() throws IOException {
        final Issue issue = ENV.sandbox().createIssue(ENV.name("reaction"));
        SmokeCleanup.run(
                () -> {
                    final Reaction created = issue.react("thumbsup");
                    final List<Reaction> listed = issue.reactions().limit(20).toList();
                    created.delete();
                    final List<Reaction> afterDelete =
                            issue.reactions().limit(20).toList();

                    assertSoftly(softly -> {
                        softly.assertThat(created.name()).contains("thumbsup");
                        softly.assertThat(listed).extracting(Reaction::id).contains(created.id());
                        softly.assertThat(afterDelete).extracting(Reaction::id).doesNotContain(created.id());
                    });
                },
                issue::delete);
    }
}
