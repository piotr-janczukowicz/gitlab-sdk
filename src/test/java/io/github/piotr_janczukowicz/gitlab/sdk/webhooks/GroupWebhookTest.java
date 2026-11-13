package io.github.piotr_janczukowicz.gitlab.sdk.webhooks;

import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.groupHook;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.groupHookNamed;
import static io.github.piotr_janczukowicz.gitlab.sdk.webhooks.WebhooksFixtures.groupHookWithOnly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupGroupWebhooksCreateTestTrigger;
import io.github.piotr_janczukowicz.gitlab.api.groupwebhooks.GitLabGroupHook;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class GroupWebhookTest {

    private static final String ID = "my-group";
    private static final String URL = "https://h.example";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private GroupWebhook sut(final GitLabGroupHook response) {
        return new GroupWebhook(GitLabSdk.of(gitLab).group(ID), response);
    }

    @Test
    void accessors_shouldReadResponse_whenNamed() {
        final GroupWebhook sut = sut(groupHookNamed(7L, URL, "ci"));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(7L);
            softly.assertThat(sut.url()).isEqualTo(URL);
            softly.assertThat(sut.name()).contains("ci");
            softly.assertThat(sut.group().id()).isEqualTo(ID);
        });
    }

    @Test
    void name_shouldBeEmpty_whenAbsent() {
        assertThat(sut(groupHook(7L, URL)).name()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(WebhookEvent.class)
    void isEnabledFor_shouldReadFlagFromResponse_whenOnlyThatFlagIsSet(final WebhookEvent event) {
        final GroupWebhook sut = sut(groupHookWithOnly(event));

        assertSoftly(softly -> {
            for (final WebhookEvent other : WebhookEvent.values()) {
                softly.assertThat(sut.isEnabledFor(other)).isEqualTo(other == event);
            }
        });
    }

    @Test
    void isEnabledFor_shouldBeFalse_whenFlagIsNull() {
        assertThat(sut(groupHook(7L, URL)).isEnabledFor(WebhookEvent.PUSH)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(WebhookEvent.class)
    void test_shouldSendMatchingTrigger_whenEventGiven(final WebhookEvent event) throws IOException {
        final GroupWebhook sut = sut(groupHook(7L, URL));

        sut.test(event);

        verify(gitLab.groupwebhooks())
                .createTest(
                        ID,
                        7L,
                        GitLabGroupGroupWebhooksCreateTestTrigger.fromWire(
                                event.name().toLowerCase(Locale.ROOT) + "_events"));
    }

    @Test
    void delete_shouldCallCoreDelete_whenCalled() throws IOException {
        sut(groupHook(7L, URL)).delete();

        verify(gitLab.groupwebhooks()).delete(ID, 7L);
    }

    @Test
    void refresh_shouldReturnLoadedWebhook_whenPresent() throws IOException {
        when(gitLab.groupwebhooks().get(ID, 7L))
                .thenReturn(Optional.of(groupHookNamed(7L, "https://new.example", "x")));

        final GroupWebhook refreshed = sut(groupHook(7L, URL)).refresh();

        assertThat(refreshed.url()).isEqualTo("https://new.example");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.groupwebhooks().get(ID, 7L)).thenReturn(Optional.empty());
        final GroupWebhook sut = sut(groupHook(7L, URL));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabGroupHook record = groupHook(7L, URL);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
