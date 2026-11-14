package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateFile;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projectsnippets.GitLabProjectProjectSnippetsCreateVisibility;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabNotFoundException;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.PersonalSnippet;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.PersonalSnippets;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.ProjectSnippets;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.Snippet;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetFile;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.SnippetVisibility;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Snippets. The project test works in the sandbox project: it creates a private two-file snippet titled
 * {@code smoke-<runId>} through the payload overload, reads its content and one file on the ref {@code main} (that
 * check is skipped when GitLab answers 404, because the default branch name of a snippet repository is unverified),
 * updates it (adds a file, moves a file with its previous path, deletes a file), reloads it and deletes it. The
 * personal test is READ-ONLY: personal snippets live on the token user's account, outside the sandbox, and writing
 * there is not approved, so it lists the user's snippets and loads the first one, and is skipped when the user has
 * none. NOT run live: creating, updating and deleting a personal snippet (covered by unit tests only), and public or
 * internal visibility. Each test is skipped when its first list call answers 403 or 404 (see {@link SmokeSkips}).
 */
@Tag("smoke")
class SnippetsSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String FIRST = "Created by the gitlab-sdk smoke tests.";
    private static final String SECOND = "puts 'second file'";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void projectSnippet_shouldRoundTrip_whenFilesChange() throws IOException {
        final ProjectSnippets snippets = ENV.sandbox().snippets();
        SmokeSkips.probe(
                "the snippets of the sandbox", () -> snippets.all().limit(5).toList());
        final String title = "smoke-" + ENV.runId();
        final Snippet created = snippets.create(GitLabProjectProjectSnippetsCreatePayloadBuilder.builder()
                .title(title)
                .visibility(GitLabProjectProjectSnippetsCreateVisibility.PRIVATE)
                .files(List.of(
                        new GitLabProjectProjectSnippetsCreateFile("first.txt", FIRST),
                        new GitLabProjectProjectSnippetsCreateFile("dir/second.rb", SECOND)))
                .build());
        SmokeCleanup.run(
                () -> {
                    final Optional<Snippet> found = snippets.find(created.id());
                    final String content = created.content();
                    final String fileContent = fileContentOnMain(created, "dir/second.rb");
                    final Snippet updated = created.update()
                            .addFile("third.txt", "third")
                            .moveFile("first.txt", "moved/first.txt")
                            .deleteFile("dir/second.rb")
                            .send();
                    final Snippet refreshed = updated.refresh();
                    final List<String> paths =
                            refreshed.files().stream().map(SnippetFile::path).toList();
                    refreshed.delete();
                    final Optional<Snippet> afterDelete = snippets.find(created.id());

                    assertSoftly(softly -> {
                        softly.assertThat(created.title()).isEqualTo(title);
                        softly.assertThat(created.visibility()).contains(SnippetVisibility.PRIVATE);
                        softly.assertThat(created.files())
                                .extracting(SnippetFile::path)
                                .containsExactlyInAnyOrder("first.txt", "dir/second.rb");
                        softly.assertThat(found).map(Snippet::id).contains(created.id());
                        softly.assertThat(content).isIn(FIRST, SECOND);
                        if (fileContent != null) {
                            softly.assertThat(fileContent).isEqualTo(SECOND);
                        }
                        softly.assertThat(paths).containsExactlyInAnyOrder("moved/first.txt", "third.txt");
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> deleteIfPresent(snippets, created.id()));
    }

    @Test
    void personalSnippet_shouldListAndFind_whenUserHasOne() throws IOException {
        final PersonalSnippets snippets = ENV.sdk().snippets();
        final List<PersonalSnippet> listed = SmokeSkips.probe(
                "the personal snippets", () -> snippets.all().limit(5).toList());
        assumeFalse(listed.isEmpty(), "the token user has no personal snippet");
        final PersonalSnippet first = listed.getFirst();

        final Optional<PersonalSnippet> found = snippets.find(first.id());

        assertSoftly(softly -> {
            softly.assertThat(found).map(PersonalSnippet::id).contains(first.id());
            softly.assertThat(found).map(PersonalSnippet::title).contains(first.title());
            softly.assertThat(first.visibilityName()).isPresent();
        });
    }

    /**
     * Reads a file on the ref {@code main}, or returns {@code null} when GitLab answers 404 (another default branch).
     */
    private static @Nullable String fileContentOnMain(final Snippet snippet, final String path) throws IOException {
        try {
            return snippet.fileContent("main", path);
        } catch (final GitLabNotFoundException e) {
            return null;
        }
    }

    private static void deleteIfPresent(final ProjectSnippets snippets, final long id) throws IOException {
        final Optional<Snippet> remaining = snippets.find(id);
        if (remaining.isPresent()) {
            remaining.get().delete();
        }
    }
}
