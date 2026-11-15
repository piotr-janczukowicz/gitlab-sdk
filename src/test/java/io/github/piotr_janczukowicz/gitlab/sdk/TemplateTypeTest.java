package io.github.piotr_janczukowicz.gitlab.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.gitlab.api.projecttemplates.GitLabProjectProjectTemplatesGetType;
import io.github.piotr_janczukowicz.gitlab.api.projecttemplates.GitLabProjectProjectTemplatesListType;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

@UnitTest
class TemplateTypeTest {

    @Test
    void toListType_shouldReturnMatchingCoreConstant_whenEveryConstantIsMapped() {
        for (final TemplateType each : TemplateType.values()) {
            assertThat(each.toListType().wireValue()).isEqualTo(each.value());
        }
    }

    @Test
    void toGetType_shouldReturnMatchingCoreConstant_whenEveryConstantIsMapped() {
        for (final TemplateType each : TemplateType.values()) {
            assertThat(each.toGetType().wireValue()).isEqualTo(each.value());
        }
    }

    @Test
    void values_shouldCoverEveryCoreListType_whenCoreEnumIsListed() {
        final String[] core = Arrays.stream(GitLabProjectProjectTemplatesListType.values())
                .map(GitLabProjectProjectTemplatesListType::wireValue)
                .toArray(String[]::new);

        final String[] sdk =
                Arrays.stream(TemplateType.values()).map(TemplateType::value).toArray(String[]::new);

        assertThat(sdk).containsExactlyInAnyOrder(core);
    }

    @Test
    void values_shouldCoverEveryCoreGetType_whenCoreEnumIsListed() {
        final String[] core = Arrays.stream(GitLabProjectProjectTemplatesGetType.values())
                .map(GitLabProjectProjectTemplatesGetType::wireValue)
                .toArray(String[]::new);

        final String[] sdk =
                Arrays.stream(TemplateType.values()).map(TemplateType::value).toArray(String[]::new);

        assertThat(sdk).containsExactlyInAnyOrder(core);
    }
}
