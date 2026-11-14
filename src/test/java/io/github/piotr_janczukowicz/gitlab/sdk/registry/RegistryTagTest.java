package io.github.piotr_janczukowicz.gitlab.sdk.registry;

import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.details;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.fullDetails;
import static io.github.piotr_janczukowicz.gitlab.sdk.registry.RegistryFixtures.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.containerregistry.GitLabContainerRegistryTagDetails;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RegistryTagTest {

    private static final String ID = "42";
    private static final long REPOSITORY_ID = 77L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final RegistryRepository repository =
            new RegistryRepository(GitLabSdk.of(gitLab).project(ID), repository(REPOSITORY_ID));

    private RegistryTag wrap(final GitLabContainerRegistryTagDetails response) {
        return new RegistryTag(repository, response);
    }

    @Test
    void accessors_shouldReadResponse_whenAllSet() {
        final GitLabContainerRegistryTagDetails response = fullDetails("v1");

        final RegistryTag sut = wrap(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("v1");
            softly.assertThat(sut.location()).isEqualTo("registry.example.com/my-group/demo/app:v1");
            softly.assertThat(sut.digest()).contains("sha256:abc");
            softly.assertThat(sut.totalSize()).contains(1024L);
            softly.assertThat(sut.repository()).isSameAs(repository);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void accessors_shouldBeEmpty_whenOptionalValuesAbsent() {
        final RegistryTag sut = wrap(details("v1"));

        assertSoftly(softly -> {
            softly.assertThat(sut.digest()).isEmpty();
            softly.assertThat(sut.totalSize()).isEmpty();
        });
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new RegistryTag(null, details("v1"))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new RegistryTag(repository, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void refresh_shouldLoadDetails_whenCalled() throws IOException {
        final GitLabContainerRegistryTagDetails fresh = fullDetails("release/1:rc");
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "release/1:rc"))
                .thenReturn(Optional.of(fresh));
        final RegistryTag sut = wrap(details("release/1:rc"));

        final RegistryTag result = sut.refresh();

        assertThat(result.response()).isSameAs(fresh);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.containerregistry().getTag(ID, REPOSITORY_ID, "v1")).thenReturn(Optional.empty());
        final RegistryTag sut = wrap(details("v1"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("v1");
    }

    @Test
    void delete_shouldPassNameUnchanged_whenItHasSlashAndColon() throws IOException {
        wrap(details("release/1:rc")).delete();

        verify(gitLab.containerregistry()).deleteTagByTagName(ID, REPOSITORY_ID, "release/1:rc");
    }
}
