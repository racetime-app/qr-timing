package app.racetime.qrtiming.api.model;

import java.util.Objects;

/**
 * What a checkpoint QR code identifies.
 *
 * <p>The QR code holds a link to this app, {@code https://<host>/c/{eventId}/{checkpointId}/{key}}, and the three
 * path segments are kept here. The {@code key} is a secret: it proves the athlete scanned the real code at the
 * checkpoint, and it is all the RaceTime API needs to accept a check-in. Never write it to logs.
 *
 * @param eventId      the RaceTime event
 * @param checkpointId the checkpoint (or the finish) within that event
 * @param key          the secret that belongs to the current QR code of that checkpoint
 */
public record QrCode(String eventId, String checkpointId, String key) {

    public QrCode {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(key, "key");
    }

    /** Leaves the secret key out, so a {@code QrCode} can be logged safely. */
    @Override
    public String toString() {
        return "QrCode[eventId=" + eventId + ", checkpointId=" + checkpointId + "]";
    }
}
