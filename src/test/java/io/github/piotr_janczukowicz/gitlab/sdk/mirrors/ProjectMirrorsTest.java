package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.URL_WITH_CREDENTIALS;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.fullPull;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.fullRemote;
import static io.github.piotr_janczukowicz.gitlab.sdk.mirrors.MirrorsFixtures.remote;
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
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabProjectPullMirroringUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabProjectRemoteMirrorsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabApiException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectMirrorsTest {

    private static final String ID = "my-group/demo";
    private static final String URL = "https://mirror.example.test/repo.git";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    @Test
    void remoteMirrors_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.remotemirrors().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return remote(id);
                }));

        final Stream<RemoteMirror> mirrors = project.remoteMirrors();

        assertThat(consumed).hasValue(0);
        assertThat(mirrors.limit(1).toList())
                .singleElement()
                .satisfies(m -> assertThat(m.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.remotemirrors(), never()).get(any(String.class), any(String.class));
    }

    @Test
    void remoteMirror_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.remotemirrors().get(ID, "5")).thenReturn(Optional.empty());

        assertThat(project.remoteMirror(5L)).isEmpty();
    }

    @Test
    void remoteMirror_shouldReturnMirror_whenPresent() throws IOException {
        when(gitLab.remotemirrors().get(ID, "5")).thenReturn(Optional.of(fullRemote(5L)));

        assertThat(project.remoteMirror(5L))
                .get()
                .satisfies(m -> assertThat(m.isEnabled()).isTrue());
    }

    @Test
    void createRemoteMirror_shouldSendOnlyTheUrl_whenCalled() throws IOException {
        when(gitLab.remotemirrors().create(eq(ID), any(GitLabProjectRemoteMirrorsCreatePayload.class)))
                .thenReturn(remote(5L));

        final RemoteMirror result = project.createRemoteMirror(URL);

        final ArgumentCaptor<GitLabProjectRemoteMirrorsCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectRemoteMirrorsCreatePayload.class);
        verify(gitLab.remotemirrors()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().url()).isEqualTo(URL);
            softly.assertThat(payload.getValue().enabled()).isNull();
            softly.assertThat(payload.getValue().authMethod()).isNull();
            softly.assertThat(payload.getValue().hostKeys()).isNull();
            softly.assertThat(result.id()).isEqualTo(5L);
            softly.assertThat(result.url()).isEqualTo(URL_WITH_CREDENTIALS);
        });
    }

    @Test
    void createRemoteMirror_shouldPassPayloadThrough_whenPayloadIsGiven() throws IOException {
        final GitLabProjectRemoteMirrorsCreatePayload payload = GitLabProjectRemoteMirrorsCreatePayloadBuilder.builder()
                .url(URL)
                .enabled(true)
                .build();
        when(gitLab.remotemirrors().create(ID, payload)).thenReturn(fullRemote(5L));

        final RemoteMirror result = project.createRemoteMirror(payload);

        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void createRemoteMirror_shouldThrowOnBadArguments_whenNullOrBlank() {
        final Throwable blank = catchThrowable(() -> project.createRemoteMirror(" "));
        final Throwable nullUrl = catchThrowable(() -> project.createRemoteMirror((String) null));
        final Throwable nullPayload =
                catchThrowable(() -> project.createRemoteMirror((GitLabProjectRemoteMirrorsCreatePayload) null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullUrl).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullPayload).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void pullMirror_shouldReturnMirror_whenPresent() throws IOException {
        when(gitLab.pullmirroring().get(ID)).thenReturn(fullPull(9L));

        final Optional<PullMirror> result = project.pullMirror();

        assertSoftly(softly -> {
            softly.assertThat(result).isPresent();
            softly.assertThat(result.orElseThrow().id()).isEqualTo(9L);
            softly.assertThat(result.orElseThrow().project().id()).isEqualTo(ID);
        });
    }

    @Test
    void pullMirror_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.pullmirroring().get(ID)).thenThrow(mock(GitLabNotFoundException.class));

        assertThat(project.pullMirror()).isEmpty();
    }

    @Test
    void pullMirror_shouldReturnEmpty_whenGitLabAnswers400BecauseTheProjectIsNotMirrored() throws IOException {
        final GitLabApiException notMirrored = mock(GitLabApiException.class);
        when(notMirrored.statusCode()).thenReturn(400);
        when(gitLab.pullmirroring().get(ID)).thenThrow(notMirrored);

        assertThat(project.pullMirror()).isEmpty();
    }

    @Test
    void pullMirror_shouldPassOtherErrorsThrough_whenGitLabRefusesOrFails() throws IOException {
        final GitLabForbiddenException refused = mock(GitLabForbiddenException.class);
        when(gitLab.pullmirroring().get(ID)).thenThrow(refused);

        final Throwable thrown = catchThrowable(project::pullMirror);

        assertThat(thrown).isSameAs(refused);
    }

    @Test
    void pullMirror_shouldPassAServerErrorThrough_whenStatusIs500() throws IOException {
        final GitLabApiException failed = mock(GitLabApiException.class);
        when(failed.statusCode()).thenReturn(500);
        when(gitLab.pullmirroring().get(ID)).thenThrow(failed);

        final Throwable thrown = catchThrowable(project::pullMirror);

        assertThat(thrown).isSameAs(failed);
    }

    @Test
    void pullMirror_shouldPassTheTierErrorThrough_whenTierIsTooLow() throws IOException {
        final GitLabTierException tier = new GitLabTierException("op", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.pullmirroring().get(ID)).thenThrow(tier);

        final Throwable thrown = catchThrowable(project::pullMirror);

        assertThat(thrown).isSameAs(tier);
    }

    @Test
    void configurePullMirror_shouldSendOnlyTheUrl_whenCalled() throws IOException {
        when(gitLab.pullmirroring().update(eq(ID), any(GitLabProjectPullMirroringUpdatePayload.class)))
                .thenReturn(fullPull(9L));

        final PullMirror result = project.configurePullMirror(URL);

        final ArgumentCaptor<GitLabProjectPullMirroringUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectPullMirroringUpdatePayload.class);
        verify(gitLab.pullmirroring()).update(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().url()).isEqualTo(URL);
            softly.assertThat(payload.getValue().enabled()).isNull();
            softly.assertThat(payload.getValue().authUser()).isNull();
            softly.assertThat(payload.getValue().authPassword()).isNull();
            softly.assertThat(result.id()).isEqualTo(9L);
        });
        verify(gitLab.pullmirroring(), never()).create(any(String.class), any());
    }

    @Test
    void configurePullMirror_shouldThrowOnBadArguments_whenNullOrBlank() {
        final Throwable blank = catchThrowable(() -> project.configurePullMirror(" "));
        final Throwable nullUrl = catchThrowable(() -> project.configurePullMirror(null));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullUrl).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
