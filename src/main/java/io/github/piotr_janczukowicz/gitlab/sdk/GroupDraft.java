package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.io.IOException;

/** Collects the fields of a new group and creates it in one request when {@link #send()} is called. */
public final class GroupDraft {

    private final GitLabSdk sdk;
    private final ManagementForms form = new ManagementForms();

    GroupDraft(final GitLabSdk sdk, final String name, final String path) {
        this.sdk = requireNonNull(sdk, "sdk");
        form.put("name", requireNotBlank(name, "name"));
        form.put("path", requireNotBlank(path, "path"));
    }

    /**
     * Sets the description.
     *
     * @param description the description
     * @return this draft
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public GroupDraft description(final String description) {
        form.put("description", requireNonNull(description, "description"));
        return this;
    }

    /**
     * Sets the visibility.
     *
     * @param visibility the visibility
     * @return this draft
     * @throws NullPointerException if {@code visibility} is {@code null}
     */
    public GroupDraft visibility(final Visibility visibility) {
        form.put("visibility", requireNonNull(visibility, "visibility").wireValue());
        return this;
    }

    /**
     * Makes the group a subgroup of another group.
     *
     * @param parentId the numeric id of the parent group
     * @return this draft
     */
    public GroupDraft parentId(final long parentId) {
        form.put("parent_id", String.valueOf(parentId));
        return this;
    }

    /**
     * Creates the group ({@code POST /groups}) and loads it again ({@code GET /groups/{id}}), because GitLab answers
     * the creation with a shorter record.
     *
     * @return the created group as GitLab returns it afterwards
     * @throws IllegalStateException if the group is gone when it is loaded again
     * @throws IOException if the transport fails
     */
    public Group send() throws IOException {
        final String created = String.valueOf(
                requireNonNull(sdk.gitLab().groups().create(form.build()).id(), "id"));
        return sdk.group(created).fetch().orElseThrow(() -> new IllegalStateException("Group " + created + " is gone"));
    }
}
