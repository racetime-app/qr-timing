package app.racetime.qrtiming.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A checkpoint as the RaceTime API describes it.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} keeps this app working when the API adds new fields.
 * It is a good habit for any API client.
 *
 * @param name   the name the organiser gave the checkpoint, such as "Windmill turn"; may be {@code null} or empty
 * @param type   whether this is a checkpoint or the finish
 * @param number position of the checkpoint along the course, starting at 1; may be {@code null}.
 *               The pages show "Checkpoint 3" when the checkpoint has no name.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Checkpoint(String name, CheckpointType type, Integer number) {

    /** {@code true} for the finish line. Templates read this as {@code checkpoint.finish}. */
    public boolean isFinish() {
        return type == CheckpointType.FINISH;
    }
}
