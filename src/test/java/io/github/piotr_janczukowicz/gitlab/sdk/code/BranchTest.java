package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.gitlab.sdk.code.CodeFixtures.branch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranch;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabProjectBranchesProtectPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class BranchTest {

    private static final String ID = "my-group/my-project";
    private static final String NAME = "feature/login";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabBranch response = branch(NAME, false);
    private final Branch sut = new Branch(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenBranchIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo(NAME);
            softly.assertThat(sut.isProtected()).isFalse();
            softly.assertThat(sut.isDefault()).isFalse();
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldReturnReloadedBranch_whenGitLabHasIt() throws IOException {
        final GitLabBranch reloaded = branch(NAME, true);
        when(gitLab.branches().get(ID, NAME)).thenReturn(Optional.of(reloaded));

        final Branch result = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(result.response()).isSameAs(reloaded);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.branches().get(ID, NAME)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Branch " + NAME + " is gone");
    }

    @Test
    void protect_shouldReturnProtectedBranch_whenGitLabAnswers() throws IOException {
        final GitLabBranch protectedBranch = branch(NAME, true);
        when(gitLab.branches().protect(any(), any(), any(GitLabProjectBranchesProtectPayload.class)))
                .thenReturn(protectedBranch);

        final Branch result = sut.protect();

        assertSoftly(softly -> {
            softly.assertThat(result.isProtected()).isTrue();
            softly.assertThat(result.response()).isSameAs(protectedBranch);
            softly.assertThat(sut.isProtected()).isFalse();
        });
        verify(gitLab.branches()).protect(ID, NAME, new GitLabProjectBranchesProtectPayload(null, null));
    }

    @Test
    void unprotect_shouldReturnUnprotectedBranch_whenGitLabAnswers() throws IOException {
        final Branch protectedSut = new Branch(project, branch(NAME, true));
        when(gitLab.branches().unprotect(ID, NAME)).thenReturn(branch(NAME, false));

        final Branch result = protectedSut.unprotect();

        assertSoftly(softly -> {
            softly.assertThat(result.isProtected()).isFalse();
            softly.assertThat(protectedSut.isProtected()).isTrue();
        });
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.branches()).delete(ID, NAME);
    }
}
