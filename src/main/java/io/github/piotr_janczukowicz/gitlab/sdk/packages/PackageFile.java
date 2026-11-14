package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.packages.GitLabPackageFile;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A file of a {@link ProjectPackage}, as GitLab returned it. The object is immutable: it keeps the data of the moment
 * it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover. A
 * package file can contain secrets; the SDK writes a download only where the caller says.
 */
public final class PackageFile {

    private final ProjectPackage projectPackage;
    private final GitLabPackageFile response;

    /**
     * Wraps a response. Applications normally get this object from {@link ProjectPackage#files()}; call the constructor
     * only to wrap a response that was loaded some other way.
     *
     * @param projectPackage the package the file belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public PackageFile(final ProjectPackage projectPackage, final GitLabPackageFile response) {
        this.projectPackage = requireNonNull(projectPackage, "projectPackage");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the id of the file, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the file name.
     *
     * @return the file name
     */
    public String fileName() {
        return requireNonNull(response.fileName(), "fileName");
    }

    /**
     * Returns the size of the file in bytes.
     *
     * @return the size, or empty when GitLab sent none
     */
    public Optional<Long> size() {
        return Optional.ofNullable(response.size());
    }

    /**
     * Returns the SHA-256 checksum of the file.
     *
     * @return the checksum, or empty when GitLab sent none
     */
    public Optional<String> sha256() {
        return Optional.ofNullable(response.fileSha256());
    }

    /**
     * Downloads the file into a path ({@code GET
     * /projects/{id}/packages/{package_id}/package_files/{file_id}/download}). The client's own authentication is used;
     * the SDK offers no download mode parameter. The body is first written to a temporary file in the directory of
     * {@code target} and moved onto {@code target} (replacing an existing file) only when it was received completely,
     * so {@code target} never holds a partial download; the temporary file is deleted on failure and nothing is created
     * for an unsuccessful response. The directory of {@code target} must exist. Where the file system has POSIX
     * permissions the file is created owner-only. A package file may contain secrets; the SDK writes only where the
     * caller says.
     *
     * @param target the file to write
     * @return {@code target}
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code target} has no parent directory
     * @throws IllegalStateException if the package response names no project id
     * @throws IOException if the transport fails or the file cannot be written, among others a
     *     {@link java.nio.file.FileAlreadyExistsException} when {@code target} is an existing directory
     */
    public Path download(final Path target) throws IOException {
        final Path checked = requireNonNull(target, "target");
        return projectPackage
                .gitLab()
                .packages()
                .getPackageFilesDownload(projectPackage.address(), packageId(), id(), checked);
    }

    /**
     * Downloads the file into a stream ({@code GET
     * /projects/{id}/packages/{package_id}/package_files/{file_id}/download}). The client's own authentication is used;
     * the SDK offers no download mode parameter. The body is copied without buffering it. The stream is the caller's:
     * the SDK never closes it. When the transfer fails the stream may hold a partial body. A package file may contain
     * secrets; the SDK writes only where the caller says.
     *
     * @param out the stream to write to
     * @return the number of bytes written
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IllegalStateException if the package response names no project id
     * @throws IOException if the transport fails or the stream cannot be written
     */
    public long download(final OutputStream out) throws IOException {
        final OutputStream checked = requireNonNull(out, "out");
        return projectPackage
                .gitLab()
                .packages()
                .getPackageFilesDownload(projectPackage.address(), packageId(), id(), checked);
    }

    /**
     * Deletes the file ({@code DELETE /projects/{id}/packages/{package_id}/package_files/{file_id}}).
     *
     * @throws IllegalStateException if the package response names no project id
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        projectPackage.gitLab().packages().deletePackageFile(projectPackage.address(), packageId(), id());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabPackageFile response() {
        return response;
    }

    private long packageId() {
        return projectPackage.id();
    }
}
