# What is covered

Every object of the SDK, with the methods it has, by package. The [README](../../README.md) gives the short overview.
Every object also has `response()`, which returns the complete `gitlab-rest-client` record it is built from, and navigates
to its parent (`issue.project()`, `note.issue()`, `job.project()` and so on). A lookup returns an `Optional`, a list is a
lazy, single-use `Stream`, and the `iid` of an issue or a merge request is a `long`.

## Root package: `io.github.piotr_janczukowicz.gitlab.sdk`

| Entry point | Objects and what they do |
|---|---|
| `GitLabSdk.of(...)` | Wraps a `GitLab`, a `GitLabCaller` or a `GitLabClient`. `project(id)` and `group(id)` give handles, `user(id)` loads a user, `me()` gives the signed-in account |
| `Project` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `archive()`, `unarchive()`, `delete()`, `response()`. Issues: `issues()`, `issues(filter)`, `issue(iid)`, `createIssue(title)`, `createIssue(payload)`. Labels: `labels()`, `createLabel(name, color)`. Merge requests: `mergeRequests()`, `mergeRequests(filter)`, `mergeRequest(iid)`, `createMergeRequest(payload)`. Pipelines and jobs: `pipelines()`, `pipeline(id)`, `createPipeline(ref)`, `createPipeline(payload)`, `jobs()`, `job(id)`. Code: `file(path, ref)`, `createFile(path, branch, content, message)`, `branches()`, `branch(name)`, `createBranch(name, ref)`, `commits()`, `commit(sha)` |
| `Group` | A handle, loaded by `fetch()`. `id()`, `fetch()`, `refresh()`, `archive()`, `unarchive()`, `delete()`, `projects()` (each project is loaded as the stream reaches it), `subgroups()` (each group is loaded as the stream reaches it), `response()` |
| `User` | `id()`, `username()`, `refresh()`, `response()` |
| `Me` | `fetch()` loads the signed-in account as a `User` |

## `issues`

| Object | Methods |
|---|---|
| `Issue` | `project()`, `iid()`, `title()`, `description()`, `state()` (`IssueState`: `OPENED`, `CLOSED`), `labels()`, `update()`, `close()`, `reopen()`, `comment(body)`, `notes()`, `delete()`, `refresh()`, `response()` |
| `IssueUpdate` | `title(...)`, `description(...)`, `clearDescription()`, `labels(names)`, `assignees(userIds)`, `send()`; `send()` with nothing set throws `IllegalStateException` |
| `Note` | `issue()`, `id()`, `body()`, `update(body)`, `delete()`, `response()` |
| `Label` | `project()`, `name()`, `color()`, `description()`, `update()`, `delete()`, `refresh()`, `response()` |
| `LabelUpdate` | `name(...)`, `color(...)`, `description(...)`, `send()` |

## `mergerequests`

| Object | Methods |
|---|---|
| `MergeRequest` | `project()`, `iid()`, `title()`, `description()`, `state()` (`MergeRequestState`), `targetBranch()`, `sourceBranch()`, `labels()`, `update()`, `close()`, `reopen()`, `merge()`, `approve()`, `unapprove()`, `approvals()`, `comment(body)`, `notes()`, `pipelines()`, `commits()`, `delete()`, `refresh()`, `response()` |
| `MergeRequestUpdate` | `title(...)`, `description(...)`, `targetBranch(...)`, `labels(names)`, `send()` |
| `MergeRequestNote` | `mergeRequest()`, `id()`, `body()`, `update(body)`, `delete()`, `refresh()`, `response()` |
| `Approvals` | `mergeRequest()`, `approved()`, `approvalsLeft()`, `approvalsRequired()`, `userHasApproved()`, `response()` |

## `ci`

| Object | Methods |
|---|---|
| `Pipeline` | `project()`, `id()`, `status()` (`PipelineStatus`), `ref()`, `sha()`, `webUrl()`, `retry()`, `cancel()`, `jobs()`, `delete()`, `refresh()`, `response()` |
| `Job` | `project()`, `id()`, `name()`, `status()` (`JobStatus`), `stage()`, `pipeline()`, `retry()`, `cancel()`, `play()`, `erase()`, `refresh()`, `response()` |

## `code`

| Object | Methods |
|---|---|
| `RepositoryFile` | `project()`, `path()`, `name()`, `ref()`, `bytes()` (exact bytes, also of a binary file), `text()` (strict UTF-8), `update(content, message)`, `delete(message)`, `response()`. Content is sent as UTF-8 text |
| `Branch` | `project()`, `name()`, `isProtected()`, `isDefault()`, `protect()` (GitLab's default rules), `unprotect()`, `delete()`, `refresh()`, `response()` |
| `Commit` | `project()`, `sha()`, `title()`, `message()`, `diff()` (the core's `GitLabDiff` records), `cherryPick(branch)`, `revert(branch)`, `response()` |

## What is not covered

- **Creating and updating a project or a group.** The core takes a `MultipartFormData` for them, so v1 has no
  `createProject`, `Project.update()` or `Group.update()`. Archive, unarchive and delete are covered.
- **Writing a binary file.** `createFile` and `RepositoryFile.update` send text as UTF-8. Reading a binary file is covered:
  `bytes()` returns the exact bytes.
- **Options of a merge and of an approval.** `merge()` and `approve()` use GitLab's defaults (no squash, no message, no SHA
  guard, no approval password). Likewise `Branch.protect()` uses GitLab's default rules. Call the core for the options.
- **Filters on pipelines and jobs.** `project.pipelines()`, `pipeline.jobs()` and `project.jobs()` list without a filter;
  issues and merge requests accept the core's filter. Pipeline and job logs, artifacts and variables are not covered.
- **Everything outside the areas above**, such as releases, tags, milestones, snippets, protected branches in detail, members
  and access tokens, webhooks, the container registry and packages, deploy keys and tokens, environments, discussions and
  threads, labels of a group, and the administration of an instance. Use `gitlab-rest-client` for them; see
  [When the SDK does not cover a call](../../README.md#when-the-sdk-does-not-cover-a-call).
- **Live verification.** No area has been run against a real GitLab instance yet; see
  [Live smoke tests](smoke-tests.md).
