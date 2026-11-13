package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.hook;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.hookNamed;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.hookWithOnly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectHook;
import io.github.piotr_janczukowicz.gitlab.api.projectwebhooks.GitLabProjectProjectWebhooksCreateTestTrigger;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class WebhookTest {

    private static final String ID = "my-group/demo";
    private static final String URL = "https://h.example";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private Webhook sut(final GitLabProjectHook response) {
        return new Webhook(GitLabSdk.of(gitLab).project(ID), response);
    }

    @Test
    void accessors_shouldReadResponse_whenNamed() {
        final Webhook sut = sut(hookNamed(7L, URL, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(7L);
            softly.assertThat(sut.url()).isEqualTo(URL);
            softly.assertThat(sut.name()).contains("ci");
            softly.assertThat(sut.project().id()).isEqualTo(ID);
        });
    }

    @Test
    void name_shouldBeEmpty_whenAbsent() {
        assertThat(sut(hook(7L, URL)).name()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(WebhookEvent.class)
    void isEnabledFor_shouldReadFlagFromResponse_whenOnlyThatFlagIsSet(final WebhookEvent event) {
        final Webhook sut = sut(hookWithOnly(event));

        assertSoftly(softly -> {
            for (final WebhookEvent other : WebhookEvent.values()) {
                softly.assertThat(sut.isEnabledFor(other)).isEqualTo(other == event);
            }
        });
    }

    @Test
    void isEnabledFor_shouldBeFalse_whenFlagIsNull() {
        assertThat(sut(hook(7L, URL)).isEnabledFor(WebhookEvent.PUSH)).isFalse();
    }

    @Test
    void isEnabledFor_shouldThrowNullPointerException_whenEventIsNull() {
        final Webhook sut = sut(hook(7L, URL));

        final Throwable thrown = catchThrowable(() -> sut.isEnabledFor(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(WebhookEvent.class)
    void test_shouldSendMatchingTrigger_whenEventGiven(final WebhookEvent event) throws IOException {
        final Webhook sut = sut(hook(7L, URL));

        sut.test(event);

        verify(gitLab.projectwebhooks())
                .createTest(
                        ID,
                        7L,
                        GitLabProjectProjectWebhooksCreateTestTrigger.fromWire(
                                event.name().toLowerCase(Locale.ROOT) + "_events"));
    }

    @Test
    void test_shouldThrowNullPointerException_whenEventIsNull() {
        final Webhook sut = sut(hook(7L, URL));

        final Throwable thrown = catchThrowable(() -> sut.test(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void delete_shouldCallCoreDelete_whenCoreReturnsOptional() throws IOException {
        when(gitLab.projectwebhooks().delete(ID, 7L)).thenReturn(Optional.of(hook(7L, URL)));

        sut(hook(7L, URL)).delete();

        verify(gitLab.projectwebhooks()).delete(ID, 7L);
    }

    @Test
    void refresh_shouldReturnLoadedWebhook_whenPresent() throws IOException {
        when(gitLab.projectwebhooks().get(ID, 7L)).thenReturn(Optional.of(hookNamed(7L, "https://new.example", "x")));

        final Webhook refreshed = sut(hook(7L, URL)).refresh();

        assertThat(refreshed.url()).isEqualTo("https://new.example");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projectwebhooks().get(ID, 7L)).thenReturn(Optional.empty());
        final Webhook sut = sut(hook(7L, URL));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabProjectHook record = hook(7L, URL);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
