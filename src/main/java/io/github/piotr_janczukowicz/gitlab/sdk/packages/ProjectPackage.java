package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabPackage;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A package of a project, as GitLab returned it. The object is immutable: it keeps the data of the moment it was
 * loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. GitLab
 * lists and loads a package with the same record, so none of them needs an extra request.
 *
 * <p>A package from {@link Project#packages()} belongs to that project and {@link #project()} returns it without a
 * request. A package from a group list ({@code Group.packages()}) belongs to whichever project of the group published
 * it, and its {@code project_id} says which: {@link #project()} then returns a handle of that project, without a
 * request. When the response has no project id, such a package cannot address its project, so {@link #project()},
 * {@link #refresh()}, {@link #files()} and {@link #delete()} throw {@link IllegalStateException}; listing never does.
 * The package ecosystems' own endpoints (Maven, npm, NuGet and the others) and the package protection rules are not
 * wrapped; use the core client for them.
 */
public final class ProjectPackage {

    private final GitLabSdk sdk;
    private final @Nullable Project owner;
    private final @Nullable String projectKey;
    private final GitLabPackage response;

    /**
     * Wraps a response of a package that belongs to a project. Applications normally get this object from
     * {@link Project#packages()}; call the constructor only to wrap a response that was loaded some other way.
     *
     * @param owner the project the package belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ProjectPackage(final Project owner, final GitLabPackage response) {
        this(Internals.sdk(requireNonNull(owner, "owner")), owner, owner.id(), requireNonNull(response, "response"));
    }

    /**
     * Wraps a response of a package whose project is named by the response, as a group list returns it. Applications
     * normally get this object from {@code Group.packages()}; call the constructor only to wrap a response that was
     * loaded some other way.
     *
     * @param sdk the SDK whose client the package calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public ProjectPackage(final GitLabSdk sdk, final GitLabPackage response) {
        this(requireNonNull(sdk, "sdk"), null, keyOf(requireNonNull(response, "response")), response);
    }

    private ProjectPackage(
            final GitLabSdk sdk,
            final @Nullable Project owner,
            final @Nullable String projectKey,
            final GitLabPackage response) {
        this.sdk = sdk;
        this.owner = owner;
        this.projectKey = projectKey;
        this.response = response;
    }

    /**
     * Returns the id of the package, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the version.
     *
     * @return the version, or empty when GitLab sent none
     */
    public Optional<String> version() {
        return Optional.ofNullable(response.version());
    }

    /**
     * Returns the type of the package.
     *
     * @return the type
     * @throws IllegalArgumentException if the SDK does not know the type; {@link #typeName()} gives the raw name
     */
    public PackageType type() {
        return PackageType.from(typeName());
    }

    /**
     * Returns the type of the package as GitLab sent it, such as {@code maven}.
     *
     * @return the raw name
     */
    public String typeName() {
        return requireNonNull(response.packageType(), "packageType");
    }

    /**
     * Returns the status as GitLab spells it, such as {@code default}, {@code hidden}, {@code processing},
     * {@code error}, {@code pending_destruction} or {@code deprecated}.
     *
     * @return the raw status
     */
    public String status() {
        return requireNonNull(response.status(), "status");
    }

    /**
     * Returns the time the package was created.
     *
     * @return the time
     */
    public Instant createdAt() {
        return requireNonNull(response.createdAt(), "createdAt");
    }

    /**
     * Returns the time the package was last downloaded.
     *
     * @return the time, or empty when GitLab sent none
     */
    public Optional<Instant> lastDownloadedAt() {
        return Optional.ofNullable(response.lastDownloadedAt());
    }

    /**
     * Returns the project the package belongs to; no request is made. A package that came from
     * {@link Project#packages()} answers that project. A package that came from a group list answers a handle of the
     * project named by its {@code project_id}; call {@link Project#fetch()} on it to load the project.
     *
     * @return the project
     * @throws IllegalStateException if the package has no owner and the response names no project id
     */
    public Project project() {
        if (owner != null) {
            return owner;
        }
        return sdk.project(address());
    }

    /**
     * Loads the package again ({@code GET /projects/{id}/packages/{package_id}}). This object is not changed. The
     * returned package belongs to the same project as this one, even when the answer names none.
     *
     * @return the package as GitLab holds it now
     * @throws IllegalStateException if the package is gone, or the response names no project id
     * @throws IOException if the transport fails
     */
    public ProjectPackage refresh() throws IOException {
        final long packageId = id();
        final String key = address();
        return gitLab().packages()
                .get(key, packageId)
                .map(loaded -> new ProjectPackage(sdk, owner, projectKey, loaded))
                .orElseThrow(() -> new IllegalStateException("Package " + packageId + " is gone"));
    }

    /**
     * Lists the files of the package ({@code GET /projects/{id}/packages/{package_id}/package_files}). Pages are
     * fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the files
     * @throws IllegalStateException if the response names no project id
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PackageFile> files() throws IOException {
        return gitLab().packages().listPackageFiles(address(), id()).stream().map(file -> new PackageFile(this, file));
    }

    /**
     * Deletes the package with all its files ({@code DELETE /projects/{id}/packages/{package_id}}).
     *
     * @throws IllegalStateException if the response names no project id
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().packages().delete(address(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabPackage response() {
        return response;
    }

    String address() {
        if (projectKey == null) {
            throw new IllegalStateException("The package response names no project id");
        }
        return projectKey;
    }

    GitLab gitLab() {
        return Internals.gitLab(sdk);
    }

    private static @Nullable String keyOf(final GitLabPackage response) {
        final Long projectId = response.projectId();
        return projectId == null ? null : String.valueOf(projectId);
    }
}
