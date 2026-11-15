package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsCreateState;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabProjectCommitsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.code.CommitComment;
import io.github.piotr_janczukowicz.gitlab.sdk.code.CommitStatus;
import io.github.piotr_janczukowicz.gitlab.sdk.code.RepositoryFile;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.BlameRange;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.Comparison;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.TreeEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class CodeSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String NON_ASCII = "zażółć gęślą jaźń ✓";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void file_shouldRoundTripUtf8Text_whenCreatedUpdatedReadAndDeletedOnABranch() throws IOException {
        final Project project = ENV.sandbox();
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        final String branchName = ENV.name("files");
        final String path = ENV.name("file") + ".txt";
        project.createBranch(branchName, defaultBranch);
        SmokeCleanup.run(
                () -> {
                    final RepositoryFile created = project.createFile(path, branchName, NON_ASCII, "smoke: create");
                    final RepositoryFile updated = created.update(NON_ASCII + " 2", "smoke: update");
                    final Optional<RepositoryFile> read = project.file(path, branchName);
                    final Optional<RepositoryFile> missing = project.file(ENV.name("missing") + ".txt", branchName);
                    updated.delete("smoke: delete");
                    final Optional<RepositoryFile> afterDelete = project.file(path, branchName);

                    assertSoftly(softly -> {
                        softly.assertThat(created.text()).isEqualTo(NON_ASCII);
                        softly.assertThat(created.bytes()).isEqualTo(NON_ASCII.getBytes(StandardCharsets.UTF_8));
                        softly.assertThat(updated.text()).isEqualTo(NON_ASCII + " 2");
                        softly.assertThat(read).map(RepositoryFile::text).contains(NON_ASCII + " 2");
                        softly.assertThat(missing).isEmpty();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<Branch> branch = project.branch(branchName);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void branch_shouldRoundTrip_whenCreatedProtectedUnprotectedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        final Branch created = project.createBranch(ENV.name("branch"), defaultBranch);
        SmokeCleanup.run(
                () -> {
                    final Branch protectedBranch = created.protect();
                    final Branch unprotected = protectedBranch.unprotect();
                    final Optional<Branch> loaded = project.branch(created.name());

                    assertSoftly(softly -> {
                        softly.assertThat(created.isProtected()).isFalse();
                        softly.assertThat(protectedBranch.isProtected()).isTrue();
                        softly.assertThat(unprotected.isProtected()).isFalse();
                        softly.assertThat(loaded).map(Branch::name).contains(created.name());
                    });
                },
                () -> {
                    final Optional<Branch> branch = project.branch(created.name());
                    if (branch.isPresent() && branch.get().isProtected()) {
                        branch.get().unprotect();
                    }
                },
                () -> {
                    final Optional<Branch> branch = project.branch(created.name());
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void commits_shouldListAndLoad_whenSandboxHasCommits() throws IOException {
        final Project project = ENV.sandbox();

        final Optional<Commit> first = project.commits().findFirst();
        final Optional<Commit> loaded =
                first.isPresent() ? project.commit(first.get().sha()) : Optional.empty();

        assertThat(first).isPresent();
        assertThat(loaded).map(Commit::sha).isEqualTo(first.map(Commit::sha));
    }

    @Test
    void commit_shouldCreateStatusAndComment_whenAMultiFileCommitIsMadeOnABranch() throws IOException {
        final Project project = ENV.sandbox();
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        final String branchName = ENV.name("commit");
        final String textPath = ENV.name("commit-text") + ".txt";
        final String binaryPath = ENV.name("commit-bin") + ".bin";
        final String movedPath = ENV.name("commit-moved") + ".txt";
        final byte[] binary = {0, -1, 10, 13, 127};
        project.createBranch(branchName, defaultBranch);
        SmokeCleanup.run(
                () -> {
                    final Commit created = project.createCommit(branchName, "smoke: multi-file commit")
                            .create(textPath, NON_ASCII)
                            .create(binaryPath, binary)
                            .send();
                    final Commit changed = project.createCommit(branchName, "smoke: change files")
                            .update(textPath, NON_ASCII + " 2")
                            .move(textPath, movedPath)
                            .delete(binaryPath)
                            .send();
                    final Optional<RepositoryFile> moved = project.file(movedPath, branchName);
                    final Optional<RepositoryFile> deleted = project.file(binaryPath, branchName);
                    final CommitComment comment = created.comment("smoke: comment");
                    final List<CommitComment> comments = created.comments().toList();
                    final List<Commit> listed = project.commits(GitLabProjectCommitsListFilterBuilder.builder()
                                    .refName(branchName)
                                    .build())
                            .toList();

                    assertSoftly(softly -> {
                        softly.assertThat(created.sha()).isNotBlank();
                        softly.assertThat(changed.sha()).isNotEqualTo(created.sha());
                        softly.assertThat(moved).map(RepositoryFile::text).contains(NON_ASCII + " 2");
                        softly.assertThat(deleted).isEmpty();
                        softly.assertThat(comment.body()).contains("smoke: comment");
                        softly.assertThat(comments)
                                .extracting(each -> each.body().orElse(""))
                                .contains("smoke: comment");
                        softly.assertThat(listed).extracting(Commit::sha).contains(created.sha(), changed.sha());
                    });

                    final String statusName = ENV.name("status");
                    final CommitStatus status = SmokeSkips.probe(
                            "commit statuses",
                            () -> changed.addStatus(GitLabProjectCommitsCreatePayloadBuilder.builder()
                                    .state(GitLabProjectCommitsCreateState.SUCCESS)
                                    .name(statusName)
                                    .ref(branchName)
                                    .build()));
                    final List<CommitStatus> statuses =
                            changed.statuses(branchName, statusName, true).toList();

                    assertSoftly(softly -> {
                        softly.assertThat(status.status()).contains("success");
                        softly.assertThat(statuses)
                                .extracting(each -> each.name().orElse(""))
                                .contains(statusName);
                    });
                },
                () -> {
                    final Optional<Branch> branch = project.branch(branchName);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void repository_shouldReadTreeCompareAndBlame_whenABranchHasOneNewFile() throws IOException {
        final Project project = ENV.sandbox();
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        final String branchName = ENV.name("repo");
        final String path = ENV.name("repo-file") + ".txt";
        final List<TreeEntry> rootTree =
                SmokeSkips.probe("repository tree", () -> project.tree().toList());
        project.createBranch(branchName, defaultBranch);
        SmokeCleanup.run(
                () -> {
                    project.createFile(path, branchName, "one\ntwo\n", "smoke: repository");
                    final List<TreeEntry> recursive =
                            project.tree(null, branchName, true).toList();
                    final Comparison comparison = project.compare(defaultBranch, branchName, true);
                    final RepositoryFile file = project.file(path, branchName).orElseThrow();
                    final List<BlameRange> blame = file.blame().toList();
                    final Optional<Commit> mergeBase = project.mergeBase(defaultBranch, branchName);

                    assertSoftly(softly -> {
                        softly.assertThat(rootTree).isNotEmpty();
                        softly.assertThat(recursive).extracting(TreeEntry::path).contains(path);
                        softly.assertThat(comparison.commits()).hasSize(1);
                        softly.assertThat(blame)
                                .flatExtracting(BlameRange::lines)
                                .containsExactly("one", "two");
                        softly.assertThat(mergeBase).map(Commit::sha).isPresent();
                    });
                },
                () -> {
                    final Optional<Branch> branch = project.branch(branchName);
                    if (branch.isPresent()) {
                        branch.get().delete();
                    }
                });
    }

    @Test
    void branch_shouldReturnEmpty_whenNameDoesNotExist() throws IOException {
        final Optional<Branch> missing = ENV.sandbox().branch(ENV.name("missing"));

        assertThat(missing).isEmpty();
    }
}
