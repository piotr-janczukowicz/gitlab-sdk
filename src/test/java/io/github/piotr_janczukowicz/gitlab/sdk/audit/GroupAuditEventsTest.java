package io.github.piotr_janczukowicz.gitlab.sdk.audit;

import static io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditFixtures.event;
import static io.github.piotr_janczukowicz.gitlab.sdk.audit.AuditFixtures.page;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabAuditEvent;
import io.github.piotr_janczukowicz.gitlab.api.groups.GitLabGroupGroupsListAuditEventsFilter;
import io.github.piotr_janczukowicz.gitlab.client.GitLabTier;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabForbiddenException;
import io.github.piotr_janczukowicz.gitlab.client.exception.GitLabTierException;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabKeysetRequest;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPage;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageRequest;
import io.github.piotr_janczukowicz.gitlab.pagination.GitLabPageWalk;
import io.github.piotr_janczukowicz.gitlab.sdk.GitLabSdk;
import io.github.piotr_janczukowicz.gitlab.sdk.Group;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class GroupAuditEventsTest {

    private static final String ID = "my-group";
    private static final Instant AFTER = Instant.parse("2026-11-13T00:00:00Z");
    private static final Instant BEFORE = Instant.parse("2026-11-14T00:00:00Z");

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Group group = GitLabSdk.of(gitLab).group(ID);
    private final AuditEvents sut = group.auditEvents();

    @SuppressWarnings("unchecked")
    private final GitLabPageWalk<GitLabAuditEvent> walk = mock(GitLabPageWalk.class);

    @Test
    void auditEvents_shouldMakeNoRequest_whenOnlyHandleIsCreated() {
        final GitLab untouched = mock(GitLab.class);

        GitLabSdk.of(untouched).group(ID).auditEvents();

        verifyNoInteractions(untouched);
    }

    @Test
    void forGroup_shouldThrowNullPointerException_whenGroupIsNull() {
        final Throwable thrown = catchThrowable(() -> AuditEvents.forGroup(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void all_shouldRequestKeysetById_whenListing() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk, event(2L), event(1L)));

        final List<AuditEvent> result = sut.all().toList();

        final ArgumentCaptor<GitLabGroupGroupsListAuditEventsFilter> filter =
                ArgumentCaptor.forClass(GitLabGroupGroupsListAuditEventsFilter.class);
        final ArgumentCaptor<GitLabPageRequest> request = ArgumentCaptor.forClass(GitLabPageRequest.class);
        verify(gitLab.groups(), times(1)).listAuditEvents(eq(ID), filter.capture(), request.capture());
        assertSoftly(softly -> {
            softly.assertThat(request.getValue()).isInstanceOfSatisfying(GitLabKeysetRequest.class, keyset -> {
                softly.assertThat(keyset.orderBy()).isEqualTo("id");
                softly.assertThat(keyset.sort()).isEqualTo("desc");
                softly.assertThat(keyset.perPage()).isNull();
            });
            softly.assertThat(filter.getValue().createdAfter()).isNull();
            softly.assertThat(filter.getValue().createdBefore()).isNull();
            softly.assertThat(result).extracting(AuditEvent::id).containsExactly(2L, 1L);
            softly.assertThat(result.getFirst().auditEvents()).isSameAs(sut);
        });
    }

    @Test
    void all_shouldNeverUseOffsetOverloads_whenListing() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk, event(2L)));

        sut.all().toList();
        sut.createdBetween(AFTER, BEFORE).toList();

        verify(gitLab.groups(), never()).listAuditEvents(anyString());
        verify(gitLab.groups(), never()).listAuditEvents(anyString(), any(GitLabPageRequest.class));
        verify(gitLab.groups(), never())
                .listAuditEvents(anyString(), any(GitLabGroupGroupsListAuditEventsFilter.class));
    }

    @Test
    void all_shouldNotLoadSecondPage_whenLimitedToOne() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk, event(2L)));

        final List<AuditEvent> result = sut.all().limit(1).toList();

        assertThat(result).extracting(AuditEvent::id).containsExactly(2L);
        verify(walk, never()).loadNext(any());
    }

    @Test
    void all_shouldBeLazy_whenNotConsumed() throws IOException {
        final AtomicInteger mapped = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final GitLabPage<GitLabAuditEvent> counting = mock(GitLabPage.class);
        when(counting.stream()).thenReturn(Stream.of(2L, 1L).map(id -> {
            mapped.incrementAndGet();
            return event(id);
        }));
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(counting);

        final Stream<AuditEvent> result = sut.all();

        assertThat(result).isNotNull();
        assertThat(mapped).hasValue(0);
    }

    @Test
    void createdBetween_shouldSendBounds() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk, event(2L)));

        final List<AuditEvent> result = sut.createdBetween(AFTER, BEFORE).toList();

        final ArgumentCaptor<GitLabGroupGroupsListAuditEventsFilter> filter =
                ArgumentCaptor.forClass(GitLabGroupGroupsListAuditEventsFilter.class);
        final ArgumentCaptor<GitLabPageRequest> request = ArgumentCaptor.forClass(GitLabPageRequest.class);
        verify(gitLab.groups(), times(1)).listAuditEvents(eq(ID), filter.capture(), request.capture());
        assertSoftly(softly -> {
            softly.assertThat(filter.getValue().createdAfter()).isEqualTo(AFTER);
            softly.assertThat(filter.getValue().createdBefore()).isEqualTo(BEFORE);
            softly.assertThat(filter.getValue().toQueryParameters())
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of("created_after", "2026-11-13T00:00:00Z", "created_before", "2026-11-14T00:00:00Z"));
            softly.assertThat(request.getValue()).isEqualTo(GitLabKeysetRequest.of("id", "desc"));
            softly.assertThat(result).extracting(AuditEvent::id).containsExactly(2L);
        });
    }

    @Test
    void createdBetween_shouldWidenToWholeSeconds_whenBoundsHaveFraction() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk, event(2L)));

        sut.createdBetween(Instant.parse("2026-11-13T00:00:00.123456789Z"), Instant.parse("2026-11-14T00:00:00.999Z"))
                .toList();

        final ArgumentCaptor<GitLabGroupGroupsListAuditEventsFilter> filter =
                ArgumentCaptor.forClass(GitLabGroupGroupsListAuditEventsFilter.class);
        verify(gitLab.groups(), times(1)).listAuditEvents(eq(ID), filter.capture(), any());
        assertThat(filter.getValue().toQueryParameters())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("created_after", "2026-11-13T00:00:00Z", "created_before", "2026-11-14T00:00:01Z"));
    }

    @Test
    void createdBetween_shouldKeepLatestBound_whenBeforeIsInstantMax() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk, event(2L)));

        sut.createdBetween(AFTER, Instant.MAX).toList();

        final ArgumentCaptor<GitLabGroupGroupsListAuditEventsFilter> filter =
                ArgumentCaptor.forClass(GitLabGroupGroupsListAuditEventsFilter.class);
        verify(gitLab.groups(), times(1)).listAuditEvents(eq(ID), filter.capture(), any());
        assertThat(filter.getValue().createdBefore()).isEqualTo(Instant.MAX.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void createdBetween_shouldAcceptEqualBounds_whenOneInstant() throws IOException {
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(page(walk));

        sut.createdBetween(AFTER, AFTER).toList();

        final ArgumentCaptor<GitLabGroupGroupsListAuditEventsFilter> filter =
                ArgumentCaptor.forClass(GitLabGroupGroupsListAuditEventsFilter.class);
        verify(gitLab.groups(), times(1)).listAuditEvents(eq(ID), filter.capture(), any());
        assertSoftly(softly -> {
            softly.assertThat(filter.getValue().createdAfter()).isEqualTo(AFTER);
            softly.assertThat(filter.getValue().createdBefore()).isEqualTo(AFTER);
        });
    }

    @Test
    void createdBetween_shouldRejectNullBounds_withoutCoreCall() {
        final GitLab untouched = mock(GitLab.class);
        final AuditEvents guarded = GitLabSdk.of(untouched).group(ID).auditEvents();

        assertSoftly(softly -> {
            softly.assertThat(catchThrowable(() -> guarded.createdBetween(null, BEFORE)))
                    .isInstanceOf(NullPointerException.class);
            softly.assertThat(catchThrowable(() -> guarded.createdBetween(AFTER, null)))
                    .isInstanceOf(NullPointerException.class);
        });
        verifyNoInteractions(untouched);
    }

    @Test
    void all_shouldLoadNextPage_whenFirstPageIsConsumed() throws IOException {
        final GitLabPage<GitLabAuditEvent> first = page(walk, event(2L));
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenReturn(first);
        when(walk.loadNext(first)).thenReturn(Optional.of(page(walk, event(1L))));

        final List<AuditEvent> result = sut.all().limit(2).toList();

        assertThat(result).extracting(AuditEvent::id).containsExactly(2L, 1L);
        verify(walk, times(1)).loadNext(first);
    }

    @Test
    void createdBetween_shouldRejectReversedBounds_withoutCoreCall() {
        final GitLab untouched = mock(GitLab.class);
        final AuditEvents guarded = GitLabSdk.of(untouched).group(ID).auditEvents();

        final Throwable thrown = catchThrowable(() -> guarded.createdBetween(BEFORE, AFTER));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(untouched);
    }

    @Test
    void find_shouldWrapEvent_whenGitLabHasIt() throws IOException {
        when(gitLab.groups().getAuditEvent(ID, 2L)).thenReturn(Optional.of(event(2L)));

        final Optional<AuditEvent> result = sut.find(2L);

        assertSoftly(softly -> {
            softly.assertThat(result).map(AuditEvent::id).contains(2L);
            softly.assertThat(result).map(AuditEvent::auditEvents).containsSame(sut);
        });
        verify(gitLab.groups(), times(1)).getAuditEvent(ID, 2L);
    }

    @Test
    void find_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.groups().getAuditEvent(ID, 99L)).thenReturn(Optional.empty());

        final Optional<AuditEvent> result = sut.find(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void find_shouldPassForbiddenException_whenNotOwner() throws IOException {
        final GitLabForbiddenException forbidden = mock(GitLabForbiddenException.class);
        when(gitLab.groups().getAuditEvent(ID, 2L)).thenThrow(forbidden);

        final Throwable thrown = catchThrowable(() -> sut.find(2L));

        assertThat(thrown).isSameAs(forbidden);
    }

    @Test
    void all_shouldPassForbiddenException_whenTierMissing() throws IOException {
        final GitLabForbiddenException forbidden = mock(GitLabForbiddenException.class);
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenThrow(forbidden);

        final Throwable thrown = catchThrowable(sut::all);

        assertThat(thrown).isSameAs(forbidden);
    }

    @Test
    void all_shouldPassTierException_whenCoreRefusesTier() throws IOException {
        final GitLabTierException tier =
                new GitLabTierException("getApiV4GroupsIdAuditEvents", GitLabTier.PREMIUM, GitLabTier.FREE);
        when(gitLab.groups().listAuditEvents(eq(ID), any(GitLabGroupGroupsListAuditEventsFilter.class), any()))
                .thenThrow(tier);

        final Throwable thrown = catchThrowable(sut::all);

        assertThat(thrown).isSameAs(tier);
    }
}
