package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.groupHook;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.groupHookNamed;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.hook;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.hookNamed;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.sent;
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
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

@UnitTest
class WebhookUpdateTest {

    private static final String PROJECT_ID = "42";
    private static final String GROUP_ID = "my-group";
    private static final String URL = "https://h.example";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final GitLabSdk sdk = GitLabSdk.of(gitLab);
    private final Webhook projectHook = new Webhook(sdk.project(PROJECT_ID), hook(7L, URL));
    private final GroupWebhook groupHook = new GroupWebhook(sdk.group(GROUP_ID), groupHook(7L, URL));

    private GitLabProjectProjectWebhooksUpdatePayload projectPayload() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectWebhooksUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectWebhooksUpdatePayload.class);
        verify(gitLab.projectwebhooks()).update(eq(PROJECT_ID), eq(7L), payload.capture());
        return payload.getValue();
    }

    private GitLabGroupGroupWebhooksUpdatePayload groupPayload() throws IOException {
        final ArgumentCaptor<GitLabGroupGroupWebhooksUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupGroupWebhooksUpdatePayload.class);
        verify(gitLab.groupwebhooks()).update(eq(GROUP_ID), eq(7L), payload.capture());
        return payload.getValue();
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void send_shouldSendOnlySetFields_whenUrlAndNameAreSet() throws IOException {
        when(gitLab.projectwebhooks()
                        .update(eq(PROJECT_ID), eq(7L), any(GitLabProjectProjectWebhooksUpdatePayload.class)))
                .thenReturn(hookNamed(7L, "https://new.example", "ci"));

        final Webhook result =
                projectHook.update().url("https://new.example").name("ci").send();

        final GitLabProjectProjectWebhooksUpdatePayload payload = projectPayload();
        assertSoftly(softly -> {
            softly.assertThat(payload.url()).isEqualTo("https://new.example");
            softly.assertThat(payload.name()).isEqualTo("ci");
            softly.assertThat(payload.description()).isNull();
            softly.assertThat(payload.token()).isNull();
            softly.assertThat(payload.enableSslVerification()).isNull();
            softly.assertThat(sent(payload)).isEmpty();
            softly.assertThat(result.url()).isEqualTo("https://new.example");
        });
        verify(gitLab.projectwebhooks(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void send_shouldSendDescriptionTokenAndSslFlag_whenSet() throws IOException {
        when(gitLab.projectwebhooks()
                        .update(eq(PROJECT_ID), eq(7L), any(GitLabProjectProjectWebhooksUpdatePayload.class)))
                .thenReturn(hook(7L, URL));

        projectHook
                .update()
                .description("d")
                .token("s3cret")
                .enableSslVerification(false)
                .send();

        final GitLabProjectProjectWebhooksUpdatePayload payload = projectPayload();
        assertSoftly(softly -> {
            softly.assertThat(payload.description()).isEqualTo("d");
            softly.assertThat(payload.token()).isEqualTo("s3cret");
            softly.assertThat(payload.enableSslVerification()).isFalse();
            softly.assertThat(payload.url()).isNull();
            softly.assertThat(payload.toString()).doesNotContain("s3cret");
        });
    }

    @ParameterizedTest
    @EnumSource(WebhookEvent.class)
    void events_shouldSetOnlyNamedFlags_whenOneEventGiven(final WebhookEvent event) throws IOException {
        when(gitLab.projectwebhooks()
                        .update(eq(PROJECT_ID), eq(7L), any(GitLabProjectProjectWebhooksUpdatePayload.class)))
                .thenReturn(hook(7L, URL));

        projectHook.update().events(event).send();

        assertThat(sent(projectPayload())).containsExactly(event);
    }

    @Test
    void events_shouldSetOnlyNamedFlags_whenSeveralEventsGiven() throws IOException {
        when(gitLab.projectwebhooks()
                        .update(eq(PROJECT_ID), eq(7L), any(GitLabProjectProjectWebhooksUpdatePayload.class)))
                .thenReturn(hook(7L, URL));

        projectHook.update().events(WebhookEvent.PUSH, WebhookEvent.NOTE).send();

        final Set<WebhookEvent> expected = EnumSet.of(WebhookEvent.PUSH, WebhookEvent.NOTE);
        assertThat(sent(projectPayload())).isEqualTo(expected);
    }

    @Test
    void events_shouldThrowIllegalArgumentException_whenEmpty() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(update::events);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void events_shouldThrowIllegalArgumentException_whenNull() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.events((WebhookEvent[]) null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void events_shouldThrowNullPointerException_whenElementIsNull() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.events(WebhookEvent.PUSH, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void events_shouldNotMarkUpdate_whenRejected() {
        final WebhookUpdate update = projectHook.update();
        catchThrowable(update::events);

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void url_shouldThrowIllegalArgumentException_whenBlank() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.url(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void url_shouldThrowNullPointerException_whenNull() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.url(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenBlank() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.name(""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void name_shouldThrowNullPointerException_whenNull() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.name(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void description_shouldThrowNullPointerException_whenNull() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.description(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void token_shouldThrowNullPointerException_whenNull() {
        final WebhookUpdate update = projectHook.update();

        final Throwable thrown = catchThrowable(() -> update.token(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSetOnGroupWebhook() {
        final GroupWebhookUpdate update = groupHook.update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void send_shouldSendOnlySetFields_whenGroupWebhookFieldsAreSet() throws IOException {
        when(gitLab.groupwebhooks().update(eq(GROUP_ID), eq(7L), any(GitLabGroupGroupWebhooksUpdatePayload.class)))
                .thenReturn(groupHookNamed(7L, "https://new.example", "ci"));

        final GroupWebhook result = groupHook
                .update()
                .url("https://new.example")
                .name("ci")
                .description("d")
                .token("s3cret")
                .enableSslVerification(true)
                .send();

        final GitLabGroupGroupWebhooksUpdatePayload payload = groupPayload();
        assertSoftly(softly -> {
            softly.assertThat(payload.url()).isEqualTo("https://new.example");
            softly.assertThat(payload.name()).isEqualTo("ci");
            softly.assertThat(payload.description()).isEqualTo("d");
            softly.assertThat(payload.token()).isEqualTo("s3cret");
            softly.assertThat(payload.enableSslVerification()).isTrue();
            softly.assertThat(sent(payload)).isEmpty();
            softly.assertThat(result.name()).contains("ci");
        });
        verify(gitLab.groupwebhooks(), never()).get(any(String.class), any(Long.class));
    }

    @ParameterizedTest
    @EnumSource(WebhookEvent.class)
    void events_shouldSetOnlyNamedFlags_whenOneEventGivenOnGroupWebhook(final WebhookEvent event) throws IOException {
        when(gitLab.groupwebhooks().update(eq(GROUP_ID), eq(7L), any(GitLabGroupGroupWebhooksUpdatePayload.class)))
                .thenReturn(groupHook(7L, URL));

        groupHook.update().events(event).send();

        assertThat(sent(groupPayload())).containsExactly(event);
    }

    @Test
    void events_shouldThrowIllegalArgumentException_whenEmptyOnGroupWebhook() {
        final GroupWebhookUpdate update = groupHook.update();

        final Throwable thrown = catchThrowable(update::events);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void url_shouldThrowIllegalArgumentException_whenBlankOnGroupWebhook() {
        final GroupWebhookUpdate update = groupHook.update();

        final Throwable thrown = catchThrowable(() -> update.url(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void name_shouldThrowIllegalArgumentException_whenBlankOnGroupWebhook() {
        final GroupWebhookUpdate update = groupHook.update();

        final Throwable thrown = catchThrowable(() -> update.name(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
