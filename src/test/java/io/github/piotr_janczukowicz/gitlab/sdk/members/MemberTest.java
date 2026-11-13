package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.member;
import static io.github.piotr_janczukowicz.gitlab.sdk.members.MembersFixtures.memberExpiring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMember;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class MemberTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);

    private Member sut(final GitLabMember response) {
        return new Member(GitLabSdk.of(gitLab).project(ID), response);
    }

    @Test
    void accessors_shouldReadResponse_whenLevelIsKnown() {
        final Member sut = sut(member(5L, "ann", 40));

        assertSoftly(softly -> {
            softly.assertThat(sut.userId()).isEqualTo(5L);
            softly.assertThat(sut.username()).isEqualTo("ann");
            softly.assertThat(sut.accessLevel()).isEqualTo(AccessLevel.MAINTAINER);
            softly.assertThat(sut.accessLevelValue()).isEqualTo(40);
            softly.assertThat(sut.project().id()).isEqualTo(ID);
        });
    }

    @Test
    void accessLevelValue_shouldReturnRawInt_whenLevelUnknown() {
        final Member sut = sut(member(5L, "ann", 35));

        final Throwable thrown = catchThrowable(sut::accessLevel);

        assertThat(sut.accessLevelValue()).isEqualTo(35);
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiresAt_shouldBeEmpty_whenAbsent() {
        assertThat(sut(member(5L, "ann", 30)).expiresAt()).isEmpty();
    }

    @Test
    void expiresAt_shouldBeEmpty_whenBlank() {
        assertThat(sut(memberExpiring(5L, "ann", " ")).expiresAt()).isEmpty();
    }

    @Test
    void expiresAt_shouldParseDate_whenPresent() {
        assertThat(sut(memberExpiring(5L, "ann", "2027-03-01")).expiresAt()).contains(LocalDate.of(2027, 3, 1));
    }

    @Test
    void expiresAt_shouldThrowIllegalStateException_whenUnparsable() {
        final Member sut = sut(memberExpiring(5L, "ann", "next week"));

        final Throwable thrown = catchThrowable(sut::expiresAt);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("next week");
    }

    @Test
    void remove_shouldCallDelete_whenCalled() throws IOException {
        sut(member(5L, "ann", 30)).remove();

        verify(gitLab.members().project()).delete(ID, 5L);
    }

    @Test
    void refresh_shouldReturnLoadedMember_whenPresent() throws IOException {
        when(gitLab.members().project().get(ID, 5L)).thenReturn(Optional.of(member(5L, "ann", 50)));

        final Member refreshed = sut(member(5L, "ann", 30)).refresh();

        assertThat(refreshed.accessLevel()).isEqualTo(AccessLevel.OWNER);
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.members().project().get(ID, 5L)).thenReturn(Optional.empty());
        final Member sut = sut(member(5L, "ann", 30));

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("5");
    }

    @Test
    void response_shouldReturnRecord_whenCalled() {
        final GitLabMember record = member(5L, "ann", 30);

        assertThat(sut(record).response()).isSameAs(record);
    }
}
