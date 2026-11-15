package io.github.piotr_janczukowicz.gitlab.sdk.mirrors;

import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabPullMirror;
import io.github.piotr_janczukowicz.gitlab.api.pullmirroring.GitLabPullMirrorBuilder;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabMirrorHostKeyBuilder;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabRemoteMirror;
import io.github.piotr_janczukowicz.gitlab.api.remotemirrors.GitLabRemoteMirrorBuilder;
import java.time.Instant;
import java.util.List;

/** Builds the core records for the tests of the mirror objects. */
final class MirrorsFixtures {

    /** A made-up address with a made-up credential, to check that no object prints it. */
    static final String URL_WITH_CREDENTIALS = "https://mirror-user:not-a-real-password@mirror.example.test/repo.git";

    static final Instant UPDATED = Instant.parse("2026-05-06T07:08:09Z");

    private MirrorsFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabRemoteMirror remote(final long id) {
        return GitLabRemoteMirrorBuilder.builder()
                .id(id)
                .url(URL_WITH_CREDENTIALS)
                .build();
    }

    static GitLabRemoteMirror fullRemote(final long id) {
        return GitLabRemoteMirrorBuilder.builder()
                .id(id)
                .url(URL_WITH_CREDENTIALS)
                .enabled(true)
                .updateStatus("finished")
                .lastUpdateAt(UPDATED)
                .lastUpdateStartedAt(UPDATED.minusSeconds(5))
                .lastSuccessfulUpdateAt(UPDATED)
                .lastError("none yet")
                .onlyProtectedBranches(true)
                .keepDivergentRefs(true)
                .authMethod("password")
                .hostKeys(List.of(
                        GitLabMirrorHostKeyBuilder.builder()
                                .fingerprintSha256("SHA256:abc")
                                .build(),
                        GitLabMirrorHostKeyBuilder.builder()
                                .fingerprintSha256("SHA256:def")
                                .build()))
                .mirrorBranchRegex("^release/")
                .build();
    }

    static GitLabPullMirror pull(final long id) {
        return GitLabPullMirrorBuilder.builder()
                .id(id)
                .url(URL_WITH_CREDENTIALS)
                .build();
    }

    static GitLabPullMirror barePull(final long id) {
        return GitLabPullMirrorBuilder.builder().id(id).build();
    }

    static GitLabPullMirror fullPull(final long id) {
        return GitLabPullMirrorBuilder.builder()
                .id(id)
                .url(URL_WITH_CREDENTIALS)
                .updateStatus("finished")
                .lastError("none yet")
                .lastUpdateAt(UPDATED)
                .lastUpdateStartedAt(UPDATED.minusSeconds(5))
                .lastSuccessfulUpdateAt(UPDATED)
                .enabled(true)
                .mirrorTriggerBuilds(true)
                .onlyMirrorProtectedBranches(true)
                .mirrorOverwritesDivergedBranches(true)
                .mirrorBranchRegex("^release/")
                .build();
    }
}
