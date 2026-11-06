package app.racetime.qrtiming.checkin;

/**
 * A checkpoint as the pages show it.
 *
 * @param name   the name the organiser gave it; may be {@code null} or empty
 * @param finish whether it is the finish line
 * @param number for checkpoints along the course: its position, starting at 1. The pages show "Checkpoint 2"
 *               when it has no name. {@code null} for the finish.
 */
public record CheckpointView(String name, boolean finish, Integer number) {
}
