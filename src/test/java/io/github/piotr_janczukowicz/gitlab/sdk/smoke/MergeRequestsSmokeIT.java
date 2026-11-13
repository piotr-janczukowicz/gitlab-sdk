package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabProjectMergeRequestsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestNote;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequestState;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Merging and approving are not run live: the sandbox settings may forbid them, and a merge cannot be undone. */
@Tag("smoke")
class MergeRequestsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void mergeRequest_shouldRoundTrip_whenCreatedCommentedUpdatedClosedAndReopened() throws IOException {
        final Project project = ENV.sandbox();
        final String target = project.fetch().orElseThrow().response().defaultBranch();
        final String source = ENV.name("mr");
        project.createBranch(source, target);
        SmokeCleanup.run(
                () -> {
                    project.createFile(ENV.name("mr") + ".txt", source, "smoke", "smoke: change for a merge request");
                    final MergeRequest merge =
                            project.createMergeRequest(GitLabProjectMergeRequestsCreatePayloadBuilder.builder()
                                    .title(ENV.name("merge-request"))
                                    .sourceBranch(source)
                                    .targetBranch(target)
                                    .build());
                    SmokeCleanup.run(
                            () -> {
                                final MergeRequestNote note = merge.comment("smoke comment");
                                final MergeRequestNote edited = note.update("smoke comment, edited");
                                final List<MergeRequestNote> notes =
                                        merge.notes().limit(20).toList();
                                final MergeRequest updated = merge.update()
                                        .title(ENV.name("merge-request-updated"))
                                        .description("changed by a smoke test")
                                        .send();
                                final MergeRequest closed = updated.close();
                                final MergeRequest reopened = closed.reopen();
                                final Optional<MergeRequest> reloaded = project.mergeRequest(merge.iid());
                                final long commits = reopened.commits().limit(5).count();

                                assertSoftly(softly -> {
                                    softly.assertThat(merge.sourceBranch()).contains(source);
                                    softly.assertThat(merge.targetBranch()).contains(target);
                                    softly.assertThat(notes)
                                            .extracting(MergeRequestNote::id)
                                            .contains(note.id());
                                    softly.assertThat(edited.body()).contains("smoke comment, edited");
                                    softly.assertThat(updated.title()).isEqualTo(ENV.name("merge-request-updated"));
                                    softly.assertThat(closed.state()).isEqualTo(MergeRequestState.CLOSED);
                                    softly.assertThat(reopened.state()).isEqualTo(MergeRequestState.OPENED);
                                    softly.assertThat(reloaded)
                                            .map(MergeRequest::iid)
                                            .contains(merge.iid());
                                    softly.assertThat(commits).isGreaterThanOrEqualTo(1);
                                });
                            },
                            merge::delete);
                },
                () -> {
                    final Optional<Branch> branch = project.branch(source);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void mergeRequest_shouldReturnEmpty_whenIidDoesNotExist() throws IOException {
        final Optional<MergeRequest> missing = ENV.sandbox().mergeRequest(Integer.MAX_VALUE);

        assertThat(missing).isEmpty();
    }
}
