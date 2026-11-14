package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.sdk.wikis.Wiki;
import io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikiFormat;
import io.github.piotr_janczukowicz.gitlab.sdk.wikis.WikiPage;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Throwaway wiki pages titled {@code smoke-<runId>/page}, so the slug holds a slash. The project test creates a page,
 * finds it by its slug, updates its content and then its title (GitLab gives the page a new slug), lists the wiki and
 * deletes the page. The group test needs the optional {@code SMOKE_GITLAB_GROUP} and a Premium instance, and creates,
 * finds and deletes a page. A test is skipped when its first list call answers 403 or 404, as when the wiki is disabled
 * or the tier lacks group wikis (see {@link SmokeSkips}). Uploading an attachment is NOT run live, because GitLab
 * offers no call that removes an uploaded file.
 */
@Tag("smoke")
class WikisSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void page_shouldRoundTripAndGetNewSlug_whenTitleChanges() throws IOException {
        final Wiki wiki = ENV.sandbox().wiki();
        final String title = "smoke-" + ENV.runId() + "/page";
        final String renamed = "smoke-" + ENV.runId() + "/renamed";
        SmokeSkips.probe("the wiki of the sandbox", () -> wiki.all().limit(5).toList());
        final WikiPage created = wiki.create(title, "Created by the gitlab-sdk smoke tests.");
        final AtomicReference<String> current = new AtomicReference<>(created.slug());
        SmokeCleanup.run(
                () -> {
                    final Optional<WikiPage> found = wiki.find(created.slug());
                    final WikiPage edited = created.update()
                            .content("Edited by the smoke tests.")
                            .send();
                    final WikiPage moved = edited.update().title(renamed).send();
                    current.set(moved.slug());
                    final Optional<WikiPage> foundMoved = wiki.find(moved.slug());
                    final boolean listed =
                            wiki.all().anyMatch(each -> each.slug().equals(moved.slug()));
                    final WikiPage refreshed = moved.refresh();
                    refreshed.delete();
                    final Optional<WikiPage> afterDelete = wiki.find(moved.slug());

                    assertSoftly(softly -> {
                        softly.assertThat(created.title()).isEqualTo(title);
                        softly.assertThat(created.formatName()).isPresent();
                        softly.assertThat(found).map(WikiPage::slug).contains(created.slug());
                        softly.assertThat(edited.content()).contains("Edited by the smoke tests.");
                        softly.assertThat(moved.slug()).isNotEqualTo(created.slug());
                        softly.assertThat(foundMoved).map(WikiPage::title).contains(renamed);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> deleteIfPresent(wiki, current.get()),
                () -> deleteIfPresent(wiki, created.slug()));
    }

    @Test
    void groupPage_shouldRoundTrip_whenSandboxGroupIsSet() throws IOException {
        ENV.requireGroupConfigured();
        final Wiki wiki = ENV.sandboxGroup().wiki();
        final String title = "smoke-" + ENV.runId() + "/group-page";
        SmokeSkips.probe(
                "the wiki of the sandbox group", () -> wiki.all().limit(5).toList());
        final WikiPage created = wiki.create(title, "Created by the gitlab-sdk smoke tests.", WikiFormat.MARKDOWN);
        SmokeCleanup.run(
                () -> {
                    final Optional<WikiPage> found = wiki.find(created.slug());
                    created.delete();
                    final Optional<WikiPage> afterDelete = wiki.find(created.slug());

                    assertSoftly(softly -> {
                        softly.assertThat(created.title()).isEqualTo(title);
                        softly.assertThat(found).map(WikiPage::slug).contains(created.slug());
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> deleteIfPresent(wiki, created.slug()));
    }

    private static void deleteIfPresent(final Wiki wiki, final String slug) throws IOException {
        final Optional<WikiPage> remaining = wiki.find(slug);
        if (remaining.isPresent()) {
            remaining.get().delete();
        }
    }
}
