package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLicense;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabTemplateList;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabInstanceProjectsListFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.TemplateType;
import io.github.piotr_janczukowicz.gitlab.sdk.User;
import io.github.piotr_janczukowicz.gitlab.sdk.account.Email;
import io.github.piotr_janczukowicz.gitlab.sdk.account.GpgKey;
import io.github.piotr_janczukowicz.gitlab.sdk.account.SshKey;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only, as every smoke test of the account and of the lists: nothing here writes. Adding and deleting keys, email
 * addresses and GPG keys of the signed-in user, starring and unstarring, and housekeeping are not run live: they change
 * the account or the repository, outside the sandbox project's own data.
 */
@Tag("smoke")
class AccountSmokeIT {

    private static final int LIMIT = 5;

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void lists_shouldLoadObjects_whenTokenCanSeeThem() throws IOException {
        final List<Project> projects = ENV.sdk().projects().limit(LIMIT).toList();
        final long groups = ENV.sdk().groups().limit(LIMIT).count();
        final long users = ENV.sdk().users().limit(LIMIT).count();

        assertSoftly(softly -> {
            softly.assertThat(projects)
                    .allSatisfy(project -> assertThat(project.response().id()).isNotNull());
            softly.assertThat(groups).isGreaterThanOrEqualTo(0);
            softly.assertThat(users).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void userLists_shouldWork_whenQueriedForTheSignedInUser() throws IOException {
        final User me = ENV.sdk().me().profile();

        final long personal = ENV.sdk()
                .userProjects(me.id(), GitLabInstanceProjectsListFilter.empty())
                .limit(LIMIT)
                .count();
        final long starred = ENV.sdk().starredProjects(me.id()).limit(LIMIT).count();
        final Optional<User> byName = ENV.sdk().userByUsername(me.username());

        assertSoftly(softly -> {
            softly.assertThat(personal).isGreaterThanOrEqualTo(0);
            softly.assertThat(starred).isGreaterThanOrEqualTo(0);
            softly.assertThat(byName).map(User::id).contains(me.id());
        });
    }

    @Test
    void sandboxReads_shouldWork_whenTokenHasAccess() throws IOException {
        final Project sandbox = ENV.sandbox();

        final long users = sandbox.users().limit(LIMIT).count();
        final long ancestors = sandbox.groups().limit(LIMIT).count();
        final long invited = sandbox.sharedGroups().limit(LIMIT).count();
        final long events = sandbox.events().limit(LIMIT).count();
        final Map<String, Double> languages = sandbox.languages();

        assertSoftly(softly -> {
            softly.assertThat(users).isGreaterThanOrEqualTo(1);
            softly.assertThat(ancestors).isGreaterThanOrEqualTo(0);
            softly.assertThat(invited).isGreaterThanOrEqualTo(0);
            softly.assertThat(events).isGreaterThanOrEqualTo(0);
            softly.assertThat(languages).isNotNull();
        });
    }

    @Test
    void templates_shouldListAndLoadOne_whenGitLabHasLicenseTemplates() throws IOException {
        final Project sandbox = ENV.sandbox();
        final Optional<GitLabTemplateList> first =
                sandbox.templates(TemplateType.LICENSES).findFirst();
        assumeTrue(first.isPresent(), "the instance lists no license templates");

        final Optional<GitLabLicense> loaded =
                sandbox.template(TemplateType.LICENSES, first.orElseThrow().key());
        final Optional<GitLabLicense> missing = sandbox.template(TemplateType.LICENSES, ENV.name("missing"));

        assertSoftly(softly -> {
            softly.assertThat(loaded).isPresent();
            softly.assertThat(missing).isEmpty();
        });
    }

    @Test
    void accountKeys_shouldBeListed_whenTokenBelongsToAUser() throws IOException {
        final List<SshKey> sshKeys = ENV.sdk().me().sshKeys().toList();
        final List<Email> emails = ENV.sdk().me().emails().toList();
        final List<GpgKey> gpgKeys = ENV.sdk().me().gpgKeys().toList();

        assertSoftly(softly -> {
            softly.assertThat(sshKeys)
                    .allSatisfy(key -> softly.assertThat(key.id()).isPositive());
            softly.assertThat(emails)
                    .allSatisfy(email -> softly.assertThat(email.address()).isNotBlank());
            softly.assertThat(gpgKeys)
                    .allSatisfy(key -> softly.assertThat(key.id()).isPositive());
        });
    }
}
