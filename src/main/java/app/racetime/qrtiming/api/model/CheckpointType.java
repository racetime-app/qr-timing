package app.racetime.qrtiming.api.model;

/** Whether a QR code belongs to an intermediate checkpoint or to the finish line. */
public enum CheckpointType {

    /** A checkpoint along the course. */
    CHECKPOINT,

    /** The finish line. Checking in here records the athlete's finish time. */
    FINISH
}
