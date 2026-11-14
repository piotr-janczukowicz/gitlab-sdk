package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import static io.github.piotr_janczukowicz.gitlab.sdk.ci.CiFixtures.job;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJob;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabCiJobArtifactEntry;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabCiJobArtifactEntryBuilder;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabCiJobArtifactEntryType;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabProjectJobsArtifactsListTreeFilter;
import io.github.piotr_janczukowicz.gitlab.api.jobsartifacts.GitLabProjectJobsArtifactsListTreeFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.json.JsonTypes;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class JobArtifactsTest {

    private static final String ID = "my-group/my-project";
    private static final long JOB_ID = 4L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Job sut = new Job(project, job(JOB_ID, "build", "success"));

    private static GitLabCiJobArtifactEntry entry(final String name) {
        return GitLabCiJobArtifactEntryBuilder.builder()
                .name(name)
                .path(name)
                .type(GitLabCiJobArtifactEntryType.FILE)
                .build();
    }

    @Test
    void downloadArtifacts_shouldWriteToPathAndReturnIt_whenTargetGiven() throws IOException {
        final Path target = Path.of("out", "artifacts.zip");
        when(gitLab.jobsartifacts().get(ID, JOB_ID, target)).thenReturn(target);

        final Path result = sut.downloadArtifacts(target);

        assertThat(result).isSameAs(target);
    }

    @Test
    void downloadArtifacts_shouldReturnByteCount_whenStreamGiven() throws IOException {
        final OutputStream out = new ByteArrayOutputStream();
        when(gitLab.jobsartifacts().get(ID, JOB_ID, out)).thenReturn(1234L);

        final long written = sut.downloadArtifacts(out);

        assertThat(written).isEqualTo(1234L);
    }

    @Test
    void downloadArtifacts_shouldPassCoreException_whenTransportFails() throws IOException {
        final Path target = Path.of("artifacts.zip");
        when(gitLab.jobsartifacts().get(ID, JOB_ID, target)).thenThrow(new IOException("boom"));

        final Throwable thrown = catchThrowable(() -> sut.downloadArtifacts(target));

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void downloadArtifacts_shouldThrowNullPointerException_whenTargetIsNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.downloadArtifacts((Path) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.jobsartifacts(), never()).get(any(), anyLong(), any(Path.class));
    }

    @Test
    void downloadArtifacts_shouldThrowNullPointerException_whenStreamIsNull() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.downloadArtifacts((OutputStream) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verify(gitLab.jobsartifacts(), never()).get(any(), anyLong(), any(OutputStream.class));
    }

    @Test
    void artifactTree_shouldBeLazy_whenOnlyOneEntryIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.jobsartifacts().listTree(ID, JOB_ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return entry(name);
                }));

        final Stream<GitLabCiJobArtifactEntry> stream = sut.artifactTree();

        assertSoftly(softly -> {
            softly.assertThat(consumed).hasValue(0);
            softly.assertThat(stream.limit(1).toList())
                    .extracting(GitLabCiJobArtifactEntry::name)
                    .containsExactly("a");
            softly.assertThat(consumed).hasValue(1);
        });
    }

    @Test
    void artifactTree_shouldSendNoFilter_whenCalledForTheRoot() throws IOException {
        when(gitLab.jobsartifacts().listTree(ID, JOB_ID).stream()).thenReturn(Stream.of(entry("a")));

        final List<GitLabCiJobArtifactEntry> result = sut.artifactTree().toList();

        assertThat(result).extracting(GitLabCiJobArtifactEntry::name).containsExactly("a");
        verify(gitLab.jobsartifacts(), never())
                .listTree(any(), anyLong(), any(GitLabProjectJobsArtifactsListTreeFilter.class));
    }

    @Test
    void artifactTree_shouldSendPathAndRecursive_whenGiven() throws IOException {
        final GitLabProjectJobsArtifactsListTreeFilter filter =
                GitLabProjectJobsArtifactsListTreeFilterBuilder.builder()
                        .path("coverage/html")
                        .recursive(true)
                        .build();
        when(gitLab.jobsartifacts().listTree(ID, JOB_ID, filter).stream()).thenReturn(Stream.of(entry("index.html")));

        final List<GitLabCiJobArtifactEntry> result =
                sut.artifactTree("coverage/html", true).toList();

        assertThat(result).extracting(GitLabCiJobArtifactEntry::name).containsExactly("index.html");
    }

    @Test
    void artifactTree_shouldThrowIllegalArgumentException_whenPathIsBlank() throws IOException {
        final Throwable thrown = catchThrowable(() -> sut.artifactTree(" ", false));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verify(gitLab.jobsartifacts(), never())
                .listTree(any(), anyLong(), any(GitLabProjectJobsArtifactsListTreeFilter.class));
    }

    @Test
    void keepArtifacts_shouldReturnJobFromAnswer_whenGitLabKeeps() throws IOException {
        final GitLabCiJob kept = job(JOB_ID, "build", "success");
        when(gitLab.jobsartifacts().keep(ID, JOB_ID)).thenReturn(kept);

        final Job result = sut.keepArtifacts();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(kept);
            softly.assertThat(result.project()).isSameAs(project);
            softly.assertThat(result).isNotSameAs(sut);
        });
    }

    @Test
    void deleteArtifacts_shouldCallCore_whenCalled() throws IOException {
        sut.deleteArtifacts();

        verify(gitLab.jobsartifacts()).delete(ID, JOB_ID);
    }

    @Test
    void entry_shouldDecodeRealShapedJson_whenGitLabSendsTheTree() {
        final byte[] json = """
                [{"name":"a.txt","path":"a.txt","type":"file","size":12,"mode":"100644"},
                 {"name":"dir","path":"dir","type":"directory","size":0,"mode":"040000"}]""".getBytes(StandardCharsets.UTF_8);

        final List<GitLabCiJobArtifactEntry> entries =
                GitLabJsonCodecs.defaults().decode(json, JsonTypes.listOf(GitLabCiJobArtifactEntry.class));

        assertSoftly(softly -> {
            softly.assertThat(entries).hasSize(2);
            softly.assertThat(entries.get(0).name()).isEqualTo("a.txt");
            softly.assertThat(entries.get(0).type()).isEqualTo(GitLabCiJobArtifactEntryType.FILE);
            softly.assertThat(entries.get(0).size()).isEqualTo(12L);
            softly.assertThat(entries.get(0).mode()).isEqualTo("100644");
            softly.assertThat(entries.get(1).type()).isEqualTo(GitLabCiJobArtifactEntryType.DIRECTORY);
        });
    }

    @Test
    void job_shouldDecodeRealShapedArtifactFields_whenJobCarriesArtifacts() {
        final byte[] json = """
                {"id":4,"name":"build","status":"success","artifacts_file":{"filename":"artifacts.zip","size":1000},
                 "artifacts":[{"file_type":"archive","size":1000,"filename":"artifacts.zip","file_format":"zip"}],
                 "artifacts_expire_at":"2016-01-19T09:05:50.355Z"}""".getBytes(StandardCharsets.UTF_8);

        final GitLabCiJob decoded = GitLabJsonCodecs.defaults().decode(json, GitLabCiJob.class);

        assertSoftly(softly -> {
            softly.assertThat(decoded.artifacts()).hasSize(1);
            softly.assertThat(decoded.artifacts().get(0).filename()).isEqualTo("artifacts.zip");
            softly.assertThat(decoded.artifactsExpireAt()).isEqualTo(Instant.parse("2016-01-19T09:05:50.355Z"));
            softly.assertThat(decoded.artifactsFile().size()).isEqualTo(1000L);
        });
    }
}
