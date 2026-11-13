package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.commit;
import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.tag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabRelease;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabReleaseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTag;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTagBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTagSignature;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTagSignatureBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.releases.Release;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class TagTest {

    private static final String ID = "my-group/my-project";
    private static final String NAME = "release/1.x";
    private static final String SHA = "abc12345";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabTag response = tag(NAME, SHA);
    private final Tag sut = new Tag(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenTagIsLoaded() {
        final Instant created = Instant.parse("2026-01-02T03:04:05Z");
        final GitLabTag full = GitLabTagBuilder.builder()
                .name(NAME)
                .message("Release")
                .target("t1")
                .protectedValue(true)
                .createdAt(created)
                .build();

        final Tag result = new Tag(project, full);

        assertSoftly(softly -> {
            softly.assertThat(result.name()).isEqualTo(NAME);
            softly.assertThat(result.message()).contains("Release");
            softly.assertThat(result.target()).isEqualTo("t1");
            softly.assertThat(result.isProtected()).isTrue();
            softly.assertThat(result.createdAt()).contains(created);
            softly.assertThat(result.project()).isSameAs(project);
            softly.assertThat(result.response()).isSameAs(full);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenOptionalFieldsAreAbsent() {
        final Tag result =
                new Tag(project, GitLabTagBuilder.builder().name(NAME).build());

        assertSoftly(softly -> {
            softly.assertThat(result.message()).isEmpty();
            softly.assertThat(result.createdAt()).isEmpty();
        });
    }

    @Test
    void isProtected_shouldBeFalse_whenFlagIsAbsent() {
        final Tag result =
                new Tag(project, GitLabTagBuilder.builder().name(NAME).build());

        assertThat(result.isProtected()).isFalse();
    }

    @Test
    void commit_shouldLoadThroughProject_whenRecordHasCommit() throws IOException {
        final GitLabCommitDetail detail = commit(SHA, "Msg");
        when(gitLab.commits().get(ID, SHA)).thenReturn(Optional.of(detail));

        final Optional<Commit> result = sut.commit();

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().response()).isSameAs(detail);
        });
    }

    @Test
    void commit_shouldBeEmpty_whenRecordHasNoCommit() throws IOException {
        final Tag noCommit =
                new Tag(project, GitLabTagBuilder.builder().name(NAME).build());

        final Optional<Commit> result = noCommit.commit();

        assertThat(result).isEmpty();
        verify(gitLab.commits(), never()).get(ID, SHA);
    }

    @Test
    void signature_shouldPassThrough_whenGitLabHasOne() throws IOException {
        final GitLabTagSignature signature =
                GitLabTagSignatureBuilder.builder().signatureType("x509").build();
        when(gitLab.tags().getSignature(ID, NAME)).thenReturn(Optional.of(signature));

        final Optional<GitLabTagSignature> result = sut.signature();

        assertThat(result).containsSame(signature);
    }

    @Test
    void signature_shouldBeEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.tags().getSignature(ID, NAME)).thenReturn(Optional.empty());

        final Optional<GitLabTagSignature> result = sut.signature();

        assertThat(result).isEmpty();
    }

    @Test
    void refresh_shouldReturnReloadedTag_whenGitLabHasIt() throws IOException {
        final GitLabTag reloaded = tag(NAME, "def");
        when(gitLab.tags().get(ID, NAME)).thenReturn(Optional.of(reloaded));

        final Tag result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.tags().get(ID, NAME)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Tag " + NAME + " is gone");
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.tags()).delete(ID, NAME);
    }

    @Test
    void release_shouldLoadReleaseByTagName_whenGitLabHasOne() throws IOException {
        final GitLabRelease loaded =
                GitLabReleaseBuilder.builder().tagName(NAME).build();
        when(gitLab.releases().get(ID, NAME)).thenReturn(Optional.of(loaded));

        final Optional<Release> result = sut.release();

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().response()).isSameAs(loaded);
            softly.assertThat(result.orElseThrow().project()).isSameAs(project);
        });
    }

    @Test
    void release_shouldPassSlashNameUnchanged_whenTagNameContainsSlash() throws IOException {
        final Tag slashed =
                new Tag(project, GitLabTagBuilder.builder().name("v1.0/rc").build());
        when(gitLab.releases().get(ID, "v1.0/rc"))
                .thenReturn(Optional.of(
                        GitLabReleaseBuilder.builder().tagName("v1.0/rc").build()));

        final Optional<Release> result = slashed.release();

        assertThat(result).isPresent();
        verify(gitLab.releases()).get(ID, "v1.0/rc");
    }

    @Test
    void release_shouldBeEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.releases().get(ID, NAME)).thenReturn(Optional.empty());

        final Optional<Release> result = sut.release();

        assertThat(result).isEmpty();
    }
}
