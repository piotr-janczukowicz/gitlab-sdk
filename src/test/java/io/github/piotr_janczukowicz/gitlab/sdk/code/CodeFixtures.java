package io.github.piotr_janczukowicz.gitlab.sdk.code;

import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranch;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranchBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiffBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesCreateResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesCreateResponseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesUpdateResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesUpdateResponseBuilder;
import java.util.Base64;

/** Builds the core records for the tests of the file, branch and commit objects. */
final class CodeFixtures {

    private CodeFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabProjectRepositoryFilesGetResponse file(final String path, final byte[] content) {
        return fileWithContent(path, "base64", Base64.getEncoder().encodeToString(content));
    }

    static GitLabProjectRepositoryFilesGetResponse fileWithContent(
            final String path, final String encoding, final String content) {
        return GitLabProjectRepositoryFilesGetResponseBuilder.builder()
                .filePath(path)
                .fileName(path.substring(path.lastIndexOf('/') + 1))
                .encoding(encoding)
                .content(content)
                .ref("main")
                .build();
    }

    static GitLabProjectRepositoryFilesCreateResponse createdFile(final String path, final String branch) {
        return GitLabProjectRepositoryFilesCreateResponseBuilder.builder()
                .filePath(path)
                .branch(branch)
                .build();
    }

    static GitLabProjectRepositoryFilesUpdateResponse updatedFile(final String path, final String branch) {
        return GitLabProjectRepositoryFilesUpdateResponseBuilder.builder()
                .filePath(path)
                .branch(branch)
                .build();
    }

    static GitLabBranch branch(final String name, final boolean isProtected) {
        return GitLabBranchBuilder.builder()
                .name(name)
                .protectedValue(isProtected)
                .build();
    }

    static GitLabCommit listedCommit(final String sha) {
        return GitLabCommitBuilder.builder().id(sha).title("listed").build();
    }

    static GitLabCommitDetail commit(final String sha, final String message) {
        return GitLabCommitDetailBuilder.builder()
                .id(sha)
                .shortId(sha.substring(0, Math.min(8, sha.length())))
                .title(message)
                .message(message)
                .build();
    }

    static GitLabDiff diff(final String path) {
        return GitLabDiffBuilder.builder().newPath(path).oldPath(path).build();
    }
}
