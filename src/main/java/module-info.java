/** The GitLab SDK: an object layer over the GitLab REST client. */
module io.github.piotr_janczukowicz.gitlabsdk {
    requires transitive io.github.piotr_janczukowicz.gitlabrestclient;
    requires common.core;
    requires org.jspecify;

    exports io.github.piotr_janczukowicz.gitlab.sdk;
    exports io.github.piotr_janczukowicz.gitlab.sdk.account;
    exports io.github.piotr_janczukowicz.gitlab.sdk.audit;
    exports io.github.piotr_janczukowicz.gitlab.sdk.ci;
    exports io.github.piotr_janczukowicz.gitlab.sdk.code;
    exports io.github.piotr_janczukowicz.gitlab.sdk.dependencies;
    exports io.github.piotr_janczukowicz.gitlab.sdk.deploy;
    exports io.github.piotr_janczukowicz.gitlab.sdk.discussions;
    exports io.github.piotr_janczukowicz.gitlab.sdk.environments;
    exports io.github.piotr_janczukowicz.gitlab.sdk.issues;
    exports io.github.piotr_janczukowicz.gitlab.sdk.members;
    exports io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;
    exports io.github.piotr_janczukowicz.gitlab.sdk.milestones;
    exports io.github.piotr_janczukowicz.gitlab.sdk.packages;
    exports io.github.piotr_janczukowicz.gitlab.sdk.protection;
    exports io.github.piotr_janczukowicz.gitlab.sdk.reactions;
    exports io.github.piotr_janczukowicz.gitlab.sdk.repository;
    exports io.github.piotr_janczukowicz.gitlab.sdk.registry;
    exports io.github.piotr_janczukowicz.gitlab.sdk.releases;
    exports io.github.piotr_janczukowicz.gitlab.sdk.runners;
    exports io.github.piotr_janczukowicz.gitlab.sdk.schedules;
    exports io.github.piotr_janczukowicz.gitlab.sdk.snippets;
    exports io.github.piotr_janczukowicz.gitlab.sdk.variables;
    exports io.github.piotr_janczukowicz.gitlab.sdk.webhooks;
    exports io.github.piotr_janczukowicz.gitlab.sdk.wikis;
}
