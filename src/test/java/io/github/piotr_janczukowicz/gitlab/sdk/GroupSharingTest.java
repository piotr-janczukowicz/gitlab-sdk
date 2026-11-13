package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedGroup;
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
    void transfer_shouldCallCoreAndReload_whenCalled() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.of(loadedGroup(7L, "my-group")));

        final Group moved = sut.transfer(5L);

        final ArgumentCaptor<GitLabGroupGroupsTransferPayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupsTransferPayload.class);
        verify(gitLab.groups()).transfer(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().groupId()).isEqualTo(5L);
            softly.assertThat(moved.id()).isEqualTo(ID);
            softly.assertThat(moved.response().id()).isEqualTo(7L);
        });
    }

    @Test
    void transfer_shouldThrowIllegalStateException_whenGoneAfterTransfer() throws IOException {
        when(gitLab.groups().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(() -> sut.transfer(5L));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID);
        verify(gitLab.groups()).transfer(eq(ID), any());
    }

    @Test
    void unshare_shouldCallDeleteShareWithGroupId_whenCalled() throws IOException {
        sut.unshare(9L);

        verify(gitLab.groups()).deleteShare(ID, 9L);
    }
}
