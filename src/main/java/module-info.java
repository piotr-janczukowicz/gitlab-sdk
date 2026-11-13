module io.github.piotr_janczukowicz.gitlabsdk {
    requires transitive io.github.piotr_janczukowicz.gitlabrestclient;
    requires common.core;
    requires org.jspecify;

    exports io.github.piotr_janczukowicz.gitlab.sdk;
    exports io.github.piotr_janczukowicz.gitlab.sdk.ci;
    exports io.github.piotr_janczukowicz.gitlab.sdk.code;
    exports io.github.piotr_janczukowicz.gitlab.sdk.deploy;
    exports io.github.piotr_janczukowicz.gitlab.sdk.issues;
    exports io.github.piotr_janczukowicz.gitlab.sdk.members;
    exports io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;
    exports io.github.piotr_janczukowicz.gitlab.sdk.protection;
    exports io.github.piotr_janczukowicz.gitlab.sdk.webhooks;
}
