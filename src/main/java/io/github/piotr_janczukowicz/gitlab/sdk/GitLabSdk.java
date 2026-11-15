package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.OwnerValues.FILTER;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroup;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserPublic;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabInstanceIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabInstanceMergeRequestsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabInstanceProjectsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectWithAccess;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUsersListFilter;
import io.github.piotr_janczukowicz.gitlab.api.users.GitLabUsersListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.client.GitLabCaller;
import io.github.piotr_janczukowicz.gitlab.client.GitLabClient;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.issues.Issue;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import io.github.piotr_janczukowicz.gitlab.sdk.snippets.PersonalSnippets;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Entry point of the object layer. It wraps an existing {@link GitLab} client, so authentication, base URI, tier and
 * limits are configured on the client as usual; the SDK has no configuration of its own.
 */
public final class GitLabSdk {

    static {
        Internals.registerSdkAccess(GitLabSdk::gitLab);
    }

    private final GitLab gitlab;

    private GitLabSdk(final GitLab gitlab) {
        this.gitlab = requireNonNull(gitlab, "gitlab");
    }

    /**
     * Wraps a client.
     *
     * @param gitlab the client every object of this SDK calls
     * @return the entry point
     * @throws NullPointerException if {@code gitlab} is {@code null}
     */
    public static GitLabSdk of(final GitLab gitlab) {
        return new GitLabSdk(gitlab);
    }

    /**
     * Wraps a caller, so every object of this SDK calls the resources of {@code new GitLab(caller)}.
     *
     * @param caller the caller that executes the requests of every object of this SDK
     * @return the entry point
     * @throws NullPointerException if {@code caller} is {@code null}
     */
    public static GitLabSdk of(final GitLabCaller caller) {
        return of(new GitLab(requireNonNull(caller, "caller")));
    }

    /**
     * Wraps a client, the shortest way to start: it is the same as {@code of(new GitLab(GitLabCaller.of(client)))}. The
     * client stays yours; closing it is up to you.
     *
     * @param client the client that sends the requests of every object of this SDK
     * @return the entry point
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public static GitLabSdk of(final GitLabClient client) {
        return of(GitLabCaller.of(requireNonNull(client, "client")));
    }

    /**
     * Returns a handle to a project. No request is made until {@link Project#fetch()}.
     *
     * @param id the numeric id or the plain full path of the project, for example {@code my-group/my-project}; the core
     *     encodes the path, so do not encode it yourself
     * @return the project handle
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public Project project(final String id) {
        return new Project(this, requireNotBlank(id, "id"), null);
    }

    /**
     * Returns a handle to a group. No request is made until {@link Group#fetch()}.
     *
     * @param id the numeric id or the plain full path of the group, for example {@code my-group}; the core encodes the
     *     path
     * @return the group handle
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public Group group(final String id) {
        return new Group(this, requireNotBlank(id, "id"), null);
    }

    /**
     * Starts a new project. Nothing is sent until {@link ProjectDraft#send()}.
     *
     * @param name the name of the project
     * @return the draft
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public ProjectDraft createProject(final String name) {
        return new ProjectDraft(this, name);
    }

    /**
     * Starts a new group. Nothing is sent until {@link GroupDraft#send()}.
     *
     * @param name the name of the group
     * @param path the path (URL slug) of the group
     * @return the draft
     * @throws NullPointerException if {@code name} or {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code name} or {@code path} is blank
     */
    public GroupDraft createGroup(final String name, final String path) {
        return new GroupDraft(this, name, path);
    }

    /**
     * Loads a user ({@code GET /users/{id}}).
     *
     * @param id the numeric id of the user
     * @return the user, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<User> user(final long id) throws IOException {
        return gitlab.users().get(id).map(loaded -> new User(this, loaded));
    }

    /**
     * Returns the account that the client signs in as.
     *
     * @return the current account
     */
    public Me me() {
        return new Me(this);
    }

    /**
     * Loads a runner ({@code GET /runners/{id}}).
     *
     * @param id the id of the runner
     * @return the runner, or empty when GitLab answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Runner> runner(final long id) throws IOException {
        return gitlab.runners().instance().get(id).map(loaded -> new Runner(this, loaded));
    }

    /**
     * Returns the personal snippets of the account the client signs in as: list, load, create, read their content,
     * update and delete. No request is made.
     *
     * @return the personal snippets
     */
    public PersonalSnippets snippets() {
        return PersonalSnippets.forSdk(this);
    }

    /**
     * Lists the issues the account can see on the instance ({@code GET /issues}); by default GitLab limits them to the
     * ones the account created. Each issue is addressed through the project GitLab names for it. Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return issues(GitLabInstanceIssuesListFilter.empty());
    }

    /**
     * Lists the issues on the instance that match a filter ({@code GET /issues}). Each issue is addressed through the
     * project GitLab names for it. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the query parameters of the core
     * @return the issues
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues(final GitLabInstanceIssuesListFilter filter) throws IOException {
        return gitlab.issues().instance().list(requireNonNull(filter, FILTER)).stream()
                .map(this::issueOf);
    }

    /**
     * Lists the merge requests on the instance that GitLab shows to the caller ({@code GET /merge_requests}). Without a
     * filter GitLab lists only the merge requests the account created. GitLab lists a summary of each merge request, so
     * each one is loaded ({@code GET /projects/{id}/merge_requests/{merge_request_iid}}) through the project GitLab
     * names for it as the stream reaches it, and one that is gone by then is skipped. Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the merge requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests() throws IOException {
        return mergeRequests(GitLabInstanceMergeRequestsListFilter.empty());
    }

    /**
     * Lists the merge requests on the instance that match a filter ({@code GET /merge_requests}). Each one is loaded as
     * the stream reaches it, and one that is gone by then is skipped, as for {@link #mergeRequests()}.
     *
     * @param filter the query parameters of the core
     * @return the merge requests
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<MergeRequest> mergeRequests(final GitLabInstanceMergeRequestsListFilter filter) throws IOException {
        return loadMergeRequests(gitlab.mergerequests().instance().list(requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Lists the projects the account can see ({@code GET /projects}). Per GitLab's API description the default is every
     * project the account can access, newest first ({@code order_by=created_at}, {@code sort=desc}); on a large
     * instance that is a great many projects, so limit the stream or pass a filter. GitLab lists a summary of each
     * project, so each one is loaded ({@code GET /projects/{id}}) as the stream reaches it, one GET per project, and
     * one that is gone by then is skipped; the filter's {@code simple} and {@code statistics} therefore shape the list
     * only, not the loaded objects. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the projects
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Project> projects() throws IOException {
        return projects(GitLabProjectProjectsListFilter.empty());
    }

    /**
     * Lists the projects that match a filter ({@code GET /projects}), loaded one by one as described at
     * {@link #projects()}. In the core's filter, {@code owned(true)} limits the list to projects the account owns,
     * {@code starred(true)} to the ones it starred and {@code membership(true)} to the ones it is a member of.
     *
     * @param filter the query parameters of the core
     * @return the projects
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Project> projects(final GitLabProjectProjectsListFilter filter) throws IOException {
        return loadedProjects(gitlab.projects().project().list(requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Lists the personal projects of a user ({@code GET /users/{user_id}/projects}): per GitLab's API description only
     * the projects in the user's personal namespace, not group projects, and a private profile gives an empty list.
     * They are loaded one by one as described at {@link #projects()}. The filter's {@code owned}, {@code starred} and
     * {@code membership} flags have the meaning they have for {@link #projects()}; GitLab's API description does not
     * say here whether they refer to the user or to the account of the client, and that is not verified live.
     *
     * @param userId the id of the user
     * @param filter the query parameters of the core
     * @return the projects
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Project> userProjects(final long userId, final GitLabInstanceProjectsListFilter filter)
            throws IOException {
        return loadedProjects(
                gitlab.projects().instance().list(String.valueOf(userId), requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Lists the projects a user starred ({@code GET /users/{user_id}/starred_projects}), loaded one by one as described
     * at {@link #projects()}. The GitLab documentation page for this call was not read, so its default order is not
     * stated here.
     *
     * @param userId the id of the user
     * @return the projects
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Project> starredProjects(final long userId) throws IOException {
        return loadedProjects(gitlab.projects().instance().listUsersStarredProjects(String.valueOf(userId)).stream());
    }

    /**
     * Lists the groups the account can see ({@code GET /groups}). Per GitLab's API description, for a user who is not
     * an administrator {@code all_available} defaults to {@code false}, so only the groups the account is a member of
     * are listed, ordered by name ascending; for an administrator it defaults to {@code true}. GitLab lists a summary
     * of each group, so each one is loaded ({@code GET /groups/{id}}) as the stream reaches it, and one that is gone by
     * then is skipped; the filter's {@code statistics} shapes the list only, not the loaded objects. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}. The
     * stream is single-use.
     *
     * @return the groups
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Group> groups() throws IOException {
        return groups(GitLabGroupGroupsListFilter.empty());
    }

    /**
     * Lists the groups that match a filter ({@code GET /groups}), loaded one by one as described at {@link #groups()}.
     * In the core's filter, {@code owned(true)} limits the list to the groups the account owns and
     * {@code minAccessLevel} to the ones where it has at least that role; there is no starred or membership flag for
     * groups.
     *
     * @param filter the query parameters of the core
     * @return the groups
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Group> groups(final GitLabGroupGroupsListFilter filter) throws IOException {
        return loadedGroups(gitlab.groups().list(requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Lists the users ({@code GET /users}). Per GitLab's API description a user who is not an administrator gets no
     * default filter, so every user GitLab shows to the account is listed. GitLab lists a summary of each user, so each
     * one is loaded ({@code GET /users/{id}}) as the stream reaches it, and one that is gone by then is skipped. Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}. The stream is single-use.
     *
     * @return the users
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<User> users() throws IOException {
        return users(GitLabUsersListFilter.empty());
    }

    /**
     * Lists the users that match a filter ({@code GET /users}), loaded one by one as described at {@link #users()}. The
     * core's filter has {@code search} and {@code username}, among others; many of its parameters are for
     * administrators.
     *
     * @param filter the query parameters of the core
     * @return the users
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<User> users(final GitLabUsersListFilter filter) throws IOException {
        return loadedUsers(gitlab.users().list(requireNonNull(filter, FILTER)).stream());
    }

    /**
     * Finds a user by username ({@code GET /users?username=...}, then {@code GET /users/{id}}). Per GitLab's API
     * description the lookup is case-insensitive.
     *
     * @param username the username
     * @return the user, or empty when no user has that username
     * @throws NullPointerException if {@code username} is {@code null}
     * @throws IllegalArgumentException if {@code username} is blank; no request is made
     * @throws IOException if the transport fails
     */
    public Optional<User> userByUsername(final String username) throws IOException {
        final GitLabUsersListFilter filter = GitLabUsersListFilterBuilder.builder()
                .username(requireNotBlank(username, "username"))
                .build();
        final Optional<GitLabUserBasic> found =
                gitlab.users().list(filter).stream().findFirst();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return user(requireNonNull(found.get().id(), "id"));
    }

    /**
     * Asks GitLab for the id of the account the client signs in as ({@code GET /user}).
     *
     * @return the id
     * @throws IOException if the transport fails
     */
    @SuppressWarnings("PMD.LawOfDemeter")
    long currentUserId() throws IOException {
        final GitLabUserPublic current = gitlab.users().getUser();
        return requireNonNull(current.id(), "id");
    }

    /**
     * Wraps a listed issue with the handle of the project GitLab names for it.
     *
     * @param listed the issue as a list returned it
     * @return the issue
     */
    Issue issueOf(final GitLabIssue listed) {
        return new Issue(project(String.valueOf(requireNonNull(listed.projectId(), "projectId"))), listed);
    }

    /**
     * Loads each merge request a list summarized, through the project GitLab names for it, and skips one that is gone
     * by the time the stream reaches it.
     *
     * @param listed the summaries as a list returned them
     * @return the merge requests
     */
    Stream<MergeRequest> loadMergeRequests(final Stream<GitLabMergeRequestBasic> listed) {
        return listed.map(summary ->
                        Lookups.find(() -> project(String.valueOf(requireNonNull(summary.projectId(), "projectId")))
                                .mergeRequest(requireNonNull(summary.iid(), "iid"))))
                .flatMap(Optional::stream);
    }

    /**
     * Loads each project a list summarized and skips one that is gone by the time the stream reaches it.
     *
     * @param listed the summaries as a list returned them
     * @return the projects
     */
    Stream<Project> loadedProjects(final Stream<GitLabProjectWithAccess> listed) {
        return listed.map(item -> String.valueOf(requireNonNull(item.id(), "id")))
                .flatMap(projectId -> Lookups.find(() -> project(projectId).fetch()).stream());
    }

    /**
     * Loads each group a list summarized and skips one that is gone by the time the stream reaches it.
     *
     * @param listed the summaries as a list returned them
     * @return the groups
     */
    Stream<Group> loadedGroups(final Stream<GitLabGroup> listed) {
        return listed.map(item -> String.valueOf(requireNonNull(item.id(), "id")))
                .flatMap(groupId -> Lookups.find(() -> group(groupId).fetch()).stream());
    }

    /**
     * Loads each user a list summarized and skips one that is gone by the time the stream reaches it.
     *
     * @param listed the summaries as a list returned them
     * @return the users
     */
    Stream<User> loadedUsers(final Stream<GitLabUserBasic> listed) {
        return listed.map(item -> requireNonNull(item.id(), "id"))
                .flatMap(userId -> Lookups.find(() -> user(userId)).stream());
    }

    /**
     * Returns the wrapped client for the objects of this SDK.
     *
     * @return the client
     */
    GitLab gitLab() {
        return gitlab;
    }
}
