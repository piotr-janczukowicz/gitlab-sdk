package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLink;
import io.github.piotr_janczukowicz.gitlab.api.projects.GitLabProjectGroupLinkBuilder;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

@UnitTest
class ProjectGroupLinkTest {

    @Test
    void expiresAt_shouldReturnDate_whenPresent() {
        final GitLabProjectGroupLink response = GitLabProjectGroupLinkBuilder.builder()
                .groupId(9L)
                .expiresAt(LocalDate.of(2027, 1, 2))
                .build();

        final ProjectGroupLink sut = new ProjectGroupLink(response);

        assertSoftly(softly -> {
            softly.assertThat(sut.groupId()).isEqualTo(9L);
            softly.assertThat(sut.expiresAt()).contains(LocalDate.of(2027, 1, 2));
            softly.assertThat(sut.response()).isSameAs(response);
        });
    }

    @Test
    void expiresAt_shouldBeEmpty_whenAbsent() {
        final ProjectGroupLink sut = new ProjectGroupLink(
                GitLabProjectGroupLinkBuilder.builder().groupId(9L).build());

        assertThat(sut.expiresAt()).isEmpty();
    }
}
