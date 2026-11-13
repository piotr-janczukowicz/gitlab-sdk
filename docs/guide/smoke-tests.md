# Live smoke tests

Opt-in tests that call a real GitLab instance through the SDK. `mvn verify` never runs them.

**Status: no area has been run live yet.** The tests compile and are skipped without a token, but no token for a GitLab
instance was available when they were written, so none of them has ever run. Every area below is covered by unit tests only
until someone runs `mvn -Psmoke verify` and records the result here.

## Setup

Use a sandbox project, as the smoke tests of
[gitlab-rest-client](https://github.com/piotr-janczukowicz/gitlab-rest-client/blob/main/docs/guide/smoke-tests.md) do: a
project that holds nothing else, whose path contains `smoke`. The tests refuse any other project, and a numeric id.

| Variable | Meaning |
|---|---|
| `SMOKE_GITLAB_TOKEN` | A personal access token with the `api` scope of a user who owns the sandbox project |
| `SMOKE_GITLAB_PROJECT` | The path of the sandbox project, `group/name`; the path must contain `smoke` |
| `SMOKE_GITLAB_URL` | The instance root; default `https://gitlab.com` |
| `SMOKE_GITLAB_USER_ID` | Optional. The numeric id of a second user who may be added to and removed from the sandbox; without it the add, change and remove of a member is skipped |

```bash
export SMOKE_GITLAB_TOKEN=glpat-...
export SMOKE_GITLAB_PROJECT=my-group/my-project-smoke
export SMOKE_GITLAB_URL=https://gitlab.example.com   # optional, https://gitlab.com is the default
export SMOKE_GITLAB_USER_ID=1234                     # optional, a second user for the member test
```

## Run

```bash
mvn -Psmoke verify
mvn -Psmoke verify -Dit.test=CodeSmokeIT
```

Without `SMOKE_GITLAB_TOKEN` and `SMOKE_GITLAB_PROJECT`, every smoke class is skipped, not failed.

## What is covered

| Class | Area |
|---|---|
| `ProjectsSmokeIT` | load and refresh the sandbox, 404 on a missing project; the group of the sandbox, its projects and subgroups (skipped when the sandbox lives in a user namespace); the signed-in user, a user by id, 404 on a missing user. Archive, unarchive and delete of a project are not run live, because they would change the sandbox |
| `IssuesSmokeIT` | create, comment, edit a note, list notes, update, close, reopen, reload and delete an issue; create, update and delete a label and apply it to an issue; 404 on a missing issue |
| `MergeRequestsSmokeIT` | on a throwaway branch with one new file: create, comment, edit a note, list notes, update, close, reopen, reload and delete a merge request, list its commits; 404 on a missing merge request. Merging and approving are not run live, because the sandbox settings may forbid them and a merge cannot be undone |
| `PipelinesSmokeIT` | list, load and refresh pipelines and list their jobs, list and load jobs, 404 on a missing pipeline. Reads only, and skipped when the sandbox has no pipelines or jobs: creating, retrying, cancelling and deleting pipelines and retrying, cancelling, playing and erasing jobs need a CI configuration and runners, so they are not run live |
| `CodeSmokeIT` | a file with non-ASCII text (`zażółć gęślą jaźń ✓`) on a throwaway branch: create, read back with `text()` and `bytes()`, update, read, delete, 404 on a missing file; create, protect, unprotect and delete a branch; list and load commits; 404 on a missing branch |
| `ManagementSmokeIT` | create a project named `smoke-<runId>-project` in the namespace of the sandbox (the namespace id is read from the sandbox, so the token's user must be allowed to create projects there), update its description, reload it, list its forks and shared groups, delete it; list the forks and shared groups of the sandbox. Creating and deleting a **group** is not run, because it needs the right to create groups, which a sandbox token may not have; updating the sandbox itself is not run either. Transfer, fork, share, unshare and deleting the sandbox are not run live, because they would change or lose the sandbox. GitLab.com may keep a deleted project for a while before it disappears |
| `MembersSmokeIT` | list the direct and all members of the sandbox, find the signed-in user among them, load and refresh a member by user id, 404 on a user who is not a member. With `SMOKE_GITLAB_USER_ID` set (and that user not yet a direct member): add the user as Developer, change the level to Reporter, reload, remove. Access requests are not run live |
| `WebhooksSmokeIT` | create a webhook with the URL `https://example.invalid/smoke-<runId>`, update its name and events, reload, list, delete, 404 on a missing hook. `Webhook.test(...)` is **not** run live, because it makes GitLab send a real HTTP request to the hook URL. Group webhooks (Premium) are not run live |
| `DeploySmokeIT` | add a deploy key (a fixed public key made for the tests, titled `smoke-<runId>`), update its title and push flag, reload, list, delete; create a deploy token with `READ_REPOSITORY`, check that the secret is present and absent from `toString()` (it is never printed), reload, list, delete; 404 on a missing key. Needs a Maintainer. GitLab accepts a key only once, so a run that failed to delete the key blocks the next run until you remove the key by hand |
| `ProtectionSmokeIT` | create the branch `smoke-<runId>/protected` from the default branch, protect it for Maintainers, allow force pushes, reload, list, unprotect, delete the branch; protect the tag pattern `smoke-<runId>-*` for Maintainers, reload, list, unprotect; 404 on a branch and a tag that are not protected. Needs the Maintainer role. Group protected branches (Premium) are not run live |

Every resource a test creates is named `smoke-<runId>-...` and is removed in a `finally` block, so a failed run leaves
nothing behind. Issues are deleted outright. Cleanup relies on the same SDK calls that the tests exercise, so a failure in
`delete()` can leave a `smoke-` resource in the sandbox; remove it by hand. GitLab may keep a deleted resource for a short
time before it disappears.

## Not yet verified live

All of it. Besides the areas listed above as not run live, none of the covered calls has met a real GitLab yet, so these
questions are open until a run:

| Question | Why it matters |
|---|---|
| Non-ASCII file content through the multipart form | A unit test proves the form carries the UTF-8 bytes of the text without a charset of its own, which is what RFC 7578 expects. `CodeSmokeIT` is the test that checks GitLab reads it back unchanged |
| Tier-gated calls (for example approvals) | They depend on the tier of the instance; the SDK adds no tier logic, and the core's tier guard applies only when a tier is configured |
| Multipart project and group create and update on a real instance | The form field names and the boolean and non-ASCII values are only unit-tested. `ManagementSmokeIT` is the first check of project creation and update; group creation and update have no smoke test |
| The expiry of a member as an `Instant` | The SDK sends the date as the start of that day in UTC and reads an ISO date back; whether GitLab accepts and returns exactly that is unchecked, because `MembersSmokeIT` does not set an expiry |
| The shape of the protected-branch access arrays | The role-based rules are read from the arrays GitLab returns and a level is sent for each push, merge or create rule; `ProtectionSmokeIT` is the first check, including that user- and group-based rules never show up as role rules |
| The deploy token secret is shown once | The SDK relies on the secret being in the create answer only; `DeploySmokeIT` checks the create answer but not that a later load omits it |
| Calls that need permissions beyond `api` on a project | Protecting a branch needs the Maintainer role; a token of a lower role fails with 403 |

## Reading a failure

A failing smoke test means the SDK and the live API disagree. A 401 means the token is wrong or expired, a 403 that the token
lacks the `api` scope or the user lacks the role the call needs (protecting a branch needs Maintainer), and a 404 on the
sandbox that `SMOKE_GITLAB_PROJECT` or `SMOKE_GITLAB_URL` is wrong.
