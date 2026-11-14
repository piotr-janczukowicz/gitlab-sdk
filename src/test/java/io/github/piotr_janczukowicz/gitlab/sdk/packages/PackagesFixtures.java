package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabPackage;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabPackageBuilder;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabPackageFile;
import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabPackageFileBuilder;
import java.time.Instant;

/** Builds the core records for the tests of the package objects. */
final class PackagesFixtures {

    static final Instant CREATED = Instant.parse("2026-01-02T03:04:05Z");

    private PackagesFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** A package as the project list and get answer it: no project id. */
    static GitLabPackage pkg(final long id) {
        return GitLabPackageBuilder.builder()
                .id(id)
                .name("my-lib")
                .packageType("npm")
                .status("default")
                .createdAt(CREATED)
                .build();
    }

    /** A package as the group list answers it: with the project id. */
    static GitLabPackage groupPkg(final long id, final long projectId) {
        return GitLabPackageBuilder.builder(pkg(id)).projectId(projectId).build();
    }

    static GitLabPackage fullPackage(final long id) {
        return GitLabPackageBuilder.builder(groupPkg(id, 99L))
                .version("1.2.3")
                .lastDownloadedAt(Instant.parse("2026-02-03T04:05:06Z"))
                .build();
    }

    static GitLabPackageFile file(final long id) {
        return GitLabPackageFileBuilder.builder()
                .id(id)
                .packageId(7L)
                .fileName("lib-1.2.3.tgz")
                .build();
    }

    static GitLabPackageFile fullFile(final long id) {
        return GitLabPackageFileBuilder.builder(file(id))
                .size(2048L)
                .fileSha256("abc123")
                .build();
    }
}
