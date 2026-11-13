package io.github.piotr_janczukowicz.gitlab.sdk.protection;

import static io.github.piotr_janczukowicz.gitlab.sdk.protection.ProtectionFixtures.tag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.protectedtags.GitLabProtectedTag;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ProtectedTagTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);

    private ProtectedTag sut(final GitLabProtectedTag response) {
        return new ProtectedTag(project, response);
    }

    @Test
    void accessors_shouldReadResponse_whenPopulated() {
        final ProtectedTag sut = sut(tag("v*", 40));

        assertSoftly(softly -> {
            softly.assertThat(sut.name()).isEqualTo("v*");
            softly.assertThat(sut.createAccess()).containsExactly(ProtectedAccess.MAINTAINER);
            softly.assertThat(sut.createAccessValues()).containsExactly(40);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
        });
    }

    @Test
    void createAccess_shouldBeEmpty_whenAbsentInResponse() {
        final ProtectedTag sut = sut(tag("v*"));

        assertSoftly(softly -> {
            softly.assertThat(sut.createAccess()).isEmpty();
            softly.assertThat(sut.createAccessValues()).isEmpty();
        });
    }

    @Test
    void createAccess_shouldThrowIllegalArgumentException_whenUnknownLevel() {
        final ProtectedTag sut = sut(tag("v*", 20));

        final Throwable thrown = catchThrowable(sut::createAccess);

        assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(sut.createAccessValues()).containsExactly(20);
        });
    }

    @Test
    void unprotect_shouldCallDelete_whenCalled() throws IOException {
        sut(tag("release/*")).unprotect();

        verify(gitLab.protectedtags()).delete(ID, "release/*");
    }

    @Test
    void refresh_shouldReturnLoadedTag_whenPresent() throws IOException {
        when(gitLab.protectedtags().get(ID, "v*")).thenReturn(Optional.of(tag("v*", 30)));

        final ProtectedTag refreshed = sut(tag("v*", 40)).refresh();

        assertThat(refreshed.createAccess()).containsExactly(ProtectedAccess.DEVELOPER);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.protectedtags().get(ID, "v*")).thenReturn(Optional.empty());
        final ProtectedTag sut = sut(tag("v*"));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("v*");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabProtectedTag record = tag("v*");

        assertThat(sut(record).response()).isSameAs(record);
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenArgumentIsNull() {
        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> new ProtectedTag(null, tag("v*"))))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> new ProtectedTag(project, null)))
                    .isInstanceOf(NullPointerException.class);
        });
    }
}
