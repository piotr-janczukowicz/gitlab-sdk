package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.groupHook;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.hook;
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
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksCreatePayloadBuilder;
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
class ProjectWebhooksTest {

    private static final String PROJECT_ID = "my-group/demo";
    private static final String GROUP_ID = "my-group";
    private static final String URL = "https://h.example";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(PROJECT_ID);
    private final Group group = GitLabSdk.of(gitLab).group(GROUP_ID);

    @Test
    void webhooks_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectwebhooks().list(PROJECT_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return hook(id, URL);
                }));

        final Stream<Webhook> webhooks = project.webhooks();

        assertThat(consumed).hasValue(0);
        assertThat(webhooks.limit(1).toList())
                .singleElement()
                .satisfies(w -> assertThat(w.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.projectwebhooks(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void webhook_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectwebhooks().get(PROJECT_ID, 7L)).thenReturn(Optional.empty());

        assertThat(project.webhook(7L)).isEmpty();
    }

    @Test
    void webhook_shouldReturnWebhook_whenPresent() throws IOException {
        when(gitLab.projectwebhooks().get(PROJECT_ID, 7L)).thenReturn(Optional.of(hook(7L, URL)));

        assertThat(project.webhook(7L)).get().satisfies(w -> assertThat(w.url()).isEqualTo(URL));
    }

    @Test
    void createWebhook_shouldSendUrl_whenUrlGiven() throws IOException {
        when(gitLab.projectwebhooks().create(eq(PROJECT_ID), any(GitLabProjectProjectWebhooksCreatePayload.class)))
                .thenReturn(hook(7L, URL));

        final Webhook result = project.createWebhook(URL);

        final ArgumentCaptor<GitLabProjectProjectWebhooksCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectWebhooksCreatePayload.class);
        verify(gitLab.projectwebhooks()).create(eq(PROJECT_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().url()).isEqualTo(URL);
            softly.assertThat(payload.getValue().name()).isNull();
            softly.assertThat(payload.getValue().pushEvents()).isNull();
            softly.assertThat(result.id()).isEqualTo(7L);
        });
    }

    @Test
    void createWebhook_shouldThrowIllegalArgumentException_whenUrlIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.createWebhook(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenUrlIsNull() {
        final Throwable thrown = catchThrowable(() -> project.createWebhook((String) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createWebhook_shouldSendPayload_whenPayloadGiven() throws IOException {
        final GitLabProjectProjectWebhooksCreatePayload payload =
                GitLabProjectProjectWebhooksCreatePayloadBuilder.builder()
                        .url(URL)
                        .pushEvents(true)
                        .build();
        when(gitLab.projectwebhooks().create(PROJECT_ID, payload)).thenReturn(hook(7L, URL));

        final Webhook result = project.createWebhook(payload);

        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenPayloadIsNull() {
        final Throwable thrown =
                catchThrowable(() -> project.createWebhook((GitLabProjectProjectWebhooksCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void webhooks_shouldBeLazy_whenGroupStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.groupwebhooks().list(GROUP_ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return groupHook(id, URL);
                }));

        final Stream<GroupWebhook> webhooks = group.webhooks();

        assertThat(consumed).hasValue(0);
        assertThat(webhooks.limit(1).toList())
                .singleElement()
                .satisfies(w -> assertThat(w.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.groupwebhooks(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void webhook_shouldReturnEmpty_whenGroupGitLabAnswers404() throws IOException {
        when(gitLab.groupwebhooks().get(GROUP_ID, 7L)).thenReturn(Optional.empty());

        assertThat(group.webhook(7L)).isEmpty();
    }

    @Test
    void webhook_shouldReturnWebhook_whenGroupWebhookPresent() throws IOException {
        when(gitLab.groupwebhooks().get(GROUP_ID, 7L)).thenReturn(Optional.of(groupHook(7L, URL)));

        assertThat(group.webhook(7L)).get().satisfies(w -> assertThat(w.url()).isEqualTo(URL));
    }

    @Test
    void createWebhook_shouldSendUrl_whenGroupUrlGiven() throws IOException {
        when(gitLab.groupwebhooks().create(eq(GROUP_ID), any(GitLabGroupGroupWebhooksCreatePayload.class)))
                .thenReturn(groupHook(7L, URL));

        final GroupWebhook result = group.createWebhook(URL);

        final ArgumentCaptor<GitLabGroupGroupWebhooksCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupWebhooksCreatePayload.class);
        verify(gitLab.groupwebhooks()).create(eq(GROUP_ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().url()).isEqualTo(URL);
            softly.assertThat(payload.getValue().pushEvents()).isNull();
            softly.assertThat(result.id()).isEqualTo(7L);
        });
    }

    @Test
    void createWebhook_shouldThrowIllegalArgumentException_whenGroupUrlIsBlank() {
        final Throwable thrown = catchThrowable(() -> group.createWebhook(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createWebhook_shouldSendPayload_whenGroupPayloadGiven() throws IOException {
        final GitLabGroupGroupWebhooksCreatePayload payload = GitLabGroupGroupWebhooksCreatePayloadBuilder.builder()
                .url(URL)
                .memberEvents(true)
                .build();
        when(gitLab.groupwebhooks().create(GROUP_ID, payload)).thenReturn(groupHook(7L, URL));

        final GroupWebhook result = group.createWebhook(payload);

        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void createWebhook_shouldThrowNullPointerException_whenGroupPayloadIsNull() {
        final Throwable thrown =
                catchThrowable(() -> group.createWebhook((GitLabGroupGroupWebhooksCreatePayload) null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
