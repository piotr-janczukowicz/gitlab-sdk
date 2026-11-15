package io.github.piotr_janczukowicz.gitlab.sdk.code;

import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranch;
import io.github.piotr_janczukowicz.gitlab.api.branches.GitLabBranchBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabBasicRef;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabBasicRefBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitNote;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitNoteBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitSignature;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitSignatureBuilder;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitStatus;
import io.github.piotr_janczukowicz.gitlab.api.commits.GitLabCommitStatusBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommit;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetail;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCommitDetailBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiff;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabDiffBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequest;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesCreateResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesCreateResponseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesUpdateResponse;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesUpdateResponseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTag;
import io.github.piotr_janczukowicz.gitlab.api.tags.GitLabTagBuilder;
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

    static GitLabTag tag(final String name, final String sha) {
        return GitLabTagBuilder.builder()
                .name(name)
                .target("target-of-" + name)
                .commit(listedCommit(sha))
                .protectedValue(false)
                .build();
    }

    static GitLabDiff diff(final String path) {
        return GitLabDiffBuilder.builder().newPath(path).oldPath(path).build();
    }

    static GitLabCommitStatus commitStatus(final long id, final String status, final String name) {
        return GitLabCommitStatusBuilder.builder()
                .id(id)
                .status(status)
                .name(name)
                .ref("main")
                .description("described")
                .targetUrl("https://ci.example/1")
                .build();
    }

    static GitLabCommitNote commitNote(final String note, final String path, final Integer line, final String type) {
        return GitLabCommitNoteBuilder.builder()
                .note(note)
                .path(path)
                .line(line)
                .lineType(type)
                .build();
    }

    static GitLabBasicRef ref(final String type, final String name) {
        return GitLabBasicRefBuilder.builder().type(type).name(name).build();
    }

    static GitLabCommitSignature signature(final String type) {
        return GitLabCommitSignatureBuilder.builder().signatureType(type).build();
    }

    static GitLabMergeRequestBasic basicMergeRequest(final long iid) {
        return GitLabMergeRequestBasicBuilder.builder().iid(iid).title("t").build();
    }

    static GitLabMergeRequest mergeRequest(final long iid) {
        return GitLabMergeRequestBuilder.builder()
                .iid(iid)
                .title("t")
                .state("opened")
                .build();
    }
}
