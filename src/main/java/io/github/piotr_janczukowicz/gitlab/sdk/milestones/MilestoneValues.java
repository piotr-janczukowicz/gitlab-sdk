package io.github.piotr_janczukowicz.gitlab.sdk.milestones;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.gitlab.api.common.GitLabMilestone;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Conversions shared by the project and group milestone classes. */
final class MilestoneValues {

    private MilestoneValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    static Optional<LocalDate> date(final @Nullable String raw, final String name) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(raw.strip()));
        } catch (final DateTimeParseException e) {
            throw new IllegalStateException("GitLab returned a " + name + " that is not an ISO date: " + raw, e);
        }
    }

    static String isoDate(final @Nullable LocalDate date, final String name) {
        return requireNonNull(date, name).toString();
    }

    static MilestoneState state(final GitLabMilestone response) {
        return MilestoneState.from(stateName(response));
    }

    static String stateName(final GitLabMilestone response) {
        return requireNonNull(response.state(), "state");
    }

    static long id(final GitLabMilestone response) {
        return requireNonNull(response.id(), "id");
    }

    static long iid(final GitLabMilestone response) {
        return requireNonNull(response.iid(), "iid");
    }
}
