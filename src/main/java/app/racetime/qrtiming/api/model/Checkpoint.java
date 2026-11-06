package app.racetime.qrtiming.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A checkpoint of an event, as {@code GET /api/v1/events/{eventId}/checkpoints} returns it. The API lists the
 * checkpoints in course order: the start, the checkpoints along the way, the finish.
 *
 * @param id    the checkpoint ID, which is also the last part of the QR code's address
 * @param type  {@code "start"}, {@code "checkpoint"} or {@code "finish"}
 * @param name  the name the organiser gave it, such as "Windmill turn"; may be {@code null} or empty
 * @param isLap {@code true} on the finish of a lap race, where the finish line is crossed once per lap
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Checkpoint(String id, String type, String name, Boolean isLap) {

    public static final String TYPE_START = "start";
    public static final String TYPE_CHECKPOINT = "checkpoint";
    public static final String TYPE_FINISH = "finish";

    public boolean isStart() {
        return TYPE_START.equals(type);
    }

    public boolean isFinish() {
        return TYPE_FINISH.equals(type);
    }

    /** A checkpoint along the course, between the start and the finish. */
    public boolean isIntermediate() {
        return TYPE_CHECKPOINT.equals(type);
    }
}
