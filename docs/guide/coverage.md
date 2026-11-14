# What is covered

Every object of the SDK, with the methods it has, by package. The [README](../../README.md) gives the short overview.
Every object also has `response()`, which returns the complete `gitlab-rest-client` record it is built from, and
navigates to its parent (`issue.project()`, `note.issue()`, `job.project()` and so on), except a `Reaction`, a
`Runner`, a `PackageFile` and a `RegistrationToken`, which have none. A lookup returns an `Optional`, a list is a
lazy, single-use `Stream`, and the `iid` of an issue or a merge request is a `long`.

## Root package: `io.github.piotr_janczukowicz.gitlab.sdk`

| Entry point | Objects and what they do |
|---|---|
| `GitLabSdk.of(...)` | Wraps a `GitLab`, a `GitLabCaller` or a `GitLabClient`. `project(id)` and `group(id)` give handles, `createProject(name)` and `createGroup(name, path)` start a `ProjectDraft` and a `GroupDraft`, `user(id)` loads a user, `runner(id)` loads a runner, `me()` gives the signed-in account |
| `Project` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `update()`, `archive()`, `unarchive()`, `delete()`, `response()`. Transfer, sharing and forks: `transfer(namespace)` (the project is addressed by its numeric id afterwards), `fork(namespacePath)` (GitLab creates the fork asynchronously, so it may still be importing), `forks()` (each fork is loaded as the stream reaches it), `removeForkRelationship()`, `share(groupId, level)`, `unshare(groupId)`, `sharedGroups()` (the core's `GitLabPublicGroupDetails` records). Members: `members()`, `allMembers()` (inherited too), `member(userId)`, `addMember(userId, level)`, `addMember(username, level)`, `requestAccess()`, `accessRequests()`. Webhooks: `webhooks()`, `webhook(hookId)`, `createWebhook(url)`, `createWebhook(payload)` (two overloads: a literal `null` needs a cast). Protection: `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)`, `protectBranch(name, push, merge)`, `protectedTags()`, `protectedTag(name)`, `protectTag(name, create)`. Issues: `issues()`, `issues(filter)`, `issue(iid)`, `createIssue(title)`, `createIssue(payload)`. Labels: `labels()`, `labels(filter)` (the core's `GitLabProjectLabelsListFilter`), `label(name)` (a slash or space reaches the core unchanged), `createLabel(name, color)`. Merge requests: `mergeRequests()`, `mergeRequests(filter)`, `mergeRequest(iid)`, `createMergeRequest(payload)`. Pipelines and jobs: `pipelines()`, `pipelines(filter)`, `pipeline(id)`, `createPipeline(ref)`, `createPipeline(payload)`, `jobs()`, `jobs(filter)`, `job(id)`, `downloadArtifacts(ref, jobName, target)` (a `Path`, written atomically and never partial) and `downloadArtifacts(ref, jobName, out)` (an `OutputStream`, never closed by the SDK; the latest successful job of that name on the ref; a ref with a slash reaches the core unchanged). Code: `file(path, ref)`, `createFile(path, branch, content, message)` (content as text or `byte[]`), `branches()`, `branch(name)`, `createBranch(name, ref)`, `commits()`, `commit(sha)`, `tags()`, `tags(filter)`, `tag(name)`, `createTag(name, ref)` (lightweight), `createTag(name, ref, message)` (annotated; a `null` message makes it lightweight). Releases: `releases()`, `releases(filter)`, `release(tagName)`, `createRelease(tagName)` (existing tag), `createRelease(tagName, ref)` (GitLab creates the tag from the ref), `createRelease(payload)` (overloaded, so a literal `null` needs a cast). Milestones: `milestones()`, `milestones(filter)` (the core's `GitLabProjectProjectMilestonesListFilter`), `milestone(id)`, `createMilestone(title)`, `createMilestone(payload)` (overloaded, so a literal `null` needs a cast). CI/CD variables: `variables()`, `variable(key)`, `variable(key, environmentScope)` (narrows the lookup with `filter[environment_scope]`), `createVariable(key, value)`, `createVariable(payload)` (overloaded, so a literal `null` needs a cast). Environments: `environments()`, `environments(filter)` (the core's `GitLabProjectEnvironmentsListFilter`), `environment(id)`, `createEnvironment(name)`, `createEnvironment(payload)` (overloaded, so a literal `null` needs a cast). Deployments: `deployments()`, `deployments(filter)` (the core's `GitLabProjectDeploymentsListFilter`; each listed deployment is loaded again, gone ones are skipped), `deployment(id)`, `createDeployment(environment, sha, ref, status)` (only `RUNNING`, `SUCCESS`, `FAILED` and `CANCELED` are accepted). Pipeline schedules: `pipelineSchedules()` (each listed schedule is loaded again, gone ones are skipped), `pipelineSchedule(id)`, `createPipelineSchedule(description, ref, cron)`, `createPipelineSchedule(payload)` (overloaded, so a literal `null` needs a cast). Pipeline triggers: `pipelineTriggers()`, `pipelineTrigger(id)`, `createPipelineTrigger(description)`, `createPipelineTrigger(description, expiresAt)`. Runners: `runners()`, `runners(filter)` (the core's `GitLabProjectRunnersListFilter`; each listed runner is loaded again, gone ones are skipped), `enableRunner(runnerId)` (enables an existing runner in the project, then loads it), `resetRunnerRegistrationToken()` (secret). Packages: `packages()`, `packages(filter)` (the core's `GitLabProjectPackagesListFilter`), `projectPackage(id)`. Registry: `registryRepositories()`, `registryRepositories(withTags)`. |
| `Group` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `update()`, `archive()`, `unarchive()`, `delete()`, `transfer(newParentGroupId)` (the group is addressed by its numeric id afterwards), `share(groupId, level)` (returns this group as GitLab answers), `unshare(groupId)`, members as on `Project` (`members()`, `allMembers()`, `member(userId)`, `addMember(...)`, `requestAccess()`, `accessRequests()`), webhooks (Premium): `webhooks()`, `webhook(hookId)`, `createWebhook(url)`, `createWebhook(payload)`, protected branches (Premium): `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)`, `protectBranch(name, push, merge)`, milestones: `milestones()`, `milestones(filter)` (the core's `GitLabGroupGroupMilestonesListFilter`, which adds `includeDescendants`), `milestone(id)`, `createMilestone(title)`, `createMilestone(payload)` (overloaded, so a literal `null` needs a cast), group labels: `labels()`, `labels(filter)` (the core's `GitLabGroupGroupLabelsListFilter`), `label(name)`, `createLabel(name, color)`, CI/CD variables: `variables()`, `variable(key)`, `createVariable(key, value)`, `createVariable(payload)`, `projects()` (each project is loaded as the stream reaches it), `subgroups()` (the direct subgroups, one level down; each group is loaded as the stream reaches it), `response()`. Runners: `runners()`, `runners(filter)` (the core's `GitLabGroupGroupsListRunnersFilter`; each listed runner is loaded again, gone ones are skipped), `resetRunnerRegistrationToken()` (secret). Packages: `packages()`, `packages(filter)` (the core's `GitLabGroupGroupsListPackagesFilter`, which adds `excludeSubgroups`). Registry: `registryRepositories()` (the core offers no filter for the group list). |
| `ProjectGroupLink` | The sharing of a project with a group, returned by `Project.share(groupId, level)`: `project()`, `groupId()`, `expiresAt()`, `response()`. It has no `refresh()` |
| `Visibility` | `PRIVATE`, `INTERNAL`, `PUBLIC` |
| `ProjectDraft` | From `sdk.createProject(name)`: `path(...)`, `namespaceId(...)`, `description(...)`, `visibility(...)`, `defaultBranch(...)`, `initializeWithReadme(...)`, `send()` (creates, then loads the project again) |
| `ProjectUpdate` | `name(...)`, `path(...)`, `description(...)`, `clearDescription()` (sends an empty description), `visibility(...)`, `defaultBranch(...)`, `send()`; `send()` with nothing set throws `IllegalStateException`. Archiving stays with `archive()` |
| `GroupDraft` | From `sdk.createGroup(name, path)`: `description(...)`, `visibility(...)`, `parentId(...)`, `send()` |
| `GroupUpdate` | `name(...)`, `path(...)`, `description(...)`, `visibility(...)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `User` | Loaded. `id()`, `username()`, `refresh()`, `response()` |
| `Me` | A handle. `profile()` loads the signed-in account as a `User` |

## `issues`

| Object | Methods |
|---|---|
| `Issue` | `project()`, `iid()`, `title()`, `description()`, `state()` (`IssueState`: `OPENED`, `CLOSED`), `labels()`, `update()`, `close()`, `reopen()`, `comment(body)`, `notes()`, `discussions()`, `discussion(id)`, `startDiscussion(body)`, `reactions()`, `react(name)`, `delete()`, `refresh()`, `response()` |
| `IssueUpdate` | `title(...)`, `description(...)`, `clearDescription()` (sends an empty description), `labels(names)`, `assignees(userIds)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `Note` | `issue()`, `id()`, `body()`, `update(body)`, `reactions()`, `react(name)`, `delete()`, `refresh()`, `response()` |
| `Label` | `project()`, `name()`, `color()`, `description()`, `update()`, `promote()` (returns the `GroupLabel`; sends `GET /projects/{id}` to find the group from the project's namespace, then the promotion, so a failed GET promotes nothing; a project in a user namespace is not rejected by the SDK: GitLab's rejection passes through as the core's exception), `delete()`, `refresh()`, `response()` |
| `LabelUpdate` | `name(...)`, `color(...)`, `description(...)`, `send()` |
| `GroupLabel` | `group()`, `name()`, `color()`, `description()`, `isSubscribed()`, `update()`, `subscribe()`, `unsubscribe()`, `delete()`, `refresh()`, `response()`; addressed by name (a slash or space reaches the core unchanged) |
| `GroupLabelUpdate` | `name(...)` (the new name), `color(...)`, `description(...)`, `archived(boolean)`, `send()`; `send()` with nothing set throws `IllegalStateException` |

## `mergerequests`

| Object | Methods |
|---|---|
| `MergeRequest` | `project()`, `iid()`, `title()`, `description()`, `state()` (`MergeRequestState`), `targetBranch()`, `sourceBranch()`, `labels()`, `update()`, `close()`, `reopen()`, `merge()`, `merge(options)`, `approve()`, `approve(options)`, `unapprove()`, `approvals()`, `comment(body)`, `notes()`, `discussions()`, `discussion(id)`, `startDiscussion(body)`, `startDiscussion(payload)` (a diff comment: the core's payload carries the `position`; overloaded, so a literal `null` needs a cast), `draftNotes()` (one request, a stream over the core's list), `draftNote(id)`, `createDraftNote(note)`, `createDraftNote(payload)`, `publishDraftNotes()` (an empty bulk-publish payload), `publishDraftNotes(payload)` (both pairs are overloaded, so a literal `null` needs a cast), `reactions()`, `react(name)`, `pipelines()` (each pipeline is loaded from the project it ran in, a fork included), `commits()`, `delete()`, `refresh()`, `response()` |
| `MergeRequestUpdate` | `title(...)`, `description(...)`, `targetBranch(...)`, `labels(names)`, `send()` |
| `MergeRequestNote` | `mergeRequest()`, `id()`, `body()`, `update(body)`, `reactions()`, `react(name)`, `delete()`, `refresh()`, `response()` |
| `DraftNote` | `mergeRequest()`, `id()`, `note()`, `isResolvingDiscussion()` (a missing flag is `false`), `update()`, `publish()`, `delete()`, `refresh()`, `response()`. After `publish()` it is no longer a draft: `refresh()` throws `IllegalStateException` |
| `DraftNoteUpdate` | `note(text)`, `send()` |
| `Approvals` | `mergeRequest()`, `approved()`, `approvalsLeft()`, `approvalsRequired()`, `userHasApproved()`, `response()` |

## `ci`

| Object | Methods |
|---|---|
| `Pipeline` | `project()`, `id()`, `status()` (`PipelineStatus`), `statusName()` (the raw value), `ref()`, `sha()`, `webUrl()`, `retry()`, `cancel()`, `jobs()`, `delete()`, `refresh()`, `response()` |
| `Job` | `project()`, `id()`, `name()`, `status()` (`JobStatus`), `statusName()` (the raw value), `stage()`, `pipeline()`, `retry()`, `cancel()`, `play()`, `erase()`, `downloadArtifacts(target)` (a `Path`) and `downloadArtifacts(out)` (an `OutputStream`), `artifactTree()` and `artifactTree(path, recursive)` (streams of the core's `GitLabCiJobArtifactEntry` records: the first page is fetched when the method is called, the later ones as the stream is consumed), `keepArtifacts()` (a new `Job` from the answer), `deleteArtifacts()`, `refresh()`, `response()` |

## `code`

| Object | Methods |
|---|---|
| `RepositoryFile` | `project()`, `path()`, `name()`, `ref()`, `bytes()` (exact bytes, also of a binary file), `text()` (strict UTF-8), `update(content, message)` (content as text or `byte[]`), `delete(message)`, `response()`. Text is sent as UTF-8; `byte[]` is sent Base64-encoded with `encoding=base64` |
| `Branch` | `project()`, `name()`, `isProtected()`, `isDefault()`, `protect()` (GitLab's default rules), `unprotect()`, `delete()`, `refresh()`, `response()` |
| `Tag` | `project()`, `name()` (may contain slashes), `message()`, `target()`, `isProtected()` (a missing flag is `false`), `createdAt()`, `commit()` (loaded through `project.commit(sha)`, one extra GET; empty when the record has no commit), `signature()` (the core's `GitLabTagSignature`), `release()` (the release with the same tag name, one extra GET; empty on 404), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()`. Listed and loaded tags share one record, so a listed tag needs no extra request |
| `Commit` | `project()`, `sha()`, `title()`, `message()`, `diff()` (the core's `GitLabDiff` records), `cherryPick(branch)`, `revert(branch)`, `response()` |

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
has no `issue()` or `mergeRequest()`. `refresh()` and `delete()` use the owner the reaction was made on. A blank name makes no
request; reacting twice with the same emoji passes GitLab's error through.

## `milestones`

| Object | Methods |
|---|---|
| `Milestone`, `GroupMilestone` | `project()` or `group()`, `id()`, `iid()`, `title()`, `description()`, `state()` (`MilestoneState`: `ACTIVE`, `CLOSED`; an unknown value throws `IllegalArgumentException`), `stateName()` (the raw value), `dueDate()` and `startDate()` (`Optional<LocalDate>`, empty when absent or blank; a value that is not an ISO date throws `IllegalStateException` naming it), `isExpired()` (a missing flag is `false`), `update()` (`MilestoneUpdate` or `GroupMilestoneUpdate`: `title`, `description`, `dueDate(LocalDate)`, `startDate(LocalDate)`, `send()`), `close()`, `reopen()` (state events `close` and `activate`), `delete()`, `issues()`, `mergeRequests()`, `refresh()` (`IllegalStateException` when gone), `response()`; `Milestone` also has `promote()` (to a group milestone, returns nothing). Listed, loaded, created and updated milestones share one record, so none of them needs an extra request |

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
| `Variable` | `project()`, `key()`, `value()` (`Optional`: secret, do not log it; a hidden variable has none), `type()` (an unknown type throws `IllegalArgumentException`), `typeName()` (the raw name, never fails), `isProtected()`, `isMasked()`, `isRaw()`, `isHidden()` (a missing flag is `false`), `environmentScope()` (`Optional`: the SDK never assumes `*`), `description()` (`Optional`), `update()`, `delete()` (void), `refresh()` (`IllegalStateException` when gone), `response()` (its `toString()` is redacted by the core). `refresh()`, `update()` and `delete()` target the variable by key and its own environment scope (`filter[environment_scope]`); a variable with no scope in the answer is targeted by key alone. Listed and loaded variables share one record, so no list item is loaded again |
| `VariableUpdate` | `value(...)`, `protectedValue(boolean)`, `masked(boolean)`, `raw(boolean)`, `type(VariableType)`, `environmentScope(...)` (the NEW scope; the variable is still targeted by its old one), `description(...)`, `send()`; `send()` with nothing set throws `IllegalStateException`. The answer of the update is the new object, no extra request |
| `GroupVariable`, `GroupVariableUpdate` | The same members as `Variable` and `VariableUpdate` with `group()`. The core's group API has no environment-scope targeting, so `refresh()`, `update()` and `delete()` address the variable by key alone: same-key group variables of different scopes cannot be told apart |

Neither `Variable` nor `GroupVariable` nor an update builder defines a `toString()` that prints a value. `maskedAndHidden`
is reachable only through `createVariable(payload)`; the update builders have no such flag.

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
| `PipelineTrigger` | `project()`, `id()`, `description()`, `token()` (secret, do not log it), `lastUsed()`, `expiresAt()`, `owner()`, `update()`, `delete()`, `refresh()`, `response()` |
| `PipelineTriggerUpdate` | `description(...)`, `send()` |

A schedule from a list is loaded again, because the list holds a smaller record than a single load. GitLab has no
call that lists the variables of a schedule, so they come from the schedule's own response. Firing a trigger and
take-ownership of a trigger are not covered: the first needs the core client, the second does not exist.

## `runners`

| Object | Methods |
|---|---|
| `Runner` | `id()`, `description()`, `name()`, `type()` (an `Optional<RunnerType>`; an unknown type throws `IllegalArgumentException`), `typeName()` (the raw name), `isOnline()`, `isPaused()`, `isActive()` (absent: `false`), `status()` (raw), `tagList()` (the response's tag list, an immutable list; empty when absent), `version()`, `contactedAt()`, `update()`, `refresh()`, `delete()` (removes the runner everywhere), `disable(project)` (only valid for a runner enabled in that project), `managers()` (the core's `GitLabCiRunnerManager` records, an immutable list), `jobs()` (the core's `GitLabCiJobBasicWithProject` records, lazy), `resetAuthenticationToken()` (secret), `response()` (the detail record) |
| `RunnerUpdate` | `description(...)`, `paused(boolean)`, `active(boolean)`, `tags(List)`, `runUntagged(boolean)`, `locked(boolean)`, `maximumTimeout(int)`, `maintenanceNote(...)`, `send()` (an empty update throws `IllegalStateException` before a request). The access level is not offered |
| `RunnerType` | `INSTANCE_TYPE`, `GROUP_TYPE`, `PROJECT_TYPE` |
| `RegistrationToken` | `token()` (secret, do not log it), `expiresAt()`, `response()`; `toString()` prints the token as `<redacted>` |

`GitLabSdk.runner(id)` loads a runner by its global id (`GET /runners/{id}`). A runner from a project or group list is
loaded again, because the list holds a smaller record: one request per item. `Runner` has no parent navigation, like
`Reaction`: one class serves instance, group and project runners, and a runner can be enabled in several projects.
`update()`, `delete()`, `managers()`, `jobs()` and `resetAuthenticationToken()` always use the global runner id.

## `packages`

| Object | Methods |
|---|---|
| `ProjectPackage` | `id()`, `name()`, `version()`, `type()` (a `PackageType`; an unknown type throws `IllegalArgumentException`), `typeName()` (the raw name), `status()` (raw), `createdAt()`, `lastDownloadedAt()`, `project()` (the owning project without a request for a package of a project list; one `GET /projects/{id}` for a package of a group list), `refresh()`, `files()` (lazy), `delete()`, `response()`. A response without a project id makes `project()`, `refresh()`, `files()` and `delete()` throw `IllegalStateException` |
| `PackageFile` | `id()`, `fileName()`, `size()`, `sha256()`, `download(Path)` (written atomically, never partial), `download(OutputStream)` (never closed by the SDK; may hold a partial body on failure), `delete()`, `response()`. `download` and `delete` throw `IllegalStateException` when the package response names no project id; `download(Path)` throws `IllegalArgumentException` for a target without a parent directory and `FileAlreadyExistsException` for an existing directory |
| `PackageType` | `MAVEN`, `NPM`, `CONAN`, `NUGET`, `PYPI`, `COMPOSER`, `GENERIC`, `GOLANG`, `DEBIAN`, `RUBYGEMS`, `HELM`, `TERRAFORM_MODULE`, `RPM`, `ML_MODEL`, `CARGO`, `SWIFT` |

GitLab lists and loads a package with the same record, so nothing is loaded again. The object is `ProjectPackage` and not
`Package`, which would shadow `java.lang.Package`. The download calls use the client's own authentication and no download
mode. Package files can contain secrets: the SDK writes only where the caller says.

## `registry`

| Object | Methods |
|---|---|
| `RegistryRepository` | `id()`, `name()`, `path()`, `location()`, `tagsCount()`, `project()` (as for a package), `refresh()` (by the repository id, `GET /registry/repositories/{id}`), `tags()` (lazy; each listed tag is loaded again, gone ones are skipped), `tag(name)`, `delete()`, `deleteTags(filter)` (bulk delete by regular expression with the core's `GitLabProjectContainerRegistryDeleteTagFilter`; passed through unchanged, `null` throws `NullPointerException`), `response()`. A response without a project id makes `project()`, `tags()`, `tag(name)`, `delete()` and `deleteTags(filter)` throw `IllegalStateException`; `refresh()` still works |
| `RegistryTag` | `name()`, `location()`, `digest()`, `totalSize()`, `repository()`, `refresh()`, `delete()` (`IllegalStateException` when the repository response names no project id), `response()` (the details record) |

The tag list holds a smaller record than a single tag, so every tag is loaded through the details request: one request per
tag. A tag name is passed to the core unchanged; a blank name throws before a request. Images can contain secrets; the SDK
reads tag metadata only.

## What is not covered

- **Single artifact files and job tokens.** Reading one file out of the artifacts (the core's `call...` methods
  discard the body), a job-token parameter on the artifact calls (the client's own authentication is used), and the
  artifact upload and authorization of the runner protocol are not covered; use the core.
- **Fields of a project or group outside the typed set.** `ProjectDraft`, `ProjectUpdate`, `GroupDraft` and
  `GroupUpdate` take the common fields only (see above); anything else needs the core's `MultipartFormData` API.
- **Options of `Branch.protect()`.** It uses GitLab's default rules; use `project.protectBranch(name, push, merge)` or
  the core for the options.
- **Filter on the jobs of one pipeline.** `pipeline.jobs()` lists without a filter. Pipeline and job logs and pipeline
  variables are not covered.
- **User-, group-, deploy-key- and member-role-based protection rules.** They are skipped in the access lists and stay
  available from `response()`; the `allowed_to_push`, `allowed_to_merge`, `allowed_to_unprotect` and `allowed_to_create`
  rules and the unprotect level cannot be set; use the core.
- **Everything outside the areas above**, such as group releases, snippets, member roles (Ultimate), the premium-only
  member endpoints (approving pending members, billable members), access tokens,
  instance-level deploy keys and tokens, protected environments, the descendant groups of a group at every level, and the
  administration of an instance. Use `gitlab-rest-client` for them; see [When the SDK does not cover a
  call](../../README.md#when-the-sdk-does-not-cover-a-call).
- **Releases.** Group releases, the typed latest-release getter (the core only has `void` permalink calls), release
  asset sources and downloads, evidence beyond `collectEvidence()`, and the `milestoneIds` and `assets` of a release (they stay reachable through `createRelease(payload)`) are not covered.
- **Draft notes.** Building a draft-note `position` (the core's payload record) and clearing a position are not covered.
  `createDraftNote(null)` and `publishDraftNotes(null)` need a cast, because each has an overload for the core's
  payload.
- **Reactions.** Snippet and epic-note reactions are not covered.
- **Discussions.** Commit, snippet and epic discussions, editing or deleting a reply through the discussion-note
  endpoints (use the `Note` objects of `notes()`), and building a diff `position` (the core's payload record) are not
  covered. `mergeRequest.startDiscussion(null)` needs a cast, because it overloads the payload variant.
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
- **Variables.** Instance-level CI/CD variables, pipeline variables and the variables inside a pipeline creation payload are not covered (the variables of a pipeline schedule are, in the `schedules` package). `maskedAndHidden` is set only through `createVariable(payload)`. Group variables with the same key and different environment scopes cannot be told apart through the core's group API.
- **Environments and deployments.** Protected environments (Premium), deleting review apps, stopping stale environments, the auto-stop setting on update (it can be set on create through `createEnvironment(payload)`), and the tag flag of a deployment (`createDeployment(payload)` is not offered; use the core) are not covered. Whether GitLab clears a value on `clear...()` is unverified live.
- **Schedules and triggers.** The inputs of a schedule (they can be set through `createPipelineSchedule(payload)` but
  not changed with `update()`), the list of the pipelines a schedule started, and firing a trigger
  (`POST /projects/{id}/trigger/pipeline`). A schedule answer with inputs of any type (string, boolean, number, array)
  decodes; on the request side the core sends a scalar input as a one-item array, which is unverified live, so the SDK
  offers no inputs until that is checked.
- **Runners.** Runner registration and verification, the instance-level runner administration (listing all runners, deleting by token, deleting a manager, runner router discovery and runner controllers), runner creation through the authenticated user, and getting or deleting a runner through a group are not covered; use the core client. The access level of a runner is not offered on `update()`.
- **Packages and the container registry.** The package ecosystems' own endpoints (Maven, npm, NuGet, PyPI, Conan, Composer, Debian, Go, Helm, generic, RPM, Terraform and the others), package pipelines, the package and container registry protection rules, the registry event endpoint (`createContainerRegistryEventEvent`), the instance-level package and registry lists, and a filter on the group registry list (the core has none) are not covered; use the core client. `deleteTags(filter)` passes the core's filter record through.
- **Live verification.** No area has been run against a real GitLab instance yet; see
  [Live smoke tests](smoke-tests.md).
