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
     * Promotes the label to a label of the project's group ({@code PUT /projects/{id}/labels/{name}/promote}). The
     * core's answer does not name the group, so the group of the returned label is derived from the project's
     * namespace: after the promotion this sends {@code GET /projects/{id}} (one request, so that the namespace is
     * current even for a project handle that was never loaded) and addresses the group by the numeric id of that
     * namespace, or by its full path when the id is absent. No request goes out before the promotion. GitLab rejects
     * the promotion of a label of a project in a user namespace; the core's exception passes through and no group is
     * invented. This object is not changed.
     *
     * @return the group label, which belongs to the project's group
     * @throws IllegalStateException if the project is gone when it is loaded after the promotion, or its namespace is
     *     not a group
     * @throws IOException if the transport fails
     */
    public GroupLabel promote() throws IOException {
        final GitLabGroupLabel promoted = gitLab().labels().promoteByName(project.id(), name());
        final GitLabNamespaceBasic namespace = project.refresh().response().namespace();
        return new GroupLabel(Internals.group(project, groupId(namespace)), promoted);
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

    private String groupId(final @Nullable GitLabNamespaceBasic namespace) {
        if (namespace == null || namespace.kind() != null && !"group".equalsIgnoreCase(namespace.kind())) {
            throw new IllegalStateException("The namespace of project " + project.id() + " is not a group");
        }
        if (namespace.id() != null) {
            return String.valueOf(namespace.id());
        }
        if (namespace.fullPath() != null && !namespace.fullPath().isBlank()) {
            return namespace.fullPath();
        }
        throw new IllegalStateException("The namespace of project " + project.id() + " names no group");
    }

    private GitLab gitLab() {
        return Internals.gitLab(project);
    }
}
