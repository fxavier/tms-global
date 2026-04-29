package pt.xavier.tms.shared.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    private DateUtils() {
    }

    public static LocalDate todayUtc() {
        return LocalDate.now(Clock.systemUTC());
    }

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MILLIS);
    }

    public static boolean isExpired(LocalDate date) {
        return date != null && date.isBefore(todayUtc());
    }

    public static boolean isWithinDays(LocalDate date, int days) {
        if (date == null || days < 0) {
            return false;
        }

        LocalDate today = todayUtc();
        return !date.isBefore(today) && !date.isAfter(today.plusDays(days));
    }

    public static LocalDateTime toUtcLocalDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
