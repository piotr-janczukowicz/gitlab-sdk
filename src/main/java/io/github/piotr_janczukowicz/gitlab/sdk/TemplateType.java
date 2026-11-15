package io.github.piotr_janczukowicz.gitlab.sdk;

import io.github.piotr_janczukowicz.gitlab.api.projecttemplates.GitLabProjectProjectTemplatesGetType;
import io.github.piotr_janczukowicz.gitlab.api.projecttemplates.GitLabProjectProjectTemplatesListType;
import java.util.Locale;

/**
 * The kind of template that {@link Project#templates(TemplateType)} lists and {@link Project#template(TemplateType,
 * String)} loads: exactly the six kinds the core accepts for both calls.
 */
public enum TemplateType {
    /** Dockerfile templates. */
    DOCKERFILES,
    /** Gitignore templates. */
    GITIGNORES,
    /** CI/CD YAML templates. */
    GITLAB_CI_YMLS,
    /** License templates. */
    LICENSES,
    /** Issue templates. */
    ISSUES,
    /** Merge request templates. */
    MERGE_REQUESTS;

    /**
     * Returns the name of the kind as GitLab spells it in the path, such as {@code gitlab_ci_ymls}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    GitLabProjectProjectTemplatesListType toListType() {
        return GitLabProjectProjectTemplatesListType.fromWire(value());
    }

    GitLabProjectProjectTemplatesGetType toGetType() {
        return GitLabProjectProjectTemplatesGetType.fromWire(value());
    }
}
