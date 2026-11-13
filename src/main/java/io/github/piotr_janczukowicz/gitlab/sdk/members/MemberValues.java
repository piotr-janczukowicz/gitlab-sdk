package io.github.piotr_janczukowicz.gitlab.sdk.members;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Conversions shared by the project and group member classes. */
final class MemberValues {

    private MemberValues() {
        throw new UnsupportedOperationException("Utility class");
    }

    static int accessLevelValue(final @Nullable Integer accessLevel) {
        return requireNonNull(accessLevel, "accessLevel");
    }

    static Optional<LocalDate> expiresAt(final @Nullable String expiresAt) {
        if (expiresAt == null || expiresAt.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(expiresAt.strip()));
        } catch (final DateTimeParseException e) {
            throw new IllegalStateException("GitLab returned an expiry that is not an ISO date: " + expiresAt, e);
        }
    }

    static Instant startOfDayUtc(final LocalDate date) {
        return requireNonNull(date, "date").atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
