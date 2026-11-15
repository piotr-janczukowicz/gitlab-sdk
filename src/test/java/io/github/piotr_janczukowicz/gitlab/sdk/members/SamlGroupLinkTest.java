package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSamlGroupLink;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSamlGroupLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLDeleteFilter;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLGetFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SamlGroupLinkTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private SamlGroupLink sut(final GitLabSamlGroupLink response) {
        return new SamlGroupLink(GitLabSdk.of(gitLab).group(ID), response);
    }

    private static GitLabSamlGroupLink link(final String provider) {
        return GitLabSamlGroupLinkBuilder.builder()
                .name("engineering")
                .accessLevel(30)
                .provider(provider)
                .build();
    }

    @Test
    void accessors_shouldReadResponse_whenProviderIsPresent() {
        final SamlGroupLink sut = sut(link("saml-main"));

        assertSoftly(softly -> {
            softly.assertThat(sut.group().id()).isEqualTo(ID);
            softly.assertThat(sut.name()).isEqualTo("engineering");
            softly.assertThat(sut.accessLevel()).isEqualTo(AccessLevel.DEVELOPER);
            softly.assertThat(sut.provider()).contains("saml-main");
        });
    }

    @Test
    void provider_shouldBeEmpty_whenResponseHasNone() {
        assertThat(sut(link(null)).provider()).isEmpty();
    }

    @Test
    void accessLevel_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        final SamlGroupLink sut = sut(
                GitLabSamlGroupLinkBuilder.builder().name("x").accessLevel(77).build());

        final Throwable thrown = catchThrowable(sut::accessLevel);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_shouldPassTheProvider_whenLinkHasOne() throws IOException {
        sut(link("saml-main")).delete();

        verify(gitLab.saml()).delete(ID, "engineering", new GitLabGroupSAMLDeleteFilter("saml-main"));
    }

    @Test
    void delete_shouldSendNoProvider_whenLinkHasNone() throws IOException {
        sut(link(null)).delete();

        verify(gitLab.saml()).delete(ID, "engineering");
    }

    @Test
    void refresh_shouldLoadByNameAndProvider_whenLinkHasOne() throws IOException {
        when(gitLab.saml().get(ID, "engineering", new GitLabGroupSAMLGetFilter("saml-main")))
                .thenReturn(Optional.of(GitLabSamlGroupLinkBuilder.builder()
                        .name("engineering")
                        .accessLevel(40)
                        .provider("saml-main")
                        .build()));

        final SamlGroupLink refreshed = sut(link("saml-main")).refresh();

        assertThat(refreshed.accessLevel()).isEqualTo(AccessLevel.MAINTAINER);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.saml().get(ID, "engineering")).thenReturn(Optional.empty());
        final SamlGroupLink sut = sut(link(null));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("engineering");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabSamlGroupLink record = link("p");

        assertThat(sut(record).response()).isSameAs(record);
    }
}
