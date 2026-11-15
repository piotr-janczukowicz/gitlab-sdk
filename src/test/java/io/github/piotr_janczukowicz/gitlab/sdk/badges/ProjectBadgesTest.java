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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBasicBadgeDetails;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBasicBadgeDetailsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectbadges.GitLabProjectProjectBadgesCreatePayload;
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
class ProjectBadgesTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private GitLabProjectProjectBadgesCreatePayload sentCreate() throws IOException {
        final ArgumentCaptor<GitLabProjectProjectBadgesCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectBadgesCreatePayload.class);
        verify(gitLab.projectbadges()).create(eq(ID), payload.capture());
        return payload.getValue();
    }

    @Test
    void badges_shouldBeLazy_whenStreamIsLimited() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projectbadges().list(ID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return badge(id);
                }));

        final Stream<Badge> badges = project.badges();

        assertThat(consumed).hasValue(0);
        assertThat(badges.limit(1).toList())
                .singleElement()
                .satisfies(b -> assertThat(b.id()).isEqualTo(1L));
        assertThat(consumed).hasValue(1);
        verify(gitLab.projectbadges(), never()).get(any(String.class), any(Long.class));
    }

    @Test
    void badge_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projectbadges().get(ID, 5L)).thenReturn(Optional.empty());

        assertThat(project.badge(5L)).isEmpty();
    }

    @Test
    void badge_shouldReturnBadge_whenPresent() throws IOException {
        when(gitLab.projectbadges().get(ID, 5L)).thenReturn(Optional.of(namedBadge(5L, "ci")));

        assertThat(project.badge(5L)).get().satisfies(b -> assertThat(b.name()).contains("ci"));
    }

    @Test
    void addBadge_shouldSendLinkAndImageOnly_whenNoNameIsGiven() throws IOException {
        when(gitLab.projectbadges().create(eq(ID), any(GitLabProjectProjectBadgesCreatePayload.class)))
                .thenReturn(badge(5L));

        final Badge result = project.addBadge(LINK, IMAGE);

        final GitLabProjectProjectBadgesCreatePayload payload = sentCreate();
        assertSoftly(softly -> {
            softly.assertThat(payload.linkUrl()).isEqualTo(LINK);
            softly.assertThat(payload.imageUrl()).isEqualTo(IMAGE);
            softly.assertThat(payload.name()).isNull();
            softly.assertThat(result.id()).isEqualTo(5L);
        });
    }

    @Test
    void addBadge_shouldSendName_whenNameIsGiven() throws IOException {
        when(gitLab.projectbadges().create(eq(ID), any(GitLabProjectProjectBadgesCreatePayload.class)))
                .thenReturn(namedBadge(5L, "ci"));

        project.addBadge(LINK, IMAGE, "ci");

        assertThat(sentCreate().name()).isEqualTo("ci");
    }

    @Test
    void addBadge_shouldThrowIllegalArgumentException_whenArgumentIsBlank() {
        final Throwable blankLink = catchThrowable(() -> project.addBadge(" ", IMAGE));
        final Throwable blankImage = catchThrowable(() -> project.addBadge(LINK, " "));
        final Throwable blankName = catchThrowable(() -> project.addBadge(LINK, IMAGE, " "));

        assertSoftly(softly -> {
            softly.assertThat(blankLink).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankImage).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankName).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void addBadge_shouldThrowNullPointerException_whenArgumentIsNull() {
        final Throwable nullLink = catchThrowable(() -> project.addBadge(null, IMAGE));
        final Throwable nullImage = catchThrowable(() -> project.addBadge(LINK, null));

        assertSoftly(softly -> {
            softly.assertThat(nullLink).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullImage).isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void previewBadge_shouldReturnCoreRecord_whenCalled() throws IOException {
        final GitLabBasicBadgeDetails rendered = GitLabBasicBadgeDetailsBuilder.builder()
                .renderedLinkUrl("https://example.test/rendered")
                .build();
        when(gitLab.projectbadges().getRender(ID, LINK, IMAGE)).thenReturn(rendered);

        assertThat(project.previewBadge(LINK, IMAGE)).isSameAs(rendered);
    }

    @Test
    void previewBadge_shouldThrowIllegalArgumentException_whenArgumentIsBlank() {
        final Throwable thrown = catchThrowable(() -> project.previewBadge(LINK, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gitLab);
    }
}
