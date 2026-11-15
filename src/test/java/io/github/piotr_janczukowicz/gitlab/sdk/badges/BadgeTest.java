package io.github.piotr_janczukowicz.gitlab.sdk.badges;

import static io.github.piotr_janczukowicz.gitlab.sdk.badges.BadgesFixtures.IMAGE;
import static io.github.piotr_janczukowicz.gitlab.sdk.badges.BadgesFixtures.LINK;
import static io.github.piotr_janczukowicz.gitlab.sdk.badges.BadgesFixtures.badge;
import static io.github.piotr_janczukowicz.gitlab.sdk.badges.BadgesFixtures.namedBadge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBadge;
import io.github.piotr_janczukowicz.gitlab.api.projectbadges.GitLabProjectProjectBadgesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class BadgeTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private Badge sut(final GitLabBadge response) {
        return new Badge(GitLabSdk.of(gitLab).project(ID), response);
    }

    private GitLabProjectProjectBadgesUpdatePayload sentUpdate() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectBadgesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectBadgesUpdatePayload.class);
        verify(gitLab.projectbadges()).update(eq(ID), eq(7L), payload.capture());
        return payload.getValue();
    }

    @Test
    void accessors_shouldReadResponse_whenAllFieldsPresent() {
        final Badge sut = sut(namedBadge(7L, "pipeline"));

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(7L);
            softly.assertThat(sut.name()).contains("pipeline");
            softly.assertThat(sut.linkUrl()).isEqualTo(LINK);
            softly.assertThat(sut.imageUrl()).isEqualTo(IMAGE);
            softly.assertThat(sut.renderedLinkUrl()).contains("https://example.test/pipelines/demo");
            softly.assertThat(sut.renderedImageUrl()).contains("https://example.test/rendered.svg");
            softly.assertThat(sut.project().id()).isEqualTo(ID);
        });
    }

    @Test
    void optionalAccessors_shouldBeEmpty_whenResponseLeavesThemOut() {
        final Badge sut = sut(badge(7L));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEmpty();
            softly.assertThat(sut.renderedLinkUrl()).isEmpty();
            softly.assertThat(sut.renderedImageUrl()).isEmpty();
        });
    }

    @Test
    void send_shouldSendOnlySetFields_whenNameIsSet() throws IOException {
        when(gitLab.projectbadges().update(eq(ID), eq(7L), any(GitLabProjectProjectBadgesUpdatePayload.class)))
                .thenReturn(namedBadge(7L, "renamed"));

        final Badge result = sut(badge(7L)).update().name("renamed").send();

        final GitLabProjectProjectBadgesUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.name()).isEqualTo("renamed");
            softly.assertThat(payload.linkUrl()).isNull();
            softly.assertThat(payload.imageUrl()).isNull();
            softly.assertThat(result.name()).contains("renamed");
        });
    }

    @Test
    void send_shouldSendLinkAndImage_whenBothAreSet() throws IOException {
        when(gitLab.projectbadges().update(eq(ID), eq(7L), any(GitLabProjectProjectBadgesUpdatePayload.class)))
                .thenReturn(badge(7L));

        sut(badge(7L))
                .update()
                .linkUrl("https://new.test/l")
                .imageUrl("https://new.test/i.svg")
                .send();

        final GitLabProjectProjectBadgesUpdatePayload payload = sentUpdate();
        assertSoftly(softly -> {
            softly.assertThat(payload.linkUrl()).isEqualTo("https://new.test/l");
            softly.assertThat(payload.imageUrl()).isEqualTo("https://new.test/i.svg");
            softly.assertThat(payload.name()).isNull();
        });
    }

    @Test
    void send_shouldThrowIllegalStateException_whenNothingSet() {
        final BadgeUpdate update = sut(badge(7L)).update();

        final Throwable thrown = catchThrowable(update::send);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void setters_shouldThrowIllegalArgumentException_whenValueIsBlank() {
        final BadgeUpdate update = sut(badge(7L)).update();

        final Throwable blankLink = catchThrowable(() -> update.linkUrl(" "));
        final Throwable blankImage = catchThrowable(() -> update.imageUrl(" "));
        final Throwable blankName = catchThrowable(() -> update.name(" "));

        assertSoftly(softly -> {
            softly.assertThat(blankLink).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankImage).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankName).isInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void setters_shouldThrowNullPointerException_whenValueIsNull() {
        final BadgeUpdate update = sut(badge(7L)).update();

        final Throwable thrown = catchThrowable(() -> update.name(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void delete_shouldCallCoreDelete_whenCalled() throws IOException {
        sut(badge(7L)).delete();

        verify(gitLab.projectbadges()).delete(ID, 7L);
    }

    @Test
    void refresh_shouldReturnLoadedBadge_whenPresent() throws IOException {
        when(gitLab.projectbadges().get(ID, 7L)).thenReturn(Optional.of(namedBadge(7L, "fresh")));

        final Badge refreshed = sut(badge(7L)).refresh();

        assertThat(refreshed.name()).contains("fresh");
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.projectbadges().get(ID, 7L)).thenReturn(Optional.empty());
        final Badge sut = sut(badge(7L));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("7");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabBadge record = badge(7L);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
