package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMergeRequestBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserPublic;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabInstanceIssuesListFilter;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.mergerequests.GitLabInstanceMergeRequestsListFilter;
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
        return gitlab.issues().instance().list(requireNonNull(filter, "filter")).stream()
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
        return loadMergeRequests(gitlab.mergerequests().instance().list(requireNonNull(filter, "filter")).stream());
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
     * Returns the wrapped client for the objects of this SDK.
     *
     * @return the client
     */
    GitLab gitLab() {
        return gitlab;
    }
}
