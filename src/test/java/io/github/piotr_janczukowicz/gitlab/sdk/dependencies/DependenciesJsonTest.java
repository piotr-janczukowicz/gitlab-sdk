package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependency;
import io.github.piotr_janczukowicz.gitlab.json.GitLabJsonCodecs;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

@UnitTest
class DependenciesJsonTest {

    private static final String ID = "my-group/my-project";

    /**
     * The list of the REST docs ({@code GET /projects/:id/dependencies}), shaped after the docs example (the values are
     * illustrative, not verbatim): the first dependency has two vulnerabilities with integer ids and a license, the
     * second has an empty {@code vulnerabilities} array and a license without {@code url}. The second dependency is an
     * addition, because the docs example has neither of those.
     */
    private static final String LIST = """
            [
              {
                "name": "rails",
                "version": "5.0.1",
                "package_manager": "bundler",
                "dependency_file_path": "Gemfile.lock",
                "vulnerabilities": [
                  {
                    "name": "DoS Vulnerability in Action View",
                    "severity": "unknown",
                    "id": 144827,
                    "url": "https://gitlab.example.com/group/project/-/security/vulnerabilities/144827"
                  },
                  {
                    "name": "Information Exposure in Action View",
                    "severity": "unknown",
                    "id": 144828,
                    "url": "https://gitlab.example.com/group/project/-/security/vulnerabilities/144828"
                  }
                ],
                "licenses": [
                  {
                    "spdx_identifier": "BSD-3-Clause",
                    "name": "BSD 3-Clause \\"New\\" or \\"Revised\\" License",
                    "url": "https://spdx.org/licenses/BSD-3-Clause.html"
                  }
                ]
              },
              {
                "name": "hanami",
                "version": "1.3.0",
                "package_manager": "bundler",
                "dependency_file_path": "Gemfile.lock",
                "vulnerabilities": [],
                "licenses": [
                  {
                    "spdx_identifier": "MIT",
                    "name": "MIT License"
                  }
                ]
              }
            ]""";

    private static GitLabDependency[] decode() {
        return GitLabJsonCodecs.defaults().decode(LIST.getBytes(StandardCharsets.UTF_8), GitLabDependency[].class);
    }

    @Test
    void accessors_shouldReadRealShapedJson_whenDocsExample() throws IOException {
        final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
        when(gitLab.dependencies().list(ID).stream()).thenReturn(Stream.of(decode()));

        final List<Dependency> result =
                GitLabSdk.of(gitLab).project(ID).dependencies().all().toList();

        final Dependency first = result.getFirst();
        final Dependency second = result.getLast();
        assertSoftly(softly -> {
            softly.assertThat(result).extracting(Dependency::name).containsExactly("rails", "hanami");
            softly.assertThat(first.version()).contains("5.0.1");
            softly.assertThat(first.packageManager()).contains("bundler");
            softly.assertThat(first.filePath()).contains("Gemfile.lock");
            softly.assertThat(first.vulnerabilities())
                    .extracting(v -> v.id(), v -> v.name(), v -> v.severity())
                    .containsExactly(
                            tuple(144827L, "DoS Vulnerability in Action View", "unknown"),
                            tuple(144828L, "Information Exposure in Action View", "unknown"));
            softly.assertThat(first.licenses())
                    .extracting(l -> l.spdxIdentifier(), l -> l.url())
                    .containsExactly(tuple("BSD-3-Clause", "https://spdx.org/licenses/BSD-3-Clause.html"));
            softly.assertThat(first.malware()).isEmpty();
            softly.assertThat(second.vulnerabilities()).isEmpty();
            softly.assertThat(second.licenses())
                    .extracting(l -> l.spdxIdentifier(), l -> l.url())
                    .containsExactly(tuple("MIT", null));
        });
    }
}
