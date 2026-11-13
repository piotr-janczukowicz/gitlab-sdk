package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.branch;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.groupBranch;
import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.tag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.grouplevelprotectedbranches.GitLabGroupGroupLevelProtectedBranchesCreatePushAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesCreateMergeAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.protectedbranches.GitLabProjectProtectedBranchesCreatePushAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProjectProtectedTagsCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProjectProtectedTagsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectProtectionTest {

    private static final String PROJECT_ID = "my-group/demo";
    private static final String GROUP_ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(PROJECT_ID);
    private final Group group = GitLabSdk.of(gitLab).group(GROUP_ID);

    @Test
    void protectedBranches_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.protectedbranches().list(PROJECT_ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return branch(name);
                }));

        final Stream<ProtectedBranch> branches = project.protectedBranches();

        assertThat(consumed).hasValue(0);
        assertThat(branches.limit(1).toList())
                .singleElement()
                .satisfies(b -> assertThat(b.name()).isEqualTo("a"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.protectedbranches(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void protectedBranch_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.protectedbranches().get(PROJECT_ID, "main")).thenReturn(Optional.empty());

        assertThat(project.protectedBranch("main")).isEmpty();
    }

    @Test
    void protectedBranch_shouldReturnBranch_whenPresent() throws IOException {
        when(gitLab.protectedbranches().get(PROJECT_ID, "main")).thenReturn(Optional.of(branch("main")));

        assertThat(project.protectedBranch("main"))
                .get()
                .satisfies(b -> assertThat(b.name()).isEqualTo("main"));
    }

    @Test
    void protectedBranch_shouldPassNameUnchanged_whenItContainsSlash() throws IOException {
        when(gitLab.protectedbranches().get(PROJECT_ID, "release/1.x")).thenReturn(Optional.of(branch("release/1.x")));

        assertThat(project.protectedBranch("release/1.x")).isPresent();
        verify(gitLab.protectedbranches()).get(PROJECT_ID, "release/1.x");
    }

    @Test
    void protectedBranch_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.protectedBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.protectedbranches());
    }

    @Test
    void protectBranch_shouldSendOnlyName_whenNoLevelsGiven() throws IOException {
        when(gitLab.protectedbranches().create(eq(PROJECT_ID), any(GitLabProjectProtectedBranchesCreatePayload.class)))
                .thenReturn(branch("main"));

        final ProtectedBranch result = project.protectBranch("main");

        final ArgumentCaptor<GitLabProjectProtectedBranchesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProtectedBranchesCreatePayload.class);
        verify(gitLab.protectedbranches()).create(eq(PROJECT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("main");
            softly.assertThat(payload.getValue().pushAccessLevel()).isNull();
            softly.assertThat(payload.getValue().mergeAccessLevel()).isNull();
            softly.assertThat(payload.getValue().unprotectAccessLevel()).isNull();
            softly.assertThat(payload.getValue().allowForcePush()).isNull();
            softly.assertThat(payload.getValue().codeOwnerApprovalRequired()).isNull();
            softly.assertThat(result.name()).isEqualTo("main");
        });
    }

    @Test
    void protectBranch_shouldSendNameAndLevels_whenLevelsGiven() throws IOException {
        when(gitLab.protectedbranches().create(eq(PROJECT_ID), any(GitLabProjectProtectedBranchesCreatePayload.class)))
                .thenReturn(branch("release/1.x", 40, 30));

        final ProtectedBranch result =
                project.protectBranch("release/1.x", ProtectedAccess.MAINTAINER, ProtectedAccess.DEVELOPER);

        final ArgumentCaptor<GitLabProjectProtectedBranchesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProtectedBranchesCreatePayload.class);
        verify(gitLab.protectedbranches()).create(eq(PROJECT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("release/1.x");
            softly.assertThat(payload.getValue().pushAccessLevel())
                    .isEqualTo(GitLabProjectProtectedBranchesCreatePushAccessLevel.MAINTAINER);
            softly.assertThat(payload.getValue().mergeAccessLevel())
                    .isEqualTo(GitLabProjectProtectedBranchesCreateMergeAccessLevel.DEVELOPER);
            softly.assertThat(result.pushAccess()).containsExactly(ProtectedAccess.MAINTAINER);
        });
    }

    @Test
    void protectBranch_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.protectBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.protectedbranches());
    }

    @Test
    void protectBranch_shouldThrowNullPointerException_whenNameIsNull() {
        final Throwable thrown = catchThrowable(() -> project.protectBranch(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(gitLab.protectedbranches());
    }

    @Test
    void protectBranch_shouldThrowNullPointerException_whenAccessIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> project.protectBranch("main", null, ProtectedAccess.DEVELOPER)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> project.protectBranch("main", ProtectedAccess.DEVELOPER, null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.protectedbranches());
    }

    @Test
    void protectBranch_shouldThrowIllegalArgumentException_whenNameIsBlankWithLevels() {
        final Throwable thrown =
                catchThrowable(() -> project.protectBranch("", ProtectedAccess.DEVELOPER, ProtectedAccess.DEVELOPER));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void protectedTags_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.protectedtags().list(PROJECT_ID).stream())
                .thenReturn(Stream.of("v1", "v2", "v3").map(name -> {
                    consumed.incrementAndGet();
                    return tag(name);
                }));

        final Stream<ProtectedTag> tags = project.protectedTags();

        assertThat(consumed).hasValue(0);
        assertThat(tags.limit(1).toList())
                .singleElement()
                .satisfies(t -> assertThat(t.name()).isEqualTo("v1"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.protectedtags(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void protectedTag_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.protectedtags().get(PROJECT_ID, "v*")).thenReturn(Optional.empty());

        assertThat(project.protectedTag("v*")).isEmpty();
    }

    @Test
    void protectedTag_shouldReturnTag_whenPresent() throws IOException {
        when(gitLab.protectedtags().get(PROJECT_ID, "v*")).thenReturn(Optional.of(tag("v*")));

        assertThat(project.protectedTag("v*"))
                .get()
                .satisfies(t -> assertThat(t.name()).isEqualTo("v*"));
    }

    @Test
    void protectedTag_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.protectedTag(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void protectTag_shouldSendCreateAccessLevel_whenLevelGiven() throws IOException {
        when(gitLab.protectedtags().create(eq(PROJECT_ID), any(GitLabProjectProtectedTagsCreatePayload.class)))
                .thenReturn(tag("v*", 40));

        final ProtectedTag result = project.protectTag("v*", ProtectedAccess.MAINTAINER);

        final ArgumentCaptor<GitLabProjectProtectedTagsCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProtectedTagsCreatePayload.class);
        verify(gitLab.protectedtags()).create(eq(PROJECT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("v*");
            softly.assertThat(payload.getValue().createAccessLevel())
                    .isEqualTo(GitLabProjectProtectedTagsCreateAccessLevel.MAINTAINER);
            softly.assertThat(result.createAccess()).containsExactly(ProtectedAccess.MAINTAINER);
        });
    }

    @Test
    void protectTag_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.protectTag(" ", ProtectedAccess.MAINTAINER));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.protectedtags());
    }

    @Test
    void protectTag_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> project.protectTag(null, ProtectedAccess.MAINTAINER)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> project.protectTag("v*", null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.protectedtags());
    }

    @Test
    void protectedBranches_shouldBeLazy_whenGroupStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.grouplevelprotectedbranches().list(GROUP_ID).stream())
                .thenReturn(Stream.of("a", "b", "c").map(name -> {
                    consumed.incrementAndGet();
                    return groupBranch(name);
                }));

        final Stream<GroupProtectedBranch> branches = group.protectedBranches();

        assertThat(consumed).hasValue(0);
        assertThat(branches.limit(1).toList())
                .singleElement()
                .satisfies(b -> assertThat(b.name()).isEqualTo("a"));
        assertThat(consumed).hasValue(1);
        verify(gitLab.grouplevelprotectedbranches(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void protectedBranch_shouldReturnEmpty_whenGroupGitLabAnswers404() throws IOException {
        when(gitLab.grouplevelprotectedbranches().get(GROUP_ID, "main")).thenReturn(Optional.empty());

        assertThat(group.protectedBranch("main")).isEmpty();
    }

    @Test
    void protectedBranch_shouldReturnBranch_whenGroupBranchPresent() throws IOException {
        when(gitLab.grouplevelprotectedbranches().get(GROUP_ID, "release/1.x"))
                .thenReturn(Optional.of(groupBranch("release/1.x")));

        assertThat(group.protectedBranch("release/1.x"))
                .get()
                .satisfies(b -> assertThat(b.name()).isEqualTo("release/1.x"));
        verify(gitLab.grouplevelprotectedbranches()).get(GROUP_ID, "release/1.x");
    }

    @Test
    void protectedBranch_shouldThrowIllegalArgumentException_whenGroupNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> group.protectedBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void protectBranch_shouldSendOnlyName_whenGroupBranchProtected() throws IOException {
        when(gitLab.grouplevelprotectedbranches()
                        .create(eq(GROUP_ID), any(GitLabGroupGroupLevelProtectedBranchesCreatePayload.class)))
                .thenReturn(groupBranch("main"));

        final GroupProtectedBranch result = group.protectBranch("main");

        final ArgumentCaptor<GitLabGroupGroupLevelProtectedBranchesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelProtectedBranchesCreatePayload.class);
        verify(gitLab.grouplevelprotectedbranches()).create(eq(GROUP_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().name()).isEqualTo("main");
            softly.assertThat(payload.getValue().pushAccessLevel()).isNull();
            softly.assertThat(payload.getValue().mergeAccessLevel()).isNull();
            softly.assertThat(result.name()).isEqualTo("main");
        });
    }

    @Test
    void protectBranch_shouldSendNameAndLevels_whenGroupLevelsGiven() throws IOException {
        when(gitLab.grouplevelprotectedbranches()
                        .create(eq(GROUP_ID), any(GitLabGroupGroupLevelProtectedBranchesCreatePayload.class)))
                .thenReturn(groupBranch("main", 60, 40));

        final GroupProtectedBranch result =
                group.protectBranch("main", ProtectedAccess.ADMINISTRATOR, ProtectedAccess.MAINTAINER);

        final ArgumentCaptor<GitLabGroupGroupLevelProtectedBranchesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupLevelProtectedBranchesCreatePayload.class);
        verify(gitLab.grouplevelprotectedbranches()).create(eq(GROUP_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().pushAccessLevel())
                    .isEqualTo(GitLabGroupGroupLevelProtectedBranchesCreatePushAccessLevel.ADMINISTRATOR);
            softly.assertThat(payload.getValue().mergeAccessLevel().wireValue()).isEqualTo(40);
            softly.assertThat(result.pushAccess()).containsExactly(ProtectedAccess.ADMINISTRATOR);
        });
    }

    @Test
    void protectBranch_shouldThrowIllegalArgumentException_whenGroupNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> group.protectBranch(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab.grouplevelprotectedbranches());
    }

    @Test
    void protectBranch_shouldThrowNullPointerException_whenGroupArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> group.protectBranch(null))).isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> group.protectBranch("main", null, ProtectedAccess.DEVELOPER)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab.grouplevelprotectedbranches());
    }
}
