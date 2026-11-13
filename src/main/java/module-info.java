module io.github.piotr_janczukowicz.gitlabsdk {
    requires transitive io.github.piotr_janczukowicz.gitlabrestclient;
    requires common.core;
    requires org.jspecify;

    exports io.github.piotr_janczukowicz.gitlab.sdk;
    exports io.github.piotr_janczukowicz.gitlab.sdk.ci;
    exports io.github.piotr_janczukowicz.gitlab.sdk.issues;
    exports io.github.piotr_janczukowicz.gitlab.sdk.mergerequests;
}
