# Parity with gitlab4j-api

What the SDK offers compared with [gitlab4j-api](https://github.com/gitlab4j/gitlab4j-api) 6.3.0, as Phase E left it.
[What the SDK covers](coverage.md) names every object and method; this page says where the SDK differs from gitlab4j
and why.

## How to read this page

- **Rule.** A gitlab4j method counts as offered when the SDK has a method that calls the same GitLab operation, not one
  with the same name. gitlab4j quirks are not copied: `getApprovalStatus` (returns the closes-issues list),
  `geProjectIssuesStatistics` (a typo), the hard-coded `owned=true` of `getCommits`, silent `null` returns and duplicated
  deprecated aliases.
- **What was compared.** The gitlab4j method lists were read from the gitlab4j-api repository when Phase E was planned
  (the plan records `ProjectApi`, `GroupApi`, `UserApi`, `IssuesApi`, `MergeRequestApi`, `PipelineApi`, `JobApi`,
  `CommitsApi`, `RepositoryApi`, `RepositoryFileApi`, `TagsApi` and 17 smaller classes). This page was written from the
  Phase E task tables and the rows each task filled in; the gitlab4j sources were not read again for it. Each row says
  what its comparison was:
  - **compared**: the task compared the SDK with the gitlab4j methods of that class.
  - **compared against the core's operations**: the task compared the SDK with the operations of the core
    (`gitlab-rest-client`), not with a gitlab4j method list; a gitlab4j method the core has no operation for is not
    named.
  - **not re-compared**: the class belongs to Phases A to D. Phase E did not compare it again, so this page makes no
    claim about its methods; see the `coverage.md` section named in the row.
- **Reasons.** A reason that cites "GitLab's API description" comes from the OpenAPI description the core is generated
  from (`openapi_v3.yaml`), not from a page of docs.gitlab.com. docs.gitlab.com is cited only for the pages that were
  really read; they are listed in [Deprecated by GitLab](coverage.md#deliberately-not-offered-deprecated-by-gitlab).
- **Live.** None of this has been run against a real GitLab instance; see [Live smoke tests](smoke-tests.md).

The reasons a method is not offered are one of: **deprecated** (GitLab marks it deprecated, so the SDK calls no such
operation), **the core cannot** (the core's call returns nothing, lacks a field or cannot decode the answer; each has a
[core follow-up](#core-follow-ups)), **no core operation**, **admin only, deferred to v3** (instance and administrator
APIs wait for `sdk.admin()`), **out of scope**, or **not wrapped** (the core can do it and the SDK does not offer it; use
the core client).

## Classes compared in Phase E

| gitlab4j class | Comparison | Offered | Offered differently | Not offered, and why |
|---|---|---|---|---|
| `ProjectApi`, lists and account part | compared | list all projects with the core's filter (owned, starred, member, search, order); the personal and starred projects of a user; project users, ancestor groups and invited groups; events with a filter; star and unstar; languages; housekeeping; templates (list and load) | `sharedGroups()` now lists the invited groups (`GET /projects/{id}/invited_groups`); the ancestors are `groups()` | `stars()`: the core cannot (core follow-up 7). Search by the search API: deferred (see below). The instance and administrator calls: admin only, deferred to v3 |
| `ProjectApi`, settings part | not re-compared: the operations were taken from the plan's core-call table, and the gitlab4j method list was not read again for them | badges (list, load, add, preview, update, delete); push rules (Premium); access tokens (list, load, create, rotate, revoke); remote mirrors (list, load, add, update, sync, delete); the pull mirror (Premium: load, configure, update); approval rules (Premium: list, load, create, update, delete); the approval configuration; the avatar download; the markdown upload | none recorded | avatar upload: not wrapped (a multipart update the SDK does not wrap). Custom attributes: admin only, deferred to v3. Starting a pull mirror: not wrapped (the core's payload is the pull-request event of GitHub). The public key of a remote mirror: the core cannot (follow-up 13). Creating an access token with the Planner or Security Manager role: the core cannot (follow-up 11). `approvals_before_merge` and `require_password_to_approve`: deprecated in GitLab's API description, readable only in `response()`. The `users` and `groups` fields of an approval rule: deprecated in GitLab's API description. The list and deletion of the uploads of a project: not wrapped |
| `GroupApi`, lists and account part | compared | list all groups with the core's filter; descendant groups; shared groups; the subgroups, projects, members, labels and variables are from the earlier phases | none recorded | group search: deferred (see below). The administrator calls: admin only, deferred to v3 |
| `GroupApi`, settings part | not re-compared, as for `ProjectApi` | badges (list, load, add, preview, update, delete); push rules (Premium); access tokens (list, load, create, rotate, revoke); approval rules (Premium: list, create, update); the avatar download; the LDAP group links (Premium: list, add by CN or filter, delete); the SAML group links (Premium: list, load, add, delete) | the LDAP delete takes the provider with the CN or the filter | `transferProject`: admin only per GitLab's API description, deferred to v3 (`Project.transfer(namespace)` covers the other direction). Avatar upload and the uploads of a group: not wrapped. Custom attributes: admin only, deferred to v3. Load and delete of a group approval rule: the core cannot (follow-up 12). Access tokens with the Planner or Security Manager role: the core cannot (follow-up 11). `deleteByCn` and `deleteByProviderAndCn` of the LDAP links: deprecated in GitLab's API description |
| `UserApi` | compared (the non-administrator part) | list users with the core's filter; load by id and by username; the signed-in profile; the SSH keys, email addresses and GPG keys of the signed-in account (list, add, delete); the associations count | `me.profile()` is the signed-in user | the administrator calls (create, modify, block, impersonation tokens, keys and emails of other users): admin only, deferred to v3. GPG key revocation and the lookups of a single key: not wrapped. The memberships of a user: no core operation (plan ruling 6) |
| `IssuesApi` | compared | list for a project, a group and the instance (with the core's filters); load; create; update (also confidential, milestone, due date); delete; move; time estimate and spent time with their resets and the time statistics; links (list, load, create, delete); participants; closing and related merge requests | `closeIssue` is `Issue.close()` and `reopen()`; `assignIssue` is `update().assignees(...)` | issue statistics: the core cannot (follow-up 1). Clearing a milestone or a due date: the core cannot (its payload has no way to send null). Subscribe, unsubscribe, to-do, reorder, clone, metric images and user-agent details: not wrapped. The epic issue calls: deprecated (the epics REST API) |
| `MergeRequestApi` | compared | list for a project, a group and the instance (with the core's filters); load; create; update (also assignees, reviewers, milestone, squash, remove source branch, discussion lock, allow collaboration); close; reopen; merge; cancel the automatic merge; delete; rebase (asynchronous); approve and unapprove; approvals; the approval state and the approval rules of a merge request (Premium); the diffs; the diff versions; participants; the issues it closes; create a pipeline; pipelines and commits; time estimate and spent time with their resets and the time statistics | `getMergeRequestChanges` is `diffs()`; `acceptMergeRequest` is `merge(options)` | the `changes` endpoint: deprecated since GitLab 15.7 (confirmed on docs.gitlab.com, see the deprecated list in `coverage.md`). `rebase()` returns nothing: the core cannot decode GitLab's answer (follow-up 2). Raw diffs: the core cannot (the call discards the body). Clearing a milestone: the core cannot. Blocks, status checks, suggested reviewers, reviewer states, to-do, subscribe, unsubscribe, the merge ref, resetting the approvals and the related issues: not wrapped |
| `PipelineApi` | compared against the core's operations | list; load; create; retry; cancel; delete; the jobs of a pipeline; variables (`Pipeline.variables()`); the test report summary; trigger jobs; firing a pipeline with a trigger token (`Project.triggerPipeline`) | `triggerJobs()` replaces the bridges operation | bridges: deprecated (the core marks `listBridges` deprecated). The full test report (`getTestReport`): not wrapped (the plan listed only the summary). Pipeline metadata update, latest pipeline and creating a pipeline with variables: not wrapped, no reason recorded in the Phase E ledger (`createPipeline(payload)` carries the variables) |
| `JobApi` | compared against the core's operations | list; load; retry; cancel; play; erase; the artifacts archive (to a `Path` or a stream); the artifact tree; keep and delete artifacts | none recorded | `Job.trace()`: the core cannot (follow-up 17: it decodes the text as a job record). A single artifact file: the core cannot (follow-up 18: the call returns nothing). Job-token parameters on the artifact calls: not wrapped |
| `CommitsApi` | compared | list with the core's filter; load; diffs; comments (list and add, also on a line); statuses (list, also filtered by ref, name and all, and add); a commit of several files (create, update, delete and move actions, text or binary); cherry-pick; revert; the merge requests and refs of a commit; the signature | `getCommitsStream` is `commits(filter)`; `getCommit` is `commit(sha)` (empty on 404) | the `chmod` action, `start_branch`, `start_project`, `force`, `author_name` and `author_email` of a multi-file commit, the commit sequence, a dry run or a custom message for a cherry-pick or revert, and filtering statuses by stage, pipeline or order: not wrapped (the core has the status filter; a status can still carry a pipeline through `addStatus(payload)`). Whether GitLab accepts the multipart form of a multi-file commit is not verified (follow-up 6) |
| `RepositoryApi` | compared | branches (list, load, create, protect, unprotect, delete, delete merged); tags; tree (path, ref, recursive); compare (straight or from the merge base); contributors; merge base; changelog notes | none recorded | raw blob and archive download: the core cannot (follow-up 3). Changelog creation: the core cannot (follow-up 4). Batch reads, changed paths, diverging commits, diff stats and repository health: not wrapped |
| `RepositoryFileApi` | compared | get a file at a ref (text or bytes); create; update; delete (text or binary); blame of the whole file or of a line range | none recorded | file metadata without the content (`RepositoryFile.info()`): the core cannot (follow-up 5). The raw file download: the core cannot (follow-up 3) |

## Classes not re-compared in Phase E

These belong to Phases A to D. Phase E did not compare them with the gitlab4j methods again, so no count of covered
methods is claimed. The SDK does offer the area; the `coverage.md` section says what it holds.

| gitlab4j class | Where the SDK covers the area |
|---|---|
| `TagsApi` | `Project.tags(...)` and the protected tags; `code` and `protection` |
| `LabelsApi` | project and group labels, subscriptions; `issues` |
| `NotesApi` | notes of issues and merge requests; `issues` and `mergerequests` (snippet and epic notes are not offered) |
| `MilestonesApi` | `milestones` |
| `ReleasesApi` | `releases` |
| `EnvironmentsApi` | `environments` |
| `DeploymentsApi` | `environments` |
| `DeployKeysApi` | `deploy` (project deploy keys; no instance-level deploy keys) |
| `DeployTokensApi` | `deploy` (project and group deploy tokens) |
| `ProtectedBranchesApi` | `protection` |
| `DiscussionsApi` | `discussions` (commit, snippet and epic discussions are not offered) |
| `AwardEmojiApi` | `reactions` (snippet and epic-note reactions are not offered) |
| `RunnersApi` | `runners` (registration and instance-level administration are not offered) |
| `SnippetsApi` | `snippets` |
| `WikisApi` | `wikis` |
| `PackagesApi` | `packages` |
| `ContainerRegistryApi` | `registry` |
| `AuditEventApi` | `audit` (project and group; the instance events are an administrator API) |

## Areas of gitlab4j-api the SDK does not offer

| Area | Why |
|---|---|
| Instance and administrator APIs: user administration, impersonation tokens, application settings, license, health check | admin only, deferred to v3 behind `sdk.admin()` |
| Custom attributes of users, projects and groups | admin only, deferred to v3 |
| Issue boards | out of scope for Phase E; not compared further |
| Epics | deprecated: the REST API is deprecated in GitLab 17.0 (docs.gitlab.com/api/epics, read when Phase D was written, not again in Phase E) |
| Vulnerabilities and vulnerability findings | deprecated, or in the process of being deprecated (docs.gitlab.com, read when Phase D was written, not again in Phase E) |
| Search | the core cannot: its search calls return nothing, so the result type is unknown; deferred |
| Avatar upload | not wrapped (a multipart update of a project or group) |
| A user's memberships | no core operation |

## Core follow-ups

Each of these is a change in the core (`gitlab-rest-client`, developed as `gitlab-api-internal`) that was not made
because its checkout holds work in progress that Phase E must not touch. Until it is made, the SDK method named is not
offered.

| No. | What the core lacks | Effect in the SDK |
|---|---|---|
| 1 | the three issue statistics calls return nothing (no response schema in the description) | `issuesStatistics(filter)` on `Project`, `Group` and `GitLabSdk` not offered |
| 2 | no typed answer for the rebase (`{"rebase_in_progress": true}`, 202) | `MergeRequest.rebase()` returns nothing |
| 3 | the raw blob and the archive calls return nothing (binary bodies) | `rawBlob` and `archive` not offered |
| 4 | the changelog creation payload has no `version`, which GitLab requires | changelog creation not offered |
| 5 | no file metadata call: `head` and `headBlame` return only a boolean | `RepositoryFile.info()` not offered |
| 6 | `createCommits` takes only a multipart body; no typed JSON payload with `actions` (a conditional need: only if GitLab rejects the multipart form) | `createCommit` sends form fields, unit-tested and not verified live |
| 7 | `GET /projects/{id}/starrers` is decoded as bare users, but GitLab documents `starred_since` and a nested `user` | `Project.stars()` not offered |
| 8 | every template answer is typed as the license record | works (`name` and `content` are filled); a dedicated record would be cleaner |
| 9 | deleting an email and a GPG key returns nothing and does not map 404, unlike an SSH key | a gone email or GPG key fails with the core's exception on `delete()` |
| 10 | `star` and `unstar` can answer 304 per the description; how the client treats an empty 304 body is unchecked | `star()` and `unstar()` unverified for an already starred project |
| 11 | the access token response enums lack `PLANNER` and `SECURITY_MANAGER`, though the create enums have them | `createAccessToken` refuses those two roles; listing such a token may fail to decode |
| 12 | no operation to load or delete a single group approval rule | `GroupApprovalRule` has no `refresh()` or `delete()`; `Group` has no `approvalRule(id)` |
| 13 | `GET /projects/{id}/remote_mirrors/{id}/public_key` returns nothing | the public key of a remote mirror cannot be read |
| 14 | the installed core jar types `protectedBranches` of a group approval rule as one object, while the description and the core source have a list (the jar is older than the source) | a group rule with protected branches may fail to decode until the core is rebuilt and installed |
| 15 | observation, no change needed: `POST /projects/{id}/mirror/pull` is typed with a GitHub-style pull-request payload | `configurePullMirror` uses the `PUT` call; starting a pull is not offered |
| 16 | `GitLabRemoteMirror` and `GitLabPullMirror` print the address, which can carry credentials, in `toString()` | the SDK objects print nothing; `response()` returns the core record, whose `toString()` prints it |
| 17 | `getTrace` returns a job record although `/trace` is plain text | `Job.trace()` not offered |
| 18 | the single artifact call returns nothing (the body is discarded) | a single artifact file not offered |

## Totals

The counts are of the rows of this page, not of methods. No method count is claimed: the gitlab4j method lists were not
re-read for this page.

| | Count |
|---|---|
| gitlab4j classes in the table of Phase E (28 rows of the plan) | 28 |
| compared in Phase E (`ProjectApi` and `GroupApi` only for their lists and account part) | 10 classes: 8 rows compared, 2 compared against the core's operations (`PipelineApi`, `JobApi`) |
| settings part of `ProjectApi` and `GroupApi`, not re-compared | 2 rows |
| classes not re-compared in Phase E | 18 |
| core follow-ups | 18 |
