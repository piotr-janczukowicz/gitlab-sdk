# gitlab-sdk

Java 21 object layer over [gitlab-rest-client](https://github.com/piotr-janczukowicz/gitlab-rest-client): objects that
carry methods which call the GitLab REST API, such as `project.createIssue(...)` and `issue.close()`.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.piotr-janczukowicz/gitlab-sdk)](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/gitlab-sdk)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/piotr-janczukowicz/gitlab-sdk/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/gitlab-sdk/actions/workflows/main.yml)

## Why this library

- **Objects instead of coordinates.** An issue knows its project, so `issue.comment("Thanks")` needs no project id and
  no issue number.
- **What GitLab holds, not a copy.** Every operation returns a new object built from GitLab's answer. Objects are
  immutable, and `refresh()` reloads one.
- **No hidden requests.** A `Project` from `sdk.project("my-group/my-project")` and a `Group` from
  `sdk.group("my-group")` are handles and make no request until you call `fetch()`; `Me` from `sdk.me()` is a handle
  too, loaded by `profile()`. Every other object is loaded, and every network call is a visible method call.
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

The versions follow the core: SDK `19.6.N` pairs with `gitlab-rest-client` `19.6.N`. Until the core `19.6.0` is
released, the SDK is `19.6.0-SNAPSHOT` and builds against the core `19.6.0-SNAPSHOT`. CI stays red until the core
`19.6.0` is released and the `gitlab-rest-client.version` pin in `pom.xml` changes to it: the CI build resolves only
released versions of the core, so it cannot fetch the snapshot. The newest release is on the
[releases page](https://github.com/piotr-janczukowicz/gitlab-sdk/releases). The library is new and not yet proven in
production; see the [Disclaimer](#disclaimer).

## Quick start

`GitLabSdk` wraps a `GitLab` client from `gitlab-rest-client`, so authentication, the instance (gitlab.com, GitLab
Dedicated or self-managed), the tier and limits are configured on the client as usual. The SDK has no configuration of
its own.

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

Each area below is an object you reach from `sdk`, from a `Project` or from a `Group`. The
[full list of objects and methods](docs/guide/coverage.md) names every call.

| Area | What it covers |
|---|---|
| **Projects and groups** | load a project or a group by id or path, create, update, archive, unarchive, delete, transfer, share and unshare; fork a project and list its forks; the projects and the direct subgroups of a group |
| **Members and access** | list, load, add (by id or username), update and remove the members of a project or group, with a typed `AccessLevel`; request, list, approve and deny access requests; share a project or group with a group |
| **Webhooks** | list, load, create (by URL or a core payload), update (URL, name, description, token, SSL verification, events), test, delete the webhooks of a project; the same for a group (Premium) |
| **CI/CD variables** | list, load, create (by key and value, or a core payload), update (value, protected, masked, raw, type, environment scope, description) and delete the variables of a project or group; on a project the lookup, update and delete name the environment scope, with a typed `VariableType` |
| **Environments and deployments** | list (with the core's filter), load, create, update (URL, tier, description, Kubernetes namespace, Flux path, cluster agent, each clearable where the core allows), stop, delete the environments of a project, with a typed `EnvironmentTier`; list, load, create, update the status of, approve, reject and delete the deployments of a project or of an environment, with a typed `DeploymentStatus`; the merge requests of a deployment |
| **Pipeline schedules and triggers** | list, load, create, update (description, ref, cron, time zone, active), take over, run now and delete the pipeline schedules of a project, with their variables (add, load, update, delete); list, load, create (with an optional expiry), update and delete the pipeline triggers of a project |
| **Runners** | list the runners of a project or group (with the core's filter), load a runner by id, enable an existing runner in a project, disable it for a project, update (description, paused, active, tags, run untagged, locked, maximum timeout, maintenance note), delete, list its managers and jobs, and reset the registration token of a project or group or the authentication token of a runner |
| **Job artifacts** | download the artifacts archive of a job, or of the latest successful job of a name on a ref, to a path or a stream; list the artifact tree of a job; keep and delete the artifacts of a job |
| **Packages** | list the packages of a project (with the core's filter) or of a group, load one by id, list its files, download a file to a path or a stream, delete a file or a package, with a typed `PackageType` and the raw status |
| **Container registry** | list the repositories of a project (with or without their tags) or of a group, reload one, list its tags (each loaded in full), load a tag, delete a tag, a repository, or tags in bulk by regular expression |
| **Deploy keys and tokens** | list, load, add, update, enable and delete the deploy keys of a project (GitLab has no group-level deploy keys in its API); list, load, create and delete the deploy tokens of a project or group, with a typed `DeployTokenScope` |
| **Protected branches and tags** | list, load, protect (default rules, or who may push and merge or create), allow force pushes, require code owner approval and unprotect the protected branches and the protected tags of a project; the protected branches of a group (Premium); a typed `ProtectedAccess` with the raw level numbers as an escape hatch |
| **Releases** | list (with a filter), load, create (for an existing tag, or creating the tag from a ref, or from a core payload), update (name, description, release time, milestones), collect evidence and delete the releases of a project; list, load, add, update and delete the links of a release, with a typed `ReleaseLinkType`; the release of a `Tag` |
| **Milestones** | list (with the core's filter), load, create, update (title, description, due and start date), close, reopen, delete the milestones of a project or group; promote a project milestone to a group milestone; the issues and merge requests of a milestone |
| **Wikis** | list (each page loaded in full), load by slug, create (with an optional typed `WikiFormat`), update (title, content, format), reload and delete the wiki pages of a project or group (Premium); upload an attachment to the wiki repository |
| **Snippets** | list, load, create (one file, or several through the core payload), read the content of a snippet or of one file, update (title, description, typed `SnippetVisibility`, add, update, delete and move files), reload and delete the snippets of a project and the personal snippets of the signed-in user |
| **Dependencies** | list the dependencies of a project (Ultimate), all or filtered by package manager, with their advisories and licenses; read only |
| **Settings of a project or group** | list, load, add, update and delete the badges of a project or group, with a preview of a badge; the push rules, the access tokens (list, load, create, rotate, revoke, with a typed `AccessTokenScope`) and the approval rules of a project or group; the remote mirrors, the pull mirror and the approval configuration of a project; the LDAP and SAML group links of a group; download the avatar and upload a file to a project (Premium for push rules, pull mirror, approval rules, the approval configuration and the group links) |
| **Audit events** | list the audit events of a project or group (Premium), all or created between two instants, newest first, and load one by id; read only |
| **Users** | list (with the core's filter), load by id or by username, the profile and the association counts of a user; the SSH keys, email addresses and GPG keys of the signed-in account (list, add, delete) |
| **Project and group lists** | list all projects (owned, starred or member through the core's filter), the personal and starred projects of a user, all groups, the descendant and shared groups of a group, the users, ancestor and invited groups and events of a project, star and unstar, languages, housekeeping, templates |
| **Issues** | create, list with a filter, load by `iid`, update (also confidentiality, milestone and due date), close, reopen, move to another project, delete; list the issues of a group or of the whole instance; time tracking (estimate, spent time), links between issues, participants and the merge requests that close or mention an issue; notes, discussions (threads) and emoji reactions; labels (list with the core's filter, load by name, create, update, delete), with promotion of a project label to a group label |
| **Group labels** | list (with the core's filter), load by name, create, update (name, color, description, archived), subscribe, unsubscribe, delete the labels of a group |
| **Merge requests** | create, list with a filter, load by `iid`, update, close, reopen, merge, approve and withdraw an approval, delete; notes, discussions (threads, diff comments with the core's position) and emoji reactions, draft notes (create, list, load, update, publish one or all, delete), the pipelines and the commits of a merge request; rebase (asynchronous), cancel the automatic merge, create a pipeline, diffs and diff versions, participants, the issues it closes, time tracking, the approval state and approval rules (Premium); list the merge requests of a group or of the whole instance |
| **Pipelines and jobs** | list, load, create a pipeline for a ref, retry, cancel, delete; the jobs of a pipeline, its variables, its trigger jobs and its test report summary; fire a pipeline with a trigger token; retry, cancel, play and erase a job |
| **Code** | files (read, create, update and delete, text or binary), branches (create, protect, unprotect, delete), tags (list with a filter, load, create lightweight or annotated, delete, signature, the tagged commit, the release of a tag), commits (list with a filter, load, diff, cherry-pick, revert, statuses, comments, merge requests, refs, signature, a multi-file commit of text and binary changes) |
| **Repository** | list the tree of a directory at a ref, compare two refs (straight or from the merge base), contributors, merge base, delete merged branches, generate changelog notes, blame of a file or a line range, stream the raw content of a file to a path or a stream |

Not covered: project and group fields beyond the common typed ones, project import and export, and creating a project
for another user (`createUser`); member roles; webhook URL variables, custom headers, delivery logs and resending an
event; group-level deploy keys (GitLab has none); protected-branch rules of a user, a group, a deploy key or a member
role (they stay in `response()`); group releases, the typed latest-release getter and release asset sources; milestone
burndown events; snippet, commit and epic discussions and the snippet and epic-note reactions; editing or deleting a
reply through the discussion endpoints (use the `Note` objects); building a diff `position`; clearing a milestone date
or a draft-note position (the SDK cannot send `null` for them); the priority of a label. Search is not covered: the
core's search calls return nothing, so it is deferred to a future GraphQL project. Left out of the new areas as out of
scope: wiki page notes, snippet notes, discussions and reactions, the instance-wide and public snippet lists, repository
storage moves and user-agent details of snippets, the dependency list exports, and the instance audit events (an
administrator API). Deliberately not offered, because GitLab deprecates them: epics, the vulnerabilities and
vulnerability findings REST APIs, the snippet `content` and `file_name` parameters, and offset paging of audit events
(lists walk by keyset, newest first); see [the
reasons](docs/guide/coverage.md#deliberately-not-offered-deprecated-by-gitlab).

Not covered in the CI/CD areas: instance-level variables and runners, and variables in a pipeline creation payload; `maskedAndHidden` on a
variable update (on create it goes through the payload overload); protected environments (Premium), deleting review
apps, stopping stale environments, the auto-stop setting on an environment update, and the tag flag of a deployment;
the inputs of a pipeline schedule and the list of its pipelines; runner registration and
verification, runner creation through the authenticated user, and getting or deleting a runner through a group;
reading a single file out of the artifacts and a job-token parameter on the artifact calls; the package ecosystems' own
endpoints (Maven, npm, NuGet and the others), the package and container registry protection rules, the registry event
endpoint and the instance-level package and registry lists.

The same goes for every area outside the table, such as the list of the uploads of a project and the administration of
an instance. Use
`gitlab-rest-client` for these. No area has been run against a real GitLab instance yet; see
[Live smoke tests](docs/guide/smoke-tests.md).

How the SDK compares with gitlab4j-api, and why an operation is not offered, is on [its own
page](docs/guide/gitlab4j-parity.md).

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

An expiry is set with `expiresAt(...)` and removed with `clearExpiresAt()`; the last call wins. Member roles (Ultimate)
are not covered.

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

`createWebhook` has two overloads, one for a URL and one for the core's payload, so a literal `null` needs a cast to
pick one; either overload rejects `null` with a `NullPointerException`.

### CI/CD variables

`project.createVariable("KEY", value)` sends a key and a value; the environment scope, masking, the type and
`maskedAndHidden` go through `createVariable(payload)`. A variable is addressed by its key and its environment scope:
`project.variable("KEY", "production")` narrows the lookup, and a `Variable` loaded that way refreshes, updates and
deletes itself in that scope. `update().environmentScope("staging")` sets a new scope; the old one is still the target.
`value()` is an `Optional` (a hidden variable has none) and is a secret; no `toString()` of the SDK prints it. A stale
scope (the variable was moved to another scope since it was loaded) may make `update()` change a different variable of
the same key: GitLab's documentation says that when the scope of the filter does not exist, the endpoint updates a
variable with the same key but a different scope. Call `refresh()` first when the scope may have changed. GitLab
documents a scope filter for group variables too, but the core does not offer it on the group operations, so
`GroupVariable` works by key alone (see [Conventions](#conventions)).

```java
Variable v = project.createVariable("DEPLOY_ENV", "staging");
v = v.update().value("production").masked(true).send();
Optional<Variable> prod = project.variable("DEPLOY_ENV", "production");
v.delete();
```

### Environments and deployments

`project.createEnvironment("staging")` sends a name; the URL, the tier and the auto-stop setting go through
`createEnvironment(payload)`. `update()` changes the fields it sets, and the `clear...()` methods
(`clearKubernetesNamespace()`, `clearFluxResourcePath()`, `clearClusterAgent()`) send JSON `null` through the core's
`OptionalNull`; the last call for a field wins. `environment.deployments()` filters by the environment name and loads
each deployment again, because GitLab lists a summary. GitLab accepts only `RUNNING`, `SUCCESS`, `FAILED` and `CANCELED`
when you create a deployment or set its status; any other `DeploymentStatus` throws `IllegalArgumentException` before a
request. `approve` and `reject` need Premium.

```java
Environment env = project.createEnvironment("staging");
env = env.update().tier(EnvironmentTier.STAGING).clearKubernetesNamespace().send();
Deployment d = project.createDeployment("staging", sha, "main", DeploymentStatus.RUNNING);
d = d.updateStatus(DeploymentStatus.SUCCESS);
env.stop();
```

### Pipeline schedules and triggers

`project.createPipelineSchedule("nightly", "main", "0 1 * * *")` sends a description, a ref and a cron expression as
given; the time zone and the active flag are set with `update()`. GitLab lists a smaller record than a single load, so
each listed schedule is loaded again and one that is gone is skipped. The variables of a schedule come from its own
response (GitLab has no call that lists them), so `variables()` does not reflect changes made since: use `refresh()`.
`lastPipeline()` loads the pipeline through the project, one request per call. `play()` starts a pipeline now. A
trigger's `description()` is an `Optional`, and its `token()` is a secret, as GitLab returns it: for a trigger owned by
someone else GitLab may return a shortened token (not verified).

```java
PipelineSchedule schedule = project.createPipelineSchedule("nightly", "main", "0 1 * * *");
schedule = schedule.update().cronTimezone("Europe/Warsaw").active(true).send();
schedule.addVariable("DEPLOY_ENV", "staging");
PipelineTrigger trigger = project.createPipelineTrigger("deploy hook");
String token = trigger.token();                                         // secret, do not log it
```

### Runners

`sdk.runner(7)` loads a runner by its global id. `project.runners()` and `group.runners()` list the runners, and GitLab
lists a smaller record than a single load, so each listed runner is loaded again (one request per item) and one that is
gone is skipped; each load is `GET /runners/{id}`, which GitLab may refuse with 403 for a token that is not an
administrator (not verified). `project.enableRunner(id)` enables an existing runner in the project, and
`runner.disable(project)` stops the project from using it (`enableRunner` then loads the runner by the id you passed);
`runner.delete()` removes it everywhere. `tagList()` returns the tags of the response and `update().tags(...)` takes a
list, which it copies. The registration token of a project or group and the authentication token of a runner come back
as a `RunnerToken`, whose `token()` is a secret and whose `toString()` never prints it. A runner type the core
does not know reads as `unrecognized` in `typeName()` (a limitation of the core's enum), and `type()` then throws
`IllegalArgumentException`.

```java
Runner runner = project.enableRunner(7);
runner = runner.update().paused(true).tags(List.of("docker", "linux")).send();
runner.disable(project);
String token = project.resetRunnerRegistrationToken().token();         // secret, do not log it
```

### Job artifacts

`job.downloadArtifacts(path)` writes the artifacts archive of a job to a file: GitLab's body is written to a temporary
file next to the target and moved onto it only when it is complete, so the target never holds a partial download (the
directory must exist, and an existing file is replaced). `job.downloadArtifacts(out)` copies it into your
`OutputStream`, which the SDK never closes and which may hold a partial body when the transfer fails.
`project.downloadArtifacts(ref, jobName, path)` (and the stream form) fetches the artifacts of the latest successful job
of that name on a ref; a ref such as `feature/x` is passed on unchanged. `job.artifactTree()` and
`job.artifactTree(path, recursive)` list the artifacts as the core's `GitLabCiJobArtifactEntry` records, the first page
when you call and the later ones as you consume the stream. `job.keepArtifacts()` returns the job GitLab answers with,
and `job.deleteArtifacts()` deletes them. The calls use the client's own authentication. Artifacts may contain secrets:
the SDK writes only where you say, so choose the target accordingly.

```java
Path archive = job.downloadArtifacts(Path.of("build", "artifacts.zip"));
job.artifactTree("reports", true).forEach(entry -> System.out.println(entry.path()));
Job kept = job.keepArtifacts();
long bytes = project.downloadArtifacts("main", "build", out);
```

### Packages and the container registry

`project.packages()` and `group.packages()` list packages (the filter overloads take the core's filters), and
`project.projectPackage(id)` loads one. The object is `ProjectPackage`, because `Package` would shadow
`java.lang.Package`. `package.type()` is a strict `PackageType` and `typeName()` the raw name; the status is the raw
string. `package.files()` lists the files lazily, and `file.download(path)` or `file.download(out)` write a file: the
path form never leaves a partial file, the stream form is never closed by the SDK. Package files and images can
contain secrets: the SDK writes only where you say. `project.registryRepositories()` (or
`registryRepositories(true)` to ask for the tags) and `group.registryRepositories()` list repositories;
`repository.tags()` loads each listed tag again, because the list holds a smaller record (one request per tag; a tag
that is gone is skipped). A tag name with a colon or a slash is passed to the core unchanged.
`repository.deleteTags(filter)` deletes by regular expression through the core's filter: check the expression, a
deleted tag cannot be recovered. A package or repository from a project list answers `project()` with that project;
one from a group list answers a handle of the project named by its `project_id`. Neither makes a request (see
[Conventions](#conventions)); `file.projectPackage()` returns the package a file belongs to.

```java
ProjectPackage pkg = project.projectPackage(5).orElseThrow();
pkg.files().forEach(file -> System.out.println(file.fileName()));
Path saved = pkg.files().findFirst().orElseThrow().download(Path.of("build", "lib.tgz"));
RegistryRepository repo = project.registryRepositories().findFirst().orElseThrow();
Optional<RegistryTag> tag = repo.tag("v1.0");
if (tag.isPresent()) {
    tag.get().delete();
}
```

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
same role-based rules without failing on a level the enum does not know. The v1 `Branch.protect()` stays the quick way
to protect an existing branch with the default rules.

```java
ProtectedBranch main = project.protectBranch("main", ProtectedAccess.MAINTAINER, ProtectedAccess.DEVELOPER);
main = main.allowForcePush(true);                                       // the answer of GitLab is the new object
project.protectTag("v*", ProtectedAccess.MAINTAINER);
group.protectBranch("release/*");                                       // Premium
```

### Tags

`project.tags()`, `project.tag(name)`, `project.createTag(name, ref)` (lightweight) and
`project.createTag(name, ref, message)` (annotated) work on the tags of a project; a tag name may contain slashes. A
listed tag is the same record as a loaded one, so it costs no extra request, but `tag.commit()` and `tag.release()` each
cost one more `GET`. `tag.signature()` returns the core's record, and `tag.delete()` removes the tag.

```java
Tag tag = project.createTag("v1.0", "main");
Optional<Commit> commit = tag.commit();                                 // one extra GET
Optional<Release> release = tag.release();                              // one extra GET, empty on 404
```

### Releases

A release is addressed by the name of its tag, which may contain slashes (`v1.0/rc`). `createRelease(tag)` makes a
release for an existing tag, `createRelease(tag, ref)` lets GitLab create the tag from a ref, and
`createRelease(payload)` takes the core's payload for the name, description, milestones, assets and release time.
`delete()` removes the release, not the tag. Group releases, release asset sources and the typed latest-release getter
are not covered; the `milestoneIds` and `assets` of a release stay reachable through the payload overload.
`createRelease` has two single-argument overloads (a tag name and the core's payload), so a literal `null` needs a cast.
The update builder takes a value, never `null`; an empty description is sent as given (see
[Conventions](#conventions)). `ReleaseLink.type()` is an
`Optional`, empty when GitLab sends no type; `typeName()` returns the raw value.

```java
Release release = project.createRelease("v1.0", "main");
release = release.update().name("One point oh").description("Notes").send();
ReleaseLink link = release.addLink("binary", "https://dl.example/app.zip");
link = link.update().type(ReleaseLinkType.PACKAGE).send();
Optional<Release> tagged = project.tag("v1.0").orElseThrow().release();
```

### Milestones

A milestone is addressed by its id (not its `iid`). Dates are `LocalDate`s: `dueDate()` and `startDate()` are empty when
GitLab sends none, and the update builder takes `LocalDate`s, sent as `yyyy-MM-dd`. `close()` and `reopen()` change the
state. `createMilestone(payload)` on a project or a group takes the core's payload for the description and the dates, so
a literal `null` needs a cast. `issues()` and `mergeRequests()` load each item with one more request as the stream
reaches it, through the project the item belongs to, so the items listed from a group milestone are loaded through their
own projects. Burndown events and clearing a date are not covered: the update builder takes a `LocalDate`, never `null`.
An empty description is sent as given (see [Conventions](#conventions)). `promote()` exists on project milestones only
and returns nothing.

```java
Milestone milestone = project.createMilestone("v1.0");
milestone = milestone.update().dueDate(LocalDate.of(2026, 12, 31)).send();
long open = milestone.issues().count();
milestone.promote();
GroupMilestone shared = group.milestones().findFirst().orElseThrow().close();
```

### Labels

A project `Label` and a group `GroupLabel` are addressed by name; `project.createLabel(name, color)` and
`group.createLabel(name, color)` create them, and a `GroupLabel` can also be subscribed to and unsubscribed from.
`label.promote()` turns a project label into a group label and returns the `GroupLabel`. It first sends
`GET /projects/{id}` to find the group from the project's namespace, then the promotion, so a project that cannot be
loaded promotes nothing; for a project in a user namespace the promotion is sent and GitLab's rejection passes through
as the core's exception. Label priority is not covered, and a group label has none.

```java
GroupLabel shared = group.createLabel("bug", "#d9534f").update().description("Something broke").send();
GroupLabel promoted = project.createLabel("ui", "#428bca").promote();
```

### Discussions

`issue.discussions()` and `mergeRequest.discussions()` list the threads lazily; `discussion(id)` loads one and
`startDiscussion(body)` opens one. A discussion carries its notes (`Note` or `MergeRequestNote`, so editing or deleting
a reply uses those objects), and `reply(body)`, `resolve()` and `unresolve()` change the thread. `resolve()` returns the
discussion GitLab answers with; GitLab only resolves merge request threads and resolvable issue threads, and its refusal
passes through. A diff comment is started with `mergeRequest.startDiscussion(payload)`, where the core's payload carries
the `position`; that method overloads `startDiscussion(body)`, so a literal `null` needs a cast. Commit, snippet and
epic discussions, editing or deleting a reply through the discussion endpoints, and building the `position`, are not
covered.

```java
IssueDiscussion thread = issue.startDiscussion("Can we reproduce this?");
Note reply = thread.reply("Yes, on 1.2");
thread = thread.resolve();
```

### Draft notes

A merge request has draft notes: `mergeRequest.draftNotes()` lists yours (one request), `draftNote(id)` loads one, and
`createDraftNote(note)` adds one. A `DraftNote` is changed with `update().note(text).send()`, published with `publish()`
or removed with `delete()`; `mergeRequest.publishDraftNotes()` publishes all of them. A published draft note is no
longer a draft, so `refresh()` then throws `IllegalStateException` (this assumes that GitLab answers 404 for it, which
has not been checked live). `createDraftNote` and `publishDraftNotes` have an overload for the core's payload (a reply,
a diff `position`, a reviewer state), so a literal `null` needs a cast. Building the `position` and clearing a position
are not covered.

```java
DraftNote draft = mergeRequest.createDraftNote("This loop looks off");
draft = draft.update().note("This loop is O(n^2)").send();
mergeRequest.publishDraftNotes();
```

### Emoji reactions

`issue.reactions()`, `note.reactions()`, `mergeRequest.reactions()` and `mergeRequestNote.reactions()` list the
reactions lazily, and `react(name)` adds one with an emoji name such as `thumbsup`. A `Reaction` has `id()`, `name()`,
`user()`, `createdAt()`, `refresh()` and `delete()`; it has no `issue()` or `mergeRequest()` to navigate back (see
[Conventions](#conventions)). Reacting twice with the same emoji is not checked first: GitLab's error passes through.
Snippet and epic-note reactions are not covered.

```java
Reaction reaction = issue.react("thumbsup");
reaction.delete();
```

### Wikis

`project.wiki()` and `group.wiki()` return a `Wiki` without a request; a group wiki needs Premium (GitLab is expected
to answer 403 or 404 on a lower tier). `wiki.all()` lists the pages: GitLab lists every page in one response without
its content, so each page is loaded again as the stream reaches it (one request per page, and a page that is gone is
skipped). `wiki.find(slug)` loads one page, empty when GitLab answers 404 (expected in some cases where the wiki is not
available); a 403 or the core's `GitLabTierException` passes through. A slug is passed as GitLab returns it, not
URL-encoded: `dir/page` stays as it is and the core encodes it. GitLab derives the slug from the title, so
`page.update().title(...).send()` targets the old slug and returns the page with its new one (expected; unverified
live). An update needs the title or the content: `send()` with only `format(...)` set throws `IllegalStateException`
before a request. `content()`, `format()` (a strict `WikiFormat`; `formatName()` gives the raw name) and `encoding()`
are `Optional`, and `frontMatter()` is an immutable copy of the parsed front matter, empty when there is none.
`wiki.uploadAttachment(fileName, bytes)` (or with a branch) uploads a file and returns the core's
`GitLabWikiAttachment`, whose `link().markdown()` goes into a page; GitLab offers no call that removes an upload. Page
content can hold anything the user wrote, so no SDK `toString()` prints it.

```java
Wiki wiki = project.wiki();
WikiPage page = wiki.create("guides/setup", "# Setup", WikiFormat.MARKDOWN);
page = page.update().content("# Setup\n\nRun the installer.").send();
wiki.find("guides/setup").ifPresent(found -> System.out.println(found.content().orElse("")));
GitLabWikiAttachment upload = wiki.uploadAttachment("diagram.png", bytes);
page.delete();
```

### Snippets

`project.snippets()` returns the `ProjectSnippets` of a project and `sdk.snippets()` the `PersonalSnippets` of the
signed-in user, both without a request. `all()` lists them (`GET /snippets` for the personal ones: the user's own
snippets), `find(id)` loads one (empty when GitLab answers 404, a 403 passes through; a personal `find` is expected to
load any snippet the user can see, which can be a project snippet: its `update()` and `delete()` then go to
`/snippets/{id}`, not verified for a project snippet), and `create(title, visibility, filePath, content)` creates a
snippet with one file; `create(payload)` passes the core's payload through for several files or a description. The SDK
always sends files as the `files` array, never the `content` and `file_name` parameters, which GitLab deprecates. A
`Snippet` (project) and a `PersonalSnippet` give `title()`, `description()`, `visibility()` (a strict
`SnippetVisibility`; `visibilityName()` gives the raw name), `author()`, `webUrl()` and `files()` (a `SnippetFile` per
entry: `path()` and `rawUrl()`; empty for a legacy single-file snippet). `content()` reads the snippet as text; for a
snippet with several files GitLab is expected to return one file only (which one is unchecked), so read each file with
`files()` and `fileContent(ref, path)`. The ref is any branch, tag or commit; usually the default branch, which the
`rawUrl()` of a file shows (as in `.../-/snippets/1/raw/main/add.rb`). Both reads decode binary content as UTF-8,
and a 404 is the core's `GitLabNotFoundException`, not empty. `update()` collects the title, description, visibility
and file changes (`addFile`, `updateFile`, `deleteFile`, `moveFile(previousPath, path)`), sent in call order by
`send()`. A `Snippet` returns its project from `project()`; a `PersonalSnippet` has no parent navigation. Snippet
content can hold anything the user wrote, so no SDK `toString()` prints it.

```java
Snippet snippet = project.snippets().create("setup", SnippetVisibility.PRIVATE, "setup.sh", "./configure");
snippet = snippet.update().addFile("docs/README.md", "# Setup").moveFile("setup.sh", "bin/setup.sh").send();
System.out.println(snippet.fileContent("main", "bin/setup.sh")); // branch name expected, unchecked live
sdk.snippets().all().forEach(mine -> System.out.println(mine.title()));
snippet.delete();
```

### Dependencies

`project.dependencies()` returns the `Dependencies` of a project without a request. It is an Ultimate feature: GitLab
is expected to answer 403 or 404 on a lower tier, and a client configured with a lower tier refuses the call with the
core's `GitLabTierException` before a request. `all()` lists the dependencies (`GET /projects/{id}/dependencies`), and
`all(first, more...)` only those of the given package managers (the core's
`GitLabProjectDependenciesListPackageManager`; a `null` throws before a request). GitLab has no call that loads one
dependency, so a `Dependency` is a value, with no `find` and no `refresh()`. It gives `name()`, `version()`,
`packageManager()` (the raw name, while the filter of `all(first, more...)` takes the core's enum, so a name the enum
does not know is still read), `filePath()`, `malware()` (an `Optional<Boolean>`), `vulnerabilities()` and
`licenses()` (the core's records, immutable, empty when GitLab sends none) and `project()`. The `vulnerabilities()` are
the advisories GitLab matched to the dependency, not SDK vulnerability objects: the vulnerabilities REST API is not
offered.

```java
project.dependencies().all(GitLabProjectDependenciesListPackageManager.NPM).forEach(dependency ->
        System.out.println(dependency.name() + " " + dependency.version().orElse("?")
                + ": " + dependency.vulnerabilities().size() + " advisories"));
```

### Audit events

`project.auditEvents()` and `group.auditEvents()` return the `AuditEvents` of a project or group without a request.
They are a Premium feature: GitLab is expected to answer 403 or 404 on a lower tier, and a client configured with a
lower tier refuses the call with the core's `GitLabTierException` before a request. `all()` lists the events and
`createdBetween(after, before)` those created in a range (both bounds inclusive per the GitLab docs, both required;
`after` later than `before` throws `IllegalArgumentException` before a request). The bounds are sent in the docs'
format `YYYY-MM-DDTHH:MM:SSZ`, because a fraction is not verified to be accepted: the range is widened to whole
seconds (`after` rounded down, `before` rounded up), so GitLab may return events up to one second outside the
requested bounds; filter on `createdAt()` for exactness. Lists walk by keyset pagination
(`order_by=id`, `sort=desc`), the only keyset order the GitLab REST docs list for audit events, so events come newest
first; offset paging, which GitLab deprecates, is never sent. `find(id)` loads one event, empty on a 404 (a 403 or
the core's `GitLabTierException` passes through). Per the GitLab docs a project Developer and a group Developer or
Maintainer see only the events of their own actions, and loading one group event needs the Owner role. An
`AuditEvent` gives `id()`, `authorId()`, `entityId()`, `entityType()`, `eventName()`, `createdAt()`, `details()` (an
immutable copy of what GitLab recorded, empty when absent) and `auditEvents()`; events do not change, so it has no
`refresh()`. The details can hold names, e-mail and IP addresses, so no SDK `toString()` prints them.

```java
Instant now = Instant.now();
project.auditEvents().createdBetween(now.minus(Duration.ofDays(1)), now).limit(20).forEach(event ->
        System.out.println(event.id() + " " + event.createdAt().map(Instant::toString).orElse("?")
                + " " + event.details().getOrDefault("change", "")));
```

## When the SDK does not cover a call

The SDK covers the common work, not every endpoint. For the rest, use `gitlab-rest-client` directly: the `GitLab` client
behind the SDK is yours, so there is no second client, no second credential and no second configuration. Create the
`GitLab` object yourself, hand it to `GitLabSdk.of(...)`, and keep it for the calls that have no SDK object. Both use
the same authentication, retries and rate-limit handling.

```java
try (GitLabClient client = GitLabClients.create(
        GitLabAuthenticationProviders.personalToken(System.getenv("GITLAB_TOKEN")), "my-app/1.0")) {
    GitLab gitLab = GitLab.of(client);
    GitLabSdk sdk = GitLabSdk.of(gitLab);

    Project project = sdk.project("my-group/my-project");
    project.createIssue("Found a bug");                                     // an SDK object

    // no SDK object for the list of uploads: use the client with the same project id
    for (GitLabMarkdownUploadAdmin upload : gitLab.markdownuploads().list(project.id()).all()) {
        System.out.println(upload.filename());
    }
}
```

The client names its resources as `gitLab.<resource>()`, an aggregator for projects, groups and the instance adds
`project()`, `group()` or `instance()`, and the Javadoc of each method names the HTTP request it sends; see the
[`gitlab-rest-client` documentation](https://github.com/piotr-janczukowicz/gitlab-rest-client). What the SDK returns
links back to it as well: every object has `response()`, the complete client record, which you can pass to the client
again. [What the SDK covers](docs/guide/coverage.md) lists every object, so you can check first.

## Conventions

All objects behave the same way, so you learn them once:

- Objects are **immutable**. An operation returns a new object; the old one keeps its old state.
- A **project or group** is a `String`: its numeric id or its path, as in the core (`my-group/my-project`). Every object
  returned by create, update or transfer of a project or group (`ProjectDraft.send()`, `GroupDraft.send()`,
  `ProjectUpdate.send()`, `GroupUpdate.send()`, `transfer(...)`, and also `fork(...)`) is addressed by its numeric id
  afterwards. Issues and merge requests are addressed by their `iid`, the number within the project.
- A **lookup** (`project.issue(42)`, `sdk.user(7)`, `project.file("README.md", "main")`) returns an `Optional`, empty
  when GitLab answers 404. A lookup in an area that can be unavailable (wikis, audit events) is empty
  when GitLab answers 404 (expected in some cases where the feature is not available); a 403 or the core's
  `GitLabTierException` passes through.
- A **list** is a lazy, single-use `Stream`. Pages are fetched as you consume it, so `limit(...)` stops further
  requests; a transport failure during iteration is an `UncheckedIOException`.
- **Several fields at once** go through `update()`: `issue.update().title("New").labels(List.of("bug")).send()` sends
  one request with only the fields you set. `send()` with nothing set throws `IllegalStateException`. Projects, groups,
  members, webhooks, variables, environments, pipeline schedules, schedule variables and triggers, runners, deploy keys,
  issues, merge requests, labels, group labels, milestones, releases, release links, draft notes, wiki pages, snippets
  and personal snippets have an `update()`.
- **State changes** that are not fields are methods on the object: `close()`, `reopen()`, `merge()`, `approve()`,
  `retry()`, `cancel()`, `protect()`.
- A `ProjectPackage` and a `RegistryRepository` find their project through the `project_id` of their response: from a
  project list `project()` is that project, from a group list it is a handle of the project named by the
  `project_id`; neither makes a request, as for `Deployment`, `Milestone` and `MergeRequest`. A response without a
  project id makes `project()` and the calls that need the project (`files()`, `tags()`, `delete()`, and the
  `download` and `delete` of their files and tags) throw `IllegalStateException`.
- A `Reaction`, a `Runner`, a `RunnerToken` and a `PersonalSnippet` are the objects **without parent navigation**. A
  `Wiki`, a `ProjectSnippets`, a `PersonalSnippets`, a `Dependencies` and an `AuditEvents` are area handles (one
  accessor per owner: `project.wiki()`, `group.wiki()`, `project.snippets()`, `sdk.snippets()`,
  `project.dependencies()`, `project.auditEvents()`, `group.auditEvents()`; the accessor and the `forProject(...)`,
  `forGroup(...)` and `forSdk(...)` factories make no request) with neither `response()` nor parent navigation;
  wiki pages return their wiki from `wiki()`, a `Snippet` and a `Dependency` return their project from `project()`, and
  an `AuditEvent` returns its `AuditEvents` from `auditEvents()`.
  One class serves issues, issue notes, merge requests and merge request notes, so a `Reaction` has no `issue()` or
  `mergeRequest()`; `refresh()` and `delete()` use the owner it was made on. One class serves instance, group and
  project runners, and a runner can be enabled in several projects, so a `Runner` has no `project()` or `group()`;
  `update()`, `delete()` and the other calls use the global runner id. A `RunnerToken` is a value: a token and
  its expiry.
- **Same-key CI/CD variables** of different environment scopes: a project `Variable` names its own scope in
  `refresh()`, `update()` and `delete()`; a `GroupVariable` cannot, because the core does not offer the scope filter
  on the group operations (GitLab documents one), so same-key group variables of different scopes cannot be told apart
  through the SDK.
- **Dates** are `LocalDate`s where GitLab means a date (milestone `dueDate()` and `startDate()`, member `expiresAt()`),
  and a milestone date is sent as `yyyy-MM-dd`. A milestone date is empty when GitLab sends none or a blank one, and a
  value that is not an ISO date throws `IllegalStateException`.
- `refresh()` reloads an object and throws `IllegalStateException` when it is gone. These have no `refresh()`: access
  requests (`AccessRequest`, `GroupAccessRequest`), `ProjectGroupLink`, `Approvals`, `Commit`, `RepositoryFile`,
  `PackageFile`, `CreatedDeployToken`, `GroupCreatedDeployToken`, `RunnerToken`, `AuditEvent` and `Dependency`. Where
  the API has a lookup, load them again through it (`project.file(path, ref)`, `mergeRequest.approvals()`); it has none
  for a single access request, package file or dependency, a commit and an audit event do not change, and the secret of
  a created token is returned only once.
- Where GitLab's list returns a smaller record than a single lookup (merge requests, pipelines, commits, the forks of a
  project, the projects and subgroups of a group, and the issues and merge requests of a milestone), the SDK loads each
  item with one more request as the stream reaches it. The Javadoc says so, and `limit(...)` bounds the cost. An item
  that is gone before it is loaded is skipped.
- A field that GitLab may leave out is an `Optional` (`issue.description()`, `mergeRequest.targetBranch()`,
  `releaseLink.type()`).
- A **state or status** is an enum (`issue.state()`, `pipeline.status()`, `job.status()`) and throws
  `IllegalArgumentException` for a value this SDK does not know yet. `issue.stateName()`, `pipeline.statusName()` and
  `job.statusName()` return the raw value GitLab sent, for that case. Each such enum has `value()`, the name as GitLab
  spells it, and `from(String)`.
- **Raw escape hatches** stand next to the typed accessors and never fail on a value the SDK does not know:
  `statusName()` (pipelines, jobs, deployments), `stateName()` (issues, merge requests, milestones), `typeName()`
  (release links, variables, schedule variables, runners, packages), `tierName()` (environments), `formatName()` (wiki
  pages), `visibilityName()` (snippets and personal snippets), `accessLevelValue()` (members), `scopeNames()` (deploy
  tokens) and the `*AccessValues()` methods (`pushAccessValues()`, `mergeAccessValues()`, `createAccessValues()` of
  protected branches and tags).
- **Text content** of a file is sent and read as UTF-8: `project.createFile(path, branch, content, message)` and
  `file.text()`. `file.bytes()` returns the exact bytes, including those of a binary file.
- **Clearing a field** is a separate `clear...()` method where the core's payloads distinguish an omitted field from a
  cleared one: the update builders use its tri-state `OptionalNull` to send JSON `null` (`clearExpiresAt()` on member
  updates, `clearKubernetesNamespace()`, `clearFluxResourcePath()` and `clearClusterAgent()` on environment updates).
  A `clear...()` call counts as a change, and for one field the last call wins. Elsewhere `null` cannot be sent, so a
  milestone date or a draft-note position cannot be cleared. That GitLab then clears the value is not verified live.
- **An empty string is sent as given** (`description("")` on an update builder, and `clearDescription()` on issue and
  project updates, which sends one); whether GitLab then clears the value is not verified against a live instance.
- **No deprecated GitLab operation or parameter** is offered: the SDK has no epics, vulnerabilities or vulnerability
  findings, never sends the `content` and `file_name` parameters of a snippet, and never pages audit events by offset.
  [What the SDK covers](docs/guide/coverage.md#deliberately-not-offered-deprecated-by-gitlab) gives the reasons.
- `IOException` and the client's `GitLabApiException` pass through unchanged.
- A call that needs a tier your instance lacks fails in the core's tier guard when you configured the tier on the
  client; the SDK adds no tier logic of its own.

## Good practices

[Conventions](#conventions) says what the SDK does. This is what to do with it.

- **Create the client once and close it once.** One client per credential, with one `GitLabSdk` on top: the client owns
  the transport and the rate-limit state, so share it for the life of the application, and do not open a client per
  call. Close it when the application stops. The SDK never closes a client you passed in.
- **Keep the object an operation returns.** The object you called it on is not updated; use the result, and call
  `refresh()` only when something else may have changed the object.
- **Use handles, load when you need data.** `sdk.project("my-group/my-project")` costs nothing; call `fetch()` only for
  the fields the handle does not hold, and read them with `response()`.
- **Always bound a list you read from a large project.** Add `limit(...)` or stop the stream early, and avoid `toList()`
  on a list that can grow, especially one that loads an item per request.
- **Prefer one request over many.** `update()` collects the fields you set and sends them together, instead of one call
  per field.
- **Decide what an empty lookup means.** Handle the `Optional` instead of calling `orElseThrow()` by habit, and catch
  `GitLabApiException` (not found, forbidden, rate limit) at the place where you can act on it.
- **Configure the client, not the SDK.** Retry, backoff and page size are client settings; see
  [Configure the client](#configure-the-client).
- **Give the token what the call needs.** A personal access token with the `api` scope covers everything the SDK does;
  `read_api` covers the reads. A project or group access token covers that project or group only.
- **Keep secrets out of logs.** `response()` of a job, of a pipeline and of a project can carry URLs and variables of
  your instance; log what you need, not the whole record. These are credentials: the secret of a new deploy token
  (`token()`, and the `token()` of its `response()`), the token you set on a webhook (`update().token(...)`), the token
  of a pipeline trigger (`PipelineTrigger.token()`), the registration and authentication tokens of runners
  (`RunnerToken.token()`, and the `runners_token` in the `response()` of a project or a group), and the value of a
  CI/CD variable (`Variable.value()`, `GroupVariable.value()`, `ScheduleVariable.value()`, `update().value(...)`). The
  artifacts of a job, the files of a package and the images of the container registry can carry credentials too. Store
  secrets in a secret store and never log them; the SDK's own `toString()` methods never print a secret value.
- **Try an integration on a throwaway project first.** The [live smoke tests](docs/guide/smoke-tests.md) show how.

## Configure the client

Authentication, retries, page size, the instance and its tier are the client's job, not the SDK's: every object calls
the client you pass to `GitLabSdk.of(...)` (a `GitLabClient`, a `GitLabCaller` or a `GitLab`), so one configuration
covers all of them. The SDK has no configuration of its own, and a call made through an object behaves exactly like the
same call on the client.

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

- [Self-managed GitLab and
  tiers](https://github.com/piotr-janczukowicz/gitlab-rest-client#self-managed-gitlab-and-tiers): the instance root, the
  flavor and the tier guard
- [Pagination](https://github.com/piotr-janczukowicz/gitlab-rest-client/blob/main/docs/guide/pagination.md): page size
  and the page limit; a list of the SDK is a lazy `Stream`, so `limit(...)` stops further page requests too
- [Good to know](https://github.com/piotr-janczukowicz/gitlab-rest-client/blob/main/docs/guide/good-to-know.md): names,
  newer GitLab data and the caveats of GitLab's description
- [Transport guide](https://github.com/piotr-janczukowicz/gitlab-client/blob/main/docs/guide/README.md): configuration,
  authentication, rate limiting and exception handling

## Packages

The entry points (`GitLabSdk`, `Project`, `Group`, `User`, `Me`) are in `io.github.piotr_janczukowicz.gitlab.sdk`. The
other objects are grouped by area in the sub-packages `account`, `approvals`, `audit`, `badges`, `ci`, `code`,
`dependencies`, `deploy`, `discussions`, `environments`, `issues`, `members`, `mergerequests`, `milestones`, `mirrors`,
`packages`, `protection`, `pushrules`, `reactions`, `registry`, `releases`, `repository`, `runners`, `schedules`,
`snippets`, `tokens`, `variables`, `webhooks` and `wikis`.

The `internal` package is not exported by the module and is not part of the API. The module system hides it only on the
module path; on the classpath it is a convention and not a security boundary.

## Disclaimer

The SDK is a thin layer over `gitlab-rest-client`, which is built on the OpenAPI description that GitLab publishes; the
caveats of that project apply here too.

**No area of this SDK has been run on a live GitLab instance yet: the smoke tests exist but have not been run. Not every
operation has been tested. This library is new and not yet proven in production. Verify the behavior of anything you
depend on before using it in production.**

This is an independent project. It is not made, endorsed or sponsored by GitLab Inc.; "GitLab" is a trademark of GitLab
Inc., used here only to say what the library talks to.
