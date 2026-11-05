package app.racetime.qrtiming.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Response of {@code POST /api/v1/qr/{eventId}/{checkpointId}/{key}/checkIn}: the check-in that was recorded.
 *
 * <p>The API sets the time itself, at the moment it receives the request, so it can't be faked by a client.
 * If the athlete checks in twice at the same checkpoint, the API returns the time of the first check-in.
 *
 * @param eventName  name of the event
 * @param checkpoint the checkpoint where the athlete checked in
 * @param bib        the bib number that was checked in
 * @param recordedAt when the check-in was recorded
 * @param timeZone   IANA time-zone id of the event, such as "Europe/Warsaw"; may be {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckInResult(String eventName, Checkpoint checkpoint, String bib, Instant recordedAt, String timeZone) {

    /**
     * The recorded time in the event's time zone, which is the time the athlete sees on the race clock.
     * Falls back to UTC when the API sends no time zone, or one Java does not know.
     */
    public ZonedDateTime localRecordedAt() {
        return recordedAt.atZone(eventZone());
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
