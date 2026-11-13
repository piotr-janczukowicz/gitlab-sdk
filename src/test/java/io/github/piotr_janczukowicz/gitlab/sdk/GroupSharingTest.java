package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsTransferPayload;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupSharingTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group sut = GitLabSdk.of(gitLab).group(ID);

    @Test
    void transfer_shouldReloadByNumericId_whenGroupWasAddressedByPath() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(loadedGroup(7L, "my-group")), Optional.empty());
        when(gitLab.groups().get("7")).thenReturn(Optional.of(loadedGroup(7L, "new-parent/my-group")));

        final Group moved = sut.transfer(5L);

        final ArgumentCaptor<GitLabGroupGroupsTransferPayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupsTransferPayload.class);
        verify(gitLab.groups()).transfer(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().groupId()).isEqualTo(5L);
            softly.assertThat(moved.id()).isEqualTo("7");
            softly.assertThat(moved.response().path()).isEqualTo("new-parent/my-group");
        });
    }

    @Test
    void transfer_shouldThrowIllegalStateException_whenGoneAfterTransfer() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(loadedGroup(7L, "my-group")));
        when(gitLab.groups().get("7")).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.transfer(5L));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
        verify(gitLab.groups()).transfer(eq(ID), any());
    }

    @Test
    void transfer_shouldThrowIllegalStateException_whenGoneBeforeTransfer() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.transfer(5L));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID);
        verify(gitLab.groups(), never()).transfer(any(), any());
    }

    @Test
    void transfer_shouldNotFetchFirst_whenGroupIsAlreadyLoaded() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(loadedGroup(7L, "my-group")));
        when(gitLab.groups().get("7")).thenReturn(Optional.of(loadedGroup(7L, "new-parent/my-group")));
        final Group loaded = sut.fetch().orElseThrow();

        final Group moved = loaded.transfer(5L);

        verify(gitLab.groups(), times(1)).get(ID);
        verify(gitLab.groups()).transfer(eq(ID), any());
        assertThat(moved.id()).isEqualTo("7");
    }

    @Test
    void unshare_shouldCallDeleteShareWithGroupId_whenCalled() throws IOException {
        sut.unshare(9L);

        verify(gitLab.groups()).deleteShare(ID, 9L);
    }
}
