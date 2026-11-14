package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabCiJobArtifactEntry;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabCiJobArtifactEntryType;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.ci.Job;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only, on a job of the sandbox that already has artifacts; skipped when there is none. The download goes to a
 * temporary directory that the test deletes. Keeping and deleting artifacts are not run live, because they would change
 * or lose the artifacts of a real job.
 */
@Tag("smoke")
class ArtifactsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    private static Job jobWithArtifacts(final Project project) throws IOException {
        final Optional<Job> found = project.jobs()
                .limit(50)
                .filter(job -> job.response().artifactsFile() != null)
                .findFirst();
        assumeTrue(found.isPresent(), "the sandbox has no job with artifacts in its latest 50 jobs");
        return found.get();
    }

    @Test
    void artifactTree_shouldListEntries_whenJobHasArtifacts() throws IOException {
        final Job job = jobWithArtifacts(ENV.sandbox());

        final List<GitLabCiJobArtifactEntry> root = job.artifactTree().limit(50).toList();
        final Optional<GitLabCiJobArtifactEntry> directory = root.stream()
                .filter(entry -> entry.type() == GitLabCiJobArtifactEntryType.DIRECTORY)
                .findFirst();
        final List<GitLabCiJobArtifactEntry> nested = directory.isPresent()
                ? job.artifactTree(directory.get().path(), true).limit(50).toList()
                : List.of();

        assertSoftly(softly -> {
            softly.assertThat(root).extracting(GitLabCiJobArtifactEntry::name).doesNotContainNull();
            softly.assertThat(nested).extracting(GitLabCiJobArtifactEntry::name).doesNotContainNull();
        });
    }

    @Test
    void downloadArtifacts_shouldWriteAFile_whenJobHasArtifacts() throws IOException {
        final Job job = jobWithArtifacts(ENV.sandbox());
        final Path directory = Files.createTempDirectory("gitlab-sdk-smoke-");
        final Path target = directory.resolve("artifacts.zip");
        SmokeCleanup.run(
                () -> {
                    final Path written = job.downloadArtifacts(target);

                    assertSoftly(softly -> {
                        softly.assertThat(written).isEqualTo(target);
                        softly.assertThat(target).exists().isRegularFile();
                        softly.assertThat(sizeOf(target)).isPositive();
                    });
                },
                () -> Files.deleteIfExists(target),
                () -> Files.deleteIfExists(directory));
    }

    private static long sizeOf(final Path file) throws java.io.UncheckedIOException {
        try {
            return Files.size(file);
        } catch (final IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
