package io.github.piotr_janczukowicz.gitlab.sdk.wikis;

import static io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikisFixtures.page;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@UnitTest
class WikiPageUpdateTest {

    private static final String ID = "my-group/my-project";
    private static final String SECRET = "do-not-print-this-content";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final WikiPage page = new WikiPage(GitLabSdk.of(gitLab).project(ID).wiki(), page("home"));

    @Test
    void update_shouldThrowIllegalStateException_whenNothingSet() throws IOException {
        final Throwable thrown = catchThrowable(() -> page.update().send());

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(gitLab.projectwikis(), never()).update(anyString(), anyString(), any());
    }

    @Test
    void setters_shouldRejectInvalidArguments_withoutMarkingUpdate() throws IOException {
        final WikiPageUpdate sut = page.update();

        final Throwable blankTitle = catchThrowable(() -> sut.title(" "));
        final Throwable nullContent = catchThrowable(() -> sut.content(null));
        final Throwable nullFormat = catchThrowable(() -> sut.format(null));
        final Throwable send = catchThrowable(sut::send);

        assertSoftly(softly -> {
            softly.assertThat(blankTitle).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullContent).isInstanceOf(NullPointerException.class);
            softly.assertThat(nullFormat).isInstanceOf(NullPointerException.class);
            softly.assertThat(send).isInstanceOf(IllegalStateException.class);
        });
        verify(gitLab.projectwikis(), never()).update(anyString(), anyString(), any());
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenPageIsNull() {
        final Throwable thrown = catchThrowable(() -> new WikiPageUpdate(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void toString_shouldNotPrintContent_whenChangesHoldContent() {
        final WikiPageChanges withContent = new WikiPageChanges("Home", SECRET, WikiFormat.MARKDOWN);
        final WikiPageChanges withoutContent = new WikiPageChanges(null, null, null);

        assertSoftly(softly -> {
            softly.assertThat(withContent.toString())
                    .doesNotContain(SECRET)
                    .contains("Home", "MARKDOWN", "content set");
            softly.assertThat(withoutContent.toString()).contains("content unset");
        });
    }
}
