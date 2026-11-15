package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabBasicBadgeDetails;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.ApprovalConfiguration;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.GroupApprovalRule;
import io.github.piotr_janczukowicz.gitlab.sdk.approvals.ProjectApprovalRule;
import io.github.piotr_janczukowicz.gitlab.sdk.badges.Badge;
import io.github.piotr_janczukowicz.gitlab.sdk.badges.GroupBadge;
import io.github.piotr_janczukowicz.gitlab.sdk.members.LdapGroupLink;
import io.github.piotr_janczukowicz.gitlab.sdk.members.SamlGroupLink;
import io.github.piotr_janczukowicz.gitlab.sdk.mirrors.PullMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.mirrors.RemoteMirror;
import io.github.piotr_janczukowicz.gitlab.sdk.pushrules.GroupPushRules;
import io.github.piotr_janczukowicz.gitlab.sdk.pushrules.PushRules;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.AccessToken;
import io.github.piotr_janczukowicz.gitlab.sdk.tokens.GroupAccessToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Reads only, as every smoke test of the settings: nothing here writes. Creating, changing and deleting badges, push
 * rules, access tokens, mirrors, approval rules and group links is not run live. A token created over the API grants
 * access, and a mirror or a push rule changes how the sandbox behaves; the write paths are covered by unit tests with
 * mocks only. The areas that need a paid tier (push rules, the pull mirror, approval rules, the approval configuration,
 * the LDAP and SAML links) are availability probes: a 403 or 404 skips the test, any other error fails it. Whether the
 * core decodes a real answer of each area is checked only where the sandbox holds data of that area: a list that is
 * empty proves the request and the envelope, not the fields.
 */
@Tag("smoke")
class SettingsSmokeIT {

    private static final int LIMIT = 10;

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    @Test
    void projectBadges_shouldBeListedAndRendered_whenTokenCanReadTheProject() throws IOException {
        final Project project = ENV.sandbox();

        final List<Badge> badges = project.badges().limit(LIMIT).toList();
        final GitLabBasicBadgeDetails preview =
                project.previewBadge("https://example.test/%{project_path}", "https://example.test/badge.svg");
        final Optional<Badge> missing = project.badge(Long.MAX_VALUE);

        assertSoftly(softly -> {
            softly.assertThat(badges).allSatisfy(badge -> {
                softly.assertThat(badge.id()).isPositive();
                softly.assertThat(badge.linkUrl()).isNotBlank();
            });
            softly.assertThat(preview.renderedLinkUrl()).isNotBlank().doesNotContain("%{project_path}");
            softly.assertThat(missing).isEmpty();
        });
    }

    @Test
    void remoteMirrors_shouldBeListed_whenTokenCanReadTheProject() throws IOException {
        final Project project = ENV.sandbox();

        final List<RemoteMirror> mirrors = project.remoteMirrors().limit(LIMIT).toList();
        final Optional<RemoteMirror> missing = project.remoteMirror(Long.MAX_VALUE);

        assertSoftly(softly -> {
            softly.assertThat(mirrors)
                    .allSatisfy(mirror -> softly.assertThat(mirror.id()).isPositive());
            softly.assertThat(missing).isEmpty();
        });
    }

    @Test
    void accessTokens_shouldBeListed_whenTokenMayManageTheProject() throws IOException {
        final Project project = ENV.sandbox();

        final List<AccessToken> tokens = SmokeSkips.probe(
                "project access tokens",
                () -> project.accessTokens().limit(LIMIT).toList());
        final Optional<AccessToken> missing = project.accessToken(Long.MAX_VALUE);

        assertSoftly(softly -> {
            softly.assertThat(tokens).allSatisfy(token -> {
                softly.assertThat(token.id()).isPositive();
                softly.assertThat(token.name()).isNotBlank();
                softly.assertThat(token.scopeNames()).isNotEmpty();
            });
            softly.assertThat(missing).isEmpty();
        });
    }

    @Test
    void pushRules_shouldBeReadOrAbsent_whenTierAllows() throws IOException {
        final Optional<PushRules> rules =
                SmokeSkips.probe("push rules", () -> ENV.sandbox().pushRules());

        assertThat(rules.stream().toList())
                .allSatisfy(found -> assertThat(found.id()).isPositive());
    }

    @Test
    void pullMirror_shouldBeReadOrAbsent_whenTierAllows() throws IOException {
        final Optional<PullMirror> mirror =
                SmokeSkips.probe("the pull mirror", () -> ENV.sandbox().pullMirror());

        assertThat(mirror.stream().toList())
                .allSatisfy(found -> assertThat(found.id()).isPositive());
    }

    @Test
    void approvalRules_shouldBeListed_whenTierAllows() throws IOException {
        final Project project = ENV.sandbox();

        final List<ProjectApprovalRule> rules = SmokeSkips.probe(
                "project approval rules",
                () -> project.approvalRules().limit(LIMIT).toList());
        final Optional<ApprovalConfiguration> configuration =
                SmokeSkips.probe("the approval configuration", project::approvalConfiguration);

        assertSoftly(softly -> {
            softly.assertThat(rules.stream().toList())
                    .allSatisfy(rule -> softly.assertThat(rule.id()).isPositive());
            softly.assertThat(configuration.stream().toList())
                    .allSatisfy(found -> softly.assertThat(found.project().id()).isEqualTo(project.id()));
        });
    }

    @Test
    void avatar_shouldBeDownloadedOrAbsent_whenProjectIsReadable() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final long written =
                SmokeSkips.probe("the project avatar", () -> ENV.sandbox().avatar(out));

        assertThat(out.size()).isEqualTo(written);
    }

    @Test
    void groupSettings_shouldBeReadable_whenSandboxGroupIsConfigured() throws IOException {
        ENV.requireGroupConfigured();
        final Group group = ENV.sandboxGroup();

        final List<GroupBadge> badges = group.badges().limit(LIMIT).toList();
        final List<GroupAccessToken> tokens = SmokeSkips.probe(
                "group access tokens", () -> group.accessTokens().limit(LIMIT).toList());

        assertSoftly(softly -> {
            softly.assertThat(badges)
                    .allSatisfy(badge -> softly.assertThat(badge.id()).isPositive());
            softly.assertThat(tokens)
                    .allSatisfy(token -> softly.assertThat(token.id()).isPositive());
        });
    }

    @Test
    void groupPremiumSettings_shouldBeReadable_whenTierAllows() throws IOException {
        ENV.requireGroupConfigured();
        final Group group = ENV.sandboxGroup();

        final Optional<GroupPushRules> rules = SmokeSkips.probe("group push rules", group::pushRules);
        final List<GroupApprovalRule> approvalRules = SmokeSkips.probe(
                "group approval rules", () -> group.approvalRules().limit(LIMIT).toList());
        final List<SamlGroupLink> saml =
                SmokeSkips.probe("SAML group links", () -> group.samlLinks().toList());
        final List<LdapGroupLink> ldap =
                SmokeSkips.probe("LDAP group links", () -> group.ldapLinks().toList());

        assertSoftly(softly -> {
            softly.assertThat(rules.stream().toList())
                    .allSatisfy(found -> softly.assertThat(found.id()).isPositive());
            softly.assertThat(approvalRules)
                    .allSatisfy(rule -> softly.assertThat(rule.id()).isPositive());
            softly.assertThat(saml)
                    .allSatisfy(link -> softly.assertThat(link.name()).isNotBlank());
            softly.assertThat(ldap)
                    .allSatisfy(link -> softly.assertThat(link.provider()).isNotBlank());
        });
    }
}
