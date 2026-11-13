package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.groupBranch;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.groupBranchWithCodeOwners;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.groupBranchWithForcePush;
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
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupProtectedBranch;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupProtectedBranchTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    private GroupProtectedBranch sut(final GitLabGroupProtectedBranch response) {
        return new GroupProtectedBranch(group, response);
    }

    private void stubUpdate(final String name, final GitLabGroupProtectedBranch answer) throws IOException {
        when(gitLab.grouplevelprotectedbranches()
                        .update(eq(ID), eq(name), any(GitLabGroupGroupLevelProtectedBranchesUpdatePayload.class)))
                .thenReturn(answer);
    }

    @Test
    void accessors_shouldReadResponse_whenPopulated() {
        final GroupProtectedBranch sut = sut(groupBranch("main", 40, 30));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("main");
            softly.assertThat(sut.allowsForcePush()).isTrue();
            softly.assertThat(sut.requiresCodeOwnerApproval()).isFalse();
            softly.assertThat(sut.pushAccess()).containsExactly(ProtectedAccess.MAINTAINER);
            softly.assertThat(sut.pushAccessValues()).containsExactly(40);
            softly.assertThat(sut.mergeAccess()).containsExactly(ProtectedAccess.DEVELOPER);
            softly.assertThat(sut.mergeAccessValues()).containsExactly(30);
            softly.assertThat(sut.group().id()).isEqualTo(ID);
        });
    }

    @Test
    void flags_shouldBeFalse_whenAbsentInResponse() {
        final GroupProtectedBranch sut = sut(groupBranch("main"));

        assertSoftly(softly -> {
            softly.assertThat(sut.allowsForcePush()).isFalse();
            softly.assertThat(sut.requiresCodeOwnerApproval()).isFalse();
            softly.assertThat(sut.pushAccess()).isEmpty();
            softly.assertThat(sut.mergeAccessValues()).isEmpty();
        });
    }

    @Test
    void pushAccess_shouldThrowIllegalArgumentException_whenUnknownLevel() {
        final GroupProtectedBranch sut = sut(groupBranch("main", 20, 40));

        final Throwable thrown = catchThrowable(sut::pushAccess);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.pushAccessValues()).containsExactly(20);
        });
    }

    @Test
    void mergeAccess_shouldThrowIllegalArgumentException_whenUnknownLevel() {
        final GroupProtectedBranch sut = sut(groupBranch("main", 40, 10));

        final Throwable thrown = catchThrowable(sut::mergeAccess);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.mergeAccessValues()).containsExactly(10);
        });
    }

    @Test
    void allowForcePush_shouldReturnNewObjectFromAnswer_whenCalled() throws IOException {
        final GroupProtectedBranch sut = sut(groupBranchWithForcePush("main", false));
        stubUpdate("main", groupBranchWithForcePush("main", true));

        final GroupProtectedBranch result = sut.allowForcePush(true);

        final ArgumentCaptor<GitLabGroupGroupLevelProtectedBranchesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelProtectedBranchesUpdatePayload.class);
        verify(gitLab.grouplevelprotectedbranches()).update(eq(ID), eq("main"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().allowForcePush()).isTrue();
            softly.assertThat(payload.getValue().codeOwnerApprovalRequired()).isNull();
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.allowsForcePush()).isTrue();
            softly.assertThat(sut.allowsForcePush()).isFalse();
        });
        verify(gitLab.grouplevelprotectedbranches(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void requireCodeOwnerApproval_shouldReturnNewObjectFromAnswer_whenCalled() throws IOException {
        final GroupProtectedBranch sut = sut(groupBranchWithCodeOwners("main", false));
        stubUpdate("main", groupBranchWithCodeOwners("main", true));

        final GroupProtectedBranch result = sut.requireCodeOwnerApproval(true);

        final ArgumentCaptor<GitLabGroupGroupLevelProtectedBranchesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelProtectedBranchesUpdatePayload.class);
        verify(gitLab.grouplevelprotectedbranches()).update(eq(ID), eq("main"), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().codeOwnerApprovalRequired()).isTrue();
            softly.assertThat(payload.getValue().allowForcePush()).isNull();
            softly.assertThat(result.requiresCodeOwnerApproval()).isTrue();
            softly.assertThat(sut.requiresCodeOwnerApproval()).isFalse();
        });
    }

    @Test
    void unprotect_shouldCallDelete_whenCalled() throws IOException {
        sut(groupBranch("main")).unprotect();

        verify(gitLab.grouplevelprotectedbranches()).delete(ID, "main");
    }

    @Test
    void refresh_shouldReturnLoadedBranch_whenPresent() throws IOException {
        when(gitLab.grouplevelprotectedbranches().get(ID, "main"))
                .thenReturn(Optional.of(groupBranchWithForcePush("main", true)));

        final GroupProtectedBranch refreshed = sut(groupBranch("main")).refresh();

        assertThat(refreshed.allowsForcePush()).isTrue();
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.grouplevelprotectedbranches().get(ID, "main")).thenReturn(Optional.empty());
        final GroupProtectedBranch sut = sut(groupBranch("main"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("main");
    }

    @Test
    void name_shouldReachCoreUnchanged_whenItContainsSlash() throws IOException {
        final GroupProtectedBranch sut = sut(groupBranch("release/1.x"));
        stubUpdate("release/1.x", groupBranch("release/1.x"));

        sut.allowForcePush(true);
        sut.unprotect();

        verify(gitLab.grouplevelprotectedbranches())
                .update(eq(ID), eq("release/1.x"), any(GitLabGroupGroupLevelProtectedBranchesUpdatePayload.class));
        verify(gitLab.grouplevelprotectedbranches()).delete(ID, "release/1.x");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabGroupProtectedBranch record = groupBranch("main");

        assertThat(sut(record).response()).isSameAs(record);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new GroupProtectedBranch(null, groupBranch("main"))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new GroupProtectedBranch(group, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }
}
