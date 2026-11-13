package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Branch;
import io.github.piotr_janczukowicz.gitlab.sdk.code.Commit;
import io.github.piotr_janczukowicz.gitlab.sdk.code.RepositoryFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    void branch_shouldReturnEmpty_whenNameDoesNotExist() throws IOException {
        final Optional<Branch> missing = ENV.sandbox().branch(ENV.name("missing"));

        assertThat(missing).isEmpty();
    }
}
