package io.github.piotr_janczukowicz.gitlab.sdk.dependencies;

import static io.github.piotr_janczukowicz.gitlab.sdk.dependencies.DependenciesFixtures.advisory;
import static io.github.piotr_janczukowicz.gitlab.sdk.dependencies.DependenciesFixtures.bareDependency;
import static io.github.piotr_janczukowicz.gitlab.sdk.dependencies.DependenciesFixtures.dependency;
import static io.github.piotr_janczukowicz.gitlab.sdk.dependencies.DependenciesFixtures.license;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependency;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyBuilder;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityLicenseEntity;
import io.github.piotr_janczukowicz.gitlab.api.dependencies.GitLabDependencyEntityVulnerabilityEntity;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class DependencyTest {

    private static final String ID = "my-group/my-project";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final Dependencies dependencies = project.dependencies();

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new Dependency(null, dependency("rails"))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new Dependency(dependencies, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void accessors_shouldReadResponse_whenFieldsArePresent() {
        final GitLabDependency response = dependency("rails");

        final Dependency sut = new Dependency(dependencies, response);

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("rails");
            softly.assertThat(sut.version()).contains("6.1.6");
            softly.assertThat(sut.packageManager()).contains("bundler");
            softly.assertThat(sut.filePath()).contains("Gemfile.lock");
            softly.assertThat(sut.vulnerabilities()).containsExactly(advisory());
            softly.assertThat(sut.licenses()).containsExactly(license());
            softly.assertThat(sut.isMalware()).contains(false);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenFieldsAreAbsent() {
        final Dependency sut = new Dependency(dependencies, bareDependency("rails"));

        assertSoftly(softly -> {
            softly.assertThat(sut.version()).isEmpty();
            softly.assertThat(sut.packageManager()).isEmpty();
            softly.assertThat(sut.filePath()).isEmpty();
            softly.assertThat(sut.vulnerabilities()).isEmpty();
            softly.assertThat(sut.licenses()).isEmpty();
            softly.assertThat(sut.isMalware()).isEmpty();
        });
    }

    @Test
    void isMalware_shouldBeTrue_whenFlagged() {
        final Dependency sut = new Dependency(
                dependencies,
                GitLabDependencyBuilder.builder().name("evil").malware(true).build());

        assertThat(sut.isMalware()).contains(true);
    }

    @Test
    void name_shouldThrowNullPointerException_whenGitLabSentNone() {
        final Dependency sut =
                new Dependency(dependencies, GitLabDependencyBuilder.builder().build());

        final Throwable thrown = catchThrowable(sut::name);

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void vulnerabilities_shouldBeEmpty_whenAbsent() {
        final Dependency sut = new Dependency(dependencies, bareDependency("rails"));

        final List<GitLabDependencyEntityVulnerabilityEntity> result = sut.vulnerabilities();

        assertThat(result).isEmpty();
        assertThat(catchThrowable(() -> result.add(advisory()))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void vulnerabilities_shouldBeImmutable_whenPresent() {
        final List<GitLabDependencyEntityVulnerabilityEntity> source = new ArrayList<>(List.of(advisory()));
        final Dependency sut = new Dependency(
                dependencies,
                GitLabDependencyBuilder.builder()
                        .name("rails")
                        .vulnerabilities(source)
                        .build());

        final List<GitLabDependencyEntityVulnerabilityEntity> result = sut.vulnerabilities();

        assertThat(catchThrowable(result::clear)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(result).containsExactly(advisory());
    }

    @Test
    void licenses_shouldBeEmpty_whenAbsent() {
        final Dependency sut = new Dependency(dependencies, bareDependency("rails"));

        final List<GitLabDependencyEntityLicenseEntity> result = sut.licenses();

        assertThat(result).isEmpty();
        assertThat(catchThrowable(() -> result.add(license()))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void licenses_shouldBeImmutable_whenPresent() {
        final List<GitLabDependencyEntityLicenseEntity> source = new ArrayList<>(List.of(license()));
        final Dependency sut = new Dependency(
                dependencies,
                GitLabDependencyBuilder.builder().name("rails").licenses(source).build());

        final List<GitLabDependencyEntityLicenseEntity> result = sut.licenses();

        assertThat(catchThrowable(result::clear)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(result).containsExactly(license());
    }

    @Test
    void project_shouldReturnOwner_whenListed() {
        final Dependency sut = new Dependency(dependencies, dependency("rails"));

        final Project result = sut.project();

        assertThat(result).isSameAs(project);
        verifyNoInteractions(gitLab.dependencies());
    }
}
