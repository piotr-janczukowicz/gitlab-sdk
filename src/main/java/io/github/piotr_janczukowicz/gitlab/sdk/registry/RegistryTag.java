package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabContainerRegistryTagDetails;
import java.io.IOException;
import java.util.Optional;

/**
 * A tag of a {@link RegistryRepository}, as GitLab returned it. The object is immutable: it keeps the data of the
 * moment it was loaded, and {@link #response()} gives the complete response of the tag details for every field the
 * accessors here do not cover. The tag list holds only a summary, so a tag is always loaded through the details
 * request.
 */
public final class RegistryTag {

    private final RegistryRepository repository;
    private final GitLabContainerRegistryTagDetails response;

    /**
     * Wraps a response. Applications normally get this object from {@link RegistryRepository#tags()}; call the
     * constructor only to wrap a response that was loaded some other way.
     *
     * @param repository the repository the tag belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public RegistryTag(final RegistryRepository repository, final GitLabContainerRegistryTagDetails response) {
        this.repository = requireNonNull(repository, "repository");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the name of the tag, which addresses it in the requests.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the location of the tag, which is what {@code docker pull} takes.
     *
     * @return the location
     */
    public String location() {
        return requireNonNull(response.location(), "location");
    }

    /**
     * Returns the digest of the image.
     *
     * @return the digest, or empty when GitLab sent none
     */
    public Optional<String> digest() {
        return Optional.ofNullable(response.digest());
    }

    /**
     * Returns the total size of the image in bytes.
     *
     * @return the size, or empty when GitLab sent none
     */
    public Optional<Long> totalSize() {
        return Optional.ofNullable(response.totalSize());
    }

    /**
     * Returns the repository the tag belongs to.
     *
     * @return the repository
     */
    public RegistryRepository repository() {
        return repository;
    }

    /**
     * Loads the tag again ({@code GET /projects/{id}/registry/repositories/{repository_id}/tags/{tag_name}}). This
     * object is not changed.
     *
     * @return the tag as GitLab holds it now
     * @throws IllegalStateException if the tag is gone
     * @throws IOException if the transport fails
     */
    public RegistryTag refresh() throws IOException {
        final String tagName = name();
        return repository.tag(tagName).orElseThrow(() -> new IllegalStateException("Tag " + tagName + " is gone"));
    }

    /**
     * Deletes the tag ({@code DELETE /projects/{id}/registry/repositories/{repository_id}/tags/{tag_name}}). The name
     * is passed to the core unchanged, so a name with a colon or a slash is fine.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final String key = repository.address();
        repository.gitLab().containerregistry().deleteTagByTagName(key, repository.id(), name());
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabContainerRegistryTagDetails response() {
        return response;
    }
}
