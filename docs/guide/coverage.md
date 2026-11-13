# What is covered

Every object of the SDK, with the methods it has, by package. The [README](../../README.md) gives the short overview.
Every object also has `response()`, which returns the complete `gitlab-rest-client` record it is built from, and navigates
to its parent (`issue.project()`, `note.issue()`, `job.project()` and so on). A lookup returns an `Optional`, a list is a
lazy, single-use `Stream`, and the `iid` of an issue or a merge request is a `long`.

## Root package: `io.github.piotr_janczukowicz.gitlab.sdk`

| Entry point | Objects and what they do |
|---|---|
| `GitLabSdk.of(...)` | Wraps a `GitLab`, a `GitLabCaller` or a `GitLabClient`. `project(id)` and `group(id)` give handles, `createProject(name)` and `createGroup(name, path)` start a `ProjectDraft` and a `GroupDraft`, `user(id)` loads a user, `me()` gives the signed-in account |
| `Project` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `update()`, `archive()`, `unarchive()`, `delete()`, `response()`. Transfer, sharing and forks: `transfer(namespace)` (the project is addressed by its numeric id afterwards), `fork(namespacePath)` (GitLab creates the fork asynchronously, so it may still be importing), `forks()` (each fork is loaded as the stream reaches it), `removeForkRelationship()`, `share(groupId, level)`, `unshare(groupId)`, `sharedGroups()` (the core's `GitLabPublicGroupDetails` records). Members: `members()`, `allMembers()` (inherited too), `member(userId)`, `addMember(userId, level)`, `addMember(username, level)`, `requestAccess()`, `accessRequests()`. Webhooks: `webhooks()`, `webhook(hookId)`, `createWebhook(url)`, `createWebhook(payload)` (two overloads: a literal `null` needs a cast). Protection: `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)`, `protectBranch(name, push, merge)`, `protectedTags()`, `protectedTag(name)`, `protectTag(name, create)`. Issues: `issues()`, `issues(filter)`, `issue(iid)`, `createIssue(title)`, `createIssue(payload)`. Labels: `labels()`, `createLabel(name, color)`. Merge requests: `mergeRequests()`, `mergeRequests(filter)`, `mergeRequest(iid)`, `createMergeRequest(payload)`. Pipelines and jobs: `pipelines()`, `pipelines(filter)`, `pipeline(id)`, `createPipeline(ref)`, `createPipeline(payload)`, `jobs()`, `jobs(filter)`, `job(id)`. Code: `file(path, ref)`, `createFile(path, branch, content, message)` (content as text or `byte[]`), `branches()`, `branch(name)`, `createBranch(name, ref)`, `commits()`, `commit(sha)`, `tags()`, `tags(filter)`, `tag(name)`, `createTag(name, ref)`, `createTag(name, ref, message)`. Releases: `releases()`, `releases(filter)`, `release(tagName)`, `createRelease(tagName)` (existing tag), `createRelease(tagName, ref)` (GitLab creates the tag from the ref), `createRelease(payload)` (overloaded, so a literal `null` needs a cast) |
| `Group` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `update()`, `archive()`, `unarchive()`, `delete()`, `transfer(newParentGroupId)` (the group is addressed by its numeric id afterwards), `share(groupId, level)` (returns this group as GitLab answers), `unshare(groupId)`, members as on `Project` (`members()`, `allMembers()`, `member(userId)`, `addMember(...)`, `requestAccess()`, `accessRequests()`), webhooks (Premium): `webhooks()`, `webhook(hookId)`, `createWebhook(url)`, `createWebhook(payload)`, protected branches (Premium): `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)`, `protectBranch(name, push, merge)`, `projects()` (each project is loaded as the stream reaches it), `subgroups()` (the direct subgroups, one level down; each group is loaded as the stream reaches it), `response()` |
| `ProjectGroupLink` | The sharing of a project with a group, returned by `Project.share(groupId, level)`: `project()`, `groupId()`, `expiresAt()`, `response()`. It has no `refresh()` |
| `Visibility` | `PRIVATE`, `INTERNAL`, `PUBLIC` |
| `ProjectDraft` | From `sdk.createProject(name)`: `path(...)`, `namespaceId(...)`, `description(...)`, `visibility(...)`, `defaultBranch(...)`, `initializeWithReadme(...)`, `send()` (creates, then loads the project again) |
| `ProjectUpdate` | `name(...)`, `path(...)`, `description(...)`, `clearDescription()`, `visibility(...)`, `defaultBranch(...)`, `send()`; `send()` with nothing set throws `IllegalStateException`. Archiving stays with `archive()` |
| `GroupDraft` | From `sdk.createGroup(name, path)`: `description(...)`, `visibility(...)`, `parentId(...)`, `send()` |
| `GroupUpdate` | `name(...)`, `path(...)`, `description(...)`, `visibility(...)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `User` | Loaded. `id()`, `username()`, `refresh()`, `response()` |
| `Me` | A handle. `profile()` loads the signed-in account as a `User` |

## `issues`

| Object | Methods |
|---|---|
| `Issue` | `project()`, `iid()`, `title()`, `description()`, `state()` (`IssueState`: `OPENED`, `CLOSED`), `labels()`, `update()`, `close()`, `reopen()`, `comment(body)`, `notes()`, `delete()`, `refresh()`, `response()` |
| `IssueUpdate` | `title(...)`, `description(...)`, `clearDescription()`, `labels(names)`, `assignees(userIds)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `Note` | `issue()`, `id()`, `body()`, `update(body)`, `delete()`, `refresh()`, `response()` |
| `Label` | `project()`, `name()`, `color()`, `description()`, `update()`, `delete()`, `refresh()`, `response()` |
| `LabelUpdate` | `name(...)`, `color(...)`, `description(...)`, `send()` |

## `mergerequests`

| Object | Methods |
|---|---|
| `MergeRequest` | `project()`, `iid()`, `title()`, `description()`, `state()` (`MergeRequestState`), `targetBranch()`, `sourceBranch()`, `labels()`, `update()`, `close()`, `reopen()`, `merge()`, `merge(options)`, `approve()`, `approve(options)`, `unapprove()`, `approvals()`, `comment(body)`, `notes()`, `pipelines()` (each pipeline is loaded from the project it ran in, a fork included), `commits()`, `delete()`, `refresh()`, `response()` |
| `MergeRequestUpdate` | `title(...)`, `description(...)`, `targetBranch(...)`, `labels(names)`, `send()` |
| `MergeRequestNote` | `mergeRequest()`, `id()`, `body()`, `update(body)`, `delete()`, `refresh()`, `response()` |
| `Approvals` | `mergeRequest()`, `approved()`, `approvalsLeft()`, `approvalsRequired()`, `userHasApproved()`, `response()` |

## `ci`

| Object | Methods |
|---|---|
| `Pipeline` | `project()`, `id()`, `status()` (`PipelineStatus`), `statusName()` (the raw value), `ref()`, `sha()`, `webUrl()`, `retry()`, `cancel()`, `jobs()`, `delete()`, `refresh()`, `response()` |
| `Job` | `project()`, `id()`, `name()`, `status()` (`JobStatus`), `statusName()` (the raw value), `stage()`, `pipeline()`, `retry()`, `cancel()`, `play()`, `erase()`, `refresh()`, `response()` |

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
| `ReleaseLink` | `release()`, `id()`, `name()`, `url()`, `directAssetUrl()`, `type()` (`ReleaseLinkType`: `OTHER`, `RUNBOOK`, `PACKAGE`, `IMAGE`; an unknown value throws `IllegalArgumentException`; a missing one is `OTHER`, GitLab's default), `typeName()` (the raw value), `update()` (`ReleaseLinkUpdate`: `name`, `url`, `type(ReleaseLinkType)`, `send()`), `delete()`, `refresh()` (`IllegalStateException` when gone), `response()` |

An empty `update().send()` of a release or a link throws `IllegalStateException` and makes no request. Fields cannot be
cleared through `ReleaseUpdate`; `milestones(List.of())` is sent as an empty list, and that GitLab then removes the
milestones is not confirmed by the core's documentation.

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
`createDeployToken(name, scopes...)`; `Group` adds `deployTokens()`, `deployToken(tokenId)` and
`createDeployToken(name, scopes...)`. Creating a token with no scope throws `IllegalArgumentException` and sends nothing.

## `protection`

| Object | Methods |
|---|---|
| `ProtectedAccess` | `NO_ACCESS` (0), `DEVELOPER` (30), `MAINTAINER` (40), `ADMINISTRATOR` (60); `value()`, `from(int)` (an unknown number throws `IllegalArgumentException`) |
| `ProtectedBranch`, `GroupProtectedBranch` | `project()` or `group()`, `name()`, `allowsForcePush()` and `requiresCodeOwnerApproval()` (a missing flag is `false`), `pushAccess()` and `mergeAccess()` (role-based rules only; an unknown level throws `IllegalArgumentException`), `pushAccessValues()` and `mergeAccessValues()` (the raw numbers of the same rules, never fail), `allowForcePush(boolean)` and `requireCodeOwnerApproval(boolean)` (each returns the new object from the answer of GitLab, no extra request), `unprotect()`, `refresh()` (`IllegalStateException` when gone), `response()`. Every group call is Premium; the core's tier guard decides |
| `ProtectedTag` | `project()`, `name()`, `createAccess()` and `createAccessValues()` (role-based rules only, as above), `unprotect()`, `refresh()`, `response()`. GitLab cannot update a protected tag and has no group-level protected tags |

`Project` adds `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)` (only the name is sent, the defaults
of GitLab decide), `protectBranch(name, push, merge)`, `protectedTags()`, `protectedTag(name)` and
`protectTag(name, create)`; `Group` adds `protectedBranches()`, `protectedBranch(name)`, `protectBranch(name)` and
`protectBranch(name, push, merge)` (Premium). Names with a slash (`release/1.x`) reach the core unchanged. The v1
`Branch.protect()` and `Branch.unprotect()` stay as they are.

## What is not covered

- **Fields of a project or group outside the typed set.** `ProjectDraft`, `ProjectUpdate`, `GroupDraft` and
  `GroupUpdate` take the common fields only (see above); anything else needs the core's `MultipartFormData` API.
- **Options of `Branch.protect()`.** It uses GitLab's default rules; use `project.protectBranch(name, push, merge)` or the core
  for the options.
- **Filter on the jobs of one pipeline.** `pipeline.jobs()` lists without a filter. Pipeline and job logs, artifacts and
  variables are not covered.
- **User-, group-, deploy-key- and member-role-based protection rules.** They are skipped in the access lists and stay
  available from `response()`; the `allowed_to_push`, `allowed_to_merge`, `allowed_to_unprotect` and `allowed_to_create`
  rules and the unprotect level cannot be set; use the core.
- **Everything outside the areas above**, such as group releases, milestones, snippets, member roles
  (Ultimate), the premium-only member endpoints (approving pending members, billable members), access tokens, the container registry and packages, instance-level deploy keys and tokens, environments, discussions and
  threads, labels of a group, the descendant groups of a group at every level, and the administration of an instance. Use `gitlab-rest-client` for them; see
  [When the SDK does not cover a call](../../README.md#when-the-sdk-does-not-cover-a-call).
- **Releases.** Group releases, the typed latest-release getter (the core only has `void` permalink calls), release asset
  sources and downloads, evidence beyond `collectEvidence()`, and the `milestoneIds` and `assets` of a release (they stay
  reachable through `createRelease(payload)`) are not covered.
- **Members.** Member roles (Ultimate) are not covered (see above).
- **Project import and export, and user-scoped project creation.** `sdk.createProject(name)` creates a project for the
  signed-in user or in a namespace (`namespaceId(...)`); creating one for another user (the core's `createUser`) is not
  covered.
- **Group-level deploy keys.** GitLab has none; a group has deploy tokens only.
- **Webhook extras.** URL variables, custom headers, delivery logs and resending an event are not covered, nor are
  the events outside `WebhookEvent`; `createWebhook(payload)` takes the core's payload for them.
- **Live verification.** No area has been run against a real GitLab instance yet; see
  [Live smoke tests](smoke-tests.md).
