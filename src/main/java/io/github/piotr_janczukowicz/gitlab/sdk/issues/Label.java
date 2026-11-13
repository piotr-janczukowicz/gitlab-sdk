package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabGroupLabel;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabNamespaceBasic;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabel;
import io.github.piotr_janczukowicz.gitlab.api.labels.GitLabProjectLabelsUpdateByNamePayload;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Internals;
import java.io.IOException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A label of a {@link Project}, as GitLab returned it. A label is addressed by its name. The object is immutable, and
 * {@link #response()} gives the complete response for every field the accessors here do not cover.
 */
public final class Label {

    private final Project project;
    private final GitLabProjectLabel response;

    /**
     * Wraps a response. Applications normally get this object from the object that owns it; call the constructor only
     * to wrap a response that was loaded some other way.
     *
     * @param project the project the object belongs to
     * @param response the response record the object is built from
     * @throws NullPointerException if an argument is {@code null}
     */
    public Label(final Project project, final GitLabProjectLabel response) {
        this.project = requireNonNull(project, "project");
        this.response = requireNonNull(response, "response");
    }

    /**
     * Returns the project the label belongs to.
     *
     * @return the project
     */
    public Project project() {
        return project;
    }

    /**
     * Returns the label name.
     *
     * @return the name
     */
    public String name() {
        return requireNonNull(response.name(), "name");
    }

    /**
     * Returns the label color.
     *
     * @return the color, for example {@code #FF0000}
     */
    public String color() {
        return requireNonNull(response.color(), "color");
    }

    /**
     * Returns the label description.
     *
     * @return the description, or empty when GitLab does not return one
     */
    public Optional<String> description() {
        return Optional.ofNullable(response.description());
    }

    /**
     * Starts a change of several fields at once. Nothing is sent until {@link LabelUpdate#send()}.
     *
     * @return the update builder
     */
    public LabelUpdate update() {
        return new LabelUpdate(this);
    }

    /**
     * Promotes the label to a label of the project's group. The core's answer does not name the group, and the
     * promotion cannot be repeated, so the group is resolved first: this sends {@code GET /projects/{id}} (so that the
     * namespace is current even for a project handle that was never loaded), then {@code PUT
     * /projects/{id}/labels/{name}/promote}. The group of the returned label is addressed by the numeric id of the
     * project's namespace, or by its full path when the id is absent; a namespace without a kind but with an id is
     * treated as a group. If the project cannot be loaded (transport failure, or it is gone) nothing was promoted. A
     * project in a user namespace is not rejected by the SDK: the promotion is sent and GitLab's rejection passes
     * through as the core's exception, and no group is invented. Only if GitLab unexpectedly promotes the label while
     * no group can be derived is an {@link IllegalStateException} thrown after the promotion; that is practically
     * unreachable, and the label can then be found through its group. This object is not changed.
     *
     * @return the group label, which belongs to the project's group
     * @throws IllegalStateException if the project is gone (nothing was promoted), or if the label was promoted but no
     *     group could be derived from the project's namespace
     * @throws IOException if the transport fails
     */
    public GroupLabel promote() throws IOException {
        final @Nullable String groupId = groupId(project.refresh().response().namespace());
        final GitLabGroupLabel promoted = gitLab().labels().promoteByName(project.id(), name());
        if (groupId == null) {
            throw new IllegalStateException("Label " + name() + " of project " + project.id()
                    + " was promoted, but no group could be derived from the project's namespace");
        }
        return new GroupLabel(Internals.group(project, groupId), promoted);
    }

    /**
     * Deletes the label ({@code DELETE /projects/{id}/labels/{name}}).
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        gitLab().labels().deleteByName(project.id(), name());
    }

    /**
     * Loads the label again ({@code GET /projects/{id}/labels/{name}}). This object is not changed.
     *
     * @return the label as GitLab holds it now
     * @throws IllegalStateException if GitLab no longer has the label
     * @throws IOException if the transport fails
     */
    public Label refresh() throws IOException {
        return gitLab().labels()
                .get(project.id(), name())
                .map(reloaded -> new Label(project, reloaded))
                .orElseThrow(() -> new IllegalStateException("Label " + name() + " is gone"));
    }

    /**
     * Returns the complete response this object was built from.
     *
     * @return the response record
     */
    public GitLabProjectLabel response() {
        return response;
    }

    Label apply(final GitLabProjectLabelsUpdateByNamePayload payload) throws IOException {
        return new Label(project, gitLab().labels().updateByName(project.id(), name(), payload));
    }

    private static @Nullable String groupId(final @Nullable GitLabNamespaceBasic namespace) {
        if (namespace == null || namespace.kind() != null && !"group".equalsIgnoreCase(namespace.kind())) {
            return null;
        }
        if (namespace.id() != null) {
            return String.valueOf(namespace.id());
        }
        final @Nullable String path = namespace.fullPath();
        return path == null || path.isBlank() ? null : path;
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
