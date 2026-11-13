package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.discussions.IssueDiscussion;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Note;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A discussion on a throwaway issue. GitLab resolves only resolvable threads, and a plain issue thread may not be one:
 * the resolve and unresolve steps run only when it is, otherwise the test asserts that it is not. Merge request
 * discussions are not run live.
 */
@Tag("smoke")
class DiscussionsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void discussion_shouldRoundTrip_whenStartedRepliedListedAndRefreshed() throws IOException {
        final Issue issue = ENV.sandbox().createIssue(ENV.name("discussion"));
        SmokeCleanup.run(
                () -> {
                    final IssueDiscussion started = issue.startDiscussion("smoke thread");
                    final Note reply = started.reply("smoke reply");
                    final List<IssueDiscussion> listed =
                            issue.discussions().limit(20).toList();
                    final IssueDiscussion refreshed = started.refresh();
                    final boolean resolvable = refreshed.isResolvable();
                    final IssueDiscussion resolved = resolvable ? refreshed.resolve() : refreshed;
                    final IssueDiscussion unresolved = resolvable ? resolved.unresolve() : refreshed;

                    assertSoftly(softly -> {
                        softly.assertThat(started.notes()).hasSize(1);
                        softly.assertThat(reply.body()).contains("smoke reply");
                        softly.assertThat(listed)
                                .extracting(IssueDiscussion::id)
                                .contains(started.id());
                        softly.assertThat(refreshed.notes()).hasSize(2);
                        if (resolvable) {
                            softly.assertThat(resolved.isResolved()).isTrue();
                            softly.assertThat(unresolved.isResolved()).isFalse();
                        } else {
                            softly.assertThat(refreshed.isResolvable()).isFalse();
                        }
                    });
                },
                issue::delete);
    }
}
