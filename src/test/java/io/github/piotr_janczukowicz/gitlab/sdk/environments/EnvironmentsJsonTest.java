package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentExtended;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabEnvironment;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class EnvironmentsJsonTest {

    private static final String USER = """
            {"id": 1, "username": "john_smith", "name": "John Smith", "state": "active", "locked": false,
             "avatar_url": "http://localhost:3000/uploads/user/avatar/1/cd8.jpeg",
             "web_url": "http://localhost:3000/john_smith"}""";

    private static final String JOB = """
            {"id": 1278, "status": "success", "stage": "deploy", "name": "deploy", "ref": "main", "tag": false,
             "coverage": null, "allow_failure": false, "created_at": "2026-11-13T10:00:00.000Z",
             "started_at": "2026-11-13T10:00:01.000Z", "finished_at": "2026-11-13T10:02:00.123+01:00",
             "erased_at": null, "duration": 119.877, "queued_duration": 0.010, "user": %s,
             "commit": {"id": "a91957a858320c0e17f3a0eca7cfacbff50ea29a", "short_id": "a91957a8",
                        "created_at": "2026-11-13T09:00:00.000+02:00", "parent_ids": ["fa1e"], "title": "Merge",
                        "message": "Merge branch", "author_name": "Administrator",
                        "author_email": "admin@example.com", "authored_date": "2026-11-13T09:00:00.000+02:00",
                        "committer_name": "Administrator", "committer_email": "admin@example.com",
                        "committed_date": "2026-11-13T09:00:00.000+02:00"},
             "pipeline": {"id": 4, "iid": 2, "project_id": 3, "sha": "a91957a8", "ref": "main",
                          "status": "success", "source": "push", "created_at": "2026-11-13T10:00:00.000Z",
                          "updated_at": "2026-11-13T10:02:00.000Z", "web_url": "http://localhost/p/-/pipelines/4"},
             "web_url": "http://localhost/p/-/jobs/1278", "artifacts": [], "runner": null,
             "artifacts_expire_at": null, "tag_list": ["docker", "linux"]}""".formatted(USER);

    private static final String ENVIRONMENT = """
            {"id": 1, "name": "review/fix-foo", "slug": "review-fix-foo-dfjre3", "external_url": null,
             "created_at": "2026-11-13T10:00:00.000Z", "updated_at": "2026-11-13T10:00:00.000+01:00",
             "tier": "development",
             "project": {"id": 3, "description": null, "name": "awesome-project",
                         "name_with_namespace": "Administrator / awesome-project", "path": "awesome-project",
                         "path_with_namespace": "root/awesome-project", "created_at": "2026-11-13T10:00:00.000Z",
                         "default_branch": "main", "tag_list": [], "topics": ["ci"],
                         "web_url": "http://localhost/root/awesome-project", "readme_url": null,
                         "avatar_url": null, "star_count": 0, "forks_count": 0,
                         "last_activity_at": "2026-11-13T10:00:00.000Z",
                         "namespace": {"id": 1, "name": "root", "path": "root", "kind": "user", "full_path": "root",
                                       "parent_id": null, "avatar_url": null, "web_url": "http://localhost/root"}},
             "last_deployment": {"id": 100, "iid": 34, "ref": "fdroid", "sha": "416d8ea1",
                                 "created_at": "2026-11-13T10:00:00.000Z", "updated_at": null,
                                 "status": "success", "user": %s, "environment": null, "deployable": %s},
             "state": "available", "auto_stop_at": "2026-11-20T10:00:00.000Z",
             "cluster_agent": {"id": 1, "name": "agent-1",
                               "config_project": {"id": 20, "description": "", "name": "test",
                                                  "name_with_namespace": "Administrator / test", "path": "test",
                                                  "path_with_namespace": "root/test",
                                                  "created_at": "2026-11-13T10:00:00.000Z"},
                               "created_at": "2026-11-13T10:00:00.000Z", "created_by_user_id": 42},
             "kubernetes_namespace": "flux-system", "flux_resource_path": "HelmRelease/flux-system",
             "description": null, "auto_stop_setting": "always"}""".formatted(USER, JOB);

    private static final String DEPLOYMENT = """
            {"id": 42, "iid": 2, "ref": "main", "sha": "a91957a8", "created_at": "2026-11-13T10:00:00.000Z",
             "updated_at": "2026-11-13T10:02:00.000Z", "user": %s,
             "environment": {"id": 9, "name": "production", "slug": "production",
                             "external_url": "https://about.gitlab.com", "created_at": "2026-11-13T10:00:00.000Z",
                             "updated_at": "2026-11-13T10:00:00.000Z"},
             "deployable": %s, "status": "success"}""".formatted(USER, JOB);

    private static final String BLOCKED_DEPLOYMENT = """
            {"id": 41, "iid": 1, "ref": "main", "sha": "99d03678", "created_at": "2026-11-13T10:00:00.000Z",
             "updated_at": "2026-11-13T10:00:00.000Z", "user": %1$s,
             "environment": {"id": 9, "name": "production", "slug": "production", "external_url": null,
                             "created_at": "2026-11-13T10:00:00.000Z", "updated_at": "2026-11-13T10:00:00.000Z"},
             "deployable": %2$s, "status": "blocked", "pending_approval_count": 1,
             "approvals": [{"user": %1$s, "status": "approved", "created_at": "2026-11-13T10:05:00.000Z",
                            "comment": "LGTM"}],
             "approval_summary": {"rules": [{"user_id": null, "group_id": 134, "access_level": null,
                                             "access_level_description": "qa-group", "required_approvals": 2,
                                             "deployment_approvals": [{"user": %1$s, "status": "approved",
                                                                       "created_at": "2026-11-13T10:05:00.000Z",
                                                                       "comment": "LGTM"}]}]}}""".formatted(USER, JOB);

    private final Project project =
            GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)).project("root/awesome-project");

    @Test
    void accessors_shouldReadRealShapedJson_whenEnvironmentHasNestedObjects() {
        final GitLabEnvironment decoded = decode(ENVIRONMENT, GitLabEnvironment.class);

        final Environment sut = new Environment(project, decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(1L);
            softly.assertThat(sut.name()).isEqualTo("review/fix-foo");
            softly.assertThat(sut.slug()).isEqualTo("review-fix-foo-dfjre3");
            softly.assertThat(sut.externalUrl()).isEmpty();
            softly.assertThat(sut.tier()).contains(EnvironmentTier.DEVELOPMENT);
            softly.assertThat(sut.state()).isEqualTo("available");
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.kubernetesNamespace()).contains("flux-system");
            softly.assertThat(sut.fluxResourcePath()).contains("HelmRelease/flux-system");
            softly.assertThat(sut.response().project().pathWithNamespace()).isEqualTo("root/awesome-project");
            softly.assertThat(sut.response().lastDeployment().deployable().tagList())
                    .containsExactly("docker", "linux");
            softly.assertThat(sut.response().clusterAgent().createdByUserId()).isEqualTo(42L);
            softly.assertThat(sut.response().updatedAt()).isEqualTo(Instant.parse("2026-11-13T09:00:00Z"));
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenDeploymentSucceeded() {
        final GitLabDeploymentExtended decoded = decode(DEPLOYMENT, GitLabDeploymentExtended.class);

        final Deployment sut = new Deployment(project, decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(42L);
            softly.assertThat(sut.iid()).isEqualTo(2L);
            softly.assertThat(sut.ref()).isEqualTo("main");
            softly.assertThat(sut.sha()).isEqualTo("a91957a8");
            softly.assertThat(sut.status()).isEqualTo(DeploymentStatus.SUCCESS);
            softly.assertThat(sut.response().approvals()).isNull();
            softly.assertThat(sut.response().deployable().duration()).isEqualTo(119.877);
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenDeploymentWaitsForApproval() {
        final GitLabDeploymentExtended decoded = decode(BLOCKED_DEPLOYMENT, GitLabDeploymentExtended.class);

        final Deployment sut = new Deployment(project, decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.status()).isEqualTo(DeploymentStatus.BLOCKED);
            softly.assertThat(sut.statusName()).isEqualTo("blocked");
            softly.assertThat(sut.response().pendingApprovalCount()).isEqualTo(1);
            softly.assertThat(sut.response().approvals()).hasSize(1);
            softly.assertThat(
                            sut.response().approvalSummary().rules().getFirst().requiredApprovals())
                    .isEqualTo(2);
            softly.assertThat(sut.response().deployable().id()).isEqualTo(1278L);
        });
    }

    private static <T> T decode(final String json, final Class<T> type) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), type);
    }
}
