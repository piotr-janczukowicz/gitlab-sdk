package io.github.piotr_janczukowicz.gitlab.sdk.runners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.runners.GitLabCiRunnerDetails;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@UnitTest
class RunnerJsonTest {

    private static final String REAL = """
            {"id":1,"tag_list":%s,"run_untagged":true,"locked":false,"maximum_timeout":3600,
            "access_level":"not_protected","runner_type":"project_type","online":true,"paused":false,
            "active":true,"status":"online"}""";

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
}
