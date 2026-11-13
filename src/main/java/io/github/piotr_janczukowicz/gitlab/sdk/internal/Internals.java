package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.members.GroupMember;
import io.github.piotr_janczukowicz.gitlab.sdk.members.Member;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * Gives the object packages of the SDK access to the wrapped {@link GitLab} client, and to the SDK that made an object,
 * without exposing either in the public API. This package is not exported by the module, so applications cannot name
 * it. {@link GitLabSdk}, {@link Project}, {@link Group}, {@link Member} and {@link GroupMember} each register their
 * access when their class is initialized, which always happens before an instance exists. Only the first registration
 * of each access counts, so code that registers later cannot redirect the objects. A handle to another project or group
 * of the same SDK is made through {@code sdk(owner).project(id)} or {@code sdk(owner).group(id)}. The package is hidden
 * only when the module runs on the module path; on the classpath the accessors are public, so the encapsulation is a
 * convention and not a security boundary, and code in the same JVM is trusted.
 */
public final class Internals {

    private static final AtomicReference<@Nullable Function<Project, GitLabSdk>> PROJECT_ACCESS =
            new AtomicReference<>();
    private static final AtomicReference<@Nullable Function<Group, GitLabSdk>> GROUP_ACCESS = new AtomicReference<>();
    private static final AtomicReference<@Nullable Function<GitLabSdk, GitLab>> SDK_ACCESS = new AtomicReference<>();
    private static final AtomicReference<@Nullable UnaryOperator<Member>> ALL_MEMBERS_ACCESS = new AtomicReference<>();
    private static final AtomicReference<@Nullable UnaryOperator<GroupMember>> ALL_GROUP_MEMBERS_ACCESS =
            new AtomicReference<>();

    private Internals() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers how to reach the SDK of a project. Called by {@link Project} when it is initialized. Only the first
     * registration counts, so code that calls this later cannot redirect the objects to another SDK.
     *
     * @param access the function that returns the SDK a project was made by
     */
    public static void registerProjectAccess(final Function<Project, GitLabSdk> access) {
        PROJECT_ACCESS.compareAndSet(null, access);
    }

    /**
     * Registers how to reach the SDK of a group. Called by {@link Group} when it is initialized. Only the first
     * registration counts, so code that calls this later cannot redirect the objects to another SDK.
     *
     * @param access the function that returns the SDK a group was made by
     */
    public static void registerGroupAccess(final Function<Group, GitLabSdk> access) {
        GROUP_ACCESS.compareAndSet(null, access);
    }

    /**
     * Registers how to reach the client of an SDK. Called by {@link GitLabSdk} when it is initialized. Only the first
     * registration counts, so code that calls this later cannot redirect the objects to another client.
     *
     * @param access the function that returns the client of an SDK
     */
    public static void registerSdkAccess(final Function<GitLabSdk, GitLab> access) {
        SDK_ACCESS.compareAndSet(null, access);
    }

    /**
     * Returns the SDK a project was made by, for example to make a handle to the fork a pipeline ran in with
     * {@code sdk(project).project(id)}. No request is made.
     *
     * @param project the project
     * @return the SDK of the project
     * @throws NullPointerException if the project access was never registered
     */
    public static GitLabSdk sdk(final Project project) {
        return requireNonNull(PROJECT_ACCESS.get(), "projectAccess").apply(project);
    }

    /**
     * Returns the SDK a group was made by, for example to make a handle to the project an item of a group milestone
     * belongs to with {@code sdk(group).project(id)}. No request is made.
     *
     * @param group the group
     * @return the SDK of the group
     * @throws NullPointerException if the group access was never registered
     */
    public static GitLabSdk sdk(final Group group) {
        return requireNonNull(GROUP_ACCESS.get(), "groupAccess").apply(group);
    }

    /**
     * Returns the client behind an SDK.
     *
     * @param sdk the SDK
     * @return the wrapped client
     * @throws NullPointerException if the SDK access was never registered
     */
    public static GitLab gitLab(final GitLabSdk sdk) {
        return requireNonNull(SDK_ACCESS.get(), "sdkAccess").apply(sdk);
    }

    /**
     * Returns the client behind a project: the client of its SDK.
     *
     * @param project the project
     * @return the wrapped client
     * @throws NullPointerException if the project or SDK access was never registered
     */
    public static GitLab gitLab(final Project project) {
        return gitLab(sdk(project));
    }

    /**
     * Returns the client behind a group: the client of its SDK.
     *
     * @param group the group
     * @return the wrapped client
     * @throws NullPointerException if the group or SDK access was never registered
     */
    public static GitLab gitLab(final Group group) {
        return gitLab(sdk(group));
    }

    /**
     * Registers how to mark a project member as listed by {@code GET /projects/{id}/members/all}. Called by
     * {@link Member} when it is initialized. Only the first registration counts.
     *
     * @param access the function that returns a copy of a member marked as listed with the inherited members
     */
    public static void registerAllMembersAccess(final UnaryOperator<Member> access) {
        ALL_MEMBERS_ACCESS.compareAndSet(null, access);
    }

    /**
     * Returns a copy of a project member marked as listed with the inherited members, so that its {@code refresh()}
     * loads it through {@code GET /projects/{id}/members/all/{user_id}}. No request is made. Pass a member built with
     * its public constructor, which initializes {@link Member} and so registers the access.
     *
     * @param member the member as listed
     * @return the marked copy
     * @throws NullPointerException if the access was never registered
     */
    public static Member fromAllMembers(final Member member) {
        return requireNonNull(ALL_MEMBERS_ACCESS.get(), "allMembersAccess").apply(member);
    }

    /**
     * Registers how to mark a group member as listed by {@code GET /groups/{id}/members/all}. Called by
     * {@link GroupMember} when it is initialized. Only the first registration counts.
     *
     * @param access the function that returns a copy of a member marked as listed with the inherited members
     */
    public static void registerAllGroupMembersAccess(final UnaryOperator<GroupMember> access) {
        ALL_GROUP_MEMBERS_ACCESS.compareAndSet(null, access);
    }

    /**
     * Returns a copy of a group member marked as listed with the inherited members, so that its {@code refresh()} loads
     * it through {@code GET /groups/{id}/members/all/{user_id}}. No request is made. Pass a member built with its
     * public constructor, which initializes {@link GroupMember} and so registers the access.
     *
     * @param member the member as listed
     * @return the marked copy
     * @throws NullPointerException if the access was never registered
     */
    public static GroupMember fromAllGroupMembers(final GroupMember member) {
        return requireNonNull(ALL_GROUP_MEMBERS_ACCESS.get(), "allGroupMembersAccess")
                .apply(member);
    }
}
