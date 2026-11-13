package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.branch;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.branchWithCodeOwners;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.branchWithForcePush;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProtectedBranchTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private ProtectedBranch sut(final GitLabProtectedBranch response) {
        return new ProtectedBranch(project, response);
    }

    private void stubUpdate(final String name, final GitLabProtectedBranch answer) throws IOException {
        when(gitLab.protectedbranches()
                        .update(eq(ID), eq(name), any(GitLabProjectProtectedBranchesUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void accessors_shouldReadResponse_whenPopulated() {
        final ProtectedBranch sut = sut(branch("main", 40, 30));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("main");
            softly.assertThat(sut.allowsForcePush()).isFalse();
            softly.assertThat(sut.requiresCodeOwnerApproval()).isTrue();
            softly.assertThat(sut.pushAccess()).containsExactly(ProtectedAccess.MAINTAINER);
            softly.assertThat(sut.pushAccessValues()).containsExactly(40);
            softly.assertThat(sut.mergeAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.mergeAccessValues()).containsExactly(30);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
        });
    }

    @Test
    void flags_shouldBeFalse_whenAbsentInResponse() {
        final ProtectedBranch sut = sut(branch("main"));

        assertSoftly(softly -> {
            softly.assertThat(sut.allowsForcePush()).isFalse();
            softly.assertThat(sut.requiresCodeOwnerApproval()).isFalse();
        });
    }

    @Test
    void access_shouldBeEmpty_whenAbsentInResponse() {
        final ProtectedBranch sut = sut(branch("main"));

        assertSoftly(softly -> {
            softly.assertThat(sut.pushAccess()).isEmpty();
            softly.assertThat(sut.pushAccessValues()).isEmpty();
            softly.assertThat(sut.mergeAccess()).isEmpty();
            softly.assertThat(sut.mergeAccessValues()).isEmpty();
        });
    }

    @Test
    void pushAccess_shouldThrowIllegalArgumentException_whenUnknownLevel() {
        final ProtectedBranch sut = sut(branch("main", 20, 40));

        final Throwable thrown = catchThrowable(sut::pushAccess);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.pushAccessValues()).containsExactly(20);
        });
    }

    @Test
    void mergeAccess_shouldThrowIllegalArgumentException_whenUnknownLevel() {
        final ProtectedBranch sut = sut(branch("main", 40, 10));

        final Throwable thrown = catchThrowable(sut::mergeAccess);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.mergeAccessValues()).containsExactly(10);
        });
    }

    @Test
    void allowForcePush_shouldReturnNewObjectFromAnswer_whenCalled() throws IOException {
        final ProtectedBranch sut = sut(branchWithForcePush("main", false));
        stubUpdate("main", branchWithForcePush("main", true));

        final ProtectedBranch result = sut.allowForcePush(true);

        final ArgumentCaptor<GitLabProjectProtectedBranchesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProtectedBranchesUpdatePayload.class);
        verify(gitLab.protectedbranches()).update(eq(ID), eq("main"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().allowForcePush()).isTrue();
            softly.assertThat(payload.getValue().codeOwnerApprovalRequired()).isNull();
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.allowsForcePush()).isTrue();
            softly.assertThat(sut.allowsForcePush()).isFalse();
        });
        verify(gitLab.protectedbranches(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void requireCodeOwnerApproval_shouldReturnNewObjectFromAnswer_whenCalled() throws IOException {
        final ProtectedBranch sut = sut(branchWithCodeOwners("main", false));
        stubUpdate("main", branchWithCodeOwners("main", true));

        final ProtectedBranch result = sut.requireCodeOwnerApproval(true);

        final ArgumentCaptor<GitLabProjectProtectedBranchesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProtectedBranchesUpdatePayload.class);
        verify(gitLab.protectedbranches()).update(eq(ID), eq("main"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().codeOwnerApprovalRequired()).isTrue();
            softly.assertThat(payload.getValue().allowForcePush()).isNull();
            softly.assertThat(result.requiresCodeOwnerApproval()).isTrue();
            softly.assertThat(sut.requiresCodeOwnerApproval()).isFalse();
        });
    }

    @Test
    void unprotect_shouldCallDelete_whenCalled() throws IOException {
        sut(branch("main")).unprotect();

        verify(gitLab.protectedbranches()).delete(ID, "main");
    }

    @Test
    void refresh_shouldReturnLoadedBranch_whenPresent() throws IOException {
        when(gitLab.protectedbranches().get(ID, "main")).thenReturn(Optional.of(branchWithForcePush("main", true)));

        final ProtectedBranch refreshed = sut(branch("main")).refresh();

        assertThat(refreshed.allowsForcePush()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.protectedbranches().get(ID, "main")).thenReturn(Optional.empty());
        final ProtectedBranch sut = sut(branch("main"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("main");
    }

    @Test
    void name_shouldReachCoreUnchanged_whenItContainsSlash() throws IOException {
        final ProtectedBranch sut = sut(branch("release/1.x"));
        stubUpdate("release/1.x", branch("release/1.x"));
        when(gitLab.protectedbranches().get(ID, "release/1.x")).thenReturn(Optional.of(branch("release/1.x")));

        sut.allowForcePush(true);
        sut.refresh();
        sut.unprotect();

        verify(gitLab.protectedbranches())
                .update(eq(ID), eq("release/1.x"), any(GitLabProjectProtectedBranchesUpdatePayload.class));
        verify(gitLab.protectedbranches()).get(ID, "release/1.x");
        verify(gitLab.protectedbranches()).delete(ID, "release/1.x");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabProtectedBranch record = branch("main");

        assertThat(sut(record).response()).isSameAs(record);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new ProtectedBranch(null, branch("main"))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new ProtectedBranch(project, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }
}
