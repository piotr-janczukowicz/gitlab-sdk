package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.listedProject;
import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsTransferPayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabPublicGroupDetails;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabPublicGroupDetailsBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectSharingTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void transfer_shouldReloadUnderNewId_whenCoreReturnsMovedProject() throws IOException {
        when(gitLab.projects().project().transfer(eq(ID), any())).thenReturn(listedProject(42L));
        when(gitLab.projects().project().get("42")).thenReturn(Optional.of(loadedProject(42L, "demo")));

        final Project moved = sut.transfer("other-group");

        final ArgumentCaptor<GitLabProjectProjectsTransferPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsTransferPayload.class);
        verify(gitLab.projects().project()).transfer(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().namespace()).isEqualTo("other-group");
            softly.assertThat(moved.id()).isEqualTo("42");
            softly.assertThat(moved.response().path()).isEqualTo("demo");
        });
    }

    @Test
    void transfer_shouldThrowIllegalStateException_whenGoneAfterTransfer() throws IOException {
        when(gitLab.projects().project().transfer(eq(ID), any())).thenReturn(listedProject(42L));
        when(gitLab.projects().project().get("42")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.transfer("other-group"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("42");
    }

    @Test
    void transfer_shouldThrowIllegalArgumentException_whenNamespaceIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.transfer(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_shouldThrowNullPointerException_whenNamespaceIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.transfer(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void unshare_shouldCallDeleteShareWithGroupId_whenCalled() throws IOException {
        sut.unshare(9L);

        verify(gitLab.projects().project()).deleteShare(ID, 9L);
    }

    @Test
    void sharedGroups_shouldPassRecordsThrough_whenListed() throws IOException {
        final GitLabPublicGroupDetails group =
                GitLabPublicGroupDetailsBuilder.builder().id(9L).fullPath("a/b").build();
        when(gitLab.projects().project().listGroups(ID).stream()).thenReturn(Stream.of(group));

        final List<GitLabPublicGroupDetails> groups = sut.sharedGroups().toList();

        assertThat(groups).singleElement().isSameAs(group);
    }
}
