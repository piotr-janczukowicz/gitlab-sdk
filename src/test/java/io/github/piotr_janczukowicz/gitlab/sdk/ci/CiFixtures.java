package io.github.piotr_janczukowicz.gitlab.sdk.ci;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJob;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJobBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJobBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiJobBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipeline;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiPipelineBuilder;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadata;
import io.github.piotr_janczukowicz.gitlab.api.pipelines.GitLabCiPipelineWithMetadataBuilder;

/** Builds the core records for the tests of the pipeline and job objects. */
final class CiFixtures {

    private CiFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabCiPipelineWithMetadata pipeline(final long id, final String status) {
        return GitLabCiPipelineWithMetadataBuilder.builder()
                .id(id)
                .status(status)
                .ref("main")
                .build();
    }

    static GitLabCiPipelineBasic basicPipeline(final long id) {
        return GitLabCiPipelineBasicBuilder.builder().id(id).status("success").build();
    }

    static GitLabCiPipeline createdPipeline(final long id) {
        return GitLabCiPipelineBuilder.builder().id(id).status("created").build();
    }

    static GitLabCiJob job(final long id, final String name, final String status) {
        return GitLabCiJobBuilder.builder().id(id).name(name).status(status).build();
    }

    static GitLabCiJob jobOfPipeline(final long id, final long pipelineId) {
        return GitLabCiJobBuilder.builder()
                .id(id)
                .name("build")
                .status("success")
                .pipeline(basicPipeline(pipelineId))
                .build();
    }

    static GitLabCiJobBasic basicJob(final long id, final String status) {
        return GitLabCiJobBasicBuilder.builder()
                .id(id)
                .name("build")
                .status(status)
                .build();
    }
}
