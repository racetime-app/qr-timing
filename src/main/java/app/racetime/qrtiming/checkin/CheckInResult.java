package app.racetime.qrtiming.checkin;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A check-in that went through, shown on the confirmation page.
 *
 * @param eventName  name of the event
 * @param checkpoint where the athlete checked in
 * @param bib        the bib number that was checked in
 * @param recordedAt when the check-in was recorded; {@code null} for a repeated check-in whose first time is
 *                   unknown
 * @param timeZone   IANA time-zone id of the event; may be {@code null}
 * @param repeated   {@code true} if the athlete had already checked in here earlier, so nothing new was recorded
 */
public record CheckInResult(String eventName, CheckpointView checkpoint, String bib, Instant recordedAt,
                            String timeZone, boolean repeated) {

    /**
     * The recorded time in the event's time zone, which is the time the athlete sees on the race clock.
     * Falls back to UTC when the event has no time zone, or one Java does not know.
     */
    public Optional<ZonedDateTime> localRecordedAt() {
        return Optional.ofNullable(recordedAt).map(instant -> instant.atZone(eventZone()));
    }

    private ZoneId eventZone() {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException unknownZone) {
            return ZoneOffset.UTC;
        }
    }
}
