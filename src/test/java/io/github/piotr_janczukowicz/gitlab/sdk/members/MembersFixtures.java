package io.github.piotr_janczukowicz.gitlab.sdk.members;

import io.github.piotr_janczukowicz.gitlab.api.accessrequests.GitLabAccessRequester;
import io.github.piotr_janczukowicz.gitlab.api.accessrequests.GitLabAccessRequesterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMember;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMemberBuilder;

/** Builds the core records for the tests of the member and access request objects. */
final class MembersFixtures {

    private MembersFixtures() {
        throw new UnsupportedOperationException("Utility class");
    }

    static GitLabMember member(final long id, final String username, final Integer accessLevel) {
        return GitLabMemberBuilder.builder()
                .id(id)
                .username(username)
                .accessLevel(accessLevel)
                .build();
    }

    static GitLabMember memberExpiring(final long id, final String username, final String expiresAt) {
        return GitLabMemberBuilder.builder()
                .id(id)
                .username(username)
                .accessLevel(30)
                .expiresAt(expiresAt)
                .build();
    }

    static GitLabAccessRequester requester(final long id, final String username) {
        return GitLabAccessRequesterBuilder.builder().id(id).username(username).build();
    }
}
