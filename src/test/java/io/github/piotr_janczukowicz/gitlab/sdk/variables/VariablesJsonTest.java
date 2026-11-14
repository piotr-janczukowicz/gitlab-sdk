package io.github.piotr_janczukowicz.gitlab.sdk.variables;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiVariable;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@UnitTest
class VariablesJsonTest {

    private static final String VISIBLE = """
            {"variable_type": "env_var", "key": "DEPLOY_URL", "value": "https://example.com", "hidden": false,
             "protected": true, "masked": true, "raw": false, "environment_scope": "production",
             "description": null}""";

    private static final String HIDDEN = """
            {"variable_type": "file", "key": "SSH_KEY", "hidden": true, "protected": false, "masked": true,
             "raw": true, "environment_scope": "*", "description": "Deploy key"}""";

    private final GitLabSdk sdk = GitLabSdk.of(mock(GitLab.class, RETURNS_DEEP_STUBS));

    @Test
    void accessors_shouldReadRealShapedJson_whenProjectVariableIsScoped() {
        final GitLabCiVariable decoded = decode(VISIBLE);

        final Variable sut = new Variable(sdk.project("my-group/demo"), decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.key()).isEqualTo("DEPLOY_URL");
            softly.assertThat(sut.value()).contains("https://example.com");
            softly.assertThat(sut.type()).isEqualTo(VariableType.ENV_VAR);
            softly.assertThat(sut.environmentScope()).contains("production");
            softly.assertThat(sut.isProtected()).isTrue();
            softly.assertThat(sut.isMasked()).isTrue();
            softly.assertThat(sut.isRaw()).isFalse();
            softly.assertThat(sut.isHidden()).isFalse();
            softly.assertThat(sut.description()).isEmpty();
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenProjectVariableIsHiddenWithoutValue() {
        final GitLabCiVariable decoded = decode(HIDDEN);

        final Variable sut = new Variable(sdk.project("my-group/demo"), decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.value()).isEmpty();
            softly.assertThat(sut.isHidden()).isTrue();
            softly.assertThat(sut.isRaw()).isTrue();
            softly.assertThat(sut.isProtected()).isFalse();
            softly.assertThat(sut.type()).isEqualTo(VariableType.FILE);
            softly.assertThat(sut.environmentScope()).contains("*");
            softly.assertThat(sut.description()).contains("Deploy key");
        });
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenGroupVariableIsHiddenWithoutValue() {
        final GitLabCiVariable decoded = decode(HIDDEN);

        final GroupVariable sut = new GroupVariable(sdk.group("my-group"), decoded);

        assertSoftly(softly -> {
            softly.assertThat(sut.key()).isEqualTo("SSH_KEY");
            softly.assertThat(sut.value()).isEmpty();
            softly.assertThat(sut.isHidden()).isTrue();
            softly.assertThat(sut.isMasked()).isTrue();
            softly.assertThat(sut.typeName()).isEqualTo("file");
            softly.assertThat(sut.environmentScope()).contains("*");
        });
    }

    private static GitLabCiVariable decode(final String json) {
        return GitLabJsonCodecs.defaults().decode(json.getBytes(StandardCharsets.UTF_8), GitLabCiVariable.class);
    }
}
