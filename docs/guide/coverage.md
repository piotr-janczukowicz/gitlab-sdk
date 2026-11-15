# What is covered

Every object of the SDK, with the methods it has, by package. The [README](../../README.md) gives the short overview.
Every object also has `response()`, which returns the complete `gitlab-rest-client` record it is built from, and
navigates to its parent (`issue.project()`, `note.issue()`, `job.project()`, `file.projectPackage()` and so on), except
a `Reaction`, a `Runner`, a `RunnerToken` and a `PersonalSnippet`, which have none. A `Wiki`, a `ProjectSnippets`, a
`PersonalSnippets`, a `Dependencies` and an `AuditEvents` are area handles, not loaded objects: they have neither
`response()` nor parent navigation; wiki pages navigate to their wiki with `wiki()`, a `Snippet` and a `Dependency` to
their project with `project()`, and an `AuditEvent` to its `AuditEvents` with `auditEvents()`. A lookup returns an
`Optional`, a list is a lazy, single-use `Stream`, and the `iid` of an issue or a merge request is a `long`.

## Root package: `io.github.piotr_janczukowicz.gitlab.sdk`

| Entry point | Objects and what they do |
|---|---|
| `GitLabSdk.of(...)` | Wraps a `GitLab`, a `GitLabCaller` or a `GitLabClient`. `project(id)` and `group(id)` give handles, `createProject(name)` and `createGroup(name, path)` start a `ProjectDraft` and a `GroupDraft`, `user(id)` loads a user, `runner(id)` loads a runner, `snippets()` gives the personal snippets of the signed-in account (no request), `me()` gives the signed-in account, `issues()` and `issues(filter)` list the issues of the instance (the core's `GitLabInstanceIssuesListFilter`; each issue gets the handle of the project GitLab names for it), `mergeRequests()` and `mergeRequests(filter)` list the merge requests of the instance (the core's `GitLabInstanceMergeRequestsListFilter`; each one is loaded through the project GitLab names for it, and one that is gone is skipped). Lists (each is a lazy, single-use `Stream`; GitLab lists a summary of each item, so each one is loaded again as the stream reaches it, one GET per item, and a gone one is skipped): `projects()`, `projects(filter)` (the core's `GitLabProjectProjectsListFilter`: `owned(true)`, `starred(true)` and `membership(true)` express owned, starred and member), `userProjects(userId, filter)` (the personal projects of a user), `starredProjects(userId)`, `groups()`, `groups(filter)` (`owned(true)` and `minAccessLevel`; groups have no starred or membership flag), `users()`, `users(filter)`, and `userByUsername(username)` (`Optional`; `GET /users?username=`, then the load by id). The filters' `simple` and `statistics` shape only the list, not the loaded objects. GitLab's defaults are in the Javadoc with the source they were read from. |
| `Project` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `update()`, `archive()`, `unarchive()`, `delete()`, `response()`. Transfer, sharing and forks: `transfer(namespace)` (the project is addressed by its numeric id afterwards), `fork(namespacePath)` (GitLab creates the fork asynchronously, so it may still be importing), `forks()` (each fork is loaded as the stream reaches it), `removeForkRelationship()`, `share(groupId, level)`, `unshare(groupId)`, `sharedGroups()` (the groups invited into the project, as the core's `GitLabPublicGroupDetails` records). Members: `members()`, `allMembers()` (inherited too), `member(userId)`, `addMember(userId, level)`, `addMember(username, level)`, `requestAccess()`, `accessRequests()`. Webhooks: `webhooks()`, `webhook(hookId)`, `createWebhook(url)`, `createWebhook(payload)` (two overloads: a literal `null` needs a cast). Protection: `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)`, `protectBranch(name, push, merge)`, `protectedTags()`, `protectedTag(name)`, `protectTag(name, create)`. Issues: `issues()`, `issues(filter)`, `issue(iid)`, `createIssue(title)`, `createIssue(payload)`. Labels: `labels()`, `labels(filter)` (the core's `GitLabProjectLabelsListFilter`), `label(name)` (a slash or space reaches the core unchanged), `createLabel(name, color)`. Merge requests: `mergeRequests()`, `mergeRequests(filter)`, `mergeRequest(iid)`, `createMergeRequest(payload)`. Pipelines and jobs: `pipelines()`, `pipelines(filter)`, `pipeline(id)`, `createPipeline(ref)`, `createPipeline(payload)`, `triggerPipeline(token, ref)`, `triggerPipeline(token, ref, variables)` (a pipeline trigger token, which is secret and never printed; the ref is part of the path; the pipeline is loaded again afterwards with the client's own authentication, so an `IllegalStateException` after a successful trigger means the pipeline WAS started but cannot be read by that account, and the trigger must not be repeated; not verified live), `jobs()`, `jobs(filter)`, `job(id)`, `downloadArtifacts(ref, jobName, target)` (a `Path`, written atomically and never partial) and `downloadArtifacts(ref, jobName, out)` (an `OutputStream`, never closed by the SDK; the latest successful job of that name on the ref; a ref with a slash reaches the core unchanged). Code: `file(path, ref)`, `createFile(path, branch, content, message)` (content as text or `byte[]`), `branches()`, `branch(name)`, `createBranch(name, ref)`, `commits()`, `commits(filter)` (the core's `GitLabProjectCommitsListFilter`; each listed commit is loaded again, gone ones are skipped), `commit(sha)`, `createCommit(branch, message)` (a `CommitDraft`, see `code` below; no request until `send()`), `tags()`, `tags(filter)`, `tag(name)`, `tree()`, `tree(path, ref, recursive)`, `compare(from, to, straight)`, `contributors()`, `mergeBase(refs...)` (`Optional`), `deleteMergedBranches()`, `changelog(version)`, `changelog(version, filter)` (the repository reads, see `repository` below), `createTag(name, ref)` (lightweight), `createTag(name, ref, message)` (annotated; a `null` message makes it lightweight). Releases: `releases()`, `releases(filter)`, `release(tagName)`, `createRelease(tagName)` (existing tag), `createRelease(tagName, ref)` (GitLab creates the tag from the ref), `createRelease(payload)` (overloaded, so a literal `null` needs a cast). Milestones: `milestones()`, `milestones(filter)` (the core's `GitLabProjectProjectMilestonesListFilter`), `milestone(id)`, `createMilestone(title)`, `createMilestone(payload)` (overloaded, so a literal `null` needs a cast). CI/CD variables: `variables()`, `variable(key)`, `variable(key, environmentScope)` (narrows the lookup with `filter[environment_scope]`), `createVariable(key, value)`, `createVariable(payload)` (overloaded, so a literal `null` needs a cast). Environments: `environments()`, `environments(filter)` (the core's `GitLabProjectEnvironmentsListFilter`), `environment(id)`, `createEnvironment(name)`, `createEnvironment(payload)` (overloaded, so a literal `null` needs a cast). Deployments: `deployments()`, `deployments(filter)` (the core's `GitLabProjectDeploymentsListFilter`; each listed deployment is loaded again, gone ones are skipped), `deployment(id)`, `createDeployment(environment, sha, ref, status)` (only `RUNNING`, `SUCCESS`, `FAILED` and `CANCELED` are accepted). Pipeline schedules: `pipelineSchedules()` (each listed schedule is loaded again, gone ones are skipped), `pipelineSchedule(id)`, `createPipelineSchedule(description, ref, cron)`, `createPipelineSchedule(payload)` (overloaded, so a literal `null` needs a cast). Pipeline triggers: `pipelineTriggers()`, `pipelineTrigger(id)`, `createPipelineTrigger(description)`, `createPipelineTrigger(description, expiresAt)`. Runners: `runners()`, `runners(filter)` (the core's `GitLabProjectRunnersListFilter`; each listed runner is loaded again, gone ones are skipped), `enableRunner(runnerId)` (enables an existing runner in the project, then loads it by `runnerId`; `IllegalStateException` when it was enabled but cannot be loaded), `resetRunnerRegistrationToken()` (secret). Packages: `packages()`, `packages(filter)` (the core's `GitLabProjectPackagesListFilter`), `projectPackage(id)`. Registry: `registryRepositories()`, `registryRepositories(withTags)`. Wiki: `wiki()` (no request). Snippets: `snippets()` (no request). Dependencies: `dependencies()` (no request). Audit events (Premium): `auditEvents()` (no request). Lists and reads: `users()`, `groups()` (the ancestor groups), `events()`, `events(filter)` (the core's `GitLabProjectEventsListFilter`) (all four pass the core's records through), `languages()` (an unmodifiable copy), `housekeeping()`, `housekeeping(task)` (starts repository maintenance asynchronously; never run by the smoke tests), `templates(type)` and `template(type, name)` (a `TemplateType`; `Optional`; the core types every template answer as its license record, of which `name` and `content` are filled for every kind), `star()` and `unstar()` (each reloads the project; the 304 the core's OpenAPI description lists for a project that is already in that state is not verified live). `sharedGroups()` lists the groups invited into the project (`GET /projects/{id}/invited_groups`); earlier versions called the ancestor-groups endpoint by mistake. Settings (Task 7): `badges()` (lazy), `badge(id)`, `addBadge(linkUrl, imageUrl)`, `addBadge(linkUrl, imageUrl, name)`, `previewBadge(linkUrl, imageUrl)` (the core's record, unwrapped); `pushRules()` (`Optional`: HTTP 404 is empty; Premium), `createPushRules(payload)`; `accessTokens()`, `accessTokens(filter)` (one request, no paging), `accessToken(id)`, `createAccessToken(name, scopes, expiresAt, level)`; `remoteMirrors()` (lazy), `remoteMirror(id)`, `createRemoteMirror(url)`, `createRemoteMirror(payload)`; `pullMirror()` (`Optional`: HTTP 404 and the 400 GitLab documents for a project that is not mirrored are empty; Premium), `configurePullMirror(url)`; `approvalRules()` (lazy), `approvalRule(id)`, `createApprovalRule(name, approvalsRequired)`, `createApprovalRule(name, approvalsRequired, userIds, groupIds, usernames)` (Premium), `approvalConfiguration()` (`Optional`; Premium); `avatar(OutputStream)` and `avatar(Path)` (download only); `upload(Path)` (returns the Markdown link). See the `badges`, `pushrules`, `tokens`, `mirrors` and `approvals` sections |
| `Group` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `update()`, `archive()`, `unarchive()`, `delete()`, `transfer(newParentGroupId)` (the group is addressed by its numeric id afterwards), `share(groupId, level)` (returns this group as GitLab answers), `unshare(groupId)`, members as on `Project` (`members()`, `allMembers()`, `member(userId)`, `addMember(...)`, `requestAccess()`, `accessRequests()`), webhooks (Premium): `webhooks()`, `webhook(hookId)`, `createWebhook(url)`, `createWebhook(payload)`, protected branches (Premium): `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)`, `protectBranch(name, push, merge)`, milestones: `milestones()`, `milestones(filter)` (the core's `GitLabGroupGroupMilestonesListFilter`, which adds `includeDescendants`), `milestone(id)`, `createMilestone(title)`, `createMilestone(payload)` (overloaded, so a literal `null` needs a cast), group labels: `labels()`, `labels(filter)` (the core's `GitLabGroupGroupLabelsListFilter`), `label(name)`, `createLabel(name, color)`, CI/CD variables: `variables()`, `variable(key)`, `createVariable(key, value)`, `createVariable(payload)`, issues: `issues()`, `issues(filter)` (the core's `GitLabGroupIssuesListFilter`; GitLab's `scope` defaults to `all`; each issue gets the handle of the project GitLab names for it), `mergeRequests()`, `mergeRequests(filter)` (the core's `GitLabGroupMergeRequestsListFilter`; GitLab's `scope` defaults to `all`; each one is loaded through the project GitLab names for it), `projects()` (each project is loaded as the stream reaches it), `subgroups()` (the direct subgroups, one level down; each group is loaded as the stream reaches it), `response()`. Runners: `runners()`, `runners(filter)` (the core's `GitLabGroupGroupsListRunnersFilter`; each listed runner is loaded again, gone ones are skipped), `resetRunnerRegistrationToken()` (secret). Packages: `packages()`, `packages(filter)` (the core's `GitLabGroupGroupsListPackagesFilter`, which adds `excludeSubgroups`). Registry: `registryRepositories()` (the core offers no filter for the group list). Wiki (Premium): `wiki()` (no request). Audit events (Premium): `auditEvents()` (no request). Lists: `descendantGroups()` (all depths), `invitedGroups()` (the groups invited into this group, `GET /groups/{id}/invited_groups`: the relation `Project.sharedGroups()` lists for a project) and `invitedToGroups()` (the groups this group has been invited to, `GET /groups/{id}/groups/shared`: the opposite direction; docs.gitlab.com/api/groups, read: "List groups where the given group has been invited" and "List invited groups in a group"; earlier versions called it `sharedGroups()`), each loaded group by group like `subgroups()`. Settings (Task 7): `badges()` (lazy), `badge(id)`, `addBadge(linkUrl, imageUrl)`, `addBadge(linkUrl, imageUrl, name)`, `previewBadge(linkUrl, imageUrl)`; `pushRules()` (`Optional`: HTTP 404 is empty; Premium), `createPushRules(payload)`; `accessTokens()`, `accessTokens(filter)`, `accessToken(id)`, `createAccessToken(name, scopes, expiresAt, level)`; `approvalRules()` (lazy), `createApprovalRule(name, approvalsRequired)`, `createApprovalRule(name, approvalsRequired, userIds, groupIds)` (Premium); `avatar(OutputStream)` and `avatar(Path)` (download only); `ldapLinks()`, `addLdapLink(provider, cn, level)`, `addLdapFilterLink(provider, filter, level)`, `deleteLdapLink(provider, cn)`, `deleteLdapFilterLink(provider, filter)`, `samlLinks()`, `samlLink(name)`, `samlLink(name, provider)`, `addSamlLink(name, level)`, `addSamlLink(name, level, provider)`, `deleteSamlLink(name)`, `deleteSamlLink(name, provider)` (Premium) |
| `ProjectGroupLink` | The sharing of a project with a group, returned by `Project.share(groupId, level)`: `project()`, `groupId()`, `expiresAt()`, `response()`. It has no `refresh()` |
| `Visibility` | `PRIVATE`, `INTERNAL`, `PUBLIC`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `TemplateType` | The kinds `Project.templates(type)` and `template(type, name)` accept (`DOCKERFILES`, `GITIGNORES`, `GITLAB_CI_YMLS`, `LICENSES`, `ISSUES`, `MERGE_REQUESTS`); `value()`; unit tests compare it with both core enums |
| `ProjectDraft` | From `sdk.createProject(name)`: `path(...)`, `namespaceId(...)`, `description(...)`, `visibility(...)`, `defaultBranch(...)`, `initializeWithReadme(...)`, `send()` (creates, then loads the project again) |
| `ProjectUpdate` | `name(...)`, `path(...)`, `description(...)`, `clearDescription()` (sends an empty description), `visibility(...)`, `defaultBranch(...)`, `send()`; `send()` with nothing set throws `IllegalStateException`. Archiving stays with `archive()` |
| `GroupDraft` | From `sdk.createGroup(name, path)`: `description(...)`, `visibility(...)`, `parentId(...)`, `send()` |
| `GroupUpdate` | `name(...)`, `path(...)`, `description(...)`, `visibility(...)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `User` | Loaded. `id()`, `username()`, `refresh()`, `response()`. `associationsCount()` (`Optional` of the core's `GitLabUserAssociationsCount`; GitLab documents it for administrators and for the user themselves, and the SDK does not check; not verified live). A user has no parent navigation. |
| `Me` | A handle. `profile()` loads the signed-in account as a `User`. The keys of the account (see `account`): `sshKeys()`, `addSshKey(title, key, expiresAt)` (`expiresAt` may be `null`), `emails()`, `addEmail(email)`, `gpgKeys()`, `addGpgKey(key)`; only the signed-in account's own keys, never another user's. |

## `issues`

| Object | Methods |
|---|---|
| `Issue` | `project()`, `iid()`, `title()`, `description()`, `state()` (`IssueState`: `OPENED`, `CLOSED`), `stateName()` (the raw value), `labels()`, `update()`, `close()`, `reopen()`, `comment(body)`, `notes()`, `discussions()`, `discussion(id)`, `startDiscussion(body)`, `reactions()`, `react(name)`, `delete()`, `refresh()`, `response()`. Time tracking: `timeStats()`, `estimateTime(Duration)`, `resetEstimate()`, `addSpentTime(Duration)`, `resetSpentTime()` (whole seconds are sent; a negative duration throws `IllegalArgumentException` before any request). Links: `links()` (GitLab lists summaries, so each link is loaded as the stream reaches it and a gone one is skipped), `link(linkId)`, `linkTo(target, type)` (the target may be in another project; one extra request when GitLab's answer carries no id, which finds the id in the link list, `IllegalStateException` when it is not there; the one exception to one core operation per method). Others: `participants()` (the core's `GitLabUserBasic` records), `closedByMergeRequests()` and `relatedMergeRequests()` (summaries again: each merge request is loaded as the stream reaches it, a gone one is skipped), `move(targetProjectId)` (the issue in the target project) |
| `IssueUpdate` | `title(...)`, `description(...)`, `clearDescription()` (sends an empty description), `labels(names)`, `assignees(userIds)`, `confidential(boolean)`, `milestone(id)`, `clearMilestone()`, `dueDate(LocalDate)`, `send()`; `send()` with nothing set throws `IllegalStateException`. `clearMilestone()` sends `milestone_id=0`, which docs.gitlab.com/api/issues says unassigns the milestone. The core's update payload cannot clear a due date, so there is no `clearDueDate()` |
| `TimeStats` | `estimate()` and `totalSpent()` (`Duration`, zero when GitLab leaves them out), `humanEstimate()` and `humanTotalSpent()` (each empty when GitLab leaves it out), `response()` |
| `IssueLink` | `issue()` (the issue it was loaded from), `id()`, `type()` (`IssueLinkType`), `typeName()` (the raw value), `delete()` (removes the link, not the issues), `response()` |
| `IssueLinkType` | `RELATES_TO`, `BLOCKS`, `IS_BLOCKED_BY`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `IssueState` | `OPENED`, `CLOSED`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `Note` | `issue()`, `id()`, `body()`, `update(body)`, `reactions()`, `react(name)`, `delete()`, `refresh()`, `response()` |
| `Label` | `project()`, `name()`, `color()`, `description()`, `update()`, `promote()` (returns the `GroupLabel`; sends `GET /projects/{id}` to find the group from the project's namespace, then the promotion, so a failed GET promotes nothing; a project in a user namespace is not rejected by the SDK: GitLab's rejection passes through as the core's exception), `delete()`, `refresh()`, `response()` |
| `LabelUpdate` | `name(...)`, `color(...)`, `description(...)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `GroupLabel` | `group()`, `name()`, `color()`, `description()`, `isSubscribed()`, `update()`, `subscribe()`, `unsubscribe()`, `delete()`, `refresh()`, `response()`; addressed by name (a slash or space reaches the core unchanged) |
| `GroupLabelUpdate` | `name(...)` (the new name), `color(...)`, `description(...)`, `archived(boolean)`, `send()`; `send()` with nothing set throws `IllegalStateException` |

## `mergerequests`

The approval types in this section (`ApprovalState`, `MergeRequestApprovalRule`, `MergeRequestApprovalRuleUpdate`) are in
the package `approvals`; they are listed here because they belong to a merge request.

| Object | Methods |
|---|---|
| `MergeRequest` | `project()`, `iid()`, `title()`, `description()`, `state()` (`MergeRequestState`), `stateName()` (the raw value), `targetBranch()`, `sourceBranch()`, `labels()`, `update()`, `close()`, `reopen()`, `merge()`, `merge(options)`, `approve()`, `approve(options)`, `unapprove()`, `approvals()`, `comment(body)`, `notes()`, `discussions()`, `discussion(id)`, `startDiscussion(body)`, `startDiscussion(payload)` (a diff comment: the core's payload carries the `position`; overloaded, so a literal `null` needs a cast), `draftNotes()` (one request, a stream over the core's list), `draftNote(id)`, `createDraftNote(note)`, `createDraftNote(payload)`, `publishDraftNotes()` (an empty bulk-publish payload), `publishDraftNotes(payload)` (both pairs are overloaded, so a literal `null` needs a cast), `reactions()`, `react(name)`, `pipelines()` (each pipeline is loaded from the project it ran in, a fork included), `commits()`, `isRebaseInProgress()` (the moment the merge request was loaded; every load asks GitLab for the rebase state with `include_rebase_in_progress=true`, so `refresh()` after `rebase()` shows it), `cancelAutoMerge()`, `rebase()` and `rebase(options)` (asynchronous: they return before the rebase finishes, so follow it with `refresh()`), `createPipeline()` (loaded after the creation, from the project GitLab names), `diffs()` and `diffs(filter)` (the core's `GitLabDiff` records, unwrapped), `versions()` (`DiffVersion`), `version(id)` (`MergeRequestDiff`, empty on 404), `participants()` (the core's `GitLabUserBasic`), `closesIssues()` (each issue is loaded as the stream reaches it), `approvalState()`, `approvalRules()`, `approvalRule(id)` and `createApprovalRule(name, approvalsRequired)`, `createApprovalRule(name, approvalsRequired, userIds, groupIds)` or `createApprovalRule(payload)` (the payload also carries user names and the id of a project rule to copy) (Premium, tier errors pass through), `timeStats()`, `estimateTime(Duration)`, `resetEstimate()`, `addSpentTime(Duration)`, `resetSpentTime()` (the same whole-second `Duration` rules as `Issue`; a negative duration throws `IllegalArgumentException` before any request), `delete()`, `refresh()`, `response()` |
| `MergeRequestUpdate` | `title(...)`, `description(...)`, `targetBranch(...)`, `labels(names)`, `assignees(userIds)`, `reviewers(userIds)`, `milestone(id)`, `clearMilestone()` (sends `milestone_id=0`, per docs.gitlab.com/api/merge_requests), `squash(boolean)`, `removeSourceBranch(boolean)`, `discussionLocked(boolean)`, `allowCollaboration(boolean)`, `send()`. No `clear...()`: the core's payload cannot send null |
| `MergeRequestApprovalRule` | `mergeRequest()`, `id()`, `name()`, `ruleType()`, `approvalsRequired()` (each empty when GitLab leaves it out), `update()`, `delete()`, `refresh()` (`IllegalStateException` when gone), `response()` (Premium) |
| `MergeRequestApprovalRuleUpdate` | `name(...)`, `approvalsRequired(int)`, `userIds(list)`, `groupIds(list)`, `usernames(names)`, `removeHiddenGroups(boolean)`, `send()` |
| `ApprovalState` | `mergeRequest()`, `approvalRulesOverwritten()` (a missing flag is `false`), `rules()` (the core's `GitLabMergeRequestApprovalStateRule`, an unmodifiable copy), `response()` (Premium) |
| `DiffVersion` | `id()`, `headCommitSha()`, `baseCommitSha()`, `startCommitSha()`, `createdAt()`, `state()` (each empty when GitLab leaves it out), `response()` |
| `MergeRequestDiff` | the same as `DiffVersion`, plus `commits()` and `diffs()` (the core's records, unmodifiable copies) |
| `MergeRequestState` | `OPENED`, `CLOSED`, `LOCKED`, `MERGED`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `MergeRequestNote` | `mergeRequest()`, `id()`, `body()`, `update(body)`, `reactions()`, `react(name)`, `delete()`, `refresh()`, `response()` |
| `DraftNote` | `mergeRequest()`, `id()`, `note()`, `isResolvingDiscussion()` (a missing flag is `false`), `update()`, `publish()`, `delete()`, `refresh()`, `response()`. After `publish()` it is no longer a draft: `refresh()` throws `IllegalStateException` |
| `DraftNoteUpdate` | `note(text)`, `send()` |
| `Approvals` | `mergeRequest()`, `approved()`, `approvalsLeft()`, `approvalsRequired()`, `userHasApproved()`, `response()` |

## `ci`

| Object | Methods |
|---|---|
| `Pipeline` | `project()`, `id()`, `status()` (`PipelineStatus`), `statusName()` (the raw value), `ref()`, `sha()`, `webUrl()`, `retry()`, `cancel()`, `jobs()`, `triggerJobs()` (the trigger jobs that start downstream pipelines, as the core's `GitLabCiBridge` records; the deprecated "bridges" operation is not offered), `variables()` (the core's `GitLabCiVariable` records; the value is secret and the core prints it as redacted), `testReportSummary()` (`Optional`: empty when the core reports no summary; the core's record), `delete()`, `refresh()`, `response()` |
| `Job` | `project()`, `id()`, `name()`, `status()` (`JobStatus`), `statusName()` (the raw value), `stage()`, `pipeline()`, `retry()`, `cancel()`, `play()`, `erase()`, `downloadArtifacts(target)` (a `Path`) and `downloadArtifacts(out)` (an `OutputStream`), `artifactTree()` and `artifactTree(path, recursive)` (streams of the core's `GitLabCiJobArtifactEntry` records: the first page is fetched when the method is called, the later ones as the stream is consumed), `keepArtifacts()` (a new `Job` from the answer), `deleteArtifacts()`, `refresh()`, `response()` |
| `PipelineStatus`, `JobStatus` | The thirteen statuses the core documents: `CREATED`, `WAITING_FOR_RESOURCE`, `PREPARING`, `WAITING_FOR_CALLBACK`, `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELING`, `CANCELED`, `SKIPPED`, `MANUAL`, `SCHEDULED`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |

## `code`

| Object | Methods |
|---|---|
| `RepositoryFile` | `project()`, `path()`, `name()`, `ref()`, `bytes()` (exact bytes, also of a binary file), `text()` (strict UTF-8), `update(content, message)` (content as text or `byte[]`), `blame()` (the whole file), `blame(start, end)`, `raw(OutputStream)` and `raw(Path)` (stream the raw content at this file's ref; also `Project.rawFile(path, ref, out)` and `rawFile(path, ref, target)`: the core's streaming download, not verified live), `delete(message)`, `response()`. Text is sent as UTF-8; `byte[]` is sent Base64-encoded with `encoding=base64` |
| `Branch` | `project()`, `name()`, `isProtected()`, `isDefault()`, `protect()` (GitLab's default rules), `unprotect()`, `delete()`, `refresh()`, `response()` |
| `Tag` | `project()`, `name()` (may contain slashes), `message()`, `target()`, `isProtected()` (a missing flag is `false`), `createdAt()`, `commit()` (loaded through `project.commit(sha)`, one extra GET; empty when the record has no commit), `signature()` (the core's `GitLabTagSignature`), `release()` (the release with the same tag name, one extra GET; empty on 404), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()`. Listed and loaded tags share one record, so a listed tag needs no extra request |
| `Commit` | `project()`, `sha()`, `title()`, `message()`, `diff()` (the core's `GitLabDiff` records), `cherryPick(branch)`, `revert(branch)`, `statuses()` (GitLab's defaults: the latest status of each name, on the default branch), `statuses(ref, name, all)` (`null` leaves `ref` or `name` out; `all` is sent only when `true`), `addStatus(state)` (a `CommitStatusState`; only the state is sent), `addStatus(payload)` (the core's `GitLabProjectCommitsCreatePayload`: name, ref, description, target URL, coverage, pipeline; overloaded, so a literal `null` needs a cast), `comments()`, `comment(body)` (sends `line_type=new`, which the core requires and GitLab documents as the default, and no path or line), `comment(body, path, line, lineType)` (a `CommitLineType`; a line below 1 throws `IllegalArgumentException` before any request), `mergeRequests()` (each is loaded from the project as the stream reaches it, gone ones are skipped; no `state` is sent), `refs()` (the core's `GitLabBasicRef` records; no `type` is sent), `signature()` (the core's `GitLabCommitSignature`; empty on HTTP 404), `response()` |
| `CommitStatus` | From `commit.statuses()` and `addStatus(...)`: `commit()`, `id()`, `status()` (the raw name: a listed status can be any CI status, such as `created` or `manual`, not only a `CommitStatusState`), `name()`, `ref()`, `description()`, `targetUrl()` (each empty when GitLab leaves it out), `response()` |
| `CommitComment` | From `commit.comments()` and `comment(...)`: `commit()`, `body()`, `path()`, `line()`, `lineType()` (the raw `new` or `old`) (each empty when GitLab leaves it out), `response()` |
| `CommitDraft` | From `project.createCommit(branch, message)`; a mutable collector, as `ProjectDraft`: `create(path, text)`, `create(path, bytes)`, `update(path, text)`, `update(path, bytes)`, `delete(path)`, `move(previousPath, path)` (each returns the draft and makes no request; a `null` content throws `NullPointerException`, a blank path `IllegalArgumentException`), `send()` (one `POST /projects/{id}/repository/commits`, the actions sent as `actions[][...]` form fields in the order added, binary content Base64-encoded with `encoding=base64` as for `RepositoryFile`; a draft without an action throws `IllegalStateException` before any request; the returned `Commit` is built from GitLab's answer, and an answer without an id throws `IllegalStateException`) |
| `CommitStatusState`, `CommitLineType` | The states `addStatus(state)` accepts (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELED`, `SKIPPED`) and the sides of a diff a line comment can be on (`NEW`, `OLD`), each with `value()`; unit tests compare both with the core's enums |

## `releases`

| Object | Methods |
|---|---|
| `Release` | `project()`, `tagName()` (may contain slashes, sent unchanged), `name()`, `description()`, `releasedAt()`, `isUpcoming()` (a missing flag is `false`), `update()` (`ReleaseUpdate`: `name`, `description`, `releasedAt(Instant)`, `milestones(List<String>)`, `send()`), `collectEvidence()` (returns the release GitLab answers with), `links()`, `link(id)`, `addLink(name, url)`, `addLink(payload)` (the core's payload, for the type and the asset paths), `delete()` (the tag stays), `refresh()` (`IllegalStateException` when gone), `response()` (milestones, assets and evidences are read from it). Listed, loaded, created and updated releases share one record, so none of them needs an extra request |
| `ReleaseLink` | `release()`, `id()`, `name()`, `url()`, `directAssetUrl()`, `type()` (`ReleaseLinkType`: `OTHER`, `RUNBOOK`, `PACKAGE`, `IMAGE`; an `Optional`, empty when the response has none; an unknown value throws `IllegalArgumentException`), `typeName()` (the raw value), `update()` (`ReleaseLinkUpdate`: `name`, `url`, `type(ReleaseLinkType)`, `send()`), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()` |

An empty `update().send()` of a release or a link throws `IllegalStateException` and makes no request. `ReleaseUpdate`
rejects `null` and a blank `name`; an empty `description` is sent as given, and whether GitLab then clears it is not
verified against a live instance. `milestones(List.of())` is sent as an empty list, and whether GitLab then removes the
milestones is not verified either.

## `discussions`

| Object | Methods |
|---|---|
| `IssueDiscussion`, `MergeRequestDiscussion` | `issue()` or `mergeRequest()`, `id()`, `isIndividualNote()`, `isResolvable()`, `isResolved()` (a missing flag is `false`), `notes()` (an unmodifiable list of `Note` or `MergeRequestNote`, taken from the response with no request), `reply(body)` (returns the new note), `resolve()`, `unresolve()` (each returns the discussion GitLab answers with, no extra request), `refresh()` (`IllegalStateException` when gone), `response()` |

Listed, loaded and updated discussions share one record, so none of them needs an extra request. Replying to a
discussion that is gone, and resolving one that cannot be resolved (an individual note), pass the core's exception
through. A blank body makes no request. GitLab can resolve merge request threads and resolvable issue threads only, and
has no request to delete a discussion.

## `reactions`

| Object | Methods |
|---|---|
| `Reaction` | `id()`, `name()`, `user()` (the core's `GitLabUserBasic`), `createdAt()` (each empty when GitLab leaves it out), `refresh()` (`IllegalStateException` when gone), `delete()`, `response()`. Owners: `Issue`, `Note`, `MergeRequest` and `MergeRequestNote` each have `reactions()` (a lazy stream) and `react(name)` |

`Reaction` is an object without parent navigation (so is `Runner`): a single class serves the four kinds of owner, so it
has no `issue()` or `mergeRequest()`. `refresh()` and `delete()` use the owner the reaction was made on. A blank name
makes no request; reacting twice with the same emoji passes GitLab's error through.

## `milestones`

| Object | Methods |
|---|---|
| `Milestone`, `GroupMilestone` | `project()` or `group()`, `id()`, `iid()`, `title()`, `description()`, `state()` (`MilestoneState`: `ACTIVE`, `CLOSED`; an unknown value throws `IllegalArgumentException`), `stateName()` (the raw value), `dueDate()` and `startDate()` (`Optional<LocalDate>`, empty when absent or blank; a value that is not an ISO date throws `IllegalStateException` naming it), `isExpired()` (a missing flag is `false`), `update()` (`MilestoneUpdate` or `GroupMilestoneUpdate`: `title`, `description`, `dueDate(LocalDate)`, `startDate(LocalDate)`, `send()`), `close()`, `reopen()` (state events `close` and `activate`), `delete()`, `issues()`, `mergeRequests()`, `refresh()` (`IllegalStateException` when gone), `response()`; `Milestone` also has `promote()` (to a group milestone, returns nothing). Listed, loaded, created and updated milestones share one record, so none of them needs an extra request |
| `MilestoneState` | `ACTIVE`, `CLOSED`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |

The issues and merge requests of a milestone are listed as summaries, so each one is loaded as the stream reaches it,
through the project named in the summary (`Milestone`: the owning project when the summary names none; `GroupMilestone`:
the items belong to different projects, and one that names none is skipped). An item that is gone by then is skipped. An
empty `update().send()` throws `IllegalStateException` and makes no request. A date takes a `LocalDate`, never
`null`, so it cannot be cleared; an empty `description` is sent as given, and whether GitLab then clears it is not
verified against a live instance. Dates are sent as `yyyy-MM-dd`.

## `members`

| Object | Methods |
|---|---|
| `AccessLevel` | `MINIMAL_ACCESS` (5, groups only: `Project.share` rejects it with `IllegalArgumentException` before any request), `GUEST` (10), `PLANNER` (15), `REPORTER` (20), `SECURITY_MANAGER` (25), `DEVELOPER` (30), `MAINTAINER` (40), `OWNER` (50); `value()`, `from(int)` (an unknown number throws `IllegalArgumentException`) |
| `Member`, `GroupMember` | `project()` or `group()`, `userId()`, `username()`, `accessLevel()`, `accessLevelValue()` (the raw number, for a level `AccessLevel` does not know), `expiresAt()` (`Optional<LocalDate>`), `update()`, `remove()`, `refresh()`, `response()`. A member listed by `allMembers()` may be inherited from a parent group: its `refresh()` loads it from the list with inherited members (`GET .../members/all/{user_id}`), while `update()` and `remove()` act on direct memberships only and fail with the core's exception for GitLab's HTTP 404 for an inherited member |
| `MemberUpdate`, `GroupMemberUpdate` | `accessLevel(...)`, `expiresAt(date)` (sent as the start of that day in UTC), `clearExpiresAt()` (sends `"expires_at": null`; the last of the two calls wins), `send()`; `send()` with nothing set throws `IllegalStateException` |
| `AccessRequest`, `GroupAccessRequest` | `project()` or `group()`, `userId()`, `username()`, `approve(level)` (returns the new member), `deny()`, `response()`. It has no `refresh()` |
| `LdapGroupLink` | `group()`, `cn()` and `filter()` (`Optional`: one of them is set), `provider()`, `accessLevel()` (`AccessLevel`; an unknown number throws `IllegalArgumentException`), `delete()`, `response()`. From `group.ldapLinks()` or `addLdapLink(...)` (Premium, tier errors pass through). A link has no id and GitLab has no call for one link, so there is no `refresh()` |
| `SamlGroupLink` | `group()`, `name()`, `accessLevel()`, `provider()` (`Optional`), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()`. From `group.samlLinks()`, `samlLink(...)` or `addSamlLink(...)` (Premium) |

## `webhooks`

| Object | Methods |
|---|---|
| `WebhookEvent` | `PUSH`, `TAG_PUSH`, `ISSUES`, `NOTE`, `MERGE_REQUESTS`, `JOB`, `PIPELINE`, `WIKI_PAGE`, `RELEASES`: the events that both the create/update request and the test trigger accept for a project and a group webhook |
| `Webhook`, `GroupWebhook` | `project()` or `group()`, `id()`, `url()`, `name()` (`Optional`), `isEnabledFor(event)` (a missing flag is `false`), `update()`, `test(event)`, `delete()`, `refresh()`, `response()`. Every group webhook call is Premium; the core's tier guard decides. The token and signing token are write-only and never exposed |
| `WebhookUpdate`, `GroupWebhookUpdate` | `url(...)`, `name(...)`, `description(...)`, `token(...)`, `enableSslVerification(...)`, `events(collection)` (replaces the subscription as in `github-sdk`: all nine `WebhookEvent` flags are sent, `true` for the listed events and `false` for the others; an empty collection switches them all off), `addEvents(...)` (sends the named flags as `true`), `removeEvents(...)` (sends the named flags as `false`), `send()`; `send()` with nothing set throws `IllegalStateException`. The answer of the update is the new object, no extra request |

## `deploy`

| Object | Methods |
|---|---|
| `DeployTokenScope` | `READ_REPOSITORY`, `READ_REGISTRY`, `WRITE_REGISTRY`, `READ_PACKAGE_REGISTRY`, `WRITE_PACKAGE_REGISTRY`, `READ_VIRTUAL_REGISTRY`, `WRITE_VIRTUAL_REGISTRY`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `DeployKey` | `project()`, `id()`, `title()`, `canPush()` (a missing flag is `false`), `expiresAt()` (`Optional<Instant>`), `update()`, `delete()`, `refresh()`, `response()` (the public key is only there). GitLab has no group-level deploy keys |
| `DeployKeyUpdate` | `title(...)`, `canPush(boolean)`, `send()`; `send()` with nothing set throws `IllegalStateException`. The key is loaded again after the update, because the core answers the update with a different record |
| `DeployToken`, `GroupDeployToken` | `project()` or `group()`, `id()`, `name()`, `username()` (`Optional`), `scopes()` (an unknown scope in the response throws `IllegalArgumentException`), `scopeNames()` (the raw names, never fails), `isRevoked()`, `isExpired()`, `expiresAt()` (`Optional<Instant>`), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()` |
| `CreatedDeployToken`, `GroupCreatedDeployToken` | `deployToken()`, `token()`: the secret, returned by GitLab only when the token is created, and never part of `toString()`; `response()` (the core's record: its `toString()` is redacted by the core, but its `token()` holds the secret) |

`Project` adds `deployKeys()`, `deployKey(keyId)`, `addDeployKey(title, key)`, `addDeployKey(title, key, canPush)`,
`enableDeployKey(keyId)` (loaded again after enabling), `deployTokens()`, `deployToken(tokenId)` and
`createDeployToken(name, scopes...)`; `Group` adds `deployTokens()`, `deployToken(tokenId)` and `createDeployToken(name,
scopes...)`. Creating a token with no scope throws `IllegalArgumentException` and sends nothing.

## `protection`

| Object | Methods |
|---|---|
| `ProtectedAccess` | `NO_ACCESS` (0), `DEVELOPER` (30), `MAINTAINER` (40), `ADMINISTRATOR` (60); `value()`, `from(int)` (an unknown number throws `IllegalArgumentException`) |
| `ProtectedBranch`, `GroupProtectedBranch` | `project()` or `group()`, `name()`, `allowsForcePush()` and `requiresCodeOwnerApproval()` (a missing flag is `false`), `pushAccess()` and `mergeAccess()` (role-based rules only; an unknown level throws `IllegalArgumentException`), `pushAccessValues()` and `mergeAccessValues()` (the raw numbers of the same rules, never fail), `allowForcePush(boolean)` and `requireCodeOwnerApproval(boolean)` (each returns the new object from the answer of GitLab, no extra request), `unprotect()`, `refresh()` (`IllegalStateException` when gone), `response()`. Every group call is Premium; the core's tier guard decides |
| `ProtectedTag` | `project()`, `name()`, `createAccess()` and `createAccessValues()` (role-based rules only, as above), `unprotect()`, `refresh()`, `response()`. GitLab cannot update a protected tag and has no group-level protected tags |

`Project` adds `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)` (only the name is sent, the
defaults of GitLab decide), `protectBranch(name, push, merge)`, `protectedTags()`, `protectedTag(name)` and
`protectTag(name, create)`; `Group` adds `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)` and
`protectBranch(name, push, merge)` (Premium). Names with a slash (`release/1.x`) reach the core unchanged. The v1
`Branch.protect()` and `Branch.unprotect()` stay as they are.

## `variables`

| Object | Methods |
|---|---|
| `VariableType` | `ENV_VAR`, `FILE`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `Variable` | `project()`, `key()`, `value()` (`Optional`: secret, do not log it; a hidden variable has none), `type()` (an unknown type throws `IllegalArgumentException`), `typeName()` (the raw name, never fails), `isProtected()`, `isMasked()`, `isRaw()`, `isHidden()` (a missing flag is `false`), `environmentScope()` (`Optional`: the SDK never assumes `*`), `description()` (`Optional`), `update()`, `delete()` (void), `refresh()` (`IllegalStateException` when gone), `response()` (its `toString()` is redacted by the core). `refresh()`, `update()` and `delete()` target the variable by key and its own environment scope (`filter[environment_scope]`); a variable with no scope, or a blank one, in the answer is targeted by key alone. A stale scope may make `update()` change a different variable of the same key: GitLab's documentation says that when the scope of the filter does not exist, the endpoint updates a variable with the same key but a different scope (not verified live). Listed and loaded variables share one record, so no list item is loaded again |
| `VariableUpdate` | `value(...)`, `protectedValue(boolean)`, `masked(boolean)`, `raw(boolean)`, `type(VariableType)`, `environmentScope(...)` (the NEW scope; the variable is still targeted by its old one), `description(...)`, `send()`; `send()` with nothing set throws `IllegalStateException`. The answer of the update is the new object, no extra request |
| `GroupVariable`, `GroupVariableUpdate` | The same members as `Variable` and `VariableUpdate` with `group()`. GitLab documents a scope filter for the group operations, but the core does not offer it, so `refresh()`, `update()` and `delete()` address the variable by key alone: the SDK cannot tell same-key group variables of different scopes apart, and which one GitLab then touches is not verified |

Neither `Variable` nor `GroupVariable` nor an update builder defines a `toString()` that prints a value.
`maskedAndHidden` is reachable only through `createVariable(payload)`; the update builders have no such flag.

## `environments`

| Object | Methods |
|---|---|
| `EnvironmentTier` | `PRODUCTION`, `STAGING`, `TESTING`, `DEVELOPMENT`, `OTHER`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `DeploymentStatus` | `CREATED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELED`, `SKIPPED`, `BLOCKED`; `value()`, `isSettable()` (only `RUNNING`, `SUCCESS`, `FAILED` and `CANCELED`), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `Environment` | `project()`, `id()`, `name()`, `slug()`, `state()` (the raw GitLab value: `available`, `stopping` or `stopped`), `externalUrl()`, `description()`, `kubernetesNamespace()`, `fluxResourcePath()` (all `Optional`), `tier()` (`Optional<EnvironmentTier>`, an unknown tier throws `IllegalArgumentException`), `tierName()` (the raw name, `Optional`), `update()`, `stop()`, `stop(boolean force)`, `delete()` (void), `deployments()`, `refresh()` (`IllegalStateException` when gone), `response()`. Listed and loaded environments share one record, so no list item is loaded again. `deployments()` lists through the core's `environment` filter, which matches by the environment NAME, and loads each listed deployment again (one request per item) |
| `EnvironmentUpdate` | `externalUrl(...)`, `tier(EnvironmentTier)`, `description(...)`, `kubernetesNamespace(...)`, `clearKubernetesNamespace()`, `fluxResourcePath(...)`, `clearFluxResourcePath()`, `clusterAgentId(long)`, `clearClusterAgent()`, `send()`; `send()` with nothing set throws `IllegalStateException`, and a `clear...()` call counts as a change. A `clear...()` call sends JSON `null` (the core's `OptionalNull`); for one field the last call of the set and clear methods wins. The answer of the update is the new object, no extra request. The name cannot be changed, and the auto-stop setting is not offered on update |
| `Deployment` | `project()`, `id()`, `iid()`, `ref()`, `sha()`, `status()` (an unknown status throws `IllegalArgumentException`), `statusName()` (the raw name), `updateStatus(DeploymentStatus)` (only `RUNNING`, `SUCCESS`, `FAILED` and `CANCELED`: any other status throws `IllegalArgumentException` before a request), `approve(comment)`, `reject(comment)` (Premium; they return the core's `GitLabDeploymentApproval`; a `null` comment is sent as none, a blank one throws `IllegalArgumentException`), `mergeRequests()` (each listed merge request is loaded again through the project GitLab names, or this one; gone ones are skipped), `delete()` (void), `refresh()` (`IllegalStateException` when gone), `response()` (the record of `GET /projects/{id}/deployments/{id}`) |

Protected environments (Premium) are not wrapped.

## `schedules`

| Object | Methods |
|---|---|
| `PipelineSchedule` | `project()`, `id()`, `description()`, `ref()`, `cron()`, `cronTimezone()`, `isActive()` (absent: `false`), `nextRunAt()`, `owner()` (the core's `GitLabUserBasic`), `lastPipeline()` (loaded through `project.pipeline(id)`: one request per call; empty when the schedule has not run), `variables()` (an immutable list built from the response, no request; it does not reflect variables added, changed or deleted since, so use `refresh()`), `update()`, `takeOwnership()` (a new object from the answer), `play()` (starts a pipeline), `addVariable(key, value)`, `addVariable(key, value, type)`, `variable(key)`, `delete()`, `refresh()`, `response()` (the detail record) |
| `PipelineScheduleUpdate` | `description(...)`, `ref(...)`, `cron(...)`, `cronTimezone(...)` (cron and time zone are sent as given, not validated), `active(boolean)`, `send()` (an empty update throws `IllegalStateException` before a request). The schedule inputs are not offered |
| `ScheduleVariable` | `schedule()`, `key()`, `value()` (an `Optional`; secret, do not log it), `type()` (the `variables` package's `VariableType`; an unknown type throws `IllegalArgumentException`), `typeName()`, `update()`, `delete()`, `refresh()`, `response()` |
| `ScheduleVariableUpdate` | `value(...)` (an empty value is sent as given), `type(VariableType)`, `send()` |
| `PipelineTrigger` | `project()`, `id()`, `description()` (`Optional`), `token()` (secret, do not log it; the token as GitLab returns it: for a trigger owned by someone else GitLab may return a shortened token, not verified), `lastUsed()`, `expiresAt()`, `owner()`, `update()`, `delete()`, `refresh()`, `response()` |
| `PipelineTriggerUpdate` | `description(...)`, `send()` |

A schedule from a list is loaded again, because the list holds a smaller record than a single load. GitLab has no
call that lists the variables of a schedule, so they come from the schedule's own response. Firing a trigger is `Project.triggerPipeline(token, ref, variables)`; take-ownership of a trigger is not covered (the core has no operation).

## `runners`

| Object | Methods |
|---|---|
| `Runner` | `id()`, `description()`, `name()`, `type()` (an `Optional<RunnerType>`; an unknown type throws `IllegalArgumentException`), `typeName()` (the raw name; a type the core's enum does not know reads as `unrecognized`), `isOnline()`, `isPaused()`, `isActive()` (absent: `false`), `status()` (raw), `tagList()` (the response's tag list, an immutable list; empty when absent), `version()`, `contactedAt()`, `update()`, `refresh()`, `delete()` (removes the runner everywhere), `disable(project)` (only valid for a runner enabled in that project), `managers()` (the core's `GitLabCiRunnerManager` records, an immutable list), `jobs()` (the core's `GitLabCiJobBasicWithProject` records, lazy), `resetAuthenticationToken()` (secret), `response()` (the detail record) |
| `RunnerUpdate` | `description(...)`, `paused(boolean)`, `active(boolean)`, `tags(List)` (copied; a `null` element throws `NullPointerException`), `runUntagged(boolean)`, `locked(boolean)`, `maximumTimeout(int)`, `maintenanceNote(...)`, `send()` (an empty update throws `IllegalStateException` before a request). The access level is not offered |
| `RunnerType` | `INSTANCE_TYPE`, `GROUP_TYPE`, `PROJECT_TYPE`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |
| `RunnerToken` | `token()` (secret, do not log it), `expiresAt()`, `response()`; `toString()` prints the token as `<redacted>` |

`GitLabSdk.runner(id)` loads a runner by its global id (`GET /runners/{id}`). A runner from a project or group list is
loaded again, because the list holds a smaller record: one request per item. `Runner` has no parent navigation, like
`Reaction`: one class serves instance, group and project runners, and a runner can be enabled in several projects.
`update()`, `delete()`, `managers()`, `jobs()` and `resetAuthenticationToken()` always use the global runner id.

## `packages`

| Object | Methods |
|---|---|
| `ProjectPackage` | `id()`, `name()`, `version()`, `type()` (a `PackageType`; an unknown type throws `IllegalArgumentException`), `typeName()` (the raw name), `status()` (raw), `createdAt()`, `lastDownloadedAt()`, `project()` (no request: the owning project for a package of a project list, a handle of the project named by `project_id` for a package of a group list), `refresh()`, `files()` (lazy), `delete()`, `response()`. A response without a project id makes `project()`, `refresh()`, `files()` and `delete()` throw `IllegalStateException` |
| `PackageFile` | `projectPackage()` (the package the file belongs to, no request), `id()`, `fileName()`, `size()`, `sha256()`, `download(Path)` (written atomically, never partial), `download(OutputStream)` (never closed by the SDK; may hold a partial body on failure), `delete()`, `response()`. `download` and `delete` throw `IllegalStateException` when the package response names no project id; `download(Path)` throws `IllegalArgumentException` for a target without a parent directory and `FileAlreadyExistsException` for an existing directory |
| `PackageType` | `MAVEN`, `NPM`, `CONAN`, `NUGET`, `PYPI`, `COMPOSER`, `GENERIC`, `GOLANG`, `DEBIAN`, `RUBYGEMS`, `HELM`, `TERRAFORM_MODULE`, `RPM`, `ML_MODEL`, `CARGO`, `SWIFT`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |

GitLab lists and loads a package with the same record, so nothing is loaded again. The object is `ProjectPackage` and
not `Package`, which would shadow `java.lang.Package`. The download calls use the client's own authentication and no
download mode. Package files can contain secrets: the SDK writes only where the caller says.

## `registry`

| Object | Methods |
|---|---|
| `RegistryRepository` | `id()`, `name()`, `path()`, `location()`, `tagsCount()`, `project()` (as for a package), `refresh()` (by the repository id, `GET /registry/repositories/{id}`; a repository without a project id takes the `project_id` of the answer), `tags()` (lazy; each listed tag is loaded again, gone ones are skipped), `tag(name)`, `delete()`, `deleteTags(filter)` (bulk delete by regular expression with the core's `GitLabProjectContainerRegistryDeleteTagFilter`; passed through unchanged, `null` throws `NullPointerException`), `response()`. A response without a project id makes `project()`, `tags()`, `tag(name)`, `delete()` and `deleteTags(filter)` throw `IllegalStateException`; `refresh()` still works |
| `RegistryTag` | `name()`, `location()`, `digest()`, `totalSize()`, `repository()`, `refresh()`, `delete()` (`IllegalStateException` when the repository response names no project id), `response()` (the details record) |

The tag list holds a smaller record than a single tag, so every tag is loaded through the details request: one request
per tag. A tag name is passed to the core unchanged; a blank name throws before a request. Images can contain secrets;
the SDK reads tag metadata only.

## `wikis`

| Object | Methods |
|---|---|
| `Wiki` | From `project.wiki()` or `group.wiki()` (Premium), or `Wiki.forProject(project)` and `Wiki.forGroup(group)`; none makes a request. `all()` (one list request, then each page loaded again as the stream reaches it; gone ones are skipped), `find(slug)` (empty when GitLab answers 404, expected in some cases where the wiki is not available; a 403 or the core's `GitLabTierException` passes through), `create(title, content)`, `create(title, content, format)`, `uploadAttachment(fileName, bytes)` and `uploadAttachment(fileName, bytes, branch)` (the form part `file` as `application/octet-stream`; the core's `GitLabWikiAttachment` record is passed through) |
| `WikiPage` | `slug()` (as GitLab returns it, not URL-encoded), `title()`, `content()`, `format()` (an `Optional<WikiFormat>`; an unknown format throws `IllegalArgumentException`), `formatName()` (the raw name), `encoding()`, `frontMatter()` (an immutable copy, nested maps and lists included; empty when absent), `wiki()`, `refresh()`, `update()`, `delete()`, `response()` |
| `WikiPageUpdate` | `title(...)` (GitLab is expected to give the page a new slug; the request names the old one), `content(...)`, `format(WikiFormat)`, `send()` (an empty update, or one with only the format set, throws `IllegalStateException` before a request; GitLab needs the title or the content) |
| `WikiFormat` | `MARKDOWN`, `RDOC`, `ASCIIDOC`, `ORG`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |

GitLab lists the pages of a wiki in one unpaged response without their content, so each page is loaded through
`GET .../wikis/{slug}`: one request per page. A slug such as `dir/page` reaches the core unchanged, and the core encodes
it in the path; an already encoded slug would be encoded twice. The group calls are Premium: GitLab is expected to
answer 403 or 404 on a lower tier, and a client configured with a lower tier refuses them with the core's
`GitLabTierException` before a request. No `toString()` prints the content of a page.

## `snippets`

| Object | Methods |
|---|---|
| `ProjectSnippets` | From `project.snippets()` or `ProjectSnippets.forProject(project)`; neither makes a request. `all()` (the listed snippets are complete, so none is loaded again), `find(id)` (empty when GitLab answers 404, expected in some cases where snippets are not available; a 403 or the core's `GitLabTierException` passes through), `create(title, visibility, filePath, content)` (one file, sent as a one-entry `files` array; a blank title or path, or a `null` visibility or content, throws before a request), `create(payload)` (the core's `GitLabProjectProjectSnippetsCreatePayload`, sent unchanged) |
| `Snippet` | `id()`, `title()`, `description()`, `visibility()` (an `Optional<SnippetVisibility>`; an unknown visibility throws `IllegalArgumentException`), `visibilityName()` (the raw name), `author()` (the core's `GitLabUserBasic`), `webUrl()`, `files()` (a `SnippetFile` per `files` entry; empty when GitLab sends no `files`, as for a legacy single-file snippet; an entry without a path is skipped), `content()` (`GET .../snippets/{snippet_id}/raw` as text; for a snippet with several files GitLab is expected to return one file only, which one is unchecked, so read each file with `files()` and `fileContent(ref, path)`), `fileContent(ref, path)` (`GET .../files/{ref}/{file_path}/raw` as text; the ref is any branch, tag or commit; usually the default branch, which `SnippetFile.rawUrl()` shows, as in `.../-/snippets/1/raw/main/add.rb`; ref and path reach the core unchanged), `project()`, `refresh()`, `update()`, `delete()`, `response()` |
| `SnippetUpdate` | `title(...)`, `description(...)`, `visibility(SnippetVisibility)`, `addFile(path, content)`, `updateFile(path, content)`, `deleteFile(path)`, `moveFile(previousPath, path)` (sends `previous_path` and no content; a blank path throws before a request), `send()` (the file changes in call order as the `files` array; an empty update throws `IllegalStateException` before a request) |
| `PersonalSnippets` | From `sdk.snippets()` or `PersonalSnippets.forSdk(sdk)`; neither makes a request. `all()` (`GET /snippets`: the signed-in user's snippets; each listed `GitLabSnippet` is copied into a `GitLabPersonalSnippet`, which has the same fields, without a request), `find(id)` (`GET /snippets/{id}`: GitLab is expected to return any snippet the user can see, which can be a project snippet with `response().projectId()` set, whose `update()` and `delete()` still go to `/snippets/{id}` (not verified for a project snippet); empty when GitLab answers 404, a 403 passes through), `create(title, visibility, filePath, content)` and `create(payload)` (the core's `GitLabCodeSnippetsCreatePayload`) as for a project |
| `PersonalSnippet` | As `Snippet`, without `project()`: `id()`, `title()`, `description()`, `visibility()`, `visibilityName()`, `author()`, `webUrl()`, `files()`, `content()`, `fileContent(ref, path)` (the core declares this snippet id as text, so the SDK passes `String.valueOf(id())`), `refresh()`, `update()`, `delete()` (the core's optional answer is discarded), `response()` (a `GitLabPersonalSnippet`) |
| `PersonalSnippetUpdate` | As `SnippetUpdate`, sent with `PUT /snippets/{id}` |
| `SnippetFile` | `path()`, `rawUrl()` (an `Optional`) |
| `SnippetVisibility` | `PRIVATE`, `INTERNAL` (GitLab.com disables internal visibility for new snippets, per the GitLab docs), `PUBLIC`; `value()` (the name as GitLab spells it), `from(String)` (an unknown name throws `IllegalArgumentException`) |

The SDK sends the files of a snippet only as the `files` array: the `content` and `file_name` parameters of create and
update are deprecated by GitLab and never sent (`create(payload)` passes the core's payload through unchanged). The raw
content is read as text: binary content comes back decoded as UTF-8, and a missing snippet, file or ref is the core's
`GitLabNotFoundException`, not an empty result. No `toString()` prints the content or the description of a snippet.

## `dependencies`

| Object | Methods |
|---|---|
| `Dependencies` | From `project.dependencies()` or `Dependencies.forProject(project)` (Ultimate); neither makes a request. `all()` (`GET /projects/{id}/dependencies`, no filter), `all(first, more...)` (the same with `package_manager` set to the given `GitLabProjectDependenciesListPackageManager` values; a `null` argument or element throws `NullPointerException` before a request). There is no `find`: GitLab has no call that loads one dependency |
| `Dependency` | A value, so no `refresh()`. `name()`, `version()`, `packageManager()` (the raw name, while `Dependencies.all(first, more...)` filters by the core's `GitLabProjectDependenciesListPackageManager` enum; a name the enum does not know is still read), `filePath()` (`dependency_file_path`), `vulnerabilities()` (the core's `GitLabDependencyEntityVulnerabilityEntity` records: the advisories GitLab matched, not SDK vulnerability objects; an immutable list, empty when absent), `licenses()` (the core's `GitLabDependencyEntityLicenseEntity` records; an immutable list, empty when absent), `malware()` (an `Optional<Boolean>`), `project()`, `response()` |

The list is an Ultimate feature: GitLab is expected to answer 403 or 404 on a lower tier or when the feature is off, and
a client configured with a lower tier refuses the call with the core's `GitLabTierException` before a request; both
pass through. Listed values are as GitLab sent them: the SDK invents none, so an absent version, package manager or
file path is an empty `Optional`.

## `audit`

| Object | Methods |
|---|---|
| `AuditEvents` | From `project.auditEvents()` or `group.auditEvents()` (Premium), or `AuditEvents.forProject(project)` and `AuditEvents.forGroup(group)`; none makes a request. `all()` (`GET /projects/{id}/audit_events` with `pagination=keyset`, `order_by=id`, `sort=desc`: newest first), `createdBetween(after, before)` (the same with `created_after` and `created_before`, both inclusive per the GitLab docs; a `null` throws `NullPointerException` and `after` later than `before` throws `IllegalArgumentException`, both before a request; the bounds are sent as `YYYY-MM-DDTHH:MM:SSZ`, the docs' format, because a fraction is not verified to be accepted: the range is widened to whole seconds, `after` rounded down and `before` rounded up (not past `Instant.MAX`), after the order check; GitLab may return events up to one second outside the requested bounds, so filter on `createdAt()` for exactness), `find(id)` (empty when GitLab answers 404, which it is expected to do in some cases where audit events are not available; a 403 or the core's `GitLabTierException` passes through; on a group it needs the Owner role) |
| `AuditEvent` | Events do not change, so no `refresh()`. `id()`, `authorId()`, `entityId()`, `entityType()` (the raw name, such as `Project`), `eventName()`, `details()` (an immutable copy, nested maps and lists included, values as GitLab sent them; empty when absent), `createdAt()` (an `Optional<Instant>`), `auditEvents()`, `response()` |

The group calls are the same with `/groups/{id}`. Audit events are a Premium feature: GitLab is expected to answer 403
or 404 on a lower tier, and a client configured with a lower tier refuses the calls with the core's
`GitLabTierException` before a request; both pass through. Per the GitLab docs, a project Developer and a group
Developer or Maintainer see only the events of their own actions. Offset paging of audit events is deprecated by GitLab
and never sent, so the only order is newest first. The details can hold names, e-mail and IP addresses, so no
`toString()` prints them.

## `account`

The keys of the signed-in account, from `sdk.me()`. None has parent navigation (like `User`); each has `response()`. The
public key of an `SshKey` or a `GpgKey` is not a secret, but it is never part of `toString()`.

| Object | Methods |
|---|---|
| `SshKey` | `id()`, `title()`, `key()`, `expiresAt()` (empty when the key does not expire), `delete()` (HTTP 404 is ignored: the core answers empty), `response()` |
| `Email` | `id()`, `address()`, `confirmedAt()` (empty while unconfirmed), `delete()` (the core does not map 404, so a gone address fails with the core's exception), `response()` |
| `GpgKey` | `id()`, `key()`, `createdAt()`, `delete()` (as `Email`), `response()` |

## `repository`

| Object | Methods |
|---|---|
| `TreeEntry` | From `project.tree(...)`: `path()`, `name()`, `id()`, `mode()`, `isDirectory()` (GitLab type `tree`), `isFile()` (type `blob`; a submodule is neither), `lastCommit()` (empty: the SDK does not send `with_last_commit`), `response()` |
| `Comparison` | From `project.compare(from, to, straight)`: `commit()`, `commits()`, `diffs()` (unmodifiable lists), `isTimedOut()`, `isSameRef()`, `webUrl()`, `response()` |
| `Contributor` | From `project.contributors()`: `name()`, `email()`, `commits()` (without merge commits), `additions()`, `deletions()`, `response()` |
| `BlameRange` | From `RepositoryFile.blame()`: `commit()` (the core's `GitLabBlameRangeCommit`), `lines()`, `response()` |

`tree()` sends only `recursive=false`, which is GitLab's documented default; with no `ref` GitLab uses the default branch
of the project, and with no `path` the root, 20 entries per page. The list is walked by offset pages, which the GitLab
docs do not mark deprecated (checked). `compare` always sends `straight`; `false` compares from the merge base.
`contributors()` sends no `order_by` or `sort`; per the GitLab docs
(<https://docs.gitlab.com/api/repositories/>, "Get contributor list") "If not specified, contributors are ordered by commit
date" and the sort "Default is `asc`"; not verified live. `mergeBase(refs...)` needs two or more refs, returns an
`Optional<Commit>` (empty when GitLab answers 404 for the merge base or the found commit is gone), and loads the found
commit with one more GET. `changelog(version)` checks the version before a request and only generates the notes;
GitLab does not commit them. `RepositoryFile.blame()` counts the lines of the content the object holds and sends `range[start]=1` and
`range[end]=<lines>`, because the core's operation takes a range (the spec marks both required); an empty file sends no
request. Whether GitLab accepts a range that ends exactly at the last line is unchecked live.

## `badges`

| Object | Methods |
|---|---|
| `Badge`, `GroupBadge` | `project()` or `group()`, `id()`, `name()` (`Optional`), `linkUrl()` and `imageUrl()` (as stored, with their placeholders), `renderedLinkUrl()` and `renderedImageUrl()` (`Optional`), `update()`, `delete()`, `refresh()` (`IllegalStateException` when gone), `response()` |
| `BadgeUpdate`, `GroupBadgeUpdate` | `linkUrl(...)`, `imageUrl(...)`, `name(...)`, `send()`; `send()` with nothing set throws `IllegalStateException`. There is no way to clear a field: the core's payload cannot send a null |

## `pushrules`

Push rules are Premium in GitLab's API description (the core's tier guard decides; the error passes through). A project
or a group has one set of rules, so there is no id lookup.

| Object | Methods |
|---|---|
| `PushRules`, `GroupPushRules` | `project()` or `group()`, `id()`, `createdAt()`, the five expressions (`commitMessageRegex()`, `commitMessageNegativeRegex()`, `branchNameRegex()`, `authorEmailRegex()`, `fileNameRegex()`, each `Optional`), `maxFileSize()` (`Optional`, in megabytes), the seven flags (`denyDeleteTag()`, `memberCheck()`, `preventSecrets()`, `commitCommitterCheck()`, `commitCommitterNameCheck()`, `rejectUnsignedCommits()`, `rejectNonDcoCommits()`; a missing one is `false`), `update()`, `delete()`, `refresh()` (`IllegalStateException` when the rules are gone), `response()` |
| `PushRulesUpdate`, `GroupPushRulesUpdate` | a setter for each field above, `send()`; `send()` with nothing set throws `IllegalStateException`. An expression cannot be cleared (no `clear...()`: the core's payload cannot send a null) |

## `tokens`

| Object | Methods |
|---|---|
| `AccessTokenScope` | The 18 scopes the core accepts for a project or group access token (`API`, `READ_API`, `CREATE_RUNNER`, `MANAGE_RUNNER`, `K8S_PROXY`, `SELF_ROTATE`, `MCP`, `MCP_ORBIT`, `GRANULAR`, `READ_REPOSITORY`, `WRITE_REPOSITORY`, `READ_REGISTRY`, `WRITE_REGISTRY`, `READ_VIRTUAL_REGISTRY`, `WRITE_VIRTUAL_REGISTRY`, `READ_OBSERVABILITY`, `WRITE_OBSERVABILITY`, `AI_FEATURES`); `value()`, `from(String)`; unit tests compare it with both core enums |
| `AccessToken`, `GroupAccessToken` | `project()` or `group()`, `id()`, `name()`, `description()` (`Optional`), `scopes()` (throws `IllegalArgumentException` for a scope the SDK does not know) and `scopeNames()` (never throws), `isActive()`, `isRevoked()`, `expiresAt()`, `lastUsedAt()`, `userId()` (`Optional`), `accessLevel()` (`Optional<AccessLevel>`), `rotate()` and `rotate(LocalDate)` (return the new token with its secret), `revoke()`, `refresh()`, `response()` |
| `CreatedAccessToken`, `GroupCreatedAccessToken` | From `createAccessToken(...)` and `rotate(...)`: `accessToken()` (without the secret), `token()` (the secret; GitLab returns it only in this answer), `response()`. `toString()` never prints the secret |

`createAccessToken` accepts the roles `GUEST`, `REPORTER`, `DEVELOPER`, `MAINTAINER` and `OWNER`: the core's response
type has no value for `PLANNER` and `SECURITY_MANAGER`, so the answer of such a creation could not be decoded and the
secret would be lost; the SDK throws `IllegalArgumentException` before the request (core follow-up 11). The `get`, `delete`
and `rotate` calls take the token id as a string in the core; the SDK converts it.

## `mirrors`

| Object | Methods |
|---|---|
| `MirrorAuthMethod` | `SSH_PUBLIC_KEY`, `PASSWORD`; `value()`, `from(String)`; a unit test compares it with the core enum |
| `RemoteMirror` | `project()`, `id()`, `url()` (may carry credentials), `isEnabled()`, `updateStatus()`, `lastUpdateAt()`, `lastUpdateStartedAt()`, `lastSuccessfulUpdateAt()`, `lastError()`, `authMethod()` (raw, `Optional`), `mirrorBranchRegex()` (`Optional`), `onlyProtectedBranches()`, `keepDivergentRefs()`, `hostKeyFingerprints()`, `update()`, `sync()` (GitLab updates the mirror after the call returns), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()`. It has no `toString()` of its own, so it never prints the address; its `response()` is the core's record, whose `toString()` prints it (core follow-up 16) |
| `RemoteMirrorUpdate` | `enabled(...)`, `authMethod(MirrorAuthMethod)`, `keepDivergentRefs(...)`, `onlyProtectedBranches(...)`, `mirrorBranchRegex(...)`, `hostKeys(list)`, `send()`; the address cannot be changed (the core's payload has no field for it) |
| `PullMirror` | `project()`, `id()`, `url()` (`Optional`), `isEnabled()`, `updateStatus()`, `lastError()`, `lastUpdateAt()`, `lastUpdateStartedAt()`, `lastSuccessfulUpdateAt()`, `mirrorTriggerBuilds()`, `onlyMirrorProtectedBranches()`, `mirrorOverwritesDivergedBranches()`, `mirrorBranchRegex()`, `update()`, `refresh()`, `response()`. Premium; the update operation is marked experimental in GitLab's API description. No `delete()`: the core has no operation for it |
| `PullMirrorUpdate` | `enabled(...)`, `url(...)`, `authUser(...)`, `authPassword(...)` (write-only, never printed; the object has no `toString()`), `mirrorTriggerBuilds(...)`, `onlyMirrorProtectedBranches(...)`, `mirrorOverwritesDivergedBranches(...)`, `mirrorBranchRegex(...)`, `send()` |

`configurePullMirror(url)` calls the core's `update` (`PUT /projects/{id}/mirror/pull`). The core's `create`
(`POST /projects/{id}/mirror/pull`) starts a pull and takes the pull-request-event fields, not the settings, so it is not
used.

## `approvals` (project and group rules)

The package also holds the merge request types (see `mergerequests`). The rules below are the project-level and
group-level ones. All are Premium in GitLab's API description. The rule type names follow ruling R1.

| Object | Methods |
|---|---|
| `ProjectApprovalRule` | `project()`, `id()`, `name()`, `ruleType()`, `approvalsRequired()`, `reportType()` (each `Optional`), `containsHiddenGroups()` and `appliesToAllProtectedBranches()` (a missing flag is `false`), `update()`, `delete()`, `refresh()` (`IllegalStateException` when gone), `response()` (the approvers and protected branches are in the response) |
| `ProjectApprovalRuleUpdate` | `name(...)`, `approvalsRequired(int)`, `userIds(list)`, `groupIds(list)`, `usernames(list)`, `protectedBranchIds(list)`, `appliesToAllProtectedBranches(boolean)`, `removeHiddenGroups(boolean)`, `send()`; `send()` with nothing set throws `IllegalStateException`. The deprecated `users` and `groups` fields are not offered |
| `GroupApprovalRule` | `group()`, `id()`, `name()`, `ruleType()`, `approvalsRequired()`, `reportType()`, `containsHiddenGroups()`, `appliesToAllProtectedBranches()`, `update()`, `response()`. No `delete()` and no `refresh()`: the core has no operation to delete or to load a single group rule (core follow-up 12) |
| `GroupApprovalRuleUpdate` | `name(...)`, `approvalsRequired(int)`, `userIds(list)`, `groupIds(list)`, `send()` |
| `ApprovalConfiguration` | From `project.approvalConfiguration()` (`GET /projects/{id}/approvals`): `project()`, `resetApprovalsOnPush()`, `selectiveCodeOwnerRemovals()`, `disableOverridingApproversPerMergeRequest()`, `mergeRequestsAuthorApproval()`, `mergeRequestsDisableCommittersApproval()`, `requireReauthenticationToApprove()` (a missing flag is `false`), `update()`, `refresh()`, `response()`. `approvals_before_merge` and `require_password_to_approve` are deprecated in GitLab's API description, so they have no accessor and no setter; they stay readable in `response()` |
| `ApprovalConfigurationUpdate` | one setter for each flag above, `send()` (`POST /projects/{id}/approvals`) |

No rule type is sent on create or update: GitLab's API description says not to use the `rule_type` field when building
rules from the API. There is no `createApprovalRule(payload)`: the core's payload also has the deprecated `users` and
`groups` fields, so the SDK builds the request itself from the ids and names.

## What is not covered

- **Search.** The core's search calls (`search().instance()`, `group()` and `project()`) return nothing, because the
  OpenAPI description declares no response body and the result type depends on the `scope` parameter, so the SDK has
  no search. It is deferred to a future GraphQL project; use the core client meanwhile for what it can do. AI search
  (`getSemantic`) is out of scope.
- **Single artifact files and job tokens.** Reading one file out of the artifacts (the core's `call` methods return
  nothing, so the body is lost; core follow-up 18), a job-token parameter on the artifact calls (the client's own authentication is used), and the
  artifact upload and authorization of the runner protocol are not covered; use the core.
- **Fields of a project or group outside the typed set.** `ProjectDraft`, `ProjectUpdate`, `GroupDraft` and
  `GroupUpdate` take the common fields only (see above); anything else needs the core's `MultipartFormData` API.
- **Stars of a project (`stars()`).** The core decodes `GET /projects/{id}/starrers` as bare users, but the GitLab
  documentation shows `starred_since` and a nested `user` for every item, so every field would come back empty. Not
  offered (core follow-up 7).
- **Search, user administration and the other keys calls.** Search stays out (see above); the administrator calls on
  users (create, block, impersonation, keys and emails of other users) are deferred to the admin API; GPG key
  revocation and the lookups of a single key are not offered.
- **Options of a multi-file commit.** `CommitDraft` sends the branch, the message and the actions only. The `chmod` action (`execute_filemode`), `start_branch`, `start_project`, `force`, the author fields and the `last_commit_id` of an action are not covered; use the core's `MultipartFormData` API. The form (`branch`, `commit_message`, repeated `actions[][...]` fields) follows GitLab's description of a form-encoded commit request; it is unit-tested and not verified live. A statuses list cannot be filtered by stage, pipeline or order.
- **Options of `Branch.protect()`.** It uses GitLab's default rules; use `project.protectBranch(name, push, merge)` or
  the core for the options.
- **Filter on the jobs of one pipeline.** `pipeline.jobs()` lists without a filter. The job trace (`Job.trace()`) is not
  offered: the core's `getTrace` returns a job record instead of the plain text (core follow-up 17). The full test
  report (`getTestReport`) and the deprecated bridges operation are not offered.
- **User-, group-, deploy-key- and member-role-based protection rules.** They are skipped in the access lists and stay
  available from `response()`; the `allowed_to_push`, `allowed_to_merge`, `allowed_to_unprotect` and `allowed_to_create`
  rules and the unprotect level cannot be set; use the core.
- **Everything outside the areas above**, such as group releases, member roles (Ultimate), the premium-only
  member endpoints (approving pending members, billable members), personal and impersonation access tokens,
  instance-level deploy keys and tokens,
  protected environments, the descendant groups of a group at every level, and the administration of an instance. Use
  `gitlab-rest-client` for them; see [When the SDK does not cover a
  call](../../README.md#when-the-sdk-does-not-cover-a-call).
- **Releases.** Group releases, the typed latest-release getter (the core only has `void` permalink calls), release
  asset sources and downloads, evidence beyond `collectEvidence()`, and the `milestoneIds` and `assets` of a release
  (they stay reachable through `createRelease(payload)`) are not covered.
- **Draft notes.** Building a draft-note `position` (the core's payload record) and clearing a position are not covered.
  `createDraftNote(null)` and `publishDraftNotes(null)` need a cast, because each has an overload for the core's
  payload.
- **Reactions.** Snippet and epic-note reactions are not covered.
- **Discussions.** Commit, snippet and epic discussions, editing or deleting a reply through the discussion-note
  endpoints (use the `Note` objects of `notes()`), and building a diff `position` (the core's payload record) are not
  covered. `mergeRequest.startDiscussion(null)` needs a cast, because it overloads the payload variant.
- **Merge requests.** The `changes` endpoint is not offered: GitLab deprecated it in 15.7 in favour of the paged
  `diffs()`. Also not covered: the deprecated
  `approvals_before_merge` update field, `rebase()` returns nothing (core follow-up 2: GitLab answers `{"rebase_in_progress": true}` and
  the core declares no body; follow it with `refresh()` and `isRebaseInProgress()`), the raw diff (the core's call
  discards the body), blocking merge requests, status checks, suggested reviewers, reviewer states, to-do, subscribe and
  unsubscribe, the merge ref, resetting the approvals, and the related issues. Use the core for them.
  `mergeRequest.createApprovalRule(null)` needs a cast, because it overloads the payload variant. The approval rules
  of a project or group are separate objects (see `approvals`).
- **Milestones.** Burndown events (the core's calls return nothing), clearing the dates (`null` cannot be sent), and the
  milestone filters of issues and merge requests are not covered. Group milestones have no `promote()`.
- **Labels.** The `priority` of a label (it cannot be set), a group
  label priority (GitLab has none), and the label counts beyond what the group filter asks for (`withCounts` is a filter
  option; the counts stay in `response()`) are not covered.
- **Members.** Member roles (Ultimate) are not covered (see above).
- **Project import and export, and user-scoped project creation.** `sdk.createProject(name)` creates a project for the
  signed-in user or in a namespace (`namespaceId(...)`); creating one for another user (the core's `createUser`) is not
  covered.
- **Group-level deploy keys.** GitLab has none; a group has deploy tokens only.
- **Webhook extras.** URL variables, custom headers, delivery logs and resending an event are not covered, nor are
  the events outside `WebhookEvent`; `createWebhook(payload)` takes the core's payload for them.
- **Variables.** Instance-level CI/CD variables and the variables inside a pipeline creation payload
  are not covered (pipeline variables are read with `Pipeline.variables()`, and the variables of a pipeline schedule are in the `schedules` package). `maskedAndHidden` is set only
  through `createVariable(payload)`. The scope filter of the group variable operations is not covered: GitLab
  documents it, but the core does not offer it, so group variables with the same key and different environment scopes
  cannot be told apart through the SDK.
- **Environments and deployments.** Protected environments (Premium), deleting review apps, stopping stale environments,
  the auto-stop setting on update (it can be set on create through `createEnvironment(payload)`), and the tag flag of a
  deployment (`createDeployment(payload)` is not offered; use the core) are not covered. Whether GitLab clears a value
  on `clear...()` is unverified live.
- **Schedules and triggers.** The inputs of a schedule (they can be set through `createPipelineSchedule(payload)` but
  not changed with `update()`), the list of the pipelines a schedule started, and the pipeline trigger of
  `POST /projects/{id}/trigger/pipeline` without a ref (`Project.triggerPipeline` uses the ref path). A schedule answer with inputs of any type (string, boolean, number, array)
  decodes; on the request side the core sends a scalar input as a one-item array, which is unverified live, so the SDK
  offers no inputs until that is checked.
- **Runners.** Runner registration and verification, the instance-level runner administration (listing all runners,
  deleting by token, deleting a manager, runner router discovery and runner controllers), runner creation through the
  authenticated user, and getting or deleting a runner through a group are not covered; use the core client. The access
  level of a runner is not offered on `update()`.
- **Packages and the container registry.** The package ecosystems' own endpoints (Maven, npm, NuGet, PyPI, Conan,
  Composer, Debian, Go, Helm, generic, RPM, Terraform and the others), package pipelines, the package and container
  registry protection rules, the registry event endpoint (`createContainerRegistryEventEvent`), the instance-level
  package and registry lists, and a filter on the group registry list (the core has none) are not covered; use the core
  client. `deleteTags(filter)` passes the core's filter record through.
- **Wikis.** Wiki page notes (out of scope), the `front_matter` title on create and update, a page
  `version` and `render_html` on a load, `with_content` on the list (the SDK loads each page instead), and removing an
  uploaded attachment (GitLab has no call for it) are not covered; use the core client for all but the last.
- **Snippets.** The `content` and `file_name` parameters of create and update (deprecated by GitLab in favor of
  `files`; deliberately not offered), the instance-wide and public lists (`listAll`, `listPublic`), repository storage
  moves, user-agent details, and snippet notes, discussions and reactions are not covered; use the core
  client. Binary file content is not offered as bytes: the core reads raw content as text.
- **Dependencies and the APIs around them.** The dependency list exports (the core's call only creates the export and
  discards the body) are not covered; use the core client. The vulnerabilities REST API (list, load, confirm, resolve,
  dismiss, revert), the vulnerability findings API and the epics REST API (with epic issues, links and linked epics)
  are deliberately not offered: the GitLab REST docs mark them deprecated or in the process of being deprecated and
  point to GraphQL or the Work Items API, and the SDK calls no deprecated operation. They stay on the core client.
- **Audit events.** The instance audit events (`GET /audit_events`, an administrator API) are not covered; use the
  core client. Offset paging of project and group audit events is deliberately not offered: the GitLab REST docs
  deprecate it (17.8, removal planned for 19.0), so the SDK walks by keyset with `order_by=id` and `sort=desc`, the
  only keyset order the GitLab REST docs list for audit events; an oldest-first walk is not available.
- **Issues.** The issue statistics (`GET /issues_statistics` and its project and group forms): the core's calls return
  nothing (the OpenAPI description has no response schema), so the SDK offers no `issuesStatistics(filter)` on
  `Project`, `Group` or `GitLabSdk` (core follow-up 1); use the core client when the request itself is enough. Clearing a due date on
  `update()` (the core's payload has no way to send a null; a milestone is cleared with `clearMilestone()`), subscribing to an issue, creating a to-do,
  reordering, cloning, metric images and user-agent details are not covered; use the core client.
- **Repository.** The raw blob (`callBlobsRaw`) and the repository archive (`callArchive`) are not offered, nor is the raw diff of a merge request: the core's
  calls return nothing, so a body cannot be read (core follow-up 3: a streaming variant with `Path` and `OutputStream`
  like `jobsartifacts().get`, and a drift test for the archive format). Creating a changelog (`createChangelog`) is not
  offered: the core's payload has no `version`, which GitLab requires (core follow-up 4: the spec's request body lacks
  it). `RepositoryFile.info()` (file metadata without the content) is not offered (core follow-up 5): the core's `head` and `headBlame`
  return only whether the file exists, not the `X-Gitlab-*` headers. The batch blob, changed paths, diverging
  commits, diff stats and repository health reads, the `unidiff`, `from_project_id` and `with_last_commit` options, and
  ordering contributors are not covered; use the core client.
- **Project and group settings.** Not offered: uploading an avatar (it goes through the multipart update of a project or
  a group, which the SDK does not wrap; use the core), the list and the deletion of the uploads of a project (`markdownuploads().list` and `delete`), uploads of a group
  (`groupmarkdownuploads`), custom attributes (administrator API, deferred to v3),
  `Group.transferProject` (`POST /groups/{id}/projects/{project_id}`: GitLab's API description says administrators only,
  so it belongs to the deferred admin API; `Project.transfer(namespace)` covers the other direction), starting a pull
  mirror (`POST /projects/{id}/mirror/pull`; its payload is the pull-request event of GitHub), the public key of a remote
  mirror (the core's `callPublicKey` returns nothing: core follow-up 13), the instance-level and personal access tokens,
  the group-level approval settings, `delete()` and `refresh()` of a group approval rule (core follow-up 12), and
  clearing a field of a push rule, a badge or a mirror (the core's payloads cannot send a null). `deleteByCn` and
  `deleteByProviderAndCn` of the LDAP links are deprecated in GitLab's API description and are not used: the SDK deletes
  through the call that takes the provider and the CN or the filter, so `deleteLdapLink(provider, cn)` takes the provider
  (GitLab requires it). Whether the core decodes `protected_branches` of a group approval rule is open: the installed core
  jar has a single object where GitLab's API description has a list (core follow-up 14). None of the settings is verified
  live: the smoke test only reads, and the writes are unit-tested with mocks.
- **Live verification.** No area has been run against a real GitLab instance yet; see
  [Live smoke tests](smoke-tests.md).

## Deliberately not offered: deprecated by GitLab

The SDK offers no deprecated GitLab operation or parameter. Each item below is marked deprecated, or in the process of
being deprecated, by the GitLab REST documentation (checked when the SDK was written; GitLab may change its pages). They
stay available on the core client, which is described by the same OpenAPI description.
A row that cites the core's OpenAPI description instead of a docs.gitlab.com page was taken from that description and was
not re-read on docs.gitlab.com.

| Item | Why it is left out | GitLab docs |
|---|---|---|
| Epics, with epic issues, epic links and linked epics | The page says that the REST API is deprecated in GitLab 17.0 and planned for removal in API v5; the replacement is the Work Items API (GraphQL) | <https://docs.gitlab.com/api/epics/> |
| Vulnerabilities: list, load, confirm, resolve, dismiss, revert | The pages say that the API is in the process of being deprecated and considered unstable, and point to the GraphQL API | <https://docs.gitlab.com/api/project_vulnerabilities/>, <https://docs.gitlab.com/api/vulnerabilities/> |
| Vulnerability findings | The same notice as for vulnerabilities; the core's record also types about 15 properties as `String` | <https://docs.gitlab.com/api/vulnerability_findings/> |
| The snippet `content` and `file_name` parameters of create and update | Marked "Deprecated: Use `files` instead"; the SDK sends the `files` array only | <https://docs.gitlab.com/api/snippets/>, <https://docs.gitlab.com/api/project_snippets/> |
| Offset paging of audit events | Deprecated in GitLab 17.8, removal planned for 19.0; the SDK walks by keyset (`order_by=id`, `sort=desc`), so events come newest first | <https://docs.gitlab.com/api/audit_events/> |
| The `users` and `groups` fields of project approval rules | The core's description says "Deprecated. Use `user_ids`" and "Use `group_ids`"; the SDK sends the id and user name fields only | the core's OpenAPI description (not re-read on docs.gitlab.com) |
| `approvals_before_merge` and `require_password_to_approve` of the project approval configuration | Deprecated in GitLab 12.3 and 16.9 per the core's description and the page the Phase E notes record; the configuration itself is not deprecated, so `ApprovalConfiguration` is offered without them | <https://docs.gitlab.com/api/merge_request_approvals/> |
| `DELETE /groups/{id}/ldap_group_links/{cn}` and `.../{provider}/{cn}` | The core's description says "Deprecated. Scheduled for removal in a future release"; the SDK uses the query form with the provider and the CN or the filter | the core's OpenAPI description (not re-read on docs.gitlab.com) |

Not deprecated, checked against the same docs: project and group wikis, the snippet endpoints, the project dependency
list, and project and group audit events with keyset paging.

## Out of scope

These are not deprecated; the SDK leaves them out as out of scope, and the core client serves them: dependency list
exports (the core's call only creates the export and discards the body), the instance audit events (an administrator
API), snippet repository storage moves, user-agent details and the instance-wide and public snippet lists (administrator
or instance-wide), wiki page notes, and snippet notes, discussions and reactions.

## Parity with gitlab4j-api

The comparison with gitlab4j-api, class by class, with the reason for each operation the SDK does not offer and the
numbered list of core follow-ups, is on its own page: [Parity with gitlab4j-api](gitlab4j-parity.md).
