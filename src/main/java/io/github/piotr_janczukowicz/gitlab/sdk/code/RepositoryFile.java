package io.github.piotr_janczukowicz.gitlab.sdk.code;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.repositoryfiles.GitLabProjectRepositoryFilesGetResponse;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.FileForms;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.RepositoryRequests;
import io.github.piotr_janczukowicz.gitlab.sdk.repository.BlameRange;
import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A file of a {@link Project}, as GitLab returned it for one ref. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response for every field the accessors here do not
 * cover. GitLab does not tell the file which ref it was read at in a form this object can rely on, so the object keeps
 * the ref it was loaded with; {@link #update(String, String)} and {@link #delete(String)} commit to that ref, which
 * therefore has to be a branch. A file has no {@code refresh()}, because the ref it was read at may be a commit or a
 * tag that never changes; load it again with {@link Project#file(String, String)}.
 */
public final class RepositoryFile {

    private static final String BASE64 = "base64";
    private static final String TEXT = "text";
    private static final String FILE = "File ";
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]");

    private final Project project;
    private final String ref;
    private final GitLabProjectRepositoryFilesGetResponse response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param ref the branch, tag or commit the response was read at
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code ref} is blank
     */
    public RepositoryFile(
            final Project project, final String ref, final GitLabProjectRepositoryFilesGetResponse response) {
        this.project = requireNonNull(project, "project");
        this.ref = requireNotBlank(ref, "ref");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the file belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the path of the file within the repository.
     *
     * @return the path
     */
    public String path() {
        return requireNonNull(response.filePath(), "filePath");
    }

    /**
     * Returns the name of the file.
     *
     * @return the name, or empty when GitLab does not return one
     */
    public Optional<String> name() {
        return Optional.ofNullable(response.fileName());
    }

    /**
     * Returns the ref the file was read at.
     *
     * @return the branch, tag or commit
     */
    public String ref() {
        return ref;
    }

    /**
     * Returns the exact content of the file, decoded from the encoding GitLab used. Binary and empty files are returned
     * as they are, byte for byte. Every call returns a new array.
     *
     * @return the content
     * @throws IllegalStateException if GitLab returned no content or an encoding other than {@code base64} or
     *     {@code text}
     */
    public byte[] bytes() {
        final String content = response.content();
        if (content == null) {
            throw new IllegalStateException(FILE + path() + " has no content");
        }
        final String encoding = response.encoding();
        if (encoding == null || BASE64.equals(encoding)) {
            try {
                return Base64.getDecoder().decode(LINE_BREAKS.matcher(content).replaceAll(""));
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException(FILE + path() + " has content that is not valid base64", e);
            }
        }
        if (TEXT.equals(encoding)) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalStateException(FILE + path() + " has an unsupported encoding: " + encoding);
    }

    /**
     * Returns the content of the file as text, decoded strictly as UTF-8: invalid bytes are never replaced.
     *
     * @return the text
     * @throws IllegalStateException if the content is not valid UTF-8, or as for {@link #bytes()}
     */
    public String text() {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes()))
                    .toString();
        } catch (final CharacterCodingException e) {
            throw new IllegalStateException(FILE + path() + " is not valid UTF-8 text", e);
        }
    }

    /**
     * Commits new content of the file to the branch it was read at ({@code PUT
     * /projects/{id}/repository/files/{file_path}}) and loads the file again ({@code GET
     * /projects/{id}/repository/files/{file_path}?ref=...}). This object is not changed.
     *
     * @param content the new content, sent as UTF-8 text
     * @param message the commit message
     * @return the file as GitLab holds it afterwards
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank
     * @throws IllegalStateException if GitLab no longer has the file after the update
     * @throws IOException if the transport fails
     */
    public RepositoryFile update(final String content, final String message) throws IOException {
        final MultipartFormData form = FileForms.of(ref, content, message);
        gitLab().repositoryfiles().update(project.id(), path(), form);
        return project.file(path(), ref).orElseThrow(() -> new IllegalStateException(FILE + path() + " is gone"));
    }

    /**
     * Commits new binary content of the file to the branch it was read at ({@code PUT
     * /projects/{id}/repository/files/{file_path}}, the content Base64-encoded with {@code encoding=base64}) and loads
     * the file again ({@code GET /projects/{id}/repository/files/{file_path}?ref=...}). This object is not changed.
     *
     * @param content the new content; may be empty
     * @param message the commit message
     * @return the file as GitLab holds it afterwards
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank
     * @throws IllegalStateException if GitLab no longer has the file after the update
     * @throws IOException if the transport fails
     */
    public RepositoryFile update(final byte[] content, final String message) throws IOException {
        final MultipartFormData form = FileForms.of(ref, content, message);
        gitLab().repositoryfiles().update(project.id(), path(), form);
        return project.file(path(), ref).orElseThrow(() -> new IllegalStateException(FILE + path() + " is gone"));
    }

    /**
     * Deletes the file from the branch it was read at ({@code DELETE /projects/{id}/repository/files/{file_path}}) by a
     * commit.
     *
     * @param message the commit message
     * @throws NullPointerException if {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank
     * @throws IOException if the transport fails
     */
    public void delete(final String message) throws IOException {
        gitLab().repositoryfiles().delete(project.id(), path(), ref, requireNotBlank(message, "message"));
    }

    /**
     * Returns the blame of the whole file at the ref it was read at ({@code GET
     * /projects/{id}/repository/files/{file_path}/blame?ref=...&range[start]=1&range[end]=...}): runs of consecutive
     * lines with the commit that last changed them. The core's operation takes a line range, so the range is {@code 1}
     * to the number of lines counted in the content this object holds; an empty file has no lines and no request is
     * made. The request is made when this method is called, not when the stream is consumed.
     *
     * @return the ranges
     * @throws IllegalStateException if GitLab returned no content, or as for {@link #bytes()}
     * @throws IOException if the transport fails
     */
    public Stream<BlameRange> blame() throws IOException {
        final int lines = RepositoryRequests.lineCount(bytes());
        if (lines == 0) {
            return Stream.empty();
        }
        return blame(1, lines);
    }

    /**
     * Returns the blame of a range of lines at the ref the file was read at ({@code GET
     * /projects/{id}/repository/files/{file_path}/blame?ref=...&range[start]=...&range[end]=...}). The request is made
     * when this method is called, not when the stream is consumed.
     *
     * @param start the first line, counted from 1
     * @param end the last line, not before {@code start}
     * @return the ranges
     * @throws IllegalArgumentException if {@code start} is below 1 or {@code end} is below {@code start}
     * @throws IOException if the transport fails
     */
    public Stream<BlameRange> blame(final int start, final int end) throws IOException {
        if (start < 1 || end < start) {
            throw new IllegalArgumentException("The range must start at 1 or later and not end before it starts");
        }
        return gitLab().repositoryfiles().listBlame(project.id(), path(), ref, start, end).stream()
                .map(BlameRange::new);
    }

    /**
     * Streams the raw content of this file at its ref into a stream; see {@link Project#rawFile(String, String,
     * OutputStream)}.
     *
     * @param out the stream to write to
     * @return the number of bytes written
     * @throws NullPointerException if {@code out} is {@code null}
     * @throws IOException if the transport fails or the stream cannot be written
     */
    public long raw(final OutputStream out) throws IOException {
        return project.rawFile(path(), ref, out);
    }

    /**
     * Writes the raw content of this file at its ref to a local file; see {@link Project#rawFile(String, String,
     * Path)}.
     *
     * @param target the local file to write
     * @return {@code target}, as the core returns it
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IOException if the transport fails or the file cannot be written
     */
    public Path raw(final Path target) throws IOException {
        return project.rawFile(path(), ref, target);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabProjectRepositoryFilesGetResponse response() {
        return response;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
