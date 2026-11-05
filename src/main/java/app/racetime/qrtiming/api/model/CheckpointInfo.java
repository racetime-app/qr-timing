package app.racetime.qrtiming.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of {@code GET /api/v1/qr/{eventId}/{checkpointId}/{key}}: where the athlete who scanned the code is.
 *
 * @param eventName  name of the event, shown above the form
 * @param checkpoint the checkpoint the QR code belongs to
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckpointInfo(String eventName, Checkpoint checkpoint) {
}
