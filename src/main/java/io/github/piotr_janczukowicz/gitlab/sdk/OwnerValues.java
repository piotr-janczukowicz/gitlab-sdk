package io.github.piotr_janczukowicz.gitlab.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabCiRunnerWithUuid;
import io.github.piotr_janczukowicz.gitlab.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.gitlab.sdk.runners.Runner;
import java.util.Optional;
import java.util.stream.Stream;

/** Values and helpers shared by {@link Project} and {@link Group}. */
final class OwnerValues {

    /** The end of the message of a lookup that finds the object gone. */
    static final String IS_GONE = " is gone";

    /** The name of a {@code name} argument, for validation messages. */
    static final String NAME = "name";

    /** The name of a {@code filter} argument, for validation messages. */
    static final String FILTER = "filter";

    private OwnerValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Loads each listed runner by its global id ({@code GET /runners/{id}}) as the stream reaches it; a runner that is
     * gone by then is skipped.
     *
     * @param sdk the SDK that loads the runners
     * @param listed the listed runners
     * @return the loaded runners
     */
    static Stream<Runner> loadedRunners(final GitLabSdk sdk, final Stream<GitLabCiRunnerWithUuid> listed) {
        return listed.map(item -> Lookups.find(() -> sdk.runner(requireNonNull(item.id(), "id"))))
                .flatMap(Optional::stream);
    }
}
