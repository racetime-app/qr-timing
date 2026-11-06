package app.racetime.qrtiming.checkin;

/**
 * Where the athlete who scanned a QR code is: shown above the check-in form.
 *
 * @param eventName  name of the event
 * @param checkpoint the checkpoint the QR code belongs to
 */
public record CheckpointInfo(String eventName, CheckpointView checkpoint) {
}
