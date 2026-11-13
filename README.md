# gitlab-sdk

Java 21 object layer over [gitlab-rest-client](https://github.com/piotr-janczukowicz/gitlab-rest-client): objects that
carry methods which call the GitLab REST API, such as `project.createIssue(...)` and `issue.close()`.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.piotr-janczukowicz/gitlab-sdk)](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/gitlab-sdk)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/piotr-janczukowicz/gitlab-sdk/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/gitlab-sdk/actions/workflows/main.yml)

## Why this library

- **Objects instead of coordinates.** An issue knows its project, so `issue.comment("Thanks")` needs no project id and no
  issue number.
- **What GitLab holds, not a copy.** Every operation returns a new object built from GitLab's answer. Objects are
  immutable, and `refresh()` reloads one.
- **No hidden requests.** A `Project` from `sdk.project("my-group/my-project")` and a `Group` from `sdk.group("my-group")`
  are handles and make no request until you call `fetch()`; `Me` from `sdk.me()` is a handle too, loaded by `profile()`.
  Every other object is loaded, and every network call is a visible method call.
- **The client stays within reach.** Every object has `response()`, the complete `gitlab-rest-client` response, and you
  keep the `GitLab` client you passed in for everything the objects do not cover.
- **Small on purpose.** Synchronous REST only: no async API and no GraphQL.

## Install

**Requirements:** Java 21. The library is a Java module, `io.github.piotr_janczukowicz.gitlabsdk`, and also works on the
class path. It depends on `gitlab-rest-client`, which Maven brings in.

```xml
<dependency>
    <groupId>io.github.piotr-janczukowicz</groupId>
    <artifactId>gitlab-sdk</artifactId>
    <version>19.6.0</version>
</dependency>
```

The versions follow the core: SDK `19.6.N` pairs with `gitlab-rest-client` `19.6.N`. Until the core `19.6.0` is released,
the SDK is `19.6.0-SNAPSHOT` and builds against the core `19.6.0-SNAPSHOT`. The newest release is on the
[releases page](https://github.com/piotr-janczukowicz/gitlab-sdk/releases). The library is new and not yet proven in
production; see the [Disclaimer](#disclaimer).

## Quick start

`GitLabSdk` wraps a `GitLab` client from `gitlab-rest-client`, so authentication, the instance (gitlab.com, GitLab Dedicated
or self-managed), the tier and limits are configured on the client as usual. The SDK has no configuration of its own.

```java
try (GitLabClient client = GitLabClients.create(
        GitLabAuthenticationProviders.personalToken(System.getenv("GITLAB_TOKEN")), "my-app/1.0")) {
    GitLabSdk sdk = GitLabSdk.of(client);

    Project project = sdk.project("my-group/my-project");
    Issue issue = project.createIssue("Found a bug");
    issue.comment("Looking into it");
    Issue closed = issue.close();                       // GitLab's answer, not a local copy

    project.issues().limit(10).forEach(listed -> System.out.println(listed.iid() + " " + listed.title()));
}
```

The `try` block only keeps the example short and closes the client at its end. In an application, create the client once
and close it once, when the application stops (see [Good practices](#good-practices)).

## What you can do

Each area below is an object you reach from `sdk` or from a `Project`. The
[full list of objects and methods](docs/guide/coverage.md) names every call.

| Area | What it covers |
|---|---|
| **Projects and groups** | load a project or a group by id or path, create, update, archive, unarchive, delete, transfer, share and unshare; fork a project and list its forks; the projects and the direct subgroups of a group |
| **Members and access** | list, load, add (by id or username), update and remove the members of a project or group, with a typed `AccessLevel`; request, list, approve and deny access requests; share a project or group with a group |
| **Webhooks** | list, load, create (by URL or a core payload), update (URL, name, description, token, SSL verification, events), test, delete the webhooks of a project; the same for a group (Premium) |
| **Deploy keys and tokens** | list, load, add, update, enable and delete the deploy keys of a project (GitLab has no group-level deploy keys in its API); list, load, create and delete the deploy tokens of a project or group, with a typed `DeployTokenScope` |
| **Protected branches and tags** | list, load, protect (default rules, or who may push and merge or create), allow force pushes, require code owner approval and unprotect the protected branches and the protected tags of a project; the protected branches of a group (Premium); a typed `ProtectedAccess` with the raw level numbers as an escape hatch |
| **Releases** | list (with a filter), load, create (for an existing tag, or creating the tag from a ref, or from a core payload), update (name, description, release time, milestones), collect evidence and delete the releases of a project; list, load, add, update and delete the links of a release, with a typed `ReleaseLinkType`; the release of a `Tag` |
| **Milestones** | list (with the core's filter), load, create, update (title, description, due and start date), close, reopen, delete the milestones of a project or group; promote a project milestone to a group milestone; the issues and merge requests of a milestone |
| **Users** | load a user by id, the profile of the signed-in account |
| **Issues** | create, list with a filter, load by `iid`, update, close, reopen, delete; notes; labels, with promotion of a project label to a group label |
| **Group labels** | list (with the core's filter), load by name, create, update (name, color, description, archived), subscribe, unsubscribe, delete the labels of a group |
| **Merge requests** | create, list with a filter, load by `iid`, update, close, reopen, merge, approve and withdraw an approval, delete; notes, the pipelines and the commits of a merge request |
| **Pipelines and jobs** | list, load, create a pipeline for a ref, retry, cancel, delete; the jobs of a pipeline; retry, cancel, play and erase a job |
| **Code** | files (read, create, update and delete, text or binary), branches (create, protect, unprotect, delete), tags (list with a filter, load, create, delete, signature, the tagged commit), commits (list, load, diff, cherry-pick, revert) |

Not covered: project and group fields beyond the common typed ones, project import and export, and creating a project
for another user (`createUser`); milestone burndown events and clearing a milestone date or description; member roles; webhook URL variables, custom headers, delivery logs and resending an
event; group-level deploy keys (GitLab has none); protected-branch rules of a user, a group, a deploy key or a member role
(they stay in `response()`). The same goes for every area outside the table, such as group releases, snippets, packages, the container registry and the administration of an instance. Use
`gitlab-rest-client` for these. No area has been run against a real GitLab instance yet; see
[Live smoke tests](docs/guide/smoke-tests.md).

Two things to know about the project and group calls: a project or group you transfer is addressed by its numeric id
afterwards, because its path changed, and `project.fork(...)` returns at once while GitLab creates the fork
asynchronously, so the fork may still be importing.

### Members

A member carries a typed `AccessLevel`; `accessLevelValue()` returns the raw number for a level the SDK does not know.

```java
Member member = project.addMember("ann", AccessLevel.DEVELOPER);
member = member.update().accessLevel(AccessLevel.MAINTAINER).expiresAt(LocalDate.of(2027, 3, 1)).send();
project.members().forEach(m -> System.out.println(m.username() + " " + m.accessLevel()));
project.share(9L, AccessLevel.REPORTER);                                // share with a group
group.share(9L, AccessLevel.REPORTER);                                  // the same for a group
```

An expiry is set with `expiresAt(...)` and removed with `clearExpiresAt()`; the last call wins. Member roles (Ultimate) are
not covered.

`allMembers()` also lists the members inherited from parent groups. Such a member's `refresh()` loads it from that
list; its `update()` and `remove()` act on direct memberships only, so for a member who belongs only through a parent
group they fail with the core's exception for GitLab's HTTP 404.

### Webhooks

`WebhookEvent` names the events a webhook can be switched on for and tested with. The token is write-only: GitLab never
returns it. URL variables, custom headers and delivery logs are not covered.

As in `github-sdk`, `events(collection)` replaces the subscription: it sends all nine `WebhookEvent` flags, `true` for
the listed events and `false` for the others (an empty collection switches them all off, including the push events
GitLab enables by default). `addEvents(...)` and `removeEvents(...)` send only the named flags, `true` or `false`.

```java
Webhook hook = project.createWebhook("https://ci.example/hook");
hook = hook.update().name("ci").token("s3cret").events(List.of(WebhookEvent.PIPELINE)).send(); // only pipelines
hook = hook.update().addEvents(WebhookEvent.TAG_PUSH).removeEvents(WebhookEvent.PIPELINE).send();
hook.test(WebhookEvent.PUSH);
boolean onPush = hook.isEnabledFor(WebhookEvent.PUSH);
group.createWebhook("https://ci.example/group-hook");                   // Premium
```

`createWebhook` has two overloads, one for a URL and one for the core's payload, so a literal `null` needs a cast to pick
one; either overload rejects `null` with a `NullPointerException`.

### Deploy keys and tokens

A project has deploy keys and deploy tokens; a group has deploy tokens only, because GitLab's API has no group-level
deploy keys. The secret of a token is returned once, by the call that creates it, and `toString()` never prints it.

```java
DeployKey key = project.addDeployKey("ci", "ssh-ed25519 AAAA... ci", true);
key = key.update().title("ci-runner").canPush(false).send();
CreatedDeployToken created = project.createDeployToken("registry", DeployTokenScope.READ_REGISTRY);
String secret = created.token();                                        // the only time it is available
group.createDeployToken("packages", DeployTokenScope.READ_PACKAGE_REGISTRY);
```

### Protected branches and tags

`ProtectedAccess` names who may push, merge or create: nobody, developers, maintainers or administrators. The access
lists show the role-based rules only. Rules of a user, a group, a deploy key or a member role are not wrapped: they are
skipped in the lists (GitLab sends a level with them, which would look like a role rule) and stay available from
`response()`. `pushAccessValues()`, `mergeAccessValues()` and `createAccessValues()` return the raw level numbers of the
same role-based rules without failing on a level the enum does not know. The v1 `Branch.protect()` stays the quick way to
protect an existing branch with the default rules.

```java
ProtectedBranch main = project.protectBranch("main", ProtectedAccess.MAINTAINER, ProtectedAccess.DEVELOPER);
main = main.allowForcePush(true);                                       // the answer of GitLab is the new object
project.protectTag("v*", ProtectedAccess.MAINTAINER);
group.protectBranch("release/*");                                       // Premium
```

### Releases

A release is addressed by the name of its tag, which may contain slashes (`v1.0/rc`). `createRelease(tag)` makes a
release for an existing tag, `createRelease(tag, ref)` lets GitLab create the tag from a ref, and
`createRelease(payload)` takes the core's payload for the name, description, milestones, assets and release time.
`delete()` removes the release, not the tag. Group releases, release asset sources and the typed latest-release getter
are not covered; the `milestoneIds` and `assets` of a release stay reachable through the payload overload.

```java
Release release = project.createRelease("v1.0", "main");
release = release.update().name("One point oh").description("Notes").send();
ReleaseLink link = release.addLink("binary", "https://dl.example/app.zip");
link = link.update().type(ReleaseLinkType.PACKAGE).send();
Optional<Release> tagged = project.tag("v1.0").orElseThrow().release();
```

### Milestones

A milestone is addressed by its id (not its `iid`). Dates are `LocalDate`s: `dueDate()` and `startDate()` are empty when GitLab sends none, and the update builder takes `LocalDate`s. `close()` and `reopen()` change the state. `issues()` and `mergeRequests()` load each item as the stream reaches it, through the project the item belongs to (the items of a group milestone come from different projects). Burndown events and clearing a date or the description are not covered: the core's payload fields are plain optional values, so the SDK offers no way to clear them.

```java
Milestone milestone = project.createMilestone("v1.0");
milestone = milestone.update().dueDate(LocalDate.of(2026, 12, 31)).send();
long open = milestone.issues().count();
milestone.promote();
GroupMilestone shared = group.milestones().findFirst().orElseThrow().close();
```

## When the SDK does not cover a call

The SDK covers the common work, not every endpoint. For the rest, use `gitlab-rest-client` directly: the `GitLab` client
behind the SDK is yours, so there is no second client, no second credential and no second configuration. Create the `GitLab`
object yourself, hand it to `GitLabSdk.of(...)`, and keep it for the calls that have no SDK object. Both use the same
authentication, retries and rate-limit handling.

```java
try (GitLabClient client = GitLabClients.create(
        GitLabAuthenticationProviders.personalToken(System.getenv("GITLAB_TOKEN")), "my-app/1.0")) {
    GitLab gitLab = GitLab.of(client);
    GitLabSdk sdk = GitLabSdk.of(gitLab);

    Project project = sdk.project("my-group/my-project");
    project.createIssue("Found a bug");                                     // an SDK object

    // no SDK object for milestones: use the client with the same project id
    for (GitLabMilestone milestone : gitLab.projectmilestones().list(project.id()).all()) {
        System.out.println(milestone.title());
    }
}
```

The client names its resources as `gitLab.<resource>()`, an aggregator for projects, groups and the instance adds
`project()`, `group()` or `instance()`, and the Javadoc of each method names the HTTP request it sends; see the
[`gitlab-rest-client` documentation](https://github.com/piotr-janczukowicz/gitlab-rest-client). What the SDK returns links
back to it as well: every object has `response()`, the complete client record, which you can pass to the client again.
[What the SDK covers](docs/guide/coverage.md) lists every object, so you can check first.

## Conventions

All objects behave the same way, so you learn them once:

- Objects are **immutable**. An operation returns a new object; the old one keeps its old state.
- A **project or group** is a `String`: its numeric id or its path, as in the core (`my-group/my-project`). Every object
  returned by create, update or transfer of a project or group (`ProjectDraft.send()`, `GroupDraft.send()`,
  `ProjectUpdate.send()`, `GroupUpdate.send()`, `transfer(...)`, and also `fork(...)`) is addressed by its numeric id
  afterwards. Issues and merge requests are addressed by their `iid`, the number within
  the project.
- A **lookup** (`project.issue(42)`, `sdk.user(7)`, `project.file("README.md", "main")`) returns an `Optional`, empty when
  GitLab answers 404.
- A **list** is a lazy, single-use `Stream`. Pages are fetched as you consume it, so `limit(...)` stops further requests; a
  transport failure during iteration is an `UncheckedIOException`.
- **Several fields at once** go through `update()`:
  `issue.update().title("New").labels(List.of("bug")).send()` sends one request with only the fields you set. `send()` with
  nothing set throws `IllegalStateException`. Projects, groups, members, webhooks, deploy keys, issues, merge requests and labels have an `update()`.
- **State changes** that are not fields are methods on the object: `close()`, `reopen()`, `merge()`, `approve()`,
  `retry()`, `cancel()`, `protect()`.
- `refresh()` reloads an object and throws `IllegalStateException` when it is gone. Access requests (`AccessRequest`,
  `GroupAccessRequest`) and `ProjectGroupLink` have no `refresh()`: the API has no lookup of a single one.
- Where GitLab's list returns a smaller record than a single lookup (merge requests, pipelines, commits, the forks of a project,
  and the projects and subgroups of a group), the SDK loads each item with one more request as the stream reaches it. The Javadoc says so,
  and `limit(...)` bounds the cost. An item that is gone before it is loaded is skipped.
- A field that GitLab may leave out is an `Optional` (`issue.description()`, `mergeRequest.targetBranch()`).
- A **state or status** is an enum (`issue.state()`, `pipeline.status()`, `job.status()`) and throws
  `IllegalArgumentException` for a value this SDK does not know yet. `pipeline.statusName()` and `job.statusName()` return
  the raw value GitLab sent, for that case.
- **Raw escape hatches** stand next to the typed accessors and never fail on a value the SDK does not know:
  `statusName()` (pipelines, jobs), `accessLevelValue()` (members), `scopeNames()` (deploy tokens) and the
  `*AccessValues()` methods (`pushAccessValues()`, `mergeAccessValues()`, `createAccessValues()` of protected branches
  and tags).
- **Text content** of a file is sent and read as UTF-8: `project.createFile(path, branch, content, message)` and
  `file.text()`. `file.bytes()` returns the exact bytes, including those of a binary file.
- Where the core's payloads distinguish an omitted field from a cleared one, the update builders use its tri-state `OptionalNull` to clear it (`clearExpiresAt()` on member updates, alongside `clearDescription()`).
- `IOException` and the client's `GitLabApiException` pass through unchanged.
- A call that needs a tier your instance lacks fails in the core's tier guard when you configured the tier on the client;
  the SDK adds no tier logic of its own.

## Good practices

[Conventions](#conventions) says what the SDK does. This is what to do with it.

- **Create the client once and close it once.** One client per credential, with one `GitLabSdk` on top: the client owns the
  transport and the rate-limit state, so share it for the life of the application, and do not open a client per call. Close
  it when the application stops. The SDK never closes a client you passed in.
- **Keep the object an operation returns.** The object you called it on is not updated; use the result, and call
  `refresh()` only when something else may have changed the object.
- **Use handles, load when you need data.** `sdk.project("my-group/my-project")` costs nothing; call `fetch()` only for
  the fields the handle does not hold, and read them with `response()`.
- **Always bound a list you read from a large project.** Add `limit(...)` or stop the stream early, and avoid `toList()` on
  a list that can grow, especially one that loads an item per request.
- **Prefer one request over many.** `update()` collects the fields you set and sends them together, instead of one call per
  field.
- **Decide what an empty lookup means.** Handle the `Optional` instead of calling `orElseThrow()` by habit, and catch
  `GitLabApiException` (not found, forbidden, rate limit) at the place where you can act on it.
- **Configure the client, not the SDK.** Retry, backoff and page size are client settings; see
  [Configure the client](#configure-the-client).
- **Give the token what the call needs.** A personal access token with the `api` scope covers everything the SDK does;
  `read_api` covers the reads. A project or group access token covers that project or group only.
- **Keep secrets out of logs.** `response()` of a job, of a pipeline and of a project can carry URLs and variables of your
  instance; log what you need, not the whole record. The secret of a new deploy token (`token()`, and the `token()` of
  its `response()`) and the token you set on a webhook (`update().token(...)`) are credentials: store them in a secret
  store and never log them.
- **Try an integration on a throwaway project first.** The [live smoke tests](docs/guide/smoke-tests.md) show how.

## Configure the client

Authentication, retries, page size, the instance and its tier are the client's job, not the SDK's: every object calls the
client you pass to `GitLabSdk.of(...)` (a `GitLabClient`, a `GitLabCaller` or a `GitLab`), so one configuration covers all
of them. The SDK has no configuration of its own, and a call made through an object behaves exactly like the same call on
the client.

```java
GitLabClientConfiguration configuration = GitLabClientConfiguration.builder()
        .baseUri(URI.create("https://gitlab.example.com"))
        .tier(GitLabTier.PREMIUM)
        .build();

try (GitLabClient client = GitLabClients.create(
        GitLabAuthenticationProviders.personalToken(System.getenv("GITLAB_TOKEN")), "my-app/1.0", configuration)) {
    GitLabSdk sdk = GitLabSdk.of(client);
    // use the sdk as usual
}
```

The options are described in the `gitlab-rest-client` documentation:

- [Self-managed GitLab and tiers](https://github.com/piotr-janczukowicz/gitlab-rest-client#self-managed-gitlab-and-tiers):
  the instance root, the flavor and the tier guard
- [Pagination](https://github.com/piotr-janczukowicz/gitlab-rest-client/blob/main/docs/guide/pagination.md): page size and
  the page limit; a list of the SDK is a lazy `Stream`, so `limit(...)` stops further page requests too
- [Good to know](https://github.com/piotr-janczukowicz/gitlab-rest-client/blob/main/docs/guide/good-to-know.md): names,
  newer GitLab data and the caveats of GitLab's description
- [Transport guide](https://github.com/piotr-janczukowicz/gitlab-client/blob/main/docs/guide/README.md): configuration,
  authentication, rate limiting and exception handling

## Packages

The entry points (`GitLabSdk`, `Project`, `Group`, `User`, `Me`) are in `io.github.piotr_janczukowicz.gitlab.sdk`. The other
objects are grouped by area in the sub-packages `issues`, `mergerequests`, `ci`, `code`, `members`, `webhooks`, `deploy`, `protection`, `releases` and `milestones`.

The `internal` package is not exported by the module and is not part of the API. The module system hides it only on the
module path; on the classpath it is a convention and not a security boundary.

## Disclaimer

The SDK is a thin layer over `gitlab-rest-client`, which is built on the OpenAPI description that GitLab publishes; the
caveats of that project apply here too.

**No area of this SDK has been run on a live GitLab instance yet: the smoke tests exist but have not been run. Not every
operation has been tested. This library is new and not yet proven in production. Verify the behavior of anything you depend
on before using it in production.**

This is an independent project. It is not made, endorsed or sponsored by GitLab Inc.; "GitLab" is a trademark of GitLab
Inc., used here only to say what the library talks to.
