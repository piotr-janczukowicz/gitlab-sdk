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

```bash
export SMOKE_GITLAB_TOKEN=glpat-...
export SMOKE_GITLAB_PROJECT=my-group/my-project-smoke
export SMOKE_GITLAB_URL=https://gitlab.example.com   # optional, https://gitlab.com is the default
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
| Calls that need permissions beyond `api` on a project | Protecting a branch needs the Maintainer role; a token of a lower role fails with 403 |

## Reading a failure

A failing smoke test means the SDK and the live API disagree. A 401 means the token is wrong or expired, a 403 that the token
lacks the `api` scope or the user lacks the role the call needs (protecting a branch needs Maintainer), and a 404 on the
sandbox that `SMOKE_GITLAB_PROJECT` or `SMOKE_GITLAB_URL` is wrong.
