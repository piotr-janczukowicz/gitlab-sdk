package io.github.piotr_janczukowicz.gitlab.sdk.issues;

import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issue;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.issueLink;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.mergeRequest;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.mergeRequestBasic;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.note;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.relatedIssue;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.timeStats;
import static io.github.piotr_janczukowicz.gitlab.sdk.issues.IssuesFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabIssuableTimeStats;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLink;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabIssueLinkBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabProjectIssueLinksCreateLinkType;
import io.github.piotr_janczukowicz.gitlab.api.issuelinks.GitLabProjectIssueLinksCreatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssue;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabIssueBuilder;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesAddSpentTimePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesMovePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesTimeEstimatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdatePayload;
import io.github.piotr_janczukowicz.gitlab.api.issues.GitLabProjectIssuesUpdateStateEvent;
import io.github.piotr_janczukowicz.gitlab.api.notes.GitLabProjectNotesCreateIssuesNotesPayload;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Project;
import io.github.piotr_janczukowicz.gitlab.sdk.mergerequests.MergeRequest;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class IssueTest {

    private static final String ID = "my-group/my-project";
    private static final long IID = 7L;

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project project = GitLabSdk.of(gitLab).project(ID);
    private final GitLabIssue response = issue(IID, "Broken", "opened");
    private final Issue sut = new Issue(project, response);

    @Test
    void accessors_shouldReturnResponseValues_whenIssueIsLoaded() {
        assertSoftly(softly -> {
            softly.assertThat(sut.iid()).isEqualTo(IID);
            softly.assertThat(sut.title()).isEqualTo("Broken");
            softly.assertThat(sut.state()).isEqualTo(IssueState.OPENED);
            softly.assertThat(sut.project()).isSameAs(project);
            softly.assertThat(sut.response()).isSameAs(response);
            softly.assertThat(sut.description()).isEmpty();
            softly.assertThat(sut.labels()).isEmpty();
        });
    }

    @Test
    void labels_shouldReturnAnUnmodifiableCopy_whenTheResponseListIsMutable() {
        final List<String> names = new ArrayList<>(List.of("bug"));
        final Issue labelled = new Issue(
                project, GitLabIssueBuilder.builder(response).labels(names).build());

        final List<String> labels = labelled.labels();
        names.add("later");

        assertSoftly(softly -> {
            softly.assertThat(labels).containsExactly("bug");
            softly.assertThatThrownBy(() -> labels.add("x")).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void state_shouldReturnClosed_whenGitLabSaysClosed() {
        final Issue closed = new Issue(project, issue(IID, "Broken", "closed"));

        assertThat(closed.state()).isEqualTo(IssueState.CLOSED);
    }

    @Test
    void state_shouldThrowIllegalArgumentException_whenUnknownState() {
        final Issue unknown = new Issue(project, issue(IID, "Broken", "locked"));

        final Throwable thrown = catchThrowable(unknown::state);

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("locked");
    }

    @Test
    void stateName_shouldReturnRawValue_whenStateIsUnknown() {
        final Issue sut = new Issue(project, issue(IID, "Broken", "locked"));

        assertThat(sut.stateName()).isEqualTo("locked");
    }

    @Test
    void close_shouldSendStateEventClose_andReturnNewIssue() throws IOException {
        final GitLabIssue closed = issue(IID, "Broken", "closed");
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(closed);

        final Issue result = sut.close();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().stateEvent()).isEqualTo(GitLabProjectIssuesUpdateStateEvent.CLOSE);
            softly.assertThat(result).isNotSameAs(sut);
            softly.assertThat(result.response()).isSameAs(closed);
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void reopen_shouldSendStateEventReopen_whenCalled() throws IOException {
        when(gitLab.issues().project().update(eq(ID), eq(IID), any(GitLabProjectIssuesUpdatePayload.class)))
                .thenReturn(issue(IID, "Broken", "opened"));

        sut.reopen();

        final ArgumentCaptor<GitLabProjectIssuesUpdatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesUpdatePayload.class);
        verify(gitLab.issues().project()).update(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().stateEvent()).isEqualTo(GitLabProjectIssuesUpdateStateEvent.REOPEN);
    }

    @Test
    void comment_shouldCreateNoteOnIid_whenBodyIsGiven() throws IOException {
        when(gitLab.notes()
                        .project()
                        .createIssuesNotes(eq(ID), eq(IID), any(GitLabProjectNotesCreateIssuesNotesPayload.class)))
                .thenReturn(note(5L, "Thanks"));

        final Note created = sut.comment("Thanks");

        final ArgumentCaptor<GitLabProjectNotesCreateIssuesNotesPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectNotesCreateIssuesNotesPayload.class);
        verify(gitLab.notes().project()).createIssuesNotes(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().body()).isEqualTo("Thanks");
            softly.assertThat(created.id()).isEqualTo(5L);
            softly.assertThat(created.issue()).isSameAs(sut);
        });
    }

    @Test
    void comment_shouldThrowIllegalArgumentException_whenBodyIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.comment(" "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notes_shouldMapEachNoteToItsIssue() throws IOException {
        when(gitLab.notes().project().listIssuesNotes(ID, IID).stream())
                .thenReturn(Stream.of(note(1L, "a"), note(2L, "b")));

        final List<Note> notes = sut.notes().toList();

        assertThat(notes).hasSize(2).allSatisfy(n -> assertThat(n.issue()).isSameAs(sut));
    }

    @Test
    void notes_shouldBeLazy_andStopAtLimit() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.notes().project().listIssuesNotes(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return note(id, "n");
                }));

        final Stream<Note> notes = sut.notes();

        assertThat(consumed).hasValue(0);
        assertThat(notes.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void notes_shouldReturnEmptyStream_whenIssueHasNoNotes() throws IOException {
        when(gitLab.notes().project().listIssuesNotes(ID, IID).stream()).thenReturn(Stream.empty());

        final List<Note> notes = sut.notes().toList();

        assertThat(notes).isEmpty();
    }

    @Test
    void timeStats_shouldWrapResponse_whenGitLabHasIt() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(3600, 1800);
        when(gitLab.issues().project().getTimeStats(ID, IID)).thenReturn(Optional.of(stats));

        final Optional<TimeStats> found = sut.timeStats();

        assertThat(found)
                .hasValueSatisfying(value -> assertThat(value.response()).isSameAs(stats));
    }

    @Test
    void timeStats_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.issues().project().getTimeStats(ID, IID)).thenReturn(Optional.empty());

        final Optional<TimeStats> found = sut.timeStats();

        assertThat(found).isEmpty();
    }

    @Test
    void estimateTime_shouldSendWholeSeconds_whenDurationIsGiven() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(5400, 0);
        when(gitLab.issues().project().timeEstimate(eq(ID), eq(IID), any(GitLabProjectIssuesTimeEstimatePayload.class)))
                .thenReturn(stats);

        final TimeStats result = sut.estimateTime(Duration.ofMinutes(90).plusMillis(900));

        final ArgumentCaptor<GitLabProjectIssuesTimeEstimatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesTimeEstimatePayload.class);
        verify(gitLab.issues().project()).timeEstimate(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().duration()).isEqualTo("5400s");
            softly.assertThat(result.response()).isSameAs(stats);
        });
    }

    @Test
    void estimateTime_shouldThrowIllegalArgumentException_whenDurationIsNegative() {
        final GitLab untouched = mock(GitLab.class);
        final Issue other = new Issue(GitLabSdk.of(untouched).project(ID), response);

        final Throwable thrown = catchThrowable(() -> other.estimateTime(Duration.ofSeconds(-1)));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("estimate");
        verifyNoInteractions(untouched);
    }

    @Test
    void estimateTime_shouldThrowNullPointerException_whenDurationIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.estimateTime(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("estimate");
    }

    @Test
    void resetEstimate_shouldWrapResponse_whenCalled() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(0, 0);
        when(gitLab.issues().project().resetTimeEstimate(ID, IID)).thenReturn(stats);

        final TimeStats result = sut.resetEstimate();

        assertThat(result.response()).isSameAs(stats);
    }

    @Test
    void addSpentTime_shouldSendWholeSeconds_whenDurationIsGiven() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(0, 1800);
        when(gitLab.issues().project().addSpentTime(eq(ID), eq(IID), any(GitLabProjectIssuesAddSpentTimePayload.class)))
                .thenReturn(stats);

        final TimeStats result = sut.addSpentTime(Duration.ofMinutes(30));

        final ArgumentCaptor<GitLabProjectIssuesAddSpentTimePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesAddSpentTimePayload.class);
        verify(gitLab.issues().project()).addSpentTime(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().duration()).isEqualTo("1800s");
            softly.assertThat(result.response()).isSameAs(stats);
        });
    }

    @Test
    void addSpentTime_shouldThrowIllegalArgumentException_whenDurationIsNegative() {
        final GitLab untouched = mock(GitLab.class);
        final Issue other = new Issue(GitLabSdk.of(untouched).project(ID), response);

        final Throwable thrown = catchThrowable(() -> other.addSpentTime(Duration.ofSeconds(-1)));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("spent");
        verifyNoInteractions(untouched);
    }

    @Test
    void addSpentTime_shouldSendZeroSeconds_whenDurationIsZero() throws IOException {
        when(gitLab.issues().project().addSpentTime(eq(ID), eq(IID), any(GitLabProjectIssuesAddSpentTimePayload.class)))
                .thenReturn(timeStats(0, 0));

        sut.addSpentTime(Duration.ZERO);

        final ArgumentCaptor<GitLabProjectIssuesAddSpentTimePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesAddSpentTimePayload.class);
        verify(gitLab.issues().project()).addSpentTime(eq(ID), eq(IID), payload.capture());
        assertThat(payload.getValue().duration()).isEqualTo("0s");
    }

    @Test
    void resetSpentTime_shouldWrapResponse_whenCalled() throws IOException {
        final GitLabIssuableTimeStats stats = timeStats(3600, 0);
        when(gitLab.issues().project().resetSpentTime(ID, IID)).thenReturn(stats);

        final TimeStats result = sut.resetSpentTime();

        assertThat(result.response()).isSameAs(stats);
    }

    @Test
    void links_shouldLoadEachLink_andSkipOneThatIsGone() throws IOException {
        when(gitLab.issuelinks().list(ID, IID).stream())
                .thenReturn(Stream.of(relatedIssue(11L), relatedIssue(12L), relatedIssue(13L)));
        when(gitLab.issuelinks().get(ID, IID, "11")).thenReturn(Optional.of(issueLink(11L, "relates_to")));
        when(gitLab.issuelinks().get(ID, IID, "12")).thenReturn(Optional.empty());
        when(gitLab.issuelinks().get(ID, IID, "13")).thenReturn(Optional.of(issueLink(13L, "blocks")));

        final List<IssueLink> links = sut.links().toList();

        assertSoftly(softly -> {
            softly.assertThat(links).extracting(IssueLink::id).containsExactly(11L, 13L);
            softly.assertThat(links).allSatisfy(link -> assertThat(link.issue()).isSameAs(sut));
        });
    }

    @Test
    void links_shouldBeLazy_whenOnlyOneIsTaken() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.issuelinks().list(ID, IID).stream())
                .thenReturn(Stream.of(1L, 2L, 3L).map(id -> {
                    consumed.incrementAndGet();
                    return relatedIssue(id);
                }));
        when(gitLab.issuelinks().get(eq(ID), eq(IID), any(String.class)))
                .thenReturn(Optional.of(issueLink(1L, "relates_to")));

        final Stream<IssueLink> links = sut.links();

        assertThat(consumed).hasValue(0);
        assertThat(links.limit(1).toList()).hasSize(1);
        assertThat(consumed).hasValue(1);
    }

    @Test
    void links_shouldThrowNullPointerException_whenListedLinkHasNoId() throws IOException {
        when(gitLab.issuelinks().list(ID, IID).stream()).thenReturn(Stream.of(relatedIssue(null)));
        final Stream<IssueLink> links = sut.links();

        final Throwable thrown = catchThrowable(links::toList);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("issueLinkId");
    }

    @Test
    void link_shouldReturnLink_whenGitLabHasIt() throws IOException {
        final GitLabIssueLink loaded = issueLink(11L, "relates_to");
        when(gitLab.issuelinks().get(ID, IID, "11")).thenReturn(Optional.of(loaded));

        final Optional<IssueLink> found = sut.link(11L);

        assertThat(found).hasValueSatisfying(link -> assertThat(link.response()).isSameAs(loaded));
    }

    @Test
    void link_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.issuelinks().get(ID, IID, "11")).thenReturn(Optional.empty());

        final Optional<IssueLink> found = sut.link(11L);

        assertThat(found).isEmpty();
    }

    @Test
    void linkTo_shouldSendTargetAndType_whenTargetIsInAnotherProject() throws IOException {
        final Issue target = new Issue(GitLabSdk.of(gitLab).project("99"), issue(3L, "Other", "opened"));
        final GitLabIssueLink created = issueLink(21L, "blocks");
        when(gitLab.issuelinks().create(eq(ID), eq(IID), any(GitLabProjectIssueLinksCreatePayload.class)))
                .thenReturn(created);

        final IssueLink result = sut.linkTo(target, IssueLinkType.BLOCKS);

        final ArgumentCaptor<GitLabProjectIssueLinksCreatePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssueLinksCreatePayload.class);
        verify(gitLab.issuelinks()).create(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().targetProjectId()).isEqualTo("99");
            softly.assertThat(payload.getValue().targetIssueIid()).isEqualTo("3");
            softly.assertThat(payload.getValue().linkType()).isEqualTo(GitLabProjectIssueLinksCreateLinkType.BLOCKS);
            softly.assertThat(result.response()).isSameAs(created);
            softly.assertThat(result.issue()).isSameAs(sut);
        });
    }

    @Test
    void linkTo_shouldMakeOneRequest_whenAnswerCarriesAnId() throws IOException {
        final Issue target = new Issue(GitLabSdk.of(gitLab).project("99"), issue(3L, "Other", "opened"));
        when(gitLab.issuelinks().create(eq(ID), eq(IID), any(GitLabProjectIssueLinksCreatePayload.class)))
                .thenReturn(issueLink(21L, "relates_to"));

        final IssueLink result = sut.linkTo(target, IssueLinkType.RELATES_TO);

        assertThat(result.id()).isEqualTo(21L);
        verify(gitLab.issuelinks(), never()).list(ID, IID);
    }

    @Test
    void linkTo_shouldResolveIdThroughList_whenAnswerHasNoIdAndTargetHasNoProjectId() throws IOException {
        final Issue target = new Issue(GitLabSdk.of(gitLab).project("99"), issue(3L, "Other", "opened"));
        final GitLabIssueLink created =
                GitLabIssueLinkBuilder.builder().linkType("blocks").build();
        when(gitLab.issuelinks().create(eq(ID), eq(IID), any(GitLabProjectIssueLinksCreatePayload.class)))
                .thenReturn(created);
        when(gitLab.issuelinks().list(ID, IID))
                .thenReturn(
                        List.of(relatedIssue(null, 3L, 99L), relatedIssue(30L, 9L, 99L), relatedIssue(31L, 3L, 99L)));

        final IssueLink result = sut.linkTo(target, IssueLinkType.BLOCKS);

        assertSoftly(softly -> {
            softly.assertThat(result.id()).isEqualTo(31L);
            softly.assertThat(result.typeName()).isEqualTo("blocks");
        });
    }

    @Test
    void linkTo_shouldResolveIdByTargetProject_whenAnswerHasNoIdAndTargetKnowsItsProject() throws IOException {
        final GitLabIssue targetResponse = GitLabIssueBuilder.builder(issue(3L, "Other", "opened"))
                .projectId(99L)
                .build();
        final Issue target = new Issue(GitLabSdk.of(gitLab).project("99"), targetResponse);
        when(gitLab.issuelinks().create(eq(ID), eq(IID), any(GitLabProjectIssueLinksCreatePayload.class)))
                .thenReturn(GitLabIssueLinkBuilder.builder().build());
        when(gitLab.issuelinks().list(ID, IID))
                .thenReturn(List.of(relatedIssue(40L, 3L, 98L), relatedIssue(41L, 3L, 99L)));

        final IssueLink result = sut.linkTo(target, IssueLinkType.RELATES_TO);

        assertThat(result.id()).isEqualTo(41L);
    }

    @Test
    void linkTo_shouldThrowIllegalStateException_whenAnswerHasNoIdAndListDoesNotShowTheLink() throws IOException {
        final Issue target = new Issue(GitLabSdk.of(gitLab).project("99"), issue(3L, "Other", "opened"));
        when(gitLab.issuelinks().create(eq(ID), eq(IID), any(GitLabProjectIssueLinksCreatePayload.class)))
                .thenReturn(GitLabIssueLinkBuilder.builder().build());
        when(gitLab.issuelinks().list(ID, IID)).thenReturn(List.of(relatedIssue(30L, 9L, 99L)));

        final Throwable thrown = catchThrowable(() -> sut.linkTo(target, IssueLinkType.RELATES_TO));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("issue 7");
    }

    @Test
    void linkTo_shouldThrowNullPointerException_whenTargetOrTypeIsNull() {
        final GitLab untouched = mock(GitLab.class);
        final Issue other = new Issue(GitLabSdk.of(untouched).project(ID), response);

        final Throwable noTarget = catchThrowable(() -> other.linkTo(null, IssueLinkType.RELATES_TO));
        final Throwable noType = catchThrowable(() -> other.linkTo(other, null));

        assertSoftly(softly -> {
            softly.assertThat(noTarget).isInstanceOf(NullPointerException.class).hasMessageContaining("target");
            softly.assertThat(noType).isInstanceOf(NullPointerException.class).hasMessageContaining("type");
        });
        verifyNoInteractions(untouched);
    }

    @Test
    void participants_shouldReturnCoreUsers_whenCalled() throws IOException {
        when(gitLab.issues().project().listParticipants(ID, IID)).thenReturn(List.of(user(1L), user(2L)));

        final List<GitLabUserBasic> participants = sut.participants().toList();

        assertThat(participants).extracting(GitLabUserBasic::id).containsExactly(1L, 2L);
    }

    @Test
    void closedByMergeRequests_shouldLoadEachMergeRequest_andSkipOneThatIsGone() throws IOException {
        when(gitLab.issues().project().listClosedBy(ID, IID))
                .thenReturn(
                        List.of(mergeRequestBasic(1L, null), mergeRequestBasic(2L, null), mergeRequestBasic(3L, 99L)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(1L), any())).thenReturn(Optional.of(mergeRequest(1L)));
        when(gitLab.mergerequests().project().get(eq(ID), eq(2L), any())).thenReturn(Optional.empty());
        when(gitLab.mergerequests().project().get(eq("99"), eq(3L), any())).thenReturn(Optional.of(mergeRequest(3L)));

        final List<MergeRequest> result = sut.closedByMergeRequests().toList();

        assertSoftly(softly -> {
            softly.assertThat(result).extracting(MergeRequest::iid).containsExactly(1L, 3L);
            softly.assertThat(result.get(0).project()).isSameAs(project);
            softly.assertThat(result.get(1).project().id()).isEqualTo("99");
        });
    }

    @Test
    void relatedMergeRequests_shouldLoadThroughOwnProject_whenSummaryNamesTheOwningProject() throws IOException {
        final Project numeric = GitLabSdk.of(gitLab).project("42");
        final Issue inNumeric = new Issue(numeric, response);
        when(gitLab.issues().project().listRelatedMergeRequests("42", IID))
                .thenReturn(List.of(mergeRequestBasic(5L, 42L)));
        when(gitLab.mergerequests().project().get(eq("42"), eq(5L), any())).thenReturn(Optional.of(mergeRequest(5L)));

        final List<MergeRequest> result = inNumeric.relatedMergeRequests().toList();

        assertThat(result)
                .singleElement()
                .satisfies(found -> assertThat(found.project()).isSameAs(numeric));
    }

    @Test
    void move_shouldReturnIssueInTargetProject_whenCalled() throws IOException {
        final GitLabIssue moved = issue(2L, "Broken", "opened");
        when(gitLab.issues().project().move(eq(ID), eq(IID), any(GitLabProjectIssuesMovePayload.class)))
                .thenReturn(moved);

        final Issue result = sut.move(99L);

        final ArgumentCaptor<GitLabProjectIssuesMovePayload> payload =
                ArgumentCaptor.forClass(GitLabProjectIssuesMovePayload.class);
        verify(gitLab.issues().project()).move(eq(ID), eq(IID), payload.capture());
        assertSoftly(softly -> {
            softly.assertThat(payload.getValue().toProjectId()).isEqualTo(99L);
            softly.assertThat(result.response()).isSameAs(moved);
            softly.assertThat(result.project().id()).isEqualTo("99");
        });
    }

    @Test
    void delete_shouldCallCore_whenCalled() throws IOException {
        sut.delete();

        verify(gitLab.issues().project()).delete(ID, IID);
    }

    @Test
    void refresh_shouldReturnNewIssue_whenGitLabHasIt() throws IOException {
        final GitLabIssue reloaded = issue(IID, "Renamed", "opened");
        when(gitLab.issues().project().get(ID, IID)).thenReturn(Optional.of(reloaded));

        final Issue refreshed = sut.refresh();

        assertSoftly(softly -> {
            softly.assertThat(refreshed).isNotSameAs(sut);
            softly.assertThat(refreshed.response()).isSameAs(reloaded);
        });
    }

    @Test
    void refresh_shouldThrowIllegalStateException_whenGone() throws IOException {
        when(gitLab.issues().project().get(ID, IID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::refresh);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("Issue 7 is gone");
    }
}
