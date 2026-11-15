package io.github.piotr_janczukowicz.gitlab.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RepositoryRequestsTest {

    @Test
    void constructor_shouldThrowUnsupportedOperationException_whenRepositoryRequestsIsInstantiated()
            throws ReflectiveOperationException {
        final Constructor<RepositoryRequests> constructor = RepositoryRequests.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mergeBaseRefs_shouldReturnRefs_whenTwoOrMoreAreGiven() {
        final List<String> result = RepositoryRequests.mergeBaseRefs("a", "b", "c");

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void mergeBaseRefs_shouldRefuse_whenFewerThanTwoOrBlankOrNull() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> RepositoryRequests.mergeBaseRefs("a"))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> RepositoryRequests.mergeBaseRefs("a", " "))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> RepositoryRequests.mergeBaseRefs((String[]) null))
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void lineCount_shouldCountLinesLikeBlame_whenContentVaries() {
        assertSoftly(softly -> {
            softly.assertThat(RepositoryRequests.lineCount(new byte[0])).isZero();
            softly.assertThat(RepositoryRequests.lineCount("a".getBytes())).isEqualTo(1);
            softly.assertThat(RepositoryRequests.lineCount("a\n".getBytes())).isEqualTo(1);
            softly.assertThat(RepositoryRequests.lineCount("a\nb".getBytes())).isEqualTo(2);
            softly.assertThat(RepositoryRequests.lineCount("a\r\nb\r\n".getBytes()))
                    .isEqualTo(2);
            softly.assertThat(RepositoryRequests.lineCount("\n\n".getBytes())).isEqualTo(2);
        });
    }
}
