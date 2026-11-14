package io.github.piotr_janczukowicz.gitlab.sdk.environments;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDeployment;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDeploymentBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentApproval;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentApprovalBuilder;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentExtended;
import io.github.piotr_janczukowicz.gitlab.api.deployments.GitLabDeploymentExtendedBuilder;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabEnvironment;
import io.github.piotr_janczukowicz.gitlab.api.environments.GitLabEnvironmentBuilder;
import org.jspecify.annotations.Nullable;

/** Builds the core records for the tests of the environment and deployment objects. */
final class EnvironmentsFixtures {

    private EnvironmentsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabEnvironment environment(final long id, final String name, final @Nullable String tier) {
        return GitLabEnvironmentBuilder.builder()
                .id(id)
                .name(name)
                .slug(name + "-slug")
                .state("available")
                .tier(tier)
                .build();
    }

    static GitLabEnvironment fullEnvironment() {
        return GitLabEnvironmentBuilder.builder(environment(7, "production", "production"))
                .externalUrl("https://example.com")
                .description("The live one")
                .kubernetesNamespace("prod-ns")
                .fluxResourcePath("path/to/flux")
                .build();
    }

    static GitLabDeployment listedDeployment(final long id) {
        return GitLabDeploymentBuilder.builder().id(id).iid(id + 100).build();
    }

    static GitLabDeploymentExtended deployment(final long id, final String status) {
        return GitLabDeploymentExtendedBuilder.builder()
                .id(id)
                .iid(id + 100)
                .ref("main")
                .sha("abc123")
                .status(status)
                .build();
    }

    static GitLabDeploymentApproval approval(final String status) {
        return GitLabDeploymentApprovalBuilder.builder().status(status).build();
    }

    static GitLabMergeRequestBasic mergeRequestBasic(final long iid, final @Nullable Long projectId) {
        return GitLabMergeRequestBasicBuilder.builder()
                .iid(iid)
                .projectId(projectId)
                .build();
    }

    static GitLabMergeRequest mergeRequest(final long iid) {
        return GitLabMergeRequestBuilder.builder()
                .iid(iid)
                .title("m" + iid)
                .state("opened")
                .build();
    }
}
