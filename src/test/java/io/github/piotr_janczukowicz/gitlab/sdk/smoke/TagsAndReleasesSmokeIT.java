package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.releases.Release;
import io.github.piotr_janczukowicz.gitlab.sdk.releases.ReleaseLink;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A throwaway tag on the default branch of the sandbox, with a release and a release link on it. Needs a role that may
 * create tags and releases. Collecting release evidence is not run live.
 */
@Tag("smoke")
class TagsAndReleasesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void tagAndRelease_shouldRoundTrip_whenCreatedUpdatedAndDeleted() throws IOException {
        final Project project = ENV.sandbox();
        final String name = ENV.name("tag");
        final String defaultBranch = project.fetch().orElseThrow().response().defaultBranch();
        assumeTrue(defaultBranch != null, "the sandbox has no default branch (empty repository)");
        final io.github.piotr_janczukowicz.gitlab.sdk.code.Tag created = project.createTag(name, defaultBranch);
        SmokeCleanup.run(
                () -> {
                    final Optional<io.github.piotr_janczukowicz.gitlab.sdk.code.Tag> loaded = project.tag(name);
                    final boolean isProtected = created.isProtected();
                    final Release release = project.createRelease(name);
                    final Release named =
                            release.update().name(ENV.name("release")).send();
                    final ReleaseLink link = release.addLink("smoke link", "https://example.com/smoke-" + ENV.runId());
                    final List<ReleaseLink> links = release.links().limit(20).toList();
                    final Optional<Release> viaTag = created.release();
                    link.delete();
                    final long linksAfterDelete = release.links().limit(20).count();
                    release.delete();
                    final Optional<Release> afterDelete = project.release(name);
                    created.delete();
                    final Optional<io.github.piotr_janczukowicz.gitlab.sdk.code.Tag> tagAfterDelete = project.tag(name);

                    assertSoftly(softly -> {
                        softly.assertThat(created.name()).isEqualTo(name);
                        softly.assertThat(loaded)
                                .map(io.github.piotr_janczukowicz.gitlab.sdk.code.Tag::name)
                                .contains(name);
                        softly.assertThat(isProtected).isIn(true, false);
                        softly.assertThat(release.tagName()).isEqualTo(name);
                        softly.assertThat(named.name()).contains(ENV.name("release"));
                        softly.assertThat(links).extracting(ReleaseLink::id).contains(link.id());
                        softly.assertThat(viaTag).map(Release::tagName).contains(name);
                        softly.assertThat(linksAfterDelete).isZero();
                        softly.assertThat(afterDelete).isEmpty();
                        softly.assertThat(tagAfterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<Release> remaining = project.release(name);
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                },
                () -> {
                    final Optional<io.github.piotr_janczukowicz.gitlab.sdk.code.Tag> remaining = project.tag(name);
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }
}
