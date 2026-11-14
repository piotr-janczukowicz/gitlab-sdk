package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabContainerRegistryRepository;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabProjectContainerRegistryDeleteTagFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A container registry repository, as GitLab returned it. The object is immutable: it keeps the data of the moment it
 * was loaded, and {@link #response()} gives the complete response for every field the accessors here do not cover.
 * GitLab lists and loads a repository with the same record, so none of them needs an extra request. Images and their
 * layers can contain secrets; the SDK reads only tag metadata.
 *
 * <p>A repository from {@link Project#registryRepositories()} belongs to that project and {@link #project()} returns it
 * without a request. A repository from a group list ({@code Group.registryRepositories()}) belongs to the project named
 * by its {@code project_id}: {@link #project()} then returns a handle of that project, without a request. When the
 * response has no project id, such a repository cannot address its project, so {@link #project()}, {@link #tags()},
 * {@link #tag(String)}, {@link #delete()} and {@link #deleteTags(GitLabProjectContainerRegistryDeleteTagFilter)} throw
 * {@link IllegalStateException}; listing and {@link #refresh()} never do. The registry protection rules and the
 * registry event endpoint are not wrapped; use the core client for them.
 */
public final class RegistryRepository {

    private static final String NAME = "name";

    private final GitLabSdk sdk;
    private final @Nullable Project owner;
    private final @Nullable String projectKey;
    private final GitLabContainerRegistryRepository response;

    /**
     * Wraps a response of a repository that belongs to a project. Applications normally get this object from
     * {@link Project#registryRepositories()}; call the constructor only to wrap a response that was loaded some other
     * way.
     *
     * @param owner the project the repository belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public RegistryRepository(final Project owner, final GitLabContainerRegistryRepository response) {
        this(Internals.sdk(requireNonNull(owner, "owner")), owner, owner.id(), requireNonNull(response, "response"));
    }

    /**
     * Wraps a response of a repository whose project is named by the response, as a group list returns it. Applications
     * normally get this object from {@code Group.registryRepositories()}; call the constructor only to wrap a response
     * that was loaded some other way.
     *
     * @param sdk the SDK whose client the repository calls
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public RegistryRepository(final GitLabSdk sdk, final GitLabContainerRegistryRepository response) {
        this(requireNonNull(sdk, "sdk"), null, keyOf(requireNonNull(response, "response")), response);
    }

    private RegistryRepository(
            final GitLabSdk sdk,
            final @Nullable Project owner,
            final @Nullable String projectKey,
            final GitLabContainerRegistryRepository response) {
        this.sdk = sdk;
        this.owner = owner;
        this.projectKey = projectKey;
        this.response = response;
    }

    /**
     * Returns the id of the repository, which addresses it in the requests.
     *
     * @return the id
     */
    public long id() {
        return requireNonNull(response.id(), "id");
    }

    /**
     * Returns the name of the repository, which is empty for the repository of the project's own image path.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), NAME);
    }

    /**
     * Returns the path of the repository.
     *
     * @return the path
     */
    public String path() {
        return requireNonNull(response.path(), "path");
    }

    /**
     * Returns the location of the repository, which is what {@code docker pull} takes.
     *
     * @return the location
     */
    public String location() {
        return requireNonNull(response.location(), "location");
    }

    /**
     * Returns the number of tags. GitLab sends it only when it was asked for it.
     *
     * @return the number, or empty when GitLab sent none
     */
    public Optional<Integer> tagsCount() {
        return Optional.ofNullable(response.tagsCount());
    }

    /**
     * Returns the project the repository belongs to; no request is made. A repository that came from
     * {@link Project#registryRepositories()} answers that project. A repository that came from a group list answers a
     * handle of the project named by its {@code project_id}; call {@link Project#fetch()} on it to load the project.
     *
     * @return the project
     * @throws IllegalStateException if the repository has no owner and the response names no project id
     */
    public Project project() {
        if (owner != null) {
            return owner;
        }
        return sdk.project(address());
    }

    /**
     * Loads the repository again ({@code GET /registry/repositories/{id}}), by the id of the repository and not of the
     * project. This object is not changed. The returned repository belongs to the same project as this one, even when
     * the answer names none; when this one knows no project and the answer names a {@code project_id}, the returned
     * repository uses it.
     *
     * @return the repository as GitLab holds it now
     * @throws IllegalStateException if the repository is gone
     * @throws IOException if the transport fails
     */
    public RegistryRepository refresh() throws IOException {
        final long repositoryId = id();
        return gitLab().containerregistry()
                .get(String.valueOf(repositoryId))
                .map(loaded ->
                        new RegistryRepository(sdk, owner, projectKey != null ? projectKey : keyOf(loaded), loaded))
                .orElseThrow(() -> new IllegalStateException("Repository " + repositoryId + " is gone"));
    }

    /**
     * Lists the tags of the repository ({@code GET /projects/{id}/registry/repositories/{repository_id}/tags}). GitLab
     * lists a summary of each tag, so each one is loaded ({@code GET
     * /projects/{id}/registry/repositories/{repository_id}/tags/{tag_name}}) as the stream reaches it, and one that is
     * gone by then is skipped: one extra request per tag. Pages are fetched only as the stream is consumed; transport
     * failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the tags
     * @throws IllegalStateException if the response names no project id
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<RegistryTag> tags() throws IOException {
        final String key = address();
        return gitLab().containerregistry().listTags(key, id()).stream()
                .map(listed -> Lookups.find(() -> tag(requireNonNull(listed.name(), NAME))))
                .flatMap(Optional::stream);
    }

    /**
     * Loads a tag ({@code GET /projects/{id}/registry/repositories/{repository_id}/tags/{tag_name}}). The name is
     * passed to the core unchanged, so a name with a colon or a slash is fine; the core encodes it.
     *
     * @param name the name of the tag; not blank
     * @return the tag, or empty when GitLab answers HTTP 404
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IllegalStateException if the response names no project id
     * @throws IOException if the transport fails
     */
    public Optional<RegistryTag> tag(final String name) throws IOException {
        requireNotBlank(name, NAME);
        final String key = address();
        return gitLab().containerregistry().getTag(key, id(), name).map(loaded -> new RegistryTag(this, loaded));
    }

    /**
     * Deletes the repository with all its tags ({@code DELETE /projects/{id}/registry/repositories/{repository_id}}).
     *
     * @throws IllegalStateException if the response names no project id
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        final String key = address();
        gitLab().containerregistry().delete(key, id());
    }

    /**
     * Deletes tags in bulk ({@code DELETE /projects/{id}/registry/repositories/{repository_id}/tags}). Warning: GitLab
     * selects the tags to delete with regular expressions, and the SDK passes the core's filter through unchanged, so a
     * broad expression deletes many tags at once and a deleted tag cannot be recovered. GitLab requires
     * {@code name_regex_delete} or {@code name_regex}; the core does not check it, and GitLab refuses a request that
     * has neither. {@code name_regex_keep}, {@code keep_n} and {@code older_than} narrow what is deleted. Deleting the
     * tags is asynchronous on GitLab's side.
     *
     * @param filter the selection of the tags to delete
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IllegalStateException if the response names no project id
     * @throws IOException if the transport fails
     */
    public void deleteTags(final GitLabProjectContainerRegistryDeleteTagFilter filter) throws IOException {
        final GitLabProjectContainerRegistryDeleteTagFilter checked = requireNonNull(filter, "filter");
        final String key = address();
        gitLab().containerregistry().deleteTag(key, id(), checked);
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabContainerRegistryRepository response() {
        return response;
    }

    String address() {
        if (projectKey == null) {
            throw new IllegalStateException("The repository response names no project id");
        }
        return projectKey;
    }

    GitLab gitLab() {
        return Internals.gitLab(sdk);
    }

    private static @Nullable String keyOf(final GitLabContainerRegistryRepository response) {
        final Long projectId = response.projectId();
        return projectId == null ? null : String.valueOf(projectId);
    }
}
