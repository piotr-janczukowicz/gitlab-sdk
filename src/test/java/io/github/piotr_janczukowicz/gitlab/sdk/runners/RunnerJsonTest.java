package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerManager;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetails;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RunnerJsonTest {

    private static final String REAL = """
            {"id":1,"tag_list":%s,"run_untagged":true,"locked":false,"maximum_timeout":3600,
            "access_level":"not_protected","runner_type":"project_type","online":true,"paused":false,
            "active":true,"status":"online"}""";

    private static final String MANAGERS = """
            [{"id": 1, "system_id": "s_89e5e9956577", "version": "16.11.1", "revision": "535ced5f",
              "platform": "linux", "architecture": "amd64", "created_at": "2026-11-13T10:00:00.000Z",
              "contacted_at": "2026-11-13T10:05:00.000+01:00", "ip_address": "127.0.0.1", "status": "online",
              "job_execution_status": "active"},
             {"id": 2, "system_id": "runner-2", "version": null, "revision": null, "platform": null,
              "architecture": null, "created_at": "2026-11-13T10:00:00.000Z", "contacted_at": null,
              "ip_address": null, "status": "never_contacted", "job_execution_status": "idle"}]""";

    private Runner decode(final String tags) {
        final byte[] json = REAL.formatted(tags).getBytes(StandardCharsets.UTF_8);
        final GitLabCiRunnerDetails details = GitLabJsonCodecs.defaults().decode(json, GitLabCiRunnerDetails.class);
        return new Runner(GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS)), details);
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenTagListIsAnArray() {
        final Runner sut = decode("[\"a\",\"b\"]");

        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(1L);
            softly.assertThat(sut.tagList()).containsExactly("a", "b");
            softly.assertThat(sut.type()).contains(RunnerType.PROJECT_TYPE);
            softly.assertThat(sut.isPaused()).isFalse();
            softly.assertThat(sut.isOnline()).isTrue();
            softly.assertThat(sut.isActive()).isTrue();
            softly.assertThat(sut.status()).isEqualTo("online");
            softly.assertThat(sut.response().runUntagged()).isTrue();
            softly.assertThat(sut.response().locked()).isFalse();
            softly.assertThat(sut.response().maximumTimeout()).isEqualTo(3600);
        });
    }

    @Test
    void tagList_shouldBeEmpty_whenRealShapedJsonHasEmptyArray() {
        assertThat(decode("[]").tagList()).isEmpty();
    }

    @Test
    void managers_shouldReturnDecodedManagers_whenRealShapedJsonListsTwo() throws IOException {
        final GitLabCiRunnerManager[] decoded = GitLabJsonCodecs.defaults()
                .decode(MANAGERS.getBytes(StandardCharsets.UTF_8), GitLabCiRunnerManager[].class);
        final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitLab.runners().instance().listManagers(1L)).thenReturn(List.of(decoded));
        final byte[] json = REAL.formatted("[]").getBytes(StandardCharsets.UTF_8);
        final Runner sut =
                new Runner(GitLabSdk.of(gitLab), GitLabJsonCodecs.defaults().decode(json, GitLabCiRunnerDetails.class));

        final List<GitLabCiRunnerManager> managers = sut.managers();

        assertSoftly(softly -> {
            softly.assertThat(managers).hasSize(2);
            softly.assertThat(managers.getFirst().systemId()).isEqualTo("s_89e5e9956577");
            softly.assertThat(managers.getFirst().contactedAt()).isEqualTo(Instant.parse("2026-11-13T09:05:00Z"));
            softly.assertThat(managers.getLast().contactedAt()).isNull();
            softly.assertThat(managers.getLast().status()).isEqualTo("never_contacted");
        });
    }
}
