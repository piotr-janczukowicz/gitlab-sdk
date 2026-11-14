package io.github.piotr_janczukowicz.gitlab.sdk.smoke;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.projectlevelvariables.GitLabProjectProjectLevelVariablesCreatePayloadBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.GroupVariable;
import io.github.piotr_janczukowicz.gitlab.sdk.variables.Variable;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Throwaway CI/CD variables named {@code SMOKE_<RUNID>}. The project test proves that an update and a delete target the
 * variable by key and its own environment scope, with a second variable of the same key in another scope, and that the
 * SDK objects never print a value. The group test needs the optional {@code SMOKE_GITLAB_GROUP} and is skipped without
 * it. Nothing is masked or hidden: those flags are not run live. A test is skipped when its first list call answers 403
 * or 404, as when CI/CD is disabled or the role may not manage variables (see {@link SmokeSkips}).
 */
@Tag("smoke")
class VariablesSmokeIT {

    private static final SmokeEnvironment ENV = SmokeEnvironment.fromEnv();
    private static final String DEFAULT_SCOPE = "*";

    @BeforeAll
    static void requireToken() {
        ENV.requireConfigured();
    }

    private static String key() {
        return "SMOKE_" + ENV.runId().toUpperCase(Locale.ROOT);
    }

    @Test
    void variable_shouldRoundTripAndTargetItsScope_whenSameKeyExistsInTwoScopes() throws IOException {
        final Project project = ENV.sandbox();
        final String key = key();
        final String scope = ENV.name("scope");
        final String secret = "smoke-secret-" + ENV.runId();
        final String changed = "smoke-changed-" + ENV.runId();
        final String other = "smoke-other-" + ENV.runId();
        SmokeSkips.probe(
                "the CI/CD variables of the sandbox",
                () -> project.variables().limit(1).toList());
        final Variable created = project.createVariable(key, secret);
        SmokeCleanup.run(
                () -> {
                    final Variable updated = created.update().value(changed).send();
                    final Variable refreshed = updated.refresh();
                    final Variable rescoped =
                            refreshed.update().environmentScope(scope).send();
                    final Optional<Variable> inNewScope = project.variable(key, scope);
                    // the same key again, now in the default scope that the first variable has just left
                    final Variable second =
                            project.createVariable(GitLabProjectProjectLevelVariablesCreatePayloadBuilder.builder()
                                    .key(key)
                                    .value(other)
                                    .environmentScope(DEFAULT_SCOPE)
                                    .build());
                    final Variable secondChanged =
                            second.update().value(other + "-2").send();
                    final Variable scopedAfter = rescoped.refresh();
                    final boolean listed = project.variables()
                            .limit(100)
                            .anyMatch(each -> each.key().equals(key));
                    final String printed = created + " " + updated + " " + rescoped + " " + second + " "
                            + created.update().value(secret);
                    secondChanged.delete();
                    final Optional<Variable> secondAfterDelete = project.variable(key, DEFAULT_SCOPE);
                    final Optional<Variable> scopedStillThere = project.variable(key, scope);
                    scopedAfter.delete();
                    final Optional<Variable> scopedAfterDelete = project.variable(key, scope);

                    assertSoftly(softly -> {
                        softly.assertThat(created.key()).isEqualTo(key);
                        softly.assertThat(updated.value()).contains(changed);
                        softly.assertThat(refreshed.value()).contains(changed);
                        softly.assertThat(rescoped.environmentScope()).contains(scope);
                        softly.assertThat(inNewScope).map(Variable::key).contains(key);
                        softly.assertThat(secondChanged.environmentScope()).contains(DEFAULT_SCOPE);
                        softly.assertThat(scopedAfter.value()).contains(changed);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(printed).doesNotContain(secret, changed, other);
                        softly.assertThat(secondAfterDelete).isEmpty();
                        softly.assertThat(scopedStillThere).isPresent();
                        softly.assertThat(scopedAfterDelete).isEmpty();
                    });
                },
                () -> deleteIfPresent(project, key, scope),
                () -> deleteIfPresent(project, key, DEFAULT_SCOPE));
    }

    @Test
    void groupVariable_shouldRoundTrip_whenSandboxGroupIsSet() throws IOException {
        ENV.requireGroupConfigured();
        final Group group = ENV.sandboxGroup();
        final String key = key() + "_G";
        final String secret = "smoke-secret-" + ENV.runId();
        final String changed = "smoke-changed-" + ENV.runId();
        SmokeSkips.probe(
                "the CI/CD variables of the sandbox group",
                () -> group.variables().limit(1).toList());
        final GroupVariable created = group.createVariable(key, secret);
        SmokeCleanup.run(
                () -> {
                    final GroupVariable updated =
                            created.update().value(changed).send();
                    final GroupVariable refreshed = updated.refresh();
                    final boolean listed = group.variables()
                            .limit(100)
                            .anyMatch(each -> each.key().equals(key));
                    final String printed = created + " " + updated + " " + refreshed;
                    refreshed.delete();
                    final Optional<GroupVariable> afterDelete = group.variable(key);

                    assertSoftly(softly -> {
                        softly.assertThat(created.key()).isEqualTo(key);
                        softly.assertThat(updated.value()).contains(changed);
                        softly.assertThat(refreshed.value()).contains(changed);
                        softly.assertThat(listed).isTrue();
                        softly.assertThat(printed).doesNotContain(secret, changed);
                        softly.assertThat(afterDelete).isEmpty();
                    });
                },
                () -> {
                    final Optional<GroupVariable> remaining = group.variable(key);
                    if (remaining.isPresent()) {
                        remaining.get().delete();
                    }
                });
    }

    private static void deleteIfPresent(final Project project, final String key, final String scope)
            throws IOException {
        final Optional<Variable> remaining = project.variable(key, scope);
        if (remaining.isPresent()) {
            remaining.get().delete();
        }
    }
}
