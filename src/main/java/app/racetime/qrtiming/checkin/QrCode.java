package app.racetime.qrtiming.checkin;

import java.util.Objects;

/**
 * What a checkpoint QR code identifies.
 *
 * <p>The QR code holds a link to this app, {@code https://<host>/c/{eventId}/{checkpointId}}, built from the IDs
 * RaceTime uses for the event and the checkpoint. Nothing in it is secret: an athlete is identified by their bib
 * number and, if the organiser handed them out, their PIN.
 *
 * @param eventId      the RaceTime event
 * @param checkpointId the checkpoint, or the finish, within that event
 */
public record QrCode(String eventId, String checkpointId) {

    public QrCode {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(checkpointId, "checkpointId");
    }
}
