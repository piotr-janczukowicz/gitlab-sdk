package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issue;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issueLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLink;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@UnitTest
class IssueLinkTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;
    private static final long LINK_ID = 11L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Issue issue = new Issue(GitLabSdk.of(gitLab).project(ID), issue(IID, "Broken", "opened"));
    private final GitLabIssueLink response = issueLink(LINK_ID, "is_blocked_by");
    private final IssueLink sut = new IssueLink(issue, response);

    @Test
    void accessors_shouldReturnResponseValues_whenLinkIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.id()).isEqualTo(LINK_ID);
            softly.assertThat(sut.type()).isEqualTo(IssueLinkType.IS_BLOCKED_BY);
            softly.assertThat(sut.typeName()).isEqualTo("is_blocked_by");
            softly.assertThat(sut.issue()).isSameAs(issue);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void type_shouldThrowIllegalArgumentException_whenUnknownType() {
        final IssueLink unknown = new IssueLink(issue, issueLink(LINK_ID, "duplicates"));

        final Throwable thrown = catchThrowable(unknown::type);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicates");
        assertThat(unknown.typeName()).isEqualTo("duplicates");
    }

    @Test
    void id_shouldThrowNullPointerException_whenResponseHasNoId() {
        final IssueLink noId =
                new IssueLink(issue, GitLabIssueLinkBuilder.builder().build());

        final Throwable thrown = catchThrowable(noId::id);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("id");
    }

    @Test
    void constructor_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        final Throwable noIssue = catchThrowable(() -> new IssueLink(null, response));
        final Throwable noResponse = catchThrowable(() -> new IssueLink(issue, null));

        assertSoftly(softly -> {
            softly.assertThat(noIssue).isInstanceOf(NullPointerException.class).hasMessageContaining("issue");
            softly.assertThat(noResponse)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("response");
        });
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.issuelinks()).delete(ID, IID, "11");
    }

    @Test
    void constructor_shouldMakeNoRequest_whenLinkIsWrapped() {
        final GitLab untouched = mock(GitLab.class);

        new IssueLink(new Issue(GitLabSdk.of(untouched).project(ID), issue(IID, "t", "opened")), response);

        verifyNoInteractions(untouched);
    }
}
