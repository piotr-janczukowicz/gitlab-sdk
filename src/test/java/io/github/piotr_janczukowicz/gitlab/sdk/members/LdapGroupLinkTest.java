package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLdapGroupLink;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLdapGroupLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksDeleteFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@UnitTest
class LdapGroupLinkTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private LdapGroupLink sut(final GitLabLdapGroupLink response) {
        return new LdapGroupLink(GitLabSdk.of(gitLab).group(ID), response);
    }

    private static GitLabLdapGroupLink byCn() {
        return GitLabLdapGroupLinkBuilder.builder()
                .cn("developers")
                .provider("ldapmain")
                .groupAccess(30)
                .memberRoleId(4L)
                .build();
    }

    private static GitLabLdapGroupLink byFilter() {
        return GitLabLdapGroupLinkBuilder.builder()
                .filter("(memberOf=cn=ops)")
                .provider("ldapmain")
                .groupAccess(40)
                .build();
    }

    @Test
    void accessors_shouldReadResponse_whenLinkIsByCn() {
        final LdapGroupLink sut = sut(byCn());

        assertSoftly(softly -> {
            softly.assertThat(sut.group().id()).isEqualTo(ID);
            softly.assertThat(sut.cn()).contains("developers");
            softly.assertThat(sut.filter()).isEmpty();
            softly.assertThat(sut.provider()).isEqualTo("ldapmain");
            softly.assertThat(sut.accessLevel()).isEqualTo(AccessLevel.DEVELOPER);
        });
    }

    @Test
    void accessors_shouldReadResponse_whenLinkIsByFilter() {
        final LdapGroupLink sut = sut(byFilter());

        assertSoftly(softly -> {
            softly.assertThat(sut.cn()).isEmpty();
            softly.assertThat(sut.filter()).contains("(memberOf=cn=ops)");
            softly.assertThat(sut.accessLevel()).isEqualTo(AccessLevel.MAINTAINER);
        });
    }

    @Test
    void accessLevel_shouldThrowIllegalArgumentException_whenValueIsUnknown() {
        final LdapGroupLink sut = sut(GitLabLdapGroupLinkBuilder.builder()
                .cn("x")
                .provider("p")
                .groupAccess(77)
                .build());

        final Throwable thrown = catchThrowable(sut::accessLevel);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_shouldDeleteByProviderAndCn_whenLinkIsByCn() throws IOException {
        sut(byCn()).delete();

        verify(gitLab.ldapgrouplinks())
                .delete(ID, "ldapmain", new GitLabGroupLDAPGroupLinksDeleteFilter("developers", null));
    }

    @Test
    void delete_shouldDeleteByProviderAndFilter_whenLinkIsByFilter() throws IOException {
        sut(byFilter()).delete();

        verify(gitLab.ldapgrouplinks())
                .delete(ID, "ldapmain", new GitLabGroupLDAPGroupLinksDeleteFilter(null, "(memberOf=cn=ops)"));
    }

    @Test
    void delete_shouldThrowIllegalStateExceptionWithoutARequest_whenLinkHasNeitherCnNorFilter() {
        final LdapGroupLink sut = sut(GitLabLdapGroupLinkBuilder.builder()
                .provider("ldapmain")
                .groupAccess(30)
                .build());

        final Throwable thrown = catchThrowable(sut::delete);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(gitLab);
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabLdapGroupLink record = byCn();

        assertThat(sut(record).response()).isSameAs(record);
    }
}
