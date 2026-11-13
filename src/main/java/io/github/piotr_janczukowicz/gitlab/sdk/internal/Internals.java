package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Gives the object packages of the SDK access to the wrapped {@link GitLab} client without exposing it in the public
 * API. This package is not exported by the module, so applications cannot name it. {@code GitLabSdk} registers the
 * access when its class is initialized, which always happens before an instance exists. The package is hidden only when
 * the module runs on the module path; on the classpath the accessors are public, so the encapsulation is a convention
 * and not a security boundary, and code in the same JVM is trusted.
 */
public final class Internals {

    private static final AtomicReference<@Nullable Function<Project, GitLab>> PROJECT_ACCESS = new AtomicReference<>();
    private static final AtomicReference<@Nullable Function<Group, GitLab>> GROUP_ACCESS = new AtomicReference<>();
    private static final AtomicReference<@Nullable Function<GitLabSdk, GitLab>> SDK_ACCESS = new AtomicReference<>();
    private static final AtomicReference<@Nullable BiFunction<Project, String, Project>> SIBLING_ACCESS =
            new AtomicReference<>();

    private Internals() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers how to reach the client of a project. Called by {@link Project} when it is initialized. Only the first
     * registration counts, so code that calls this later cannot redirect the objects to another client.
     *
     * @param access the function that returns the client of a project
     */
    public static void registerProjectAccess(final Function<Project, GitLab> access) {
        PROJECT_ACCESS.compareAndSet(null, access);
    }

    /**
     * Registers how to make a handle to another project of the same SDK. Called by {@link Project} when it is
     * initialized. Only the first registration counts.
     *
     * @param access the function that returns a handle to the project with the given id, on the SDK of a project
     */
    public static void registerSiblingAccess(final BiFunction<Project, String, Project> access) {
        SIBLING_ACCESS.compareAndSet(null, access);
    }

    /**
     * Returns a handle to another project on the same SDK as a project, for example the fork a pipeline ran in. No
     * request is made.
     *
     * @param project a project of the SDK
     * @param id the numeric id or plain full path of the other project
     * @return the handle
     * @throws NullPointerException if the sibling access was never registered
     */
    public static Project project(final Project project, final String id) {
        return requireNonNull(SIBLING_ACCESS.get(), "siblingAccess").apply(project, id);
    }

    /**
     * Registers how to reach the client of a group. Called by {@link Group} when it is initialized. Only the first
     * registration counts, so code that calls this later cannot redirect the objects to another client.
     *
     * @param access the function that returns the client of a group
     */
    public static void registerGroupAccess(final Function<Group, GitLab> access) {
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
     * Returns the client behind a project.
     *
     * @param project the project
     * @return the wrapped client
     * @throws NullPointerException if the project access was never registered
     */
    public static GitLab gitLab(final Project project) {
        return requireNonNull(PROJECT_ACCESS.get(), "projectAccess").apply(project);
    }

    /**
     * Returns the client behind a group.
     *
     * @param group the group
     * @return the wrapped client
     * @throws NullPointerException if the group access was never registered
     */
    public static GitLab gitLab(final Group group) {
        return requireNonNull(GROUP_ACCESS.get(), "groupAccess").apply(group);
    }
}
