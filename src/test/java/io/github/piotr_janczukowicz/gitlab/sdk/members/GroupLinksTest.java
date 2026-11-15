package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLdapGroupLink;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLdapGroupLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSamlGroupLink;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabSamlGroupLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksCreateGroupAccess;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.ldapgrouplinks.GitLabGroupLDAPGroupLinksDeleteFilter;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLCreateAccessLevel;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLDeleteFilter;
import io.github.piotr_janczukowicz.gitlab.api.saml.GitLabGroupSAMLGetFilter;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupLinksTest {

    private static final String ID = "my-group";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);

    private static GitLabLdapGroupLink ldap(final String cn, final int level) {
        return GitLabLdapGroupLinkBuilder.builder()
                .cn(cn)
                .provider("ldapmain")
                .groupAccess(level)
                .build();
    }

    private static GitLabSamlGroupLink saml(final String name, final int level) {
        return GitLabSamlGroupLinkBuilder.builder()
                .name(name)
                .accessLevel(level)
                .build();
    }

    @Test
    void ldapLinks_shouldWrapEveryLink_whenGitLabListsTwo() throws IOException {
        when(gitLab.ldapgrouplinks().list(ID)).thenReturn(List.of(ldap("a", 30), ldap("b", 40)));

        final List<LdapGroupLink> links = group.ldapLinks().toList();

        assertThat(links).extracting(l -> l.cn().orElseThrow()).containsExactly("a", "b");
        assertThat(links).extracting(l -> l.group().id()).containsOnly(ID);
    }

    @ParameterizedTest
    @EnumSource(AccessLevel.class)
    void addLdapLink_shouldSendProviderCnAndLevel_whenLevelIsGiven(final AccessLevel level) throws IOException {
        when(gitLab.ldapgrouplinks().create(eq(ID), any(GitLabGroupLDAPGroupLinksCreatePayload.class)))
                .thenReturn(ldap("devs", level.value()));

        final LdapGroupLink result = group.addLdapLink("ldapmain", "devs", level);

        final ArgumentCaptor<GitLabGroupLDAPGroupLinksCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupLDAPGroupLinksCreatePayload.class);
        verify(gitLab.ldapgrouplinks()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().provider()).isEqualTo("ldapmain");
            softly.assertThat(payload.getValue().cn()).isEqualTo("devs");
            softly.assertThat(payload.getValue().filter()).isNull();
            softly.assertThat(payload.getValue().groupAccess().wireValue()).isEqualTo(level.value());
            softly.assertThat(payload.getValue().groupAccess())
                    .isEqualTo(GitLabGroupLDAPGroupLinksCreateGroupAccess.fromWire(level.value()));
            softly.assertThat(result.accessLevel()).isEqualTo(level);
        });
    }

    @Test
    void addLdapFilterLink_shouldSendFilterAndNoCn_whenCalled() throws IOException {
        when(gitLab.ldapgrouplinks().create(eq(ID), any(GitLabGroupLDAPGroupLinksCreatePayload.class)))
                .thenReturn(ldap("x", 30));

        group.addLdapFilterLink("ldapmain", "(memberOf=cn=ops)", AccessLevel.DEVELOPER);

        final ArgumentCaptor<GitLabGroupLDAPGroupLinksCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupLDAPGroupLinksCreatePayload.class);
        verify(gitLab.ldapgrouplinks()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().filter()).isEqualTo("(memberOf=cn=ops)");
            softly.assertThat(payload.getValue().cn()).isNull();
            softly.assertThat(payload.getValue().provider()).isEqualTo("ldapmain");
        });
    }

    @Test
    void deleteLdapLink_shouldUseTheProviderAndCnForm_whenCalled() throws IOException {
        group.deleteLdapLink("ldapmain", "devs");

        verify(gitLab.ldapgrouplinks()).delete(ID, "ldapmain", new GitLabGroupLDAPGroupLinksDeleteFilter("devs", null));
    }

    @Test
    void deleteLdapFilterLink_shouldUseTheProviderAndFilterForm_whenCalled() throws IOException {
        group.deleteLdapFilterLink("ldapmain", "(memberOf=cn=ops)");

        verify(gitLab.ldapgrouplinks())
                .delete(ID, "ldapmain", new GitLabGroupLDAPGroupLinksDeleteFilter(null, "(memberOf=cn=ops)"));
    }

    @Test
    void ldapMethods_shouldThrowOnBadArguments_whenNullOrBlank() {
        final Throwable blankProvider = catchThrowable(() -> group.addLdapLink(" ", "devs", AccessLevel.GUEST));
        final Throwable blankCn = catchThrowable(() -> group.addLdapLink("p", " ", AccessLevel.GUEST));
        final Throwable nullLevel = catchThrowable(() -> group.addLdapLink("p", "devs", null));
        final Throwable blankFilter = catchThrowable(() -> group.addLdapFilterLink("p", " ", AccessLevel.GUEST));
        final Throwable blankDelete = catchThrowable(() -> group.deleteLdapLink("p", " "));
        final Throwable blankFilterDelete = catchThrowable(() -> group.deleteLdapFilterLink(" ", "f"));

        assertSoftly(softly -> {
            softly.assertThat(blankProvider).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankCn).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullLevel).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankFilter).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankDelete).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankFilterDelete).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }

    @Test
    void samlLinks_shouldWrapEveryLink_whenGitLabListsTwo() throws IOException {
        when(gitLab.saml().list(ID)).thenReturn(List.of(saml("a", 30), saml("b", 40)));

        final List<SamlGroupLink> links = group.samlLinks().toList();

        assertThat(links).extracting(SamlGroupLink::name).containsExactly("a", "b");
    }

    @Test
    void samlLink_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.saml().get(ID, "a")).thenReturn(Optional.empty());

        assertThat(group.samlLink("a")).isEmpty();
    }

    @Test
    void samlLink_shouldReturnLink_whenPresent() throws IOException {
        when(gitLab.saml().get(ID, "a")).thenReturn(Optional.of(saml("a", 40)));

        assertThat(group.samlLink("a")).get().satisfies(l -> {
            assertThat(l.name()).isEqualTo("a");
            assertThat(l.group().id()).isEqualTo(ID);
        });
    }

    @Test
    void samlLink_shouldPassTheProvider_whenGiven() throws IOException {
        when(gitLab.saml().get(ID, "a", new GitLabGroupSAMLGetFilter("saml-main")))
                .thenReturn(Optional.of(saml("a", 30)));

        assertThat(group.samlLink("a", "saml-main"))
                .get()
                .satisfies(l -> assertThat(l.name()).isEqualTo("a"));
    }

    @ParameterizedTest
    @EnumSource(AccessLevel.class)
    void addSamlLink_shouldSendNameAndLevel_whenLevelIsGiven(final AccessLevel level) throws IOException {
        when(gitLab.saml().create(eq(ID), any(GitLabGroupSAMLCreatePayload.class)))
                .thenReturn(saml("eng", level.value()));

        final SamlGroupLink result = group.addSamlLink("eng", level);

        final ArgumentCaptor<GitLabGroupSAMLCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupSAMLCreatePayload.class);
        verify(gitLab.saml()).create(eq(ID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().samlGroupName()).isEqualTo("eng");
            softly.assertThat(payload.getValue().accessLevel())
                    .isEqualTo(GitLabGroupSAMLCreateAccessLevel.fromWire(level.value()));
            softly.assertThat(payload.getValue().provider()).isNull();
            softly.assertThat(result.accessLevel()).isEqualTo(level);
        });
    }

    @Test
    void addSamlLink_shouldSendProvider_whenGiven() throws IOException {
        when(gitLab.saml().create(eq(ID), any(GitLabGroupSAMLCreatePayload.class)))
                .thenReturn(saml("eng", 30));

        group.addSamlLink("eng", AccessLevel.DEVELOPER, "saml-main");

        final ArgumentCaptor<GitLabGroupSAMLCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabGroupSAMLCreatePayload.class);
        verify(gitLab.saml()).create(eq(ID), payload.capture());
        assertThat(payload.getValue().provider()).isEqualTo("saml-main");
    }

    @Test
    void deleteSamlLink_shouldSendNoProvider_whenOnlyTheNameIsGiven() throws IOException {
        group.deleteSamlLink("eng");

        verify(gitLab.saml()).delete(ID, "eng");
    }

    @Test
    void deleteSamlLink_shouldSendProvider_whenGiven() throws IOException {
        group.deleteSamlLink("eng", "saml-main");

        verify(gitLab.saml()).delete(ID, "eng", new GitLabGroupSAMLDeleteFilter("saml-main"));
    }

    @Test
    void samlMethods_shouldThrowOnBadArguments_whenNullOrBlank() {
        final Throwable blank = catchThrowable(() -> group.addSamlLink(" ", AccessLevel.GUEST));
        final Throwable nullLevel = catchThrowable(() -> group.addSamlLink("eng", null));
        final Throwable blankProvider = catchThrowable(() -> group.addSamlLink("eng", AccessLevel.GUEST, " "));
        final Throwable blankLookup = catchThrowable(() -> group.samlLink(" "));
        final Throwable blankDelete = catchThrowable(() -> group.deleteSamlLink(" "));

        assertSoftly(softly -> {
            softly.assertThat(blank).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(nullLevel).isInstanceOf(NullPointerException.class);
            softly.assertThat(blankProvider).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankLookup).isInstanceOf(IllegalArgumentException.class);
            softly.assertThat(blankDelete).isInstanceOf(IllegalArgumentException.class);
        });
        verifyNoInteractions(gitLab);
    }
}
