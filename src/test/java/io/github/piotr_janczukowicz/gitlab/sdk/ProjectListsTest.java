package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.gitlab.sdk.ProjectsFixtures.loadedProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.piotr_janczukowicz.gitlab.api.GitLab;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLicense;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabLicenseBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabTemplateList;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabTemplateListBuilder;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasic;
import io.github.piotr_janczukowicz.gitlab.api.common.GitLabUserBasicBuilder;
import io.github.piotr_janczukowicz.gitlab.api.events.GitLabEvent;
import io.github.piotr_janczukowicz.gitlab.api.events.GitLabEventBuilder;
import io.github.piotr_janczukowicz.gitlab.api.events.GitLabProjectEventsListFilter;
import io.github.piotr_janczukowicz.gitlab.api.events.GitLabProjectEventsListFilterBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsHousekeepingPayload;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectProjectsHousekeepingTask;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabPublicGroupDetails;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabPublicGroupDetailsBuilder;
import io.github.piotr_janczukowicz.gitlab.api.projecttemplates.GitLabProjectProjectTemplatesGetType;
import io.github.piotr_janczukowicz.gitlab.api.projecttemplates.GitLabProjectProjectTemplatesListType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ProjectListsTest {

    private static final String ID = "my-group/demo";

    private final GitLab gitLab = mock(GitLab.class, RETURNS_DEEP_STUBS);
    private final Project sut = GitLabSdk.of(gitLab).project(ID);

    @Test
    void users_shouldPassRecordsThrough_whenListed() throws IOException {
        final GitLabUserBasic user = GitLabUserBasicBuilder.builder().id(4L).build();
        when(gitLab.projects().project().listUsers(ID).stream()).thenReturn(Stream.of(user));

        final List<GitLabUserBasic> users = sut.users().toList();

        assertThat(users).singleElement().isSameAs(user);
    }

    @Test
    void users_shouldBeLazyAndSingleUse_whenNotConsumed() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projects().project().listUsers(ID).stream())
                .thenReturn(Stream.of(4L, 5L).map(id -> {
                    consumed.incrementAndGet();
                    return GitLabUserBasicBuilder.builder().id(id).build();
                }));

        final Stream<GitLabUserBasic> users = sut.users();
        final int beforeConsuming = consumed.get();
        final List<GitLabUserBasic> first = users.limit(1).toList();
        final Throwable thrown = catchThrowable(users::toList);

        assertSoftly(softly -> {
            softly.assertThat(beforeConsuming).isZero();
            softly.assertThat(first).hasSize(1);
            softly.assertThat(consumed).hasValue(1);
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void groups_shouldBeLazyAndSingleUse_whenNotConsumed() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projects().project().listGroups(ID).stream())
                .thenReturn(Stream.of(1L, 2L).map(id -> {
                    consumed.incrementAndGet();
                    return GitLabPublicGroupDetailsBuilder.builder().id(id).build();
                }));

        final Stream<GitLabPublicGroupDetails> groups = sut.groups();
        final int beforeConsuming = consumed.get();
        final List<GitLabPublicGroupDetails> first = groups.limit(1).toList();
        final Throwable thrown = catchThrowable(groups::toList);

        assertSoftly(softly -> {
            softly.assertThat(beforeConsuming).isZero();
            softly.assertThat(first).hasSize(1);
            softly.assertThat(consumed).hasValue(1);
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void templates_shouldBeLazyAndSingleUse_whenNotConsumed() throws IOException {
        final AtomicInteger consumed = new AtomicInteger();
        when(gitLab.projecttemplates().list(ID, GitLabProjectProjectTemplatesListType.LICENSES).stream())
                .thenReturn(Stream.of("MIT", "Apache").map(key -> {
                    consumed.incrementAndGet();
                    return GitLabTemplateListBuilder.builder().key(key).build();
                }));

        final Stream<GitLabTemplateList> templates = sut.templates(TemplateType.LICENSES);
        final int beforeConsuming = consumed.get();
        final List<GitLabTemplateList> first = templates.limit(1).toList();
        final Throwable thrown = catchThrowable(templates::toList);

        assertSoftly(softly -> {
            softly.assertThat(beforeConsuming).isZero();
            softly.assertThat(first).hasSize(1);
            softly.assertThat(consumed).hasValue(1);
            softly.assertThat(thrown).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void groups_shouldListAncestorGroups_whenListed() throws IOException {
        final GitLabPublicGroupDetails group =
                GitLabPublicGroupDetailsBuilder.builder().id(9L).build();
        when(gitLab.projects().project().listGroups(ID).stream()).thenReturn(Stream.of(group));

        final List<GitLabPublicGroupDetails> groups = sut.groups().toList();

        assertThat(groups).singleElement().isSameAs(group);
    }

    @Test
    void events_shouldPassRecordsThrough_whenListed() throws IOException {
        final GitLabEvent event = GitLabEventBuilder.builder().id(8L).build();
        when(gitLab.events().project().list(ID).stream()).thenReturn(Stream.of(event));

        final List<GitLabEvent> events = sut.events().toList();

        assertThat(events).singleElement().isSameAs(event);
    }

    @Test
    void events_shouldPassFilterToCore_whenFilterIsGiven() throws IOException {
        final GitLabProjectEventsListFilter filter =
                GitLabProjectEventsListFilterBuilder.builder().action("pushed").build();
        final GitLabEvent event = GitLabEventBuilder.builder().id(8L).build();
        when(gitLab.events().project().list(ID, filter).stream()).thenReturn(Stream.of(event));

        final List<GitLabEvent> events = sut.events(filter).toList();

        assertThat(events).singleElement().isSameAs(event);
    }

    @Test
    void events_shouldThrowNullPointerException_whenFilterIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.events(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void events_shouldThrowIllegalStateException_whenStreamIsConsumedTwice() throws IOException {
        when(gitLab.events().project().list(ID).stream()).thenReturn(Stream.empty());
        final Stream<GitLabEvent> events = sut.events();
        events.toList();

        final Throwable thrown = catchThrowable(events::toList);

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void star_shouldStarAndReturnReloadedProject_whenCalled() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(loadedProject(7L, "demo")));

        final Project starred = sut.star();

        verify(gitLab.projectstarring()).createStar(ID);
        assertThat(starred.response().path()).isEqualTo("demo");
    }

    @Test
    void star_shouldThrowIllegalStateException_whenGoneAfterStarring() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.empty());

        final Throwable thrown = catchThrowable(sut::star);

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining(ID);
    }

    @Test
    void unstar_shouldUnstarAndReturnReloadedProject_whenCalled() throws IOException {
        when(gitLab.projects().project().get(ID)).thenReturn(Optional.of(loadedProject(7L, "demo")));

        final Project unstarred = sut.unstar();

        verify(gitLab.projectstarring()).createUnstar(ID);
        assertThat(unstarred.response().path()).isEqualTo("demo");
    }

    @Test
    void languages_shouldReturnUnmodifiableCopy_whenLoaded() throws IOException {
        final Map<String, Double> core = new LinkedHashMap<>();
        core.put("Java", 80.5);
        core.put("Shell", 19.5);
        when(gitLab.projects().project().getLanguages(ID)).thenReturn(core);

        final Map<String, Double> languages = sut.languages();
        core.put("Ruby", 1.0);
        final Throwable thrown = catchThrowable(() -> languages.put("Go", 2.0));

        assertSoftly(softly -> {
            softly.assertThat(languages).containsExactly(Map.entry("Java", 80.5), Map.entry("Shell", 19.5));
            softly.assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void housekeeping_shouldSendNoTask_whenCalledWithoutOne() throws IOException {
        sut.housekeeping();

        final ArgumentCaptor<GitLabProjectProjectsHousekeepingPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsHousekeepingPayload.class);
        verify(gitLab.projects().project()).housekeeping(eq(ID), payload.capture());
        assertThat(payload.getValue().task()).isNull();
    }

    @Test
    void housekeeping_shouldSendTask_whenTaskIsGiven() throws IOException {
        sut.housekeeping(GitLabProjectProjectsHousekeepingTask.PRUNE);

        final ArgumentCaptor<GitLabProjectProjectsHousekeepingPayload> payload =
                ArgumentCaptor.forClass(GitLabProjectProjectsHousekeepingPayload.class);
        verify(gitLab.projects().project()).housekeeping(eq(ID), payload.capture());
        assertThat(payload.getValue().task()).isEqualTo(GitLabProjectProjectsHousekeepingTask.PRUNE);
    }

    @Test
    void templates_shouldListWithMatchingCoreType_whenTypeIsGiven() throws IOException {
        final GitLabTemplateList template =
                GitLabTemplateListBuilder.builder().key("MIT").name("MIT").build();
        when(gitLab.projecttemplates().list(ID, GitLabProjectProjectTemplatesListType.LICENSES).stream())
                .thenReturn(Stream.of(template));

        final List<GitLabTemplateList> templates =
                sut.templates(TemplateType.LICENSES).toList();

        assertThat(templates).singleElement().isSameAs(template);
    }

    @Test
    void templates_shouldThrowNullPointerException_whenTypeIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.templates(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void template_shouldLoadWithMatchingCoreType_whenFound() throws IOException {
        final GitLabLicense license =
                GitLabLicenseBuilder.builder().name("Java").content("*.class").build();
        when(gitLab.projecttemplates().get(ID, GitLabProjectProjectTemplatesGetType.GITIGNORES, "Java"))
                .thenReturn(Optional.of(license));

        final Optional<GitLabLicense> found = sut.template(TemplateType.GITIGNORES, "Java");

        assertThat(found).containsSame(license);
    }

    @Test
    void template_shouldReturnEmpty_whenGitLabAnswers404() throws IOException {
        when(gitLab.projecttemplates().get(ID, GitLabProjectProjectTemplatesGetType.ISSUES, "Bug"))
                .thenReturn(Optional.empty());

        assertThat(sut.template(TemplateType.ISSUES, "Bug")).isEmpty();
        verify(gitLab.projecttemplates()).get(ID, GitLabProjectProjectTemplatesGetType.ISSUES, "Bug");
    }

    @Test
    void template_shouldThrowIllegalArgumentException_whenNameIsBlank() {
        final Throwable thrown = catchThrowable(() -> sut.template(TemplateType.ISSUES, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void template_shouldThrowNullPointerException_whenTypeIsNull() {
        final Throwable thrown = catchThrowable(() -> sut.template(null, "Bug"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
